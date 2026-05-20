package com.example.nexus.data.repository

import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.firebase.NotificationService
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Message
import com.example.nexus.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val authService: AuthService,
    private val notificationService: NotificationService
) {

    fun getCurrentUserId(): String? {
        return authService.currentUserId
    }

    suspend fun getUserById(userId: String): User? {
        return firestoreService.getUser(userId)
    }

    fun observeChats(): Flow<Resource<List<Chat>>> = flow {
        emit(Resource.Loading)
        val userId = authService.currentUserId
        if (userId == null) {
            emit(Resource.Error("User not logged in"))
            return@flow
        }
        
        firestoreService.observeChatsForUser(userId).collect { chats ->
            emit(Resource.Success(chats))
        }
    }.catch { e ->
        emit(Resource.Error(e.message ?: "Unknown error"))
    }

    fun observeMessages(chatId: String): Flow<Resource<List<Message>>> = flow {
        emit(Resource.Loading)
        firestoreService.observeMessages(chatId).collect { messages ->
            emit(Resource.Success(messages))
        }
    }.catch { e ->
        emit(Resource.Error(e.message ?: "Unknown error"))
    }

    suspend fun sendMessage(chatId: String, text: String): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)
            
            val message = Message(
                senderId = userId,
                senderName = currentUser?.username ?: "Unknown",
                text = text,
                type = "text"
            )
            firestoreService.sendMessage(chatId, message)

            val chat = firestoreService.getChat(chatId)
            if (chat != null) {
                val otherParticipants = chat.participants.filter { it != userId }
                for (receiverId in otherParticipants) {
                    notificationService.sendMessageNotification(
                        receiverId = receiverId,
                        senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                        messageText = text,
                        chatId = chatId,
                        senderId = userId
                    )
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    suspend fun getChatById(chatId: String): Chat? {
        return firestoreService.getChat(chatId)
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        firestoreService.deleteMessage(chatId, messageId)
    }

    suspend fun recallMessage(chatId: String, messageId: String) {
        firestoreService.recallMessage(chatId, messageId)
    }

    suspend fun markMessagesAsSeen(chatId: String) {
        val userId = authService.currentUserId ?: return
        firestoreService.markMessagesAsSeen(chatId, userId)
    }

    suspend fun setTypingStatus(chatId: String, isTyping: Boolean) {
        val userId = authService.currentUserId ?: return
        firestoreService.updateTypingStatus(chatId, userId, isTyping)
    }

    suspend fun getSharedContentCounts(chatId: String): Triple<Int, Int, Int> {
        return try {
            val images = firestoreService.countMessagesByType(chatId, Constants.MESSAGE_TYPE_IMAGE)
            val files = firestoreService.countMessagesByType(chatId, Constants.MESSAGE_TYPE_FILE)
            val links = firestoreService.countLinkMessages(chatId)
            Triple(images, files, links)
        } catch (_: Exception) {
            Triple(0, 0, 0)
        }
    }

    suspend fun clearChatMessages(chatId: String) {
        firestoreService.clearChatMessages(chatId)
    }

    suspend fun findChatIdByParticipants(otherUserId: String): String? {
        val userId = authService.currentUserId ?: return null
        return try {
            firestoreService.findDirectChat(userId, otherUserId)?.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendImageMessage(chatId: String, imageUrl: String): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)

            val message = Message(
                senderId = userId,
                senderName = currentUser?.username ?: "Unknown",
                text = imageUrl,
                type = Constants.MESSAGE_TYPE_IMAGE
            )
            firestoreService.sendMessage(chatId, message)

            val chat = firestoreService.getChat(chatId)
            if (chat != null) {
                val otherParticipants = chat.participants.filter { it != userId }
                for (receiverId in otherParticipants) {
                    notificationService.sendMessageNotification(
                        receiverId = receiverId,
                        senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                        messageText = "📷 Hình ảnh",
                        chatId = chatId,
                        senderId = userId
                    )
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send image message")
        }
    }

    suspend fun sendVoiceMessage(chatId: String, voiceUrl: String, durationSec: Long): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)

            val message = Message(
                senderId = userId,
                senderName = currentUser?.username ?: "Unknown",
                text = voiceUrl,
                type = Constants.MESSAGE_TYPE_VOICE,
                duration = durationSec
            )
            firestoreService.sendMessage(chatId, message)

            val chat = firestoreService.getChat(chatId)
            if (chat != null) {
                val otherParticipants = chat.participants.filter { it != userId }
                for (receiverId in otherParticipants) {
                    notificationService.sendMessageNotification(
                        receiverId = receiverId,
                        senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                        messageText = "🎤 Tin nhắn thoại",
                        chatId = chatId,
                        senderId = userId
                    )
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send voice message")
        }
    }

    suspend fun sendCallHistoryMessage(
        chatId: String,
        callType: String,
        duration: Long,
        callStatus: String
    ): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)
            val message = Message(
                senderId = userId,
                senderName = currentUser?.username ?: "Unknown",
                text = callType,
                type = Constants.MESSAGE_TYPE_CALL,
                duration = duration,
                status = callStatus
            )
            firestoreService.sendMessage(chatId, message)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send call history")
        }
    }
}
