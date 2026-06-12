package com.example.nexus.data.firebase

import com.example.nexus.core.utils.Constants
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class CallSignal(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverAvatar: String = "",
    val type: String = "voice",
    val status: String = "ringing",
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class CallSignalingService @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val callsRef = database.getReference("calls")

    suspend fun initiateCall(signal: CallSignal): String {
        val ref = callsRef.child(signal.callId)
        ref.setValue(signal).await()
        return signal.callId
    }

    suspend fun acceptCall(callId: String) {
        callsRef.child(callId).child("status").setValue("ongoing").await()
    }

    suspend fun rejectCall(callId: String) {
        callsRef.child(callId).child("status").setValue("rejected").await()
    }

    suspend fun endCall(callId: String) {
        callsRef.child(callId).child("status").setValue("ended").await()
    }

    fun observeIncomingCalls(userId: String): Flow<CallSignal?> = callbackFlow {
        val listener = callsRef.orderByChild("receiverId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val latestCall = snapshot.children
                        .mapNotNull { it.getValue(CallSignal::class.java) }
                        .filter { it.status == "ringing" }
                        .maxByOrNull { it.timestamp }
                    trySend(latestCall)
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(null)
                }
            })
        awaitClose { callsRef.removeEventListener(listener) }
    }

    fun observeCallStatus(callId: String): Flow<String> = callbackFlow {
        val listener = callsRef.child(callId).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java) ?: "ended"
                    trySend(status)
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend("ended")
                }
            })
        awaitClose { callsRef.child(callId).child("status").removeEventListener(listener) }
    }

    suspend fun removeCall(callId: String) {
        callsRef.child(callId).removeValue().await()
    }
}
