package com.example.nexus.data.firebase

import android.R.id.message
import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
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

    fun observeChat(chatId: String): Flow<Chat?> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Chat::class.java))
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

    suspend fun pinMessage(chatId: String, message: Message) {
        val pinnedData = mapOf(
            "pinnedMessage" to mapOf(
                "messageId" to message.id,
                "text" to message.text.take(100),
                "senderName" to message.senderName,
                "pinnedBy" to (message.senderId),
                "pinnedAt" to Timestamp.now()
            )
        )
        updateChat(chatId, pinnedData)
    }

    suspend fun unpinMessage(chatId: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update("pinnedMessage", FieldValue.delete())
            .await()
    }

    suspend fun updateChatTheme(chatId: String, themeColor: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(mapOf(
                "themeColor" to themeColor,
                "updatedAt" to Timestamp.now()
            ))
            .await()
    }

    suspend fun updateChatNicknames(chatId: String, nicknames: Map<String, String>) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(mapOf(
                "nicknames" to nicknames,
                "updatedAt" to Timestamp.now()
            ))
            .await()
    }

    suspend fun archiveChat(chatId: String, userId: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update("archivedBy", FieldValue.arrayUnion(userId))
            .await()
    }

    suspend fun unarchiveChat(chatId: String, userId: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update("archivedBy", FieldValue.arrayRemove(userId))
            .await()
    }

    suspend fun setChatNickname(chatId: String, targetId: String, nickname: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(mapOf(
                "nicknames.$targetId" to nickname,
                "updatedAt" to Timestamp.now()
            ))
            .await()
    }

    suspend fun removeChatNickname(chatId: String, targetId: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(mapOf(
                "nicknames.$targetId" to FieldValue.delete(),
                "updatedAt" to Timestamp.now()
            ))
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

    fun observeTypingUsers(chatId: String): Flow<List<String>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chat = snapshot?.toObject(Chat::class.java)
                trySend(chat?.typingUsers ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ══════════════════════════════════════════════════════════════
    // MESSAGE OPERATIONS
    // ══════════════════════════════════════════════════════════════

    suspend fun sendMessage(chatId: String, message: Message): String {
        val messageToSave = if (message.type == Constants.MESSAGE_TYPE_TEXT) {
            val hasUrl = message.text.contains("http://") || message.text.contains("https://")
            message.copy(hasLink = hasUrl)
        } else message

        val docRef = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .add(messageToSave)
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
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(
                        isSending = doc.metadata.hasPendingWrites()
                    )
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun loadMoreMessages(chatId: String, lastTimestamp: Timestamp, limit: Long = 50): List<Message> {
        return firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastTimestamp)
            .limit(limit)
            .get()
            .await()
            .toObjects(Message::class.java)
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
        val aggregateQuery = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("type", type)
            .count()
        val snapshot = aggregateQuery.get(AggregateSource.SERVER).await()
        return snapshot.count.toInt()
    }

    suspend fun countLinkMessages(chatId: String): Int {
        val aggregateQuery = firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .collection(Constants.COLLECTION_MESSAGES)
            .whereEqualTo("hasLink", true)
            .count()
        val snapshot = aggregateQuery.get(AggregateSource.SERVER).await()
        return snapshot.count.toInt()
    }

    /**
     * Paginated query for shared media (images, videos, files) or link messages.
     *
     * Strategy:
     * - Single type (image/video/file/links): server-side filter with composite index
     * - Multiple types (image+video): two parallel queries merged by timestamp
     * - Fallback to client-side filtering if composite index doesn't exist yet
     *
     * @return Pair of (filtered messages, cursor for next page)
     */
    suspend fun getSharedMedia(
        chatId: String,
        types: List<String>,
        filterLinks: Boolean = false,
        senderId: String? = null,
        limit: Long = 30,
        lastTimestamp: Timestamp? = null
    ): Pair<List<Message>, Timestamp?> {
        return try {
            val col = firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)

            val results = if (filterLinks) {
                // Links: whereEqualTo("hasLink", true)
                val q = col.whereEqualTo("hasLink", true)
                    .let { if (senderId != null) it.whereEqualTo("senderId", senderId) else it }
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .let { if (lastTimestamp != null) it.startAfter(lastTimestamp) else it }
                    .limit(limit)
                q.get().await().toObjects(Message::class.java)

            } else if (types.size == 1) {
                // Single type: whereEqualTo("type", ...)
                val q = col.whereEqualTo("type", types.first())
                    .let { if (senderId != null) it.whereEqualTo("senderId", senderId) else it }
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .let { if (lastTimestamp != null) it.startAfter(lastTimestamp) else it }
                    .limit(limit)
                q.get().await().toObjects(Message::class.java)

            } else if (types.size > 1) {
                // Multiple types: query each type separately, merge, sort, take limit
                val deferreds = types.map { type ->
                    var q: Query = col.whereEqualTo("type", type)
                    if (senderId != null) q = q.whereEqualTo("senderId", senderId)
                    q = q.orderBy("timestamp", Query.Direction.DESCENDING)
                    if (lastTimestamp != null) q = q.startAfter(lastTimestamp)
                    q.limit(limit).get().await().toObjects(Message::class.java)
                }
                deferreds.flatten()
                    .sortedByDescending { it.timestamp }
                    .take(limit.toInt())

            } else {
                emptyList()
            }

            val cursor = results.lastOrNull()?.timestamp
            Pair(results, cursor)

        } catch (e: Exception) {
            // Fallback: composite index not created yet → client-side filtering
            getSharedMediaFallback(chatId, types, filterLinks, senderId, limit, lastTimestamp)
        }
    }

    /**
     * Fallback: load batches of 100, filter client-side.
     * Used when composite indexes haven't been created yet.
     */
    private suspend fun getSharedMediaFallback(
        chatId: String,
        types: List<String>,
        filterLinks: Boolean,
        senderId: String?,
        limit: Long,
        lastTimestamp: Timestamp?
    ): Pair<List<Message>, Timestamp?> {
        val matched = mutableListOf<Message>()
        var cursor: Timestamp? = lastTimestamp
        val needed = limit.toInt()

        while (matched.size < needed) {
            var query: Query = firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.DESCENDING)

            if (cursor != null) query = query.startAfter(cursor)

            val batch = query.limit(100).get().await().toObjects(Message::class.java)
            if (batch.isEmpty()) break

            val filtered = batch.filter { msg ->
                val typeMatch = if (filterLinks) msg.hasLink
                else if (types.isNotEmpty()) msg.type in types
                else true
                val senderMatch = senderId == null || msg.senderId == senderId
                typeMatch && senderMatch
            }

            matched.addAll(filtered)
            cursor = batch.lastOrNull()?.timestamp
            if (batch.size < 100) break
        }

        return Pair(matched.take(needed), cursor)
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

    /**
     * Observes whether a pending friend request exists from [fromUser] to [toUser].
     * Returns a Flow<Boolean> that emits true/false in real-time.
     */
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

    suspend fun cancelFriendRequest(fromUserId: String, toUserId: String) {
        val request = checkExistingFriendRequest(fromUserId, toUserId) ?: return
        firestore.collection(Constants.COLLECTION_FRIEND_REQUESTS)
            .document(request.id)
            .delete()
            .await()
    }

    suspend fun removeFriend(currentUserId: String, targetUserId: String) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .update("friends", FieldValue.arrayRemove(targetUserId))
            .await()
        firestore.collection(Constants.COLLECTION_USERS)
            .document(targetUserId)
            .update("friends", FieldValue.arrayRemove(currentUserId))
            .await()
    }

    suspend fun blockUser(currentUserId: String, targetUserId: String) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .update("blockedUsers", FieldValue.arrayUnion(targetUserId))
            .await()
    }

    suspend fun unblockUser(currentUserId: String, targetUserId: String) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(currentUserId)
            .update("blockedUsers", FieldValue.arrayRemove(targetUserId))
            .await()
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

    suspend fun createGroupChat(chat: Chat, group: Group): String {
        val chatRef = firestore.collection(Constants.COLLECTION_CHATS).document()
        val groupRef = firestore.collection(Constants.COLLECTION_GROUPS).document()
        val chatWithId = chat.copy(id = chatRef.id)
        val groupWithId = group.copy(id = groupRef.id, chatId = chatRef.id)
        val batch = firestore.batch()
        batch.set(chatRef, chatWithId)
        batch.set(groupRef, groupWithId)
        batch.commit().await()
        return chatRef.id
    }

    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Group::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun promoteGroupMember(groupId: String, chatId: String, userId: String) {
        val group = getGroup(groupId) ?: return
        val updatedMembers = group.members.map { member ->
            if (member.userId == userId) member.copy(role = Constants.ROLE_ADMIN) else member
        }
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update("members", updatedMembers)
            .await()
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update("adminIds", FieldValue.arrayUnion(userId))
            .await()
    }

    suspend fun demoteGroupMember(groupId: String, chatId: String, userId: String) {
        val group = getGroup(groupId) ?: return
        val updatedMembers = group.members.map { member ->
            if (member.userId == userId) member.copy(role = Constants.ROLE_MEMBER) else member
        }
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update("members", updatedMembers)
            .await()
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update("adminIds", FieldValue.arrayRemove(userId))
            .await()
    }

    suspend fun removeGroupMemberByKick(groupId: String, chatId: String, member: GroupMember) {
        val group = getGroup(groupId)
        if (group != null) {
            val updatedMembers = group.members.filter { it.userId != member.userId }
            firestore.collection(Constants.COLLECTION_GROUPS)
                .document(groupId)
                .update("members", updatedMembers)
                .await()
        }
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(
                mapOf(
                    "participants" to FieldValue.arrayRemove(member.userId),
                    "adminIds" to FieldValue.arrayRemove(member.userId),
                    "updatedAt" to Timestamp.now()
                )
            )
            .await()
    }

    suspend fun addChatParticipant(chatId: String, userId: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(
                mapOf(
                    "participants" to FieldValue.arrayUnion(userId),
                    "updatedAt" to Timestamp.now()
                )
            )
            .await()
    }

    suspend fun removeChatParticipant(chatId: String, userId: String) {
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(
                mapOf(
                    "participants" to FieldValue.arrayRemove(userId),
                    "adminIds" to FieldValue.arrayRemove(userId),
                    "updatedAt" to Timestamp.now()
                )
            )
            .await()
    }

    suspend fun dissolveGroup(chatId: String, groupId: String) {
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .delete()
            .await()
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(
                mapOf(
                    "participants" to emptyList<String>(),
                    "updatedAt" to Timestamp.now()
                )
            )
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

    // ══════════════════════════════════════════════════════════════
    // STORY / NOTE OPERATIONS
    // ══════════════════════════════════════════════════════════════

    suspend fun createStory(story: com.example.nexus.data.model.Story): String {
        val docRef = firestore.collection("stories").add(story).await()
        return docRef.id
    }

    fun observeAllActiveStories(): Flow<List<com.example.nexus.data.model.Story>> = callbackFlow {
        val listener = firestore.collection("stories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                val stories = snapshot?.documents?.mapNotNull { doc ->
                    val story = doc.toObject(com.example.nexus.data.model.Story::class.java)?.copy(id = doc.id)
                    val expiresAt = story?.expiresAt?.toDate()?.time ?: 0L
                    if (expiresAt > now) story else null
                } ?: emptyList()
                trySend(stories)
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteStory(storyId: String) {
        firestore.collection("stories").document(storyId).delete().await()
    }

    suspend fun markStoryAsViewed(storyId: String, userId: String) {
        firestore.collection("stories").document(storyId)
            .update("viewedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
            .await()
    }

    suspend fun deleteUserStoriesByType(userId: String, type: String) {
        val snapshot = firestore.collection("stories")
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type)
            .get()
            .await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }
}
