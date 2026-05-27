package com.example.nexus.feature_profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.AccountManager
import com.example.nexus.core.utils.BiometricManager
import com.example.nexus.core.utils.Resource
import com.example.nexus.core.utils.SavedAccount
import com.example.nexus.core.utils.ThemeManager
import com.example.nexus.data.repository.AuthRepository
import com.example.nexus.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val useSystemTheme: Boolean = true,
    val isDarkMode: Boolean = false,
    val savedAccounts: List<SavedAccount> = emptyList(),
    val currentAccountEmail: String? = null,
    val isAppLockEnabled: Boolean = false,
    val biometricAvailable: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val accountManager: AccountManager,
    private val biometricManager: BiometricManager
) : ViewModel() {

    private val _savedAccounts = MutableStateFlow(accountManager.getSavedAccounts())
    private val _isAppLockEnabled = MutableStateFlow(false)
    private val _switchAccountState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val switchAccountState: StateFlow<Resource<Unit>> = _switchAccountState

    private val _deleteAccountState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val deleteAccountState: StateFlow<Resource<Unit>> = _deleteAccountState

    init {
        viewModelScope.launch {
            biometricManager.isAppLockEnabled.collect { enabled ->
                _isAppLockEnabled.value = enabled
            }
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        themeManager.useSystemThemeFlow,
        themeManager.isDarkModeFlow,
        _savedAccounts,
        _isAppLockEnabled
    ) { useSystem, isDark, accounts, appLock ->
        SettingsUiState(
            useSystemTheme = useSystem,
            isDarkMode = isDark ?: false,
            savedAccounts = accounts,
            currentAccountEmail = accountManager.getCurrentAccountEmail(),
            isAppLockEnabled = appLock,
            biometricAvailable = biometricManager.canUseBiometric()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateTheme(useSystemTheme: Boolean, isDarkMode: Boolean = false) {
        viewModelScope.launch {
            if (useSystemTheme) {
                themeManager.setUseSystemTheme(true)
            } else {
                themeManager.setDarkMode(isDarkMode)
            }
        }
    }

    fun switchAccount(email: String, password: String) {
        viewModelScope.launch {
            _switchAccountState.value = Resource.Loading
            try {
                authRepository.logout()
                authRepository.login(email, password)
                _savedAccounts.value = accountManager.getSavedAccounts()
                _switchAccountState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _switchAccountState.value = Resource.Error(e.message ?: "Chuyển tài khoản thất bại")
            }
        }
    }

    fun removeAccount(email: String) {
        accountManager.removeAccount(email)
        _savedAccounts.value = accountManager.getSavedAccounts()
    }

    fun refreshAccounts() {
        _savedAccounts.value = accountManager.getSavedAccounts()
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            biometricManager.setAppLockEnabled(enabled)
        }
    }

    fun requestDeleteAccount() {
        viewModelScope.launch {
            _deleteAccountState.value = Resource.Loading
            try {
                profileRepository.requestAccountDeletion()
                _deleteAccountState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _deleteAccountState.value = Resource.Error(e.message ?: "Xóa tài khoản thất bại")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun resetSwitchAccountState() {
        _switchAccountState.value = Resource.Idle
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = Resource.Idle
    }
}
