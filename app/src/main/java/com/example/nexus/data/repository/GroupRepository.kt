package com.example.nexus.data.repository

import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Group
import com.example.nexus.data.model.GroupMember
import com.example.nexus.data.model.Message
import com.example.nexus.data.model.User
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val authService: AuthService
) {

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
}
