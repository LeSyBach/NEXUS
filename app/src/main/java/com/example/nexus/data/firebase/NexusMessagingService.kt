package com.example.nexus.data.firebase

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.nexus.MainActivity
import com.example.nexus.NexusApplication
import com.example.nexus.R
import com.example.nexus.core.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FCM Service xử lý tất cả push notification tin nhắn của NEXUS.
 *
 * Tại sao dùng data-only message (không có notification block):
 * - Khi FCM message có notification block, hệ thống tự hiển thị notification khi app ở background/killed
 *   nhưng KHÔNG gọi onMessageReceived → mất kiểm soát âm thanh, icon, deep link.
 * - Với data-only message, FCM LUÔN gọi onMessageReceived dù app đang ở trạng thái nào,
 *   cho phép ta tùy chỉnh hoàn toàn giống Messenger.
 */
class NexusMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "NexusMessaging"
        private const val SUMMARY_ID = 0

        /**
         * Conversation đang được user mở.
         * Set bởi ConversationScreen khi vào/rời, dùng để suppress notification
         * của đúng chat đang xem (giống Messenger).
         */
        @Volatile
        var activeChatId: String? = null
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: $token")
        saveTokenToFirestore(token)
    }

    /**
     * Nhận tất cả FCM message (foreground + background + killed).
     * Cloud Function gửi data-only message nên hàm này LUÔN được gọi.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM received from: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.w(TAG, "Empty data payload, skipping")
            return
        }

        when (data["type"] ?: "message") {
            "message"        -> handleMessageNotification(data)
            "friend_request" -> handleFriendRequestNotification(data)
            // Bỏ qua call — xử lý riêng sau
            "call"           -> Log.d(TAG, "Call notification ignored (handled separately)")
            else             -> handleMessageNotification(data)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // TIN NHẮN MỚI
    // ════════════════════════════════════════════════════════════════

    private fun handleMessageNotification(data: Map<String, String>) {
        val senderName  = data["senderName"]  ?: "NEXUS"
        val messageText = data["messageText"] ?: "Bạn có tin nhắn mới"
        val chatId      = data["chatId"]      ?: ""
        val senderId    = data["senderId"]    ?: ""

        // Không hiển thị notification của chính mình
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (senderId.isNotEmpty() && senderId == currentUserId) {
            Log.d(TAG, "Skipping own message notification")
            return
        }

        // Suppress notification nếu user đang xem đúng conversation này
        if (chatId.isNotEmpty() && chatId == activeChatId) {
            Log.d(TAG, "User is viewing chat $chatId, suppressing notification")
            return
        }

        // Intent mở thẳng cuộc trò chuyện khi bấm vào notification
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "conversation")
            putExtra("chatId", chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = buildMessageNotification(
            title         = senderName,
            body          = messageText,
            pendingIntent = pendingIntent
        )

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Mỗi chatId có 1 notification riêng — nhiều chat = nhiều notification,
        // bấm vào đúng notification mở đúng cuộc trò chuyện
        manager.notify(chatId.hashCode(), notification)
    }

    // ════════════════════════════════════════════════════════════════
    // LỜI MỜI KẾT BẠN
    // ════════════════════════════════════════════════════════════════

    private fun handleFriendRequestNotification(data: Map<String, String>) {
        val senderName = data["senderName"] ?: "Ai đó"
        val senderId   = data["senderId"]   ?: ""
        val requestId  = data["requestId"]  ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "friend_requests")
            putExtra("senderId", senderId)
            putExtra("requestId", requestId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = buildMessageNotification(
            title         = "Lời mời kết bạn",
            body          = "$senderName đã gửi lời mời kết bạn cho bạn",
            pendingIntent = pendingIntent
        )

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Group theo sender để không spam nhiều notification từ cùng 1 người
        manager.notify(("fr_$senderId").hashCode(), notification)
    }

    // ════════════════════════════════════════════════════════════════
    // BUILDER DÙNG CHUNG
    // ════════════════════════════════════════════════════════════════

    private fun buildMessageNotification(
        title: String,
        body: String,
        pendingIntent: PendingIntent
    ): android.app.Notification {
        // Âm thanh thông báo mặc định hệ thống (giống Messenger khi dùng âm thanh mặc định)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, NexusApplication.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_nexus_splash)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_nexus_splash))
            .setColor(Color.parseColor("#00E5FF"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)       // Hiện banner + âm thanh khi app foreground
            .setDefaults(NotificationCompat.DEFAULT_ALL)         // Âm thanh + rung + đèn theo mặc định hệ thống
            .setSound(soundUri)                                  // Đảm bảo có âm thanh (fallback cho Android < O)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Hiện nội dung trên màn hình khóa

        return builder.build()
    }

    // ════════════════════════════════════════════════════════════════
    // TOKEN MANAGEMENT
    // ════════════════════════════════════════════════════════════════

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection(Constants.COLLECTION_USERS)
                    .document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener { Log.d(TAG, "FCM token saved successfully") }
                    .addOnFailureListener { Log.e(TAG, "Failed to save FCM token", it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving FCM token", e)
            }
        }
    }
}
