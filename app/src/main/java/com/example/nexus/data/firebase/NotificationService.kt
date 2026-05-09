package com.example.nexus.data.firebase

import android.util.Log
import com.example.nexus.core.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "NotificationService"
    }

    suspend fun sendMessageNotification(
        receiverId: String,
        senderName: String,
        messageText: String,
        chatId: String,
        senderId: String
    ) {
        try {
            val receiverDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(receiverId)
                .get()
                .await()

            val fcmToken = receiverDoc.getString("fcmToken")
            if (fcmToken.isNullOrEmpty()) {
                Log.w(TAG, "No FCM token for user $receiverId")
                return
            }

            val notification = hashMapOf(
                "type" to "message",
                "senderName" to senderName,
                "messageText" to messageText,
                "chatId" to chatId,
                "senderId" to senderId,
                "receiverId" to receiverId,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "read" to false
            )

            firestore.collection("notifications")
                .add(notification)
                .await()

            Log.d(TAG, "Notification stored for $receiverId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
        }
    }

    suspend fun sendCallNotification(
        receiverId: String,
        callerName: String,
        callId: String,
        callType: String
    ) {
        try {
            val notification = hashMapOf(
                "type" to "call",
                "callerName" to callerName,
                "callId" to callId,
                "callType" to callType,
                "receiverId" to receiverId,
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("notifications")
                .add(notification)
                .await()

            Log.d(TAG, "Call notification stored for $receiverId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send call notification", e)
        }
    }
}
