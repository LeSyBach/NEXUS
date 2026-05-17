package com.example.nexus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Tạo notification channel ngay khi app khởi động.
        // PHẢI làm ở đây (Application), KHÔNG phải trong Service,
        // để channel sẵn sàng ngay cả khi app đang bị killed.
        createNotificationChannels()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("NexusApp", "FCM Token: $token")
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Âm thanh thông báo mặc định hệ thống
            val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notifAudioAttr = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // ── Channel tin nhắn (giống Messenger) ──────────────────
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Tin nhắn",
                NotificationManager.IMPORTANCE_HIGH   // HIGH = âm thanh + hiện banner
            ).apply {
                description = "Thông báo khi có tin nhắn mới"
                enableLights(true)
                lightColor = ContextCompat.getColor(this@NexusApplication, R.color.nexus_accent)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setSound(notifSound, notifAudioAttr)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Chỉ tạo mới nếu channel chưa tồn tại (không update channel cũ
            // vì user có thể đã tắt sound ở setting — Android không cho override)
            if (manager.getNotificationChannel(CHANNEL_MESSAGES) == null) {
                manager.createNotificationChannel(msgChannel)
            }
        }
    }

    companion object {
        const val CHANNEL_MESSAGES = "nexus_messages"
    }
}
