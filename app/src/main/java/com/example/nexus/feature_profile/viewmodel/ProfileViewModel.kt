package com.example.nexus.feature_profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.model.User
import com.example.nexus.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    init {
        observeUser()
    }

    private fun observeUser() {
        viewModelScope.launch {
            profileRepository.observeCurrentUser()?.collect { user ->
                _user.value = user
            }
        }
    }

    fun updateProfile(displayName: String, phone: String, bio: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                profileRepository.updateProfile(displayName, phone, bio)
                _updateSuccess.value = true
            } catch (e: Exception) {
                _updateSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        profileRepository.logout()
    }
}
