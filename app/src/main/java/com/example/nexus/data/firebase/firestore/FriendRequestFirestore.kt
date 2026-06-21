package com.example.nexus.data.firebase.firestore

import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.FriendRequest
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore operations for the `friend_requests` collection and friend/block actions.
 */
@Singleton
class FriendRequestFirestore @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userFirestore: UserFirestore,
    private val chatFirestore: ChatFirestore
) {
    suspend fun sendFriendRequest(request: FriendRequest): String {
        val docRef = firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .add(request)
            .await()
        return docRef.id
    }

    fun observeReceivedFriendRequests(userId: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", Constants.FRIEND_REQUEST_PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    fun observeSentFriendRequests(userId: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .whereEqualTo("fromUserId", userId)
            .whereEqualTo("status", Constants.FRIEND_REQUEST_PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    fun observeFriendRequestExists(fromUser: String, toUser: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .whereEqualTo("fromUserId", fromUser)
            .whereEqualTo("toUserId", toUser)
            .whereEqualTo("status", Constants.FRIEND_REQUEST_PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend((snapshot?.size() ?: 0) > 0)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSentRequestTargetIds(userId: String): Set<String> {
        return firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .whereEqualTo("fromUserId", userId)
            .whereEqualTo("status", Constants.FRIEND_REQUEST_PENDING)
            .get()
            .await()
            .toObjects(FriendRequest::class.java)
            .map { it.toUserId }
            .toSet()
    }

    suspend fun acceptFriendRequest(requestId: String, fromUserId: String, toUserId: String) {
        firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .document(requestId)
            .update("status", Constants.FRIEND_REQUEST_ACCEPTED)
            .await()

        firestore.collection(Constants.COLLECTION_USERS)
            .document(fromUserId)
            .update("friends", FieldValue.arrayUnion(toUserId))
            .await()

        firestore.collection(Constants.COLLECTION_USERS)
            .document(toUserId)
            .update("friends", FieldValue.arrayUnion(fromUserId))
            .await()

        val existing = chatFirestore.findDirectChat(fromUserId, toUserId)
        if (existing == null) {
            val fromUser = userFirestore.getUser(fromUserId)
            val toUser = userFirestore.getUser(toUserId)
            val chat = Chat(
                type = Constants.CHAT_TYPE_DIRECT,
                participants = listOf(fromUserId, toUserId),
                groupName = "${fromUser?.username ?: "User"} & ${toUser?.username ?: "User"}",
                updatedAt = Timestamp.now(),
                createdAt = Timestamp.now()
            )
            chatFirestore.createChat(chat)
        }
    }

    suspend fun rejectFriendRequest(requestId: String) {
        firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .document(requestId)
            .update("status", Constants.FRIEND_REQUEST_REJECTED)
            .await()
    }

    suspend fun checkExistingFriendRequest(fromUserId: String, toUserId: String): FriendRequest? {
        val requests = firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .whereEqualTo("fromUserId", fromUserId)
            .whereEqualTo("toUserId", toUserId)
            .whereEqualTo("status", Constants.FRIEND_REQUEST_PENDING)
            .get()
            .await()
            .toObjects(FriendRequest::class.java)
        return requests.firstOrNull()
    }

    suspend fun cancelFriendRequest(fromUserId: String, toUserId: String) {
        val request = checkExistingFriendRequest(fromUserId, toUserId) ?: return
        firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .document(request.id)
            .delete()
            .await()
    }
}
