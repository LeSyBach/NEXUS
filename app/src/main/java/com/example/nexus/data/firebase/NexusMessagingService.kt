package com.example.nexus.data.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.nexus.MainActivity
import com.example.nexus.R
import com.example.nexus.core.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NexusMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_MESSAGES = "nexus_messages"
        const val CHANNEL_CALLS = "nexus_calls"
        private const val TAG = "NexusMessaging"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        val type = data["type"] ?: "message"

        when (type) {
            "call" -> handleCallNotification(data)
            "message" -> handleMessageNotification(data)
            else -> handleMessageNotification(data)
        }
    }

    private fun handleMessageNotification(data: Map<String, String>) {
        val senderName = data["senderName"] ?: "NEXUS"
        val messageText = data["messageText"] ?: "Bạn có tin nhắn mới"
        val chatId = data["chatId"] ?: ""
        val senderId = data["senderId"] ?: ""

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (senderId == currentUserId) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "conversation")
            putExtra("chatId", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, chatId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_nexus_splash)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ic_nexus_splash))
            .setColor(Color.parseColor("#00E5FF"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(chatId.hashCode(), notification)
    }

    private fun handleCallNotification(data: Map<String, String>) {
        val callerName = data["callerName"] ?: "Người lạ"
        val callId = data["callId"] ?: ""
        val callType = data["callType"] ?: "voice"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "incoming_call")
            putExtra("callId", callId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, callId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callTypeText = if (callType == "video") "Cuộc gọi video" else "Cuộc gọi thoại"

        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_nexus_splash)
            .setContentTitle(callerName)
            .setContentText(callTypeText)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ic_nexus_splash))
            .setColor(Color.parseColor("#00E5FF"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setOngoing(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(callId.hashCode(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Tin nhắn",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo tin nhắn mới"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(messageChannel)

            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Cuộc gọi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo cuộc gọi đến"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(callChannel)
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection(Constants.COLLECTION_USERS)
                    .document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener { Log.d(TAG, "FCM token saved") }
                    .addOnFailureListener { Log.e(TAG, "Failed to save token", it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving token", e)
            }
        }
    }
}
