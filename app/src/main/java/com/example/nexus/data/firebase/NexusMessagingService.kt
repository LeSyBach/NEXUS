package com.example.nexus.data.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.nexus.core.utils.Constants

class NexusMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("NexusMessagingService", "Refreshed token: $token")
        // TODO: Send this token to Firestore to link it with the current user
        // so that Cloud Functions can send notifications to this device.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("NexusMessagingService", "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("NexusMessagingService", "Message data payload: ${remoteMessage.data}")
            // Handle the data, you can build a local Notification here
            // using NotificationCompat.Builder if needed.
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("NexusMessagingService", "Message Notification Body: ${it.body}")
            // Firebase automatically displays notifications if the app is in background.
            // If the app is in foreground, you can show a custom Snackbar or local Notification.
        }
    }
}
