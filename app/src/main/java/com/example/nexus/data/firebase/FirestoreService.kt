package com.example.nexus.data.firebase

import com.example.nexus.data.firebase.firestore.*
import com.example.nexus.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade that delegates to domain-specific Firestore services.
 * Keeps backward compatibility while code is being migrated.
 */
@Singleton
class FirestoreService @Inject constructor(
    private val userFirestore: UserFirestore,
    private val chatFirestore: ChatFirestore,
    private val messageFirestore: MessageFirestore,
    private val friendRequestFirestore: FriendRequestFirestore,
    private val groupFirestore: GroupFirestore,
    private val callFirestore: CallFirestore,
    private val storyFirestore: StoryFirestore,
    private val notificationFirestore: NotificationFirestore,
    // Keep firestore for any legacy callers
    @Suppress("unused") private val firestore: FirebaseFirestore
) {
    // ── User ──
    suspend fun createUser(user: User) = userFirestore.createUser(user)
    suspend fun getUser(userId: String): User? = userFirestore.getUser(userId)
    fun observeUser(userId: String): Flow<User?> = userFirestore.observeUser(userId)
    suspend fun updateUser(userId: String, updates: Map<String, Any>) = userFirestore.updateUser(userId, updates)
    suspend fun updateUserStatus(userId: String, status: String) = userFirestore.updateUserStatus(userId, status)
    suspend fun searchUsersByUsername(query: String): List<User> = userFirestore.searchUsersByUsername(query)
    suspend fun searchUsersByPhone(phone: String): List<User> = userFirestore.searchUsersByPhone(phone)
    suspend fun deleteUser(userId: String) = userFirestore.deleteUser(userId)
    suspend fun blockUser(currentUserId: String, targetUserId: String) = userFirestore.blockUser(currentUserId, targetUserId)
    suspend fun unblockUser(currentUserId: String, targetUserId: String) = userFirestore.unblockUser(currentUserId, targetUserId)
    suspend fun removeFriend(currentUserId: String, targetUserId: String) = userFirestore.removeFriend(currentUserId, targetUserId)

    // ── Chat ──
    suspend fun createChat(chat: Chat): String = chatFirestore.createChat(chat)
    suspend fun getChat(chatId: String): Chat? = chatFirestore.getChat(chatId)
    fun observeChatsForUser(userId: String): Flow<List<Chat>> = chatFirestore.observeChatsForUser(userId)
    fun observeChat(chatId: String): Flow<Chat?> = chatFirestore.observeChat(chatId)
    suspend fun findDirectChat(userId1: String, userId2: String): Chat? = chatFirestore.findDirectChat(userId1, userId2)
    suspend fun updateChat(chatId: String, updates: Map<String, Any>) = chatFirestore.updateChat(chatId, updates)
    suspend fun pinMessage(chatId: String, message: Message) = chatFirestore.pinMessage(chatId, message)
    suspend fun unpinMessage(chatId: String) = chatFirestore.unpinMessage(chatId)
    suspend fun updateChatTheme(chatId: String, themeColor: String) = chatFirestore.updateChatTheme(chatId, themeColor)
    suspend fun updateChatNicknames(chatId: String, nicknames: Map<String, String>) = chatFirestore.updateChatNicknames(chatId, nicknames)
    suspend fun archiveChat(chatId: String, userId: String) = chatFirestore.archiveChat(chatId, userId)
    suspend fun unarchiveChat(chatId: String, userId: String) = chatFirestore.unarchiveChat(chatId, userId)
    suspend fun setChatNickname(chatId: String, targetId: String, nickname: String) = chatFirestore.setChatNickname(chatId, targetId, nickname)
    suspend fun removeChatNickname(chatId: String, targetId: String) = chatFirestore.removeChatNickname(chatId, targetId)
    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean) = chatFirestore.updateTypingStatus(chatId, userId, isTyping)
    fun observeTypingUsers(chatId: String): Flow<List<String>> = chatFirestore.observeTypingUsers(chatId)
    suspend fun addChatParticipant(chatId: String, userId: String) = chatFirestore.addChatParticipant(chatId, userId)
    suspend fun removeChatParticipant(chatId: String, userId: String) = chatFirestore.removeChatParticipant(chatId, userId)

    // ── Message ──
    suspend fun sendMessage(chatId: String, message: Message): String = messageFirestore.sendMessage(chatId, message)
    fun observeMessages(chatId: String, limit: Long = 50): Flow<List<Message>> = messageFirestore.observeMessages(chatId, limit)
    suspend fun loadMoreMessages(chatId: String, lastTimestamp: Timestamp, limit: Long = 50): List<Message> = messageFirestore.loadMoreMessages(chatId, lastTimestamp, limit)
    suspend fun updateMessageStatus(chatId: String, messageId: String, status: String) = messageFirestore.updateMessageStatus(chatId, messageId, status)
    suspend fun markMessageAsSeen(chatId: String, messageId: String, userId: String) = messageFirestore.markMessageAsSeen(chatId, messageId, userId)
    suspend fun deleteMessage(chatId: String, messageId: String) = messageFirestore.deleteMessage(chatId, messageId)
    suspend fun getMessage(chatId: String, messageId: String): Message? = messageFirestore.getMessage(chatId, messageId)
    suspend fun updateReaction(chatId: String, messageId: String, userId: String, emoji: String?) = messageFirestore.updateReaction(chatId, messageId, userId, emoji)
    suspend fun recallMessage(chatId: String, messageId: String) = messageFirestore.recallMessage(chatId, messageId)
    suspend fun markMessagesAsSeen(chatId: String, userId: String) = messageFirestore.markMessagesAsSeen(chatId, userId)
    suspend fun countMessagesByType(chatId: String, type: String): Int = messageFirestore.countMessagesByType(chatId, type)
    suspend fun countLinkMessages(chatId: String): Int = messageFirestore.countLinkMessages(chatId)
    suspend fun getSharedMedia(chatId: String, types: List<String>, filterLinks: Boolean, senderId: String?, limit: Long, lastTimestamp: Timestamp?): Pair<List<Message>, Timestamp?> = messageFirestore.getSharedMedia(chatId, types, filterLinks, senderId, limit, lastTimestamp)
    suspend fun clearChatMessages(chatId: String) = messageFirestore.clearChatMessages(chatId)

    // ── Friend Request ──
    suspend fun sendFriendRequest(request: FriendRequest): String = friendRequestFirestore.sendFriendRequest(request)
    fun observeReceivedFriendRequests(userId: String): Flow<List<FriendRequest>> = friendRequestFirestore.observeReceivedFriendRequests(userId)
    fun observeSentFriendRequests(userId: String): Flow<List<FriendRequest>> = friendRequestFirestore.observeSentFriendRequests(userId)
    fun observeFriendRequestExists(fromUser: String, toUser: String): Flow<Boolean> = friendRequestFirestore.observeFriendRequestExists(fromUser, toUser)
    suspend fun getSentRequestTargetIds(userId: String): Set<String> = friendRequestFirestore.getSentRequestTargetIds(userId)
    suspend fun acceptFriendRequest(requestId: String, fromUserId: String, toUserId: String) = friendRequestFirestore.acceptFriendRequest(requestId, fromUserId, toUserId)
    suspend fun rejectFriendRequest(requestId: String) = friendRequestFirestore.rejectFriendRequest(requestId)
    suspend fun checkExistingFriendRequest(fromUserId: String, toUserId: String): FriendRequest? = friendRequestFirestore.checkExistingFriendRequest(fromUserId, toUserId)
    suspend fun cancelFriendRequest(fromUserId: String, toUserId: String) = friendRequestFirestore.cancelFriendRequest(fromUserId, toUserId)

    // ── Group ──
    suspend fun createGroup(group: Group): String = groupFirestore.createGroup(group)
    suspend fun getGroup(groupId: String): Group? = groupFirestore.getGroup(groupId)
    suspend fun updateGroup(groupId: String, updates: Map<String, Any>) = groupFirestore.updateGroup(groupId, updates)
    suspend fun addGroupMember(groupId: String, member: GroupMember) = groupFirestore.addGroupMember(groupId, member)
    suspend fun removeGroupMember(groupId: String, member: GroupMember) = groupFirestore.removeGroupMember(groupId, member)
    suspend fun createGroupChat(chat: Chat, group: Group): String = groupFirestore.createGroupChat(chat, group)
    fun observeGroup(groupId: String): Flow<Group?> = groupFirestore.observeGroup(groupId)
    suspend fun promoteGroupMember(groupId: String, chatId: String, userId: String) = groupFirestore.promoteGroupMember(groupId, chatId, userId)
    suspend fun demoteGroupMember(groupId: String, chatId: String, userId: String) = groupFirestore.demoteGroupMember(groupId, chatId, userId)
    suspend fun removeGroupMemberByKick(groupId: String, chatId: String, member: GroupMember) = groupFirestore.removeGroupMemberByKick(groupId, chatId, member)
    suspend fun dissolveGroup(chatId: String, groupId: String) = groupFirestore.dissolveGroup(chatId, groupId)

    // ── Call ──
    suspend fun createCall(call: CallRecord): String = callFirestore.createCall(call)
    fun observeCallsForUser(userId: String): Flow<List<CallRecord>> = callFirestore.observeCallsForUser(userId)
    suspend fun updateCallStatus(callId: String, status: String) = callFirestore.updateCallStatus(callId, status)

    // ── Story ──
    suspend fun createStory(story: Story): String = storyFirestore.createStory(story)
    fun observeAllActiveStories(): Flow<List<Story>> = storyFirestore.observeAllActiveStories()
    suspend fun deleteStory(storyId: String) = storyFirestore.deleteStory(storyId)
    suspend fun markStoryAsViewed(storyId: String, userId: String) = storyFirestore.markStoryAsViewed(storyId, userId)
    suspend fun deleteUserStoriesByType(userId: String, type: String) = storyFirestore.deleteUserStoriesByType(userId, type)

    // ── Feedback ──
    suspend fun submitFeedback(feedback: Feedback) = notificationFirestore.submitFeedback(feedback)

    // ── Notifications ──
    fun observeSystemNotifications(): Flow<List<SystemNotification>> = notificationFirestore.observeSystemNotifications()
    fun observeUserNotifications(userId: String): Flow<List<UserNotification>> = notificationFirestore.observeUserNotifications(userId)
    suspend fun markNotificationRead(notificationId: String, userId: String) = notificationFirestore.markNotificationRead(notificationId, userId)
    suspend fun createUserNotification(notificationId: String, userId: String) = notificationFirestore.createUserNotification(notificationId, userId)
}
