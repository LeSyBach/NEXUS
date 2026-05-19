package com.example.nexus.data.firebase

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallActionReceiver"
        const val ACTION_ACCEPT = "com.example.nexus.ACTION_ACCEPT_CALL"
        const val ACTION_REJECT = "com.example.nexus.ACTION_REJECT_CALL"
        const val EXTRA_CALL_ID = "call_id"
        const val EXTRA_CALL_TYPE = "call_type"
        const val EXTRA_CALLER_NAME = "caller_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return
        val action = intent.action ?: return

        Log.d(TAG, "Call action: $action for callId=$callId")

        // Dismiss the notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(callId.hashCode())

        when (action) {
            ACTION_ACCEPT -> {
                // Update call status to "ongoing" so the caller's observer detects it
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        FirebaseDatabase.getInstance()
                            .getReference("calls")
                            .child(callId)
                            .child("status")
                            .setValue("ongoing")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to accept call", e)
                    }
                }

                // Launch the ongoing call screen
                val launchIntent = Intent(context, com.example.nexus.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("navigateTo", "ongoing_call")
                    putExtra("callId", callId)
                    putExtra("callType", intent.getStringExtra(EXTRA_CALL_TYPE) ?: "voice")
                    putExtra("callerName", intent.getStringExtra(EXTRA_CALLER_NAME) ?: "")
                }
                context.startActivity(launchIntent)
            }
            ACTION_REJECT -> {
                // Update call status to "rejected"
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        FirebaseDatabase.getInstance()
                            .getReference("calls")
                            .child(callId)
                            .child("status")
                            .setValue("rejected")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to reject call", e)
                    }
                }
            }
        }
    }
}
