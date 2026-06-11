package com.example.nexus.core.utils

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks app foreground/background lifecycle and updates the current user's
 * online status + lastSeen on Firestore in real-time.
 *
 * - Foreground → status = "online"
 * - Background → status = "offline", lastSeen = now
 */
@Singleton
class PresenceManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : DefaultLifecycleObserver {

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App moved to foreground
        setOnline()
    }

    override fun onStop(owner: LifecycleOwner) {
        // App moved to background
        setOffline()
    }

    private fun setOnline() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection(Constants.COLLECTION_USERS)
            .document(uid)
            .update(
                mapOf(
                    "status" to Constants.USER_STATUS_ONLINE,
                    "updatedAt" to Timestamp.now()
                )
            )
            .addOnFailureListener { e ->
                Log.w("PresenceManager", "Failed to set online", e)
            }
    }

    private fun setOffline() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection(Constants.COLLECTION_USERS)
            .document(uid)
            .update(
                mapOf(
                    "status" to Constants.USER_STATUS_OFFLINE,
                    "lastSeen" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
            )
            .addOnFailureListener { e ->
                Log.w("PresenceManager", "Failed to set offline", e)
            }
    }
}
