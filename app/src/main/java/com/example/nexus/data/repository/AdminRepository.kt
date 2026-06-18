package com.example.nexus.data.repository

import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.model.Feedback
import com.example.nexus.data.model.SystemNotification
import com.example.nexus.data.model.UserNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val authService: AuthService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Shared unread count
    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount

    // Shared read notification IDs
    private val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds

    // System notifications
    private val _systemNotifications = MutableStateFlow<List<SystemNotification>>(emptyList())
    val systemNotifications: StateFlow<List<SystemNotification>> = _systemNotifications

    private var isObserving = false

    fun startObservingIfNeeded() {
        if (isObserving) return
        val userId = authService.currentUserId ?: return
        isObserving = true

        // Observe user notifications for read status
        scope.launch {
            firestoreService.observeUserNotifications(userId).collect { userNotifs ->
                val readIds = userNotifs.filter { it.isRead }.map { it.notificationId }.toSet()
                _readNotificationIds.value = readIds
                _unreadNotificationCount.value = userNotifs.count { !it.isRead }
            }
        }

        // Observe system notifications
        scope.launch {
            firestoreService.observeSystemNotifications().collect { notifications ->
                _systemNotifications.value = notifications
            }
        }
    }

    fun markAsReadLocally(notificationId: String) {
        _readNotificationIds.value = _readNotificationIds.value + notificationId
        _unreadNotificationCount.value = (_unreadNotificationCount.value - 1).coerceAtLeast(0)
    }

    fun revertRead(notificationId: String) {
        _readNotificationIds.value = _readNotificationIds.value - notificationId
        _unreadNotificationCount.value = _unreadNotificationCount.value + 1
    }

    suspend fun submitFeedback(type: String, subject: String, content: String): Result<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Result.failure(Exception("Not logged in"))
            val user = firestoreService.getUser(userId)
            val feedback = Feedback(
                userId = userId,
                userName = user?.displayName?.ifEmpty { user.username } ?: "Unknown",
                userEmail = user?.email ?: "",
                subject = subject,
                content = content,
                type = type
            )
            firestoreService.submitFeedback(feedback)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSystemNotifications(): Flow<List<SystemNotification>> {
        return firestoreService.observeSystemNotifications()
    }

    fun observeUserNotifications(userId: String): Flow<List<UserNotification>> {
        return firestoreService.observeUserNotifications(userId)
    }

    suspend fun markNotificationRead(notificationId: String, userId: String) {
        firestoreService.markNotificationRead(notificationId, userId)
    }
}
