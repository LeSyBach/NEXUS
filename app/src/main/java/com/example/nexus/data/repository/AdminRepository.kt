package com.example.nexus.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.model.Feedback
import com.example.nexus.data.model.SystemNotification
import com.example.nexus.data.model.UserNotification
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val authService: AuthService,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Local persistence for read notification IDs (survives app restart)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nexus_read_notifs", Context.MODE_PRIVATE)

    // Current user ID for per-account SharedPreferences key
    private var currentUserId: String? = null

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

    /** SharedPreferences key per user: "read_ids_<userId>" */
    private fun readIdsKey(userId: String) = "read_ids_$userId"

    fun startObservingIfNeeded() {
        if (isObserving) return
        val userId = authService.currentUserId ?: return
        isObserving = true
        currentUserId = userId

        // Load locally persisted read IDs for THIS user (instant, survives restart)
        val localReadIds = prefs.getStringSet(readIdsKey(userId), emptySet()) ?: emptySet()
        _readNotificationIds.value = localReadIds

        // Observe user notifications for read status from Firestore
        scope.launch {
            firestoreService.observeUserNotifications(userId).collect { userNotifs ->
                // Deduplicate by notificationId: if ANY doc for a notification is read,
                // treat it as read. This handles legacy docs with auto-generated IDs.
                val deduplicated = userNotifs.groupBy { it.notificationId }
                    .mapValues { (_, docs) -> docs.any { it.isRead } }
                val firestoreReadIds = deduplicated.filter { it.value }.keys.toSet()

                // Merge: local + Firestore (never lose a read status)
                val mergedReadIds = localReadIds + firestoreReadIds
                _readNotificationIds.value = mergedReadIds
                _unreadNotificationCount.value = _systemNotifications.value.count { it.id !in mergedReadIds }

                // Persist merged result locally for THIS user
                prefs.edit().putStringSet(readIdsKey(userId), mergedReadIds).apply()
            }
        }

        // Observe system notifications
        scope.launch {
            firestoreService.observeSystemNotifications().collect { notifications ->
                _systemNotifications.value = notifications
                // Recalculate unread count when notifications list changes
                _unreadNotificationCount.value = notifications.count { it.id !in _readNotificationIds.value }
            }
        }
    }

    fun markAsReadLocally(notificationId: String) {
        val updated = _readNotificationIds.value + notificationId
        _readNotificationIds.value = updated
        _unreadNotificationCount.value = (_unreadNotificationCount.value - 1).coerceAtLeast(0)
        // Persist locally for THIS user
        currentUserId?.let { uid ->
            prefs.edit().putStringSet(readIdsKey(uid), updated).apply()
        }
    }

    fun revertRead(notificationId: String) {
        _readNotificationIds.value = _readNotificationIds.value - notificationId
        _unreadNotificationCount.value = _unreadNotificationCount.value + 1
    }

    /** Reset state when logging out or switching accounts */
    fun reset() {
        isObserving = false
        currentUserId = null
        _readNotificationIds.value = emptySet()
        _unreadNotificationCount.value = 0
        _systemNotifications.value = emptyList()
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
