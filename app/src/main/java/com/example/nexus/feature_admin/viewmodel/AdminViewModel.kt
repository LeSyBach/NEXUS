package com.example.nexus.feature_admin.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.model.User
import com.example.nexus.data.repository.AdminRepository
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.firebase.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val firestoreService: FirestoreService,
    private val authService: AuthService
) : ViewModel() {

    companion object {
        private const val TAG = "AdminViewModel"
    }

    private val _feedbackState = MutableStateFlow<Result<Unit>?>(null)
    val feedbackState: StateFlow<Result<Unit>?> = _feedbackState

    // Use shared state from AdminRepository
    val systemNotifications = adminRepository.systemNotifications
    val readNotificationIds = adminRepository.readNotificationIds
    val unreadCount = adminRepository.unreadNotificationCount

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isBanned = MutableStateFlow(false)
    val isBanned: StateFlow<Boolean> = _isBanned

    private val _banReason = MutableStateFlow("")
    val banReason: StateFlow<String> = _banReason

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        val userId = authService.currentUserId ?: return
        viewModelScope.launch {
            firestoreService.observeUser(userId).collect { user ->
                _currentUser.value = user
                _isBanned.value = user?.isBanned == true
                _banReason.value = if (user?.isBanned == true) "Tài khoản đã bị quản trị viên khóa" else ""
            }
        }
    }

    fun submitFeedback(type: String, subject: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _feedbackState.value = null
            val result = adminRepository.submitFeedback(type, subject, content)
            _feedbackState.value = result
            Log.d(TAG, "Feedback submitted: ${result.isSuccess}")
        }
    }

    fun resetFeedbackState() {
        _feedbackState.value = null
    }

    fun markAsRead(notificationId: String) {
        val userId = authService.currentUserId ?: return
        // Update shared state immediately
        adminRepository.markAsReadLocally(notificationId)
        viewModelScope.launch {
            try {
                adminRepository.markNotificationRead(notificationId, userId)
            } catch (e: Exception) {
                Log.e(TAG, "markAsRead failed", e)
                // Revert on failure
                adminRepository.revertRead(notificationId)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                firestoreService.updateUser(authService.currentUserId ?: "", mapOf(
                    "fcmToken" to "",
                    "status" to "offline"
                ))
                authService.signOut()
            } catch (e: Exception) {
                Log.e(TAG, "logout failed", e)
            }
        }
    }
}
