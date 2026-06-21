package com.example.nexus.data.firebase.firestore

import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.CallRecord
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore operations for the `calls` collection.
 */
@Singleton
class CallFirestore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createCall(call: CallRecord): String {
        val docRef = firestore.collection(Constants.COLLECTION_CALLS)
            .add(call)
            .await()
        return docRef.id
    }

    fun observeCallsForUser(userId: String): Flow<List<CallRecord>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CALLS)
            .whereArrayContains("receiverIds", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val calls = snapshot?.toObjects(CallRecord::class.java) ?: emptyList()
                trySend(calls)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateCallStatus(callId: String, status: String) {
        val updates = mutableMapOf<String, Any>("status" to status)
        if (status == Constants.CALL_STATUS_ENDED) {
            updates["endTime"] = Timestamp.now()
        }
        firestore.collection(Constants.COLLECTION_CALLS)
            .document(callId)
            .update(updates)
            .await()
    }
}
