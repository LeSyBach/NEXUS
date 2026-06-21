package com.example.nexus.data.firebase.firestore

import android.util.Log
import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.Feedback
import com.example.nexus.data.model.SystemNotification
import com.example.nexus.data.model.UserNotification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore operations for notifications, feedback, and system collections.
 */
@Singleton
class NotificationFirestore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun submitFeedback(feedback: Feedback) {
        firestore.collection(Constants.COLLECTION_FEEDBACK)
            .add(feedback)
            .await()
    }

    fun observeSystemNotifications(): Flow<List<SystemNotification>> {
        return callbackFlow {
            val listener = firestore.collection(Constants.COLLECTION_SYSTEM_NOTIFICATIONS)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("NotificationFirestore", "observeSystemNotifications error", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val notifications = snapshot?.toObjects(SystemNotification::class.java) ?: emptyList()
                    trySend(notifications)
                }
            awaitClose { listener.remove() }
        }
    }

    fun observeUserNotifications(userId: String): Flow<List<UserNotification>> {
        return callbackFlow {
            val listener = firestore.collection(Constants.COLLECTION_USER_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val notifications = snapshot?.toObjects(UserNotification::class.java) ?: emptyList()
                    trySend(notifications)
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun markNotificationRead(notificationId: String, userId: String) {
        val docId = "${notificationId}_${userId}"
        val docRef = firestore.collection(Constants.COLLECTION_USER_NOTIFICATIONS).document(docId)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            docRef.update("isRead", true).await()
        } else {
            docRef.set(mapOf(
                "notificationId" to notificationId,
                "userId" to userId,
                "isRead" to true,
                "createdAt" to Timestamp.now()
            )).await()
        }
    }

    suspend fun createUserNotification(notificationId: String, userId: String) {
        val docId = "${notificationId}_${userId}"
        val docRef = firestore.collection(Constants.COLLECTION_USER_NOTIFICATIONS).document(docId)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val userNotif = UserNotification(
                notificationId = notificationId,
                userId = userId,
                isRead = false
            )
            docRef.set(userNotif).await()
        }
    }
}
