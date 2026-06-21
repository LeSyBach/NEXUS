package com.example.nexus.data.repository

import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.firebase.NotificationService
import com.example.nexus.data.model.Message
import com.example.nexus.data.model.Story
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val authService: AuthService,
    private val notificationService: NotificationService
) {

    suspend fun createStory(content: String, type: String = "text", caption: String? = null): Resource<String> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            // Auto-delete old notes when posting a new one
            if (type == "text") {
                firestoreService.deleteUserStoriesByType(userId, "text")
            }
            val now = Timestamp.now()
            val story = Story(
                userId = userId,
                content = content,
                type = type,
                caption = caption,
                createdAt = now,
                expiresAt = Timestamp(java.util.Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
            )
            val id = firestoreService.createStory(story)
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to post story")
        }
    }

    fun observeAllActiveStories(): Flow<List<Story>> {
        return firestoreService.observeAllActiveStories()
    }

    suspend fun deleteStory(storyId: String) {
        firestoreService.deleteStory(storyId)
    }

    suspend fun markStoryAsViewed(storyId: String, userId: String) {
        firestoreService.markStoryAsViewed(storyId, userId)
    }

    suspend fun sendStoryReplyMessage(
        chatId: String,
        storyId: String,
        storyContent: String,
        storyCaption: String,
        isNote: Boolean,
        replyText: String
    ): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)
            val message = Message(
                senderId = userId,
                senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown",
                text = replyText,
                type = if (isNote) Constants.MESSAGE_TYPE_NOTE_REPLY else Constants.MESSAGE_TYPE_STORY_REPLY,
                storyId = storyId,
                storyContent = storyContent,
                storyCaption = storyCaption
            )
            firestoreService.sendMessage(chatId, message)

            val chat = firestoreService.getChat(chatId)
            if (chat != null) {
                val otherParticipants = chat.participants.filter { it != userId }
                for (receiverId in otherParticipants) {
                    notificationService.sendMessageNotification(
                        receiverId = receiverId,
                        senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                        messageText = if (isNote) "Đã phản hồi ghi chú" else "Đã trả lời ảnh",
                        chatId = chatId,
                        senderId = userId
                    )
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send story reply")
        }
    }
}
