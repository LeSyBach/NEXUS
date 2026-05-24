package com.example.nexus.feature_profile.viewmodel

import android.net.Uri
import android.util.Log
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

    private val _avatarUploadError = MutableStateFlow<String?>(null)
    val avatarUploadError: StateFlow<String?> = _avatarUploadError

    private var pendingAvatarUri: Uri? = null

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

    fun setPendingAvatar(uri: Uri) {
        Log.d("AvatarUpload", "setPendingAvatar URI: $uri")
        pendingAvatarUri = uri
    }

    fun clearPendingAvatar() {
        pendingAvatarUri = null
    }

    fun getPendingAvatarUri(): Uri? = pendingAvatarUri

    fun updateProfile(displayName: String, phone: String, bio: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _avatarUploadError.value = null
            try {
                var avatarUrl: String? = null
                pendingAvatarUri?.let { uri ->
                    Log.d("AvatarUpload", "Starting upload for URI: $uri")
                    avatarUrl = profileRepository.uploadAvatar(uri)
                    Log.d("AvatarUpload", "Upload success, URL: $avatarUrl")
                }
                Log.d("AvatarUpload", "Updating Firestore profile")
                profileRepository.updateProfile(displayName, phone, bio, avatarUrl)
                pendingAvatarUri = null
                _updateSuccess.value = true
                Log.d("AvatarUpload", "Profile update complete")
            } catch (e: Exception) {
                Log.e("AvatarUpload", "Upload failed", e)
                _avatarUploadError.value = e.message ?: "Tải ảnh đại diện thất bại"
                _updateSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }

    fun logout() {
        profileRepository.logout()
    }
}
