package com.example.nexus.data.firebase.firestore

import android.util.Log
import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore operations for the `users` collection.
 */
@Singleton
class UserFirestore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createUser(user: User) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(user.uid)
            .set(user)
            .await()
    }

    suspend fun getUser(userId: String): User? {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            Log.w("UserFirestore", "getUser error for $userId", e)
            null
        }
    }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("UserFirestore", "observeUser error", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any>) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .update(updates + ("updatedAt" to Timestamp.now()))
            .await()
    }

    suspend fun updateUserStatus(userId: String, status: String) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to Timestamp.now()
        )
        if (status == Constants.USER_STATUS_OFFLINE) {
            updates["lastSeen"] = Timestamp.now()
        }
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .update(updates)
            .await()
    }

    suspend fun searchUsersByUsername(query: String): List<User> {
        return firestore.collection(Constants.COLLECTION_USERS)
            .orderBy("username")
            .startAt(query.lowercase())
            .endAt(query.lowercase() + "")
            .limit(20)
            .get()
            .await()
            .toObjects(User::class.java)
    }

    suspend fun searchUsersByPhone(phone: String): List<User> {
        return firestore.collection(Constants.COLLECTION_USERS)
            .whereEqualTo("phone", phone)
            .limit(10)
            .get()
            .await()
            .toObjects(User::class.java)
    }

    suspend fun deleteUser(userId: String) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .delete()
            .await()
    }

    suspend fun blockUser(currentUserId: String, targetUserId: String) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(targetUserId))
            .await()
    }

    suspend fun unblockUser(currentUserId: String, targetUserId: String) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayRemove(targetUserId))
            .await()
    }

    suspend fun removeFriend(currentUserId: String, targetUserId: String) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .update("friends", com.google.firebase.firestore.FieldValue.arrayRemove(targetUserId))
            .await()
        firestore.collection(Constants.COLLECTION_USERS)
            .document(targetUserId)
            .update("friends", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
            .await()
    }
}
