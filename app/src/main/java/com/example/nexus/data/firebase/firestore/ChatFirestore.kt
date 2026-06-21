package com.example.nexus.data.firebase.firestore

import android.util.Log
import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Message
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
 * Firestore operations for the `chats` collection (excluding message subcollection).
 */
@Singleton
class ChatFirestore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
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
                    Log.w("ChatFirestore", "observeChatsForUser error", error)
                    trySend(emptyList())
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
                "pinnedBy" to message.senderId,
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
}
