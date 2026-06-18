package com.example.nexus.data.repository

import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.firebase.NotificationService
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Group
import com.example.nexus.data.model.GroupMember
import com.example.nexus.data.model.Message
import com.example.nexus.data.model.PinnedMessage
import com.example.nexus.data.model.ReplyMessage
import com.google.firebase.Timestamp
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

    fun observeUser(userId: String): Flow<User?> {
        return firestoreService.observeUser(userId)
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

    suspend fun loadMoreMessages(chatId: String, lastTimestamp: Timestamp): List<Message> {
        return firestoreService.loadMoreMessages(chatId, lastTimestamp)
    }

    suspend fun sendMessage(chatId: String, text: String, replyTo: ReplyMessage? = null, mentions: List<String> = emptyList()): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)

            val message = Message(
                senderId = userId,
                senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown",
                text = text,
                type = "text",
                replyTo = replyTo,
                mentions = mentions
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

    fun observeTypingUsers(chatId: String): Flow<List<String>> {
        return firestoreService.observeTypingUsers(chatId)
    }

    suspend fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val userId = authService.currentUserId ?: return
        val message = firestoreService.getMessage(chatId, messageId) ?: return
        val currentReaction = message.reactions[userId]

        if (currentReaction == emoji) {
            firestoreService.updateReaction(chatId, messageId, userId, null)
        } else {
            firestoreService.updateReaction(chatId, messageId, userId, emoji)
        }
    }

    suspend fun getSharedContentCounts(chatId: String): Triple<Int, Int, Int> {
        return try {
            val images = firestoreService.countMessagesByType(chatId, Constants.MESSAGE_TYPE_IMAGE)
            val videos = firestoreService.countMessagesByType(chatId, Constants.MESSAGE_TYPE_VIDEO)
            val files = firestoreService.countMessagesByType(chatId, Constants.MESSAGE_TYPE_FILE)
            val links = firestoreService.countLinkMessages(chatId)
            Triple(images + videos, files, links)
        } catch (_: Exception) {
            Triple(0, 0, 0)
        }
    }

    suspend fun getSharedMedia(
        chatId: String,
        types: List<String>,
        filterLinks: Boolean = false,
        senderId: String? = null,
        limit: Long = 30,
        lastTimestamp: Timestamp? = null
    ): Pair<List<Message>, Timestamp?> {
        return firestoreService.getSharedMedia(chatId, types, filterLinks, senderId, limit, lastTimestamp)
    }

    suspend fun updateChatTheme(chatId: String, themeColor: String) {
        firestoreService.updateChatTheme(chatId, themeColor)
    }

    suspend fun updateChatNicknames(chatId: String, nicknames: Map<String, String>) {
        firestoreService.updateChatNicknames(chatId, nicknames)
    }

    suspend fun setChatNickname(chatId: String, targetId: String, nickname: String) {
        firestoreService.setChatNickname(chatId, targetId, nickname)
    }

    suspend fun removeChatNickname(chatId: String, targetId: String) {
        firestoreService.removeChatNickname(chatId, targetId)
    }

    suspend fun archiveChat(chatId: String) {
        val userId = getCurrentUserId() ?: return
        firestoreService.archiveChat(chatId, userId)
    }

    suspend fun unarchiveChat(chatId: String) {
        val userId = getCurrentUserId() ?: return
        firestoreService.unarchiveChat(chatId, userId)
    }

    fun observeChat(chatId: String): Flow<Chat?> {
        return firestoreService.observeChat(chatId)
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
                senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown",
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

    suspend fun sendVideoMessage(chatId: String, videoUrl: String): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)

            val message = Message(
                senderId = userId,
                senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown",
                text = videoUrl,
                type = Constants.MESSAGE_TYPE_VIDEO
            )
            firestoreService.sendMessage(chatId, message)

            val chat = firestoreService.getChat(chatId)
            if (chat != null) {
                val otherParticipants = chat.participants.filter { it != userId }
                for (receiverId in otherParticipants) {
                    notificationService.sendMessageNotification(
                        receiverId = receiverId,
                        senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                        messageText = "🎬 Video",
                        chatId = chatId,
                        senderId = userId
                    )
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send video message")
        }
    }

    suspend fun sendVoiceMessage(chatId: String, voiceUrl: String, durationSec: Long): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)

            val message = Message(
                senderId = userId,
                senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown",
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

    suspend fun sendFileMessage(chatId: String, fileUrl: String, fileName: String, fileSize: Long): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)

            val message = Message(
                senderId = userId,
                senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown",
                text = fileUrl,
                type = Constants.MESSAGE_TYPE_FILE,
                fileName = fileName,
                fileSize = fileSize
            )
            firestoreService.sendMessage(chatId, message)

            val chat = firestoreService.getChat(chatId)
            if (chat != null) {
                val otherParticipants = chat.participants.filter { it != userId }
                for (receiverId in otherParticipants) {
                    notificationService.sendMessageNotification(
                        receiverId = receiverId,
                        senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                        messageText = "📎 $fileName",
                        chatId = chatId,
                        senderId = userId
                    )
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send file message")
        }
    }

    suspend fun sendContactMessage(
        chatId: String,
        contactUserId: String,
        contactName: String,
        contactPhone: String,
        contactAvatarUrl: String
    ): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)

            val message = Message(
                senderId = userId,
                senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown",
                text = contactName,
                type = Constants.MESSAGE_TYPE_CONTACT,
                contactUserId = contactUserId,
                contactName = contactName,
                contactPhone = contactPhone,
                contactAvatarUrl = contactAvatarUrl
            )
            firestoreService.sendMessage(chatId, message)

            val chat = firestoreService.getChat(chatId)
            if (chat != null) {
                val otherParticipants = chat.participants.filter { it != userId }
                for (receiverId in otherParticipants) {
                    notificationService.sendMessageNotification(
                        receiverId = receiverId,
                        senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                        messageText = "👤 Đã chia sẻ liên hệ: $contactName",
                        chatId = chatId,
                        senderId = userId
                    )
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send contact message")
        }
    }

    suspend fun forwardMessage(targetChatId: String, original: Message): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)
            val currentUserName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown"
            val forwardedFrom = original.forwardedFrom ?: currentUserName
            val newMessage = Message(
                senderId = userId,
                senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown",
                text = original.text,
                type = original.type,
                fileName = original.fileName,
                fileSize = original.fileSize,
                duration = original.duration,
                contactUserId = original.contactUserId,
                contactName = original.contactName,
                contactPhone = original.contactPhone,
                contactAvatarUrl = original.contactAvatarUrl,
                forwardedFrom = forwardedFrom
            )
            firestoreService.sendMessage(targetChatId, newMessage)

            val chat = firestoreService.getChat(targetChatId)
            if (chat != null) {
                val otherParticipants = chat.participants.filter { it != userId }
                val previewText = when (original.type) {
                    Constants.MESSAGE_TYPE_IMAGE -> "📷 Hình ảnh"
                    Constants.MESSAGE_TYPE_VIDEO -> "🎬 Video"
                    Constants.MESSAGE_TYPE_VOICE -> "🎤 Tin nhắn thoại"
                    Constants.MESSAGE_TYPE_FILE -> "📎 ${original.fileName.ifEmpty { "Tệp" }}"
                    else -> original.text
                }
                for (receiverId in otherParticipants) {
                    notificationService.sendMessageNotification(
                        receiverId = receiverId,
                        senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                        messageText = previewText,
                        chatId = targetChatId,
                        senderId = userId
                    )
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to forward message")
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
                senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Unknown",
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

    // ══════════════════════════════════════════════════════════════
    // GROUP OPERATIONS
    // ══════════════════════════════════════════════════════════════

    suspend fun createGroup(
        name: String,
        avatarUrl: String,
        memberIds: List<String>
    ): Resource<String> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)
            val creatorName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User"

            val allParticipants = (memberIds + userId).distinct()
            val members = allParticipants.map { memberId ->
                val user = if (memberId == userId) currentUser else firestoreService.getUser(memberId)
                GroupMember(
                    userId = memberId,
                    username = user?.username ?: "user",
                    displayName = user?.displayName?.ifEmpty { user.username } ?: "user",
                    avatarUrl = user?.avatarUrl ?: "",
                    role = if (memberId == userId) Constants.ROLE_ADMIN else Constants.ROLE_MEMBER,
                    joinedAt = Timestamp.now()
                )
            }

            val chat = Chat(
                type = Constants.CHAT_TYPE_GROUP,
                participants = allParticipants,
                groupName = name,
                groupAvatarUrl = avatarUrl,
                createdBy = userId,
                adminIds = listOf(userId),
                updatedAt = Timestamp.now(),
                createdAt = Timestamp.now()
            )

            val group = Group(
                name = name,
                avatarUrl = avatarUrl,
                createdBy = userId,
                members = members,
                createdAt = Timestamp.now()
            )

            val chatId = firestoreService.createGroupChat(chat, group)

            sendSystemMessage(chatId, "$creatorName đã tạo nhóm")

            Resource.Success(chatId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create group")
        }
    }

    suspend fun sendSystemMessage(chatId: String, text: String) {
        val userId = authService.currentUserId ?: return
        val currentUser = firestoreService.getUser(userId)
        val message = Message(
            senderId = userId,
            senderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "System",
            text = text,
            type = Constants.MESSAGE_TYPE_SYSTEM
        )
        firestoreService.sendMessage(chatId, message)
    }

    suspend fun addGroupMembers(
        chatId: String,
        groupId: String,
        users: List<User>
    ): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)
            val adderName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User"

            for (user in users) {
                firestoreService.addChatParticipant(chatId, user.uid)
                val member = GroupMember(
                    userId = user.uid,
                    username = user.username,
                    displayName = user.displayName.ifEmpty { user.username },
                    avatarUrl = user.avatarUrl,
                    role = Constants.ROLE_MEMBER,
                    joinedAt = Timestamp.now()
                )
                firestoreService.addGroupMember(groupId, member)
            }

            val names = users.joinToString(", ") { it.displayName.ifEmpty { it.username } }
            sendSystemMessage(chatId, "$adderName đã thêm $names vào nhóm")

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add members")
        }
    }

    suspend fun kickMember(
        chatId: String,
        groupId: String,
        userId: String,
        username: String
    ): Resource<Unit> {
        return try {
            val currentUserId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(currentUserId)
            val kickerName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Admin"

            val group = firestoreService.getGroup(groupId)
            val member = group?.members?.find { it.userId == userId }
            if (member != null) {
                firestoreService.removeGroupMemberByKick(groupId, chatId, member)
            }

            sendSystemMessage(chatId, "$kickerName đã xóa $username khỏi nhóm")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to kick member")
        }
    }

    suspend fun leaveGroup(chatId: String, groupId: String): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)
            val userName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User"

            val group = firestoreService.getGroup(groupId)
            val member = group?.members?.find { it.userId == userId }
            if (member != null) {
                firestoreService.removeGroupMemberByKick(groupId, chatId, member)
            }

            sendSystemMessage(chatId, "$userName đã rời khỏi nhóm")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to leave group")
        }
    }

    suspend fun promoteToAdmin(
        chatId: String,
        groupId: String,
        userId: String,
        username: String
    ): Resource<Unit> {
        return try {
            val currentUserId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(currentUserId)
            val promoterName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Admin"

            firestoreService.promoteGroupMember(groupId, chatId, userId)

            sendSystemMessage(chatId, "$promoterName đã chỉ định $username làm quản trị viên")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to promote member")
        }
    }

    suspend fun demoteAdmin(
        chatId: String,
        groupId: String,
        userId: String,
        username: String
    ): Resource<Unit> {
        return try {
            val currentUserId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(currentUserId)
            val demoterName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Admin"

            firestoreService.demoteGroupMember(groupId, chatId, userId)

            sendSystemMessage(chatId, "$demoterName đã hạ cấp $username thành thành viên thường")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to demote member")
        }
    }

    suspend fun dissolveGroup(chatId: String, groupId: String): Resource<Unit> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            val currentUser = firestoreService.getUser(userId)
            val adminName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "Admin"

            sendSystemMessage(chatId, "$adminName đã giải tán nhóm")
            firestoreService.dissolveGroup(chatId, groupId)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to dissolve group")
        }
    }

    suspend fun updateGroupAvatar(chatId: String, groupId: String, avatarUrl: String) {
        firestoreService.updateGroup(groupId, mapOf("avatarUrl" to avatarUrl))
        firestoreService.updateChat(chatId, mapOf("groupAvatarUrl" to avatarUrl))
    }

    fun observeGroup(groupId: String): Flow<Group?> {
        return firestoreService.observeGroup(groupId)
    }

    suspend fun pinMessage(chatId: String, message: Message) {
        firestoreService.pinMessage(chatId, message)
    }

    suspend fun unpinMessage(chatId: String) {
        firestoreService.unpinMessage(chatId)
    }

    // ══════════════════════════════════════════════════════════════
    // STORY / NOTE OPERATIONS
    // ══════════════════════════════════════════════════════════════

    suspend fun createStory(content: String, type: String = "text", caption: String? = null): Resource<String> {
        return try {
            val userId = authService.currentUserId ?: return Resource.Error("User not logged in")
            // Auto-delete old notes when posting a new one
            if (type == "text") {
                firestoreService.deleteUserStoriesByType(userId, "text")
            }
            val now = Timestamp.now()
            val story = com.example.nexus.data.model.Story(
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

    fun observeAllActiveStories(): Flow<List<com.example.nexus.data.model.Story>> {
        return firestoreService.observeAllActiveStories()
    }

    suspend fun deleteStory(storyId: String) {
        firestoreService.deleteStory(storyId)
    }

    suspend fun markStoryAsViewed(storyId: String, userId: String) {
        firestoreService.markStoryAsViewed(storyId, userId)
    }
}
