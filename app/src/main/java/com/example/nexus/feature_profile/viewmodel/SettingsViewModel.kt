package com.example.nexus.feature_profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val useSystemTheme: Boolean = true,
    val isDarkMode: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        themeManager.useSystemThemeFlow,
        themeManager.isDarkModeFlow
    ) { useSystem, isDark ->
        SettingsUiState(
            useSystemTheme = useSystem,
            isDarkMode = isDark ?: false
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
}
