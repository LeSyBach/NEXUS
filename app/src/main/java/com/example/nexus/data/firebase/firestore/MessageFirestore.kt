package com.example.nexus.data.firebase.firestore

import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.LastMessage
import com.example.nexus.data.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore operations for messages subcollection (`chats/{chatId}/messages`).
 */
@Singleton
class MessageFirestore @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chatFirestore: ChatFirestore
) {
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

        docRef.update("id", docRef.id).await()

        val chat = chatFirestore.getChat(chatId)
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
        chatFirestore.updateChat(chatId, mapOf("lastMessage" to lastMessage))

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

        val chat = chatFirestore.getChat(chatId)
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
                val q = col.whereEqualTo("hasLink", true)
                    .let { if (senderId != null) it.whereEqualTo("senderId", senderId) else it }
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .let { if (lastTimestamp != null) it.startAfter(lastTimestamp) else it }
                    .limit(limit)
                q.get().await().toObjects(Message::class.java)
            } else if (types.size == 1) {
                val q = col.whereEqualTo("type", types.first())
                    .let { if (senderId != null) it.whereEqualTo("senderId", senderId) else it }
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .let { if (lastTimestamp != null) it.startAfter(lastTimestamp) else it }
                    .limit(limit)
                q.get().await().toObjects(Message::class.java)
            } else if (types.size > 1) {
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
            getSharedMediaFallback(chatId, types, filterLinks, senderId, limit, lastTimestamp)
        }
    }

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
        chatFirestore.updateChat(chatId, updates)
    }
}
