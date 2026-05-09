package com.example.nexus.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a user in the NEXUS app.
 * Maps directly to Firestore `users` collection.
 */
data class User(
    @DocumentId
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val status: String = "offline",
    val lastSeen: Timestamp? = null,
    val fcmToken: String = "",
    val bio: String = "",
    val friends: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
)

/**
 * Represents a chat (direct or group).
 * Maps to Firestore `chats` collection.
 */
data class Chat(
    @DocumentId
    val id: String = "",
    val type: String = "direct", // "direct" or "group"
    val participants: List<String> = emptyList(),
    val lastMessage: LastMessage? = null,
    val groupName: String = "",
    val groupAvatarUrl: String = "",
    val createdBy: String = "",
    val typingUsers: List<String> = emptyList(),
    val backgroundUrl: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
)

/**
 * Embedded object for the last message in a chat.
 */
data class LastMessage(
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "text",
    val timestamp: Timestamp? = null,
    val unreadCount: Map<String, Long> = emptyMap(),
)

/**
 * Represents a message in a chat.
 * Maps to Firestore `chats/{chatId}/messages` subcollection.
 */
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val text: String = "",
    val type: String = "text", // text, image, file, voice, location, system
    val mediaUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val duration: Long = 0, // for voice messages in seconds
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "sent", // sent, delivered, seen
    val seenBy: List<String> = emptyList(),
    val replyTo: ReplyMessage? = null,
    @ServerTimestamp
    val timestamp: Timestamp? = null,
)

/**
 * Reference to a replied message.
 */
data class ReplyMessage(
    val messageId: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "text",
)

/**
 * Represents a friend request.
 * Maps to Firestore `friend_requests` collection.
 */
data class FriendRequest(
    @DocumentId
    val id: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromAvatarUrl: String = "",
    val toUserId: String = "",
    val toUsername: String = "",
    val status: String = "pending", // pending, accepted, rejected
    @ServerTimestamp
    val timestamp: Timestamp? = null,
)

/**
 * Represents a group with membership info.
 * Maps to Firestore `groups` collection.
 */
data class Group(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val description: String = "",
    val createdBy: String = "",
    val chatId: String = "",
    val members: List<GroupMember> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
)

/**
 * Represents a member in a group.
 */
data class GroupMember(
    val userId: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val role: String = "member", // admin, member
    val joinedAt: Timestamp? = null,
)

/**
 * Represents a call record.
 * Maps to Firestore `calls` collection.
 */
data class CallRecord(
    @DocumentId
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatarUrl: String = "",
    val receiverIds: List<String> = emptyList(),
    val type: String = "voice", // voice, video
    val status: String = "ringing", // ringing, ongoing, ended, missed
    val chatId: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
)
