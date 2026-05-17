package com.example.nexus.data.firebase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gửi FCM trực tiếp qua HTTP v1 API (bypass Cloud Functions).
 */
@Singleton
class FcmDirectHelper @Inject constructor(
    private val fcmV1Sender: FcmV1Sender
) {
    companion object {
        private const val TAG = "FcmDirectHelper"
    }

    suspend fun sendToDevice(
        token: String,
        data: Map<String, String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val success = fcmV1Sender.send(token, data)
            if (success) Log.d(TAG, "FCM sent OK") else Log.e(TAG, "FCM send failed")
            success
        } catch (e: Exception) {
            Log.e(TAG, "FCM send error", e)
            false
        }
    }
}
