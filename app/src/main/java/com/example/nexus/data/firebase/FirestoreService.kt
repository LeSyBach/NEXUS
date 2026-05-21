package com.example.nexus.data.firebase

import android.R.id.message
import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Firestore operations for all NEXUS collections.
 */
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // ══════════════════════════════════════════════════════════════
    // USER OPERATIONS
    // ══════════════════════════════════════════════════════════════

    suspend fun createUser(user: User) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(user.uid)
            .set(user)
            .await()
    }

    suspend fun getUser(userId: String): User? {
        return firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .await()
            .toObject(User::class.java)
    }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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
            .endAt(query.lowercase() + "\uf8ff")
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

    // ══════════════════════════════════════════════════════════════
    // CHAT OPERATIONS
    // ══════════════════════════════════════════════════════════════

    suspend fun createChat(chat: Chat): String {
        val docRef = firestore.collection(Constants.COLLECTION_CHATS)
            .add(chat)
            .await()
        return docRef.id
    }

    suspend fun getChat(chatId: String): Chat? {
        return firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .get()
            .await()
            .toObject(Chat::class.java)
    }

    fun observeChatsForUser(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CHATS)
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(Chat::class.java)
                    ?.sortedByDescending { it.updatedAt } ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    suspend fun findDirectChat(userId1: String, userId2: String): Chat? {
        val chats = firestore.collection(Constants.COLLECTION_CHATS)
            .whereEqualTo("type", Constants.CHAT_TYPE_DIRECT)
            .whereArrayContains("participants", userId1)
            .get()
            .await()
            .toObjects(Chat::class.java)

        return chats.find { it.participants.contains(userId2) }
    }

    suspend fun updateChat(chatId: String, updates: Map<String, Any>) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(updates + ("updatedAt" to Timestamp.now()))
            .await()
    }

    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        val fieldValue = if (isTyping) {
            FieldValue.arrayUnion(userId)
        } else {
            FieldValue.arrayRemove(userId)
        }
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update("typingUsers", fieldValue)
            .await()
    }

    // ══════════════════════════════════════════════════════════════
    // MESSAGE OPERATIONS
    // ══════════════════════════════════════════════════════════════

    suspend fun sendMessage(chatId: String, message: Message): String {
        val docRef = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .add(message)
            .await()

        // Update the message with its document ID
        docRef.update("id", docRef.id).await()

        // Build unreadCount: increment for each participant except sender
        val chat = getChat(chatId)
        val unreadMap = mutableMapOf<String, Long>()
        chat?.participants?.forEach { pid ->
            if (pid != message.senderId) {
                val current = chat.lastMessage?.unreadCount?.get(pid) ?: 0L
                unreadMap[pid] = current + 1L
            }
        }
        unreadMap[message.senderId] = 0L

        val lastMessage = LastMessage(
            text = when (message.type) {
                Constants.MESSAGE_TYPE_IMAGE -> "📷 Hình ảnh"
                Constants.MESSAGE_TYPE_FILE -> "📎 ${message.fileName}"
                Constants.MESSAGE_TYPE_VOICE -> "🎤 Tin nhắn thoại"
                Constants.MESSAGE_TYPE_LOCATION -> "📍 Vị trí"
                Constants.MESSAGE_TYPE_CALL -> if (message.text == "video") "📹 Cuộc gọi video" else "📞 Cuộc gọi thoại"
                else -> message.text
            },
            senderId = message.senderId,
            senderName = message.senderName,
            type = message.type,
            timestamp = Timestamp.now(),
            unreadCount = unreadMap
        )
        updateChat(chatId, mapOf("lastMessage" to lastMessage))

        return docRef.id
    }

    fun observeMessages(chatId: String, limit: Long = 50): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateMessageStatus(chatId: String, messageId: String, status: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .document(messageId)
            .update("status", status)
            .await()
    }

    suspend fun markMessageAsSeen(chatId: String, messageId: String, userId: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .document(messageId)
            .update(
                mapOf(
                    "status" to Constants.MESSAGE_STATUS_SEEN,
                    "seenBy" to FieldValue.arrayUnion(userId)
                )
            )
            .await()
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .document(messageId)
            .delete()
            .await()
    }

    suspend fun getMessage(chatId: String, messageId: String): Message? {
        return firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .document(messageId)
            .get()
            .await()
            .toObject(Message::class.java)
    }

    suspend fun updateReaction(chatId: String, messageId: String, userId: String, emoji: String?) {
        val docRef = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .document(messageId)

        if (emoji == null) {
            docRef.update("reactions.$userId", FieldValue.delete()).await()
        } else {
            docRef.update("reactions.$userId", emoji).await()
        }
    }

    suspend fun recallMessage(chatId: String, messageId: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .document(messageId)
            .update(mapOf(
                "text" to "",
                "status" to "recalled",
                "reactions" to FieldValue.delete()
            ))
            .await()
    }

    suspend fun markMessagesAsSeen(chatId: String, userId: String) {
        // Get all messages in the chat, then filter client-side
        // (avoids compound query that needs Firestore composite index)
        val allMessages = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .get()
            .await()

        val batch = firestore.batch()
        var hasUpdates = false
        for (doc in allMessages.documents) {
            val senderId = doc.getString("senderId") ?: continue
            val status = doc.getString("status") ?: continue
            if (senderId != userId && status == Constants.MESSAGE_STATUS_SENT) {
                batch.update(doc.reference, mapOf(
                    "status" to Constants.MESSAGE_STATUS_SEEN,
                    "seenBy" to FieldValue.arrayUnion(userId)
                ))
                hasUpdates = true
            }
        }
        if (hasUpdates) batch.commit().await()

        // Also reset unread count for this user in lastMessage
        val chat = getChat(chatId)
        if (chat?.lastMessage != null) {
            val updatedUnread = chat.lastMessage.unreadCount.toMutableMap()
            updatedUnread[userId] = 0L
            firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .update("lastMessage.unreadCount", updatedUnread)
                .await()
        }
    }

    suspend fun countMessagesByType(chatId: String, type: String): Int {
        return firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("type", type)
            .get()
            .await()
            .size()
    }

    suspend fun countLinkMessages(chatId: String): Int {
        val allMessages = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("type", Constants.MESSAGE_TYPE_TEXT)
            .get()
            .await()
            .toObjects(Message::class.java)
        return allMessages.count { it.text.contains("http://") || it.text.contains("https://") }
    }

    suspend fun clearChatMessages(chatId: String) {
        val messages = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .get()
            .await()
        val batch = firestore.batch()
        for (doc in messages.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()

        val updates = mutableMapOf<String, Any>()
        @Suppress("UNCHECKED_CAST")
        updates["lastMessage"] = FieldValue.delete()
        updateChat(chatId, updates)
    }

    // ══════════════════════════════════════════════════════════════
    // FRIEND REQUEST OPERATIONS
    // ══════════════════════════════════════════════════════════════

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
                // Map doc ID manually so request.id is never empty
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
        // 1. Mark request as accepted
        firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .document(requestId)
            .update("status", Constants.FRIEND_REQUEST_ACCEPTED)
            .await()

        // 2. Add each other to friends list
        firestore.collection(Constants.COLLECTION_USERS)
            .document(fromUserId)
            .update("friends", FieldValue.arrayUnion(toUserId))
            .await()

        firestore.collection(Constants.COLLECTION_USERS)
            .document(toUserId)
            .update("friends", FieldValue.arrayUnion(fromUserId))
            .await()

        // 3. Create a direct chat between the two if none exists yet
        val existing = findDirectChat(fromUserId, toUserId)
        if (existing == null) {
            val fromUser = getUser(fromUserId)
            val toUser   = getUser(toUserId)
            val chat = Chat(
                type         = Constants.CHAT_TYPE_DIRECT,
                participants = listOf(fromUserId, toUserId),
                groupName    = "${fromUser?.username ?: "User"} & ${toUser?.username ?: "User"}",
                updatedAt    = Timestamp.now(),
                createdAt    = Timestamp.now()
            )
            createChat(chat)
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

    // ══════════════════════════════════════════════════════════════
    // GROUP OPERATIONS
    // ══════════════════════════════════════════════════════════════

    suspend fun createGroup(group: Group): String {
        val docRef = firestore.collection(Constants.COLLECTION_GROUPS)
            .add(group)
            .await()
        return docRef.id
    }

    suspend fun getGroup(groupId: String): Group? {
        return firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .get()
            .await()
            .toObject(Group::class.java)
    }

    suspend fun updateGroup(groupId: String, updates: Map<String, Any>) {
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update(updates)
            .await()
    }

    suspend fun addGroupMember(groupId: String, member: GroupMember) {
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update("members", FieldValue.arrayUnion(member))
            .await()
    }

    suspend fun removeGroupMember(groupId: String, member: GroupMember) {
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update("members", FieldValue.arrayRemove(member))
            .await()
    }

    // ══════════════════════════════════════════════════════════════
    // CALL OPERATIONS
    // ══════════════════════════════════════════════════════════════

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
