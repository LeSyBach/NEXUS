package com.example.nexus.data.firebase

import android.util.Log
import com.example.nexus.core.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val fcmDirectHelper: FcmDirectHelper
) {
    companion object {
        private const val TAG = "NotificationService"
    }

    /**
     * Gửi FCM trực tiếp đến người nhận.
     * Đọc FCM token từ Firestore → gửi qua FcmDirectHelper (không cần Cloud Function).
     */
    private suspend fun sendFcmToDevice(
        receiverId: String,
        data: Map<String, String>
    ): Boolean {
        val receiverDoc = firestore.collection(Constants.COLLECTION_USERS)
            .document(receiverId)
            .get()
            .await()

        val token = receiverDoc.getString("fcmToken")
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "No FCM token for user $receiverId")
            return false
        }

        return fcmDirectHelper.sendToDevice(token, data)
    }

    /**
     * Gửi thông báo tin nhắn mới tới người nhận.
     */
    suspend fun sendMessageNotification(
        receiverId: String,
        senderName: String,
        messageText: String,
        chatId: String,
        senderId: String
    ) {
        try {
            val data = mapOf(
                "type" to "message",
                "senderName" to senderName,
                "messageText" to messageText,
                "chatId" to chatId,
                "senderId" to senderId,
                "receiverId" to receiverId
            )

            val success = sendFcmToDevice(receiverId, data)
            if (success) {
                Log.d(TAG, "Message notification sent to $receiverId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message notification", e)
        }
    }

    /**
     * Gửi thông báo cuộc gọi đến tới người nhận.
     */
    suspend fun sendCallNotification(
        receiverId: String,
        callerName: String,
        callId: String,
        callType: String,
        callerId: String = ""
    ) {
        try {
            val data = mapOf(
                "type" to "call",
                "callerName" to callerName,
                "callId" to callId,
                "callType" to callType,
                "receiverId" to receiverId,
                "callerId" to callerId
            )

            val success = sendFcmToDevice(receiverId, data)
            if (success) {
                Log.d(TAG, "Call notification sent to $receiverId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send call notification", e)
        }
    }

    /**
     * Gửi thông báo lời mời kết bạn tới người nhận.
     */
    suspend fun sendFriendRequestNotification(
        receiverId: String,
        senderName: String,
        senderId: String,
        requestId: String
    ) {
        try {
            val data = mapOf(
                "type" to "friend_request",
                "senderName" to senderName,
                "senderId" to senderId,
                "requestId" to requestId,
                "receiverId" to receiverId
            )

            val success = sendFcmToDevice(receiverId, data)
            if (success) {
                Log.d(TAG, "Friend request notification sent to $receiverId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send friend request notification", e)
        }
    }
}
