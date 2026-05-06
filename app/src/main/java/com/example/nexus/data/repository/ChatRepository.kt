package com.example.nexus.data.repository

import com.example.nexus.core.utils.Resource
import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
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
    private val authService: AuthService
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
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }
}
