package com.example.nexus.data.firebase

import com.example.nexus.data.webrtc.IceCandidateData
import com.example.nexus.data.webrtc.SessionDescriptionData
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcSignalingService @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val rootRef = database.getReference("webrtc")

    suspend fun sendOffer(callId: String, data: SessionDescriptionData) {
        rootRef.child(callId).child("offer").setValue(data).await()
    }

    suspend fun sendAnswer(callId: String, data: SessionDescriptionData) {
        rootRef.child(callId).child("answer").setValue(data).await()
    }

    suspend fun sendIceCandidate(callId: String, data: IceCandidateData) {
        rootRef.child(callId).child("candidates").push().setValue(data).await()
    }

    fun observeOffer(callId: String): Flow<SessionDescriptionData?> = callbackFlow {
        val ref = rootRef.child(callId).child("offer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(SessionDescriptionData::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeAnswer(callId: String): Flow<SessionDescriptionData?> = callbackFlow {
        val ref = rootRef.child(callId).child("answer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(SessionDescriptionData::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeRemoteIceCandidates(callId: String, localUserId: String): Flow<IceCandidateData> = callbackFlow {
        val ref = rootRef.child(callId).child("candidates")
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val data = snapshot.getValue(IceCandidateData::class.java) ?: return
                if (data.senderId != localUserId) {
                    trySend(data)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onChildRemoved(snapshot: DataSnapshot) = Unit
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit

            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun clearSession(callId: String) {
        rootRef.child(callId).removeValue().await()
    }
}

