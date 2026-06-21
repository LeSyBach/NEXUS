package com.example.nexus.data.firebase

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import android.util.LruCache
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.tasks.await
import java.net.HttpURLConnection
import java.net.URL

class NexusMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "NexusMessaging"

        @Volatile
        var activeChatId: String? = null

        // Cache avatar bitmap (tối đa 20 avatar, ~10MB)
        private val avatarCache = object : LruCache<String, Bitmap>(20) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM received: ${remoteMessage.data}")

        val data = remoteMessage.data
        if (data.isEmpty()) return

        when (data["type"] ?: "message") {
            "message"        -> handleMessageNotification(data)
            "friend_request" -> handleFriendRequestNotification(data)
            "call"           -> handleCallNotification(data)
            "system_notification" -> handleSystemNotification(data)
            else             -> handleMessageNotification(data)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // CUỘC GỌI ĐẾN (Full-Screen Intent)
    // ════════════════════════════════════════════════════════════════

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handleCallNotification(data: Map<String, String>) {
        val callId = data["callId"] ?: return
        val callerName = data["callerName"] ?: "Ai đó"
        val callType = data["callType"] ?: "voice"
        val callerId = data["callerId"] ?: ""

        // Don't show notification for calls from self
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (callerId.isNotEmpty() && callerId == currentUserId) return

        Log.d(TAG, "Showing incoming call notification: callId=$callId, caller=$callerName")

        // Full-screen intent — launches IncomingCallScreen on lockscreen
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigateTo", "incoming_call")
            putExtra("callId", callId)
            putExtra("callType", callType)
            putExtra("callerName", callerName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, callId.hashCode(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Accept action
        val acceptIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_ACCEPT
            putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
            putExtra(CallActionReceiver.EXTRA_CALL_TYPE, callType)
            putExtra(CallActionReceiver.EXTRA_CALLER_NAME, callerName)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            this, ("accept_$callId").hashCode(), acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reject action
        val rejectIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_REJECT
            putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this, ("reject_$callId").hashCode(), rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callLabel = if (callType == "video") "Cuộc gọi video" else "Cuộc gọi thoại"
        val accentColor = ContextCompat.getColor(this, R.color.nexus_accent)

        val notification = NotificationCompat.Builder(this, NexusApplication.CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accentColor)
            .setContentTitle(callerName)
            .setContentText("$callLabel đến...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_notification, "Chấp nhận", acceptPendingIntent)
            .addAction(R.drawable.ic_notification, "Từ chối", rejectPendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setVibrate(longArrayOf(0, 500, 500, 500, 500, 500))
            .setTimeoutAfter(60_000L) // Auto-dismiss after 60s
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(callId.hashCode(), notification)
    }

    // ════════════════════════════════════════════════════════════════
    // TIN NHẮN MỚI (MessagingStyle - giống Messenger)
    // ════════════════════════════════════════════════════════════════

    private fun handleMessageNotification(data: Map<String, String>) {
        val senderName  = data["senderName"]  ?: "NEXUS"
        val messageText = data["messageText"] ?: "Bạn có tin nhắn mới"
        val chatId      = data["chatId"]      ?: ""
        val senderId    = data["senderId"]    ?: ""

        // Không hiển thị notification của chính mình
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (senderId.isNotEmpty() && senderId == currentUserId) return

        // Suppress nếu user đang xem conversation này
        if (chatId.isNotEmpty() && chatId == activeChatId) return

        // Intent mở đúng conversation
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "conversation")
            putExtra("chatId", chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, chatId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tạo MessagingStyle notification
        val accentColor = ContextCompat.getColor(this, R.color.nexus_accent)

        // Person của "bạn" (người dùng hiện tại)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val me = androidx.core.app.Person.Builder()
            .setName(currentUser?.displayName ?: "Bạn")
            .setKey(currentUser?.uid)
            .build()

        val style = NotificationCompat.MessagingStyle(me)
            .setConversationTitle(senderName)
            .addMessage(
                NotificationCompat.MessagingStyle.Message(
                    messageText,
                    System.currentTimeMillis(),
                    androidx.core.app.Person.Builder()
                        .setName(senderName)
                        .setKey(senderId)
                        .setIcon(loadAvatarIcon(senderId))
                        .build()
                )
            )

        val notification = NotificationCompat.Builder(this, NexusApplication.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accentColor)
            .setStyle(style)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            this, requestId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accentColor = ContextCompat.getColor(this, R.color.nexus_accent)

        val notification = NotificationCompat.Builder(this, NexusApplication.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accentColor)
            .setContentTitle("Lời mời kết bạn")
            .setContentText("$senderName đã gửi lời mời kết bạn cho bạn")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$senderName đã gửi lời mời kết bạn cho bạn"))
            .setLargeIcon(loadAvatarBitmap(senderId))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(("fr_$senderId").hashCode(), notification)
    }

    // ════════════════════════════════════════════════════════════════
    // THÔNG BÁO HỆ THỐNG
    // ════════════════════════════════════════════════════════════════

    private fun handleSystemNotification(data: Map<String, String>) {
        val title = data["title"] ?: "NEXUS"
        val content = data["content"] ?: "Bạn có thông báo mới"
        val notificationId = data["notificationId"] ?: ""

        // Store in user_notifications for read tracking
        // Use predictable document ID to prevent duplicates and avoid needing composite index
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && notificationId.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val docId = "${notificationId}_${userId}"
                    val docRef = FirebaseFirestore.getInstance()
                        .collection(Constants.COLLECTION_USER_NOTIFICATIONS)
                        .document(docId)
                    // Only create if not exists — don't overwrite existing read status
                    val snapshot = docRef.get().await()
                    if (!snapshot.exists()) {
                        docRef.set(mapOf(
                            "notificationId" to notificationId,
                            "userId" to userId,
                            "isRead" to false,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )).await()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create user notification", e)
                }
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "notifications")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accentColor = ContextCompat.getColor(this, R.color.nexus_accent)

        val notification = NotificationCompat.Builder(this, NexusApplication.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId.hashCode(), notification)
    }

    // ════════════════════════════════════════════════════════════════
    // AVATAR LOADING (cache + fallback)
    // ════════════════════════════════════════════════════════════════

    private fun loadAvatarIcon(userId: String): androidx.core.graphics.drawable.IconCompat? {
        val bitmap = loadAvatarBitmap(userId) ?: return null
        return androidx.core.graphics.drawable.IconCompat.createWithBitmap(bitmap)
    }

    private fun loadAvatarBitmap(userId: String): Bitmap? {
        // Check cache
        avatarCache.get(userId)?.let { return it }

        // Load avatar URL từ Firestore
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .result

            val avatarUrl = doc?.getString("avatarUrl")
            if (!avatarUrl.isNullOrEmpty()) {
                val bitmap = downloadAndCropCircle(avatarUrl)
                if (bitmap != null) {
                    avatarCache.put(userId, bitmap)
                    return bitmap
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load avatar for $userId", e)
        }

        // Fallback: dùng splash icon
        return null
    }

    private fun downloadAndCropCircle(urlStr: String): Bitmap? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()

            val input = conn.inputStream
            val original = BitmapFactory.decodeStream(input)
            input.close()
            conn.disconnect()

            original?.let { cropToCircle(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Avatar download failed: $urlStr", e)
            null
        }
    }

    private fun cropToCircle(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = Rect(
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            (bitmap.width + size) / 2,
            (bitmap.height + size) / 2
        )
        canvas.drawBitmap(bitmap, srcRect, rect, paint)

        return output
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
                    .addOnSuccessListener { Log.d(TAG, "FCM token saved") }
                    .addOnFailureListener { Log.e(TAG, "Failed to save FCM token", it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving FCM token", e)
            }
        }
    }
}
