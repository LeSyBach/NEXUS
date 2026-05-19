package com.example.nexus.data.firebase

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.nexus.MainActivity
import com.example.nexus.NexusApplication
import com.example.nexus.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallService : Service() {

    companion object {
        private const val TAG = "CallService"
        private const val NOTIFICATION_ID = 9999
        private const val WAKELOCK_TAG = "Nexus:CallWakeLock"
        private const val WAKELOCK_TIMEOUT = 60 * 60 * 1000L // 1 hour max

        const val ACTION_START = "com.example.nexus.ACTION_START_CALL"
        const val ACTION_STOP = "com.example.nexus.ACTION_STOP_CALL"
        const val EXTRA_CALL_ID = "call_id"
        const val EXTRA_CALL_TYPE = "call_type"
        const val EXTRA_PARTICIPANT_NAME = "participant_name"

        fun startService(context: Context, callId: String, callType: String, participantName: String) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_CALL_TYPE, callType)
                putExtra(EXTRA_PARTICIPANT_NAME, participantName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
                val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: "voice"
                val participantName = intent.getStringExtra(EXTRA_PARTICIPANT_NAME) ?: ""

                Log.d(TAG, "Starting call service: callId=$callId, type=$callType")

                val notification = buildNotification(callId, callType, participantName)

                val fgType = if (callType == "video") {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }

                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    fgType
                )

                acquireWakeLock()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping call service")
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        Log.d(TAG, "CallService destroyed")
    }

    private fun buildNotification(callId: String, callType: String, participantName: String): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateTo", "ongoing_call")
            putExtra("callId", callId)
            putExtra("callType", callType)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, callId.hashCode(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // End call action
        val endCallIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_REJECT
            putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
        }
        val endCallPendingIntent = PendingIntent.getBroadcast(
            this, ("end_$callId").hashCode(), endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callLabel = if (callType == "video") "Cuộc gọi video" else "Cuộc gọi thoại"

        return NotificationCompat.Builder(this, NexusApplication.CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(participantName.ifEmpty { callLabel })
            .setContentText("Đang trong cuộc gọi...")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_notification, "Kết thúc", endCallPendingIntent)
            .setSilent(true)
            .build()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKELOCK_TAG
            ).apply {
                acquire(WAKELOCK_TIMEOUT)
            }
            Log.d(TAG, "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }
}
