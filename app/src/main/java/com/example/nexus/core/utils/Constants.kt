package com.example.nexus.core.utils

/**
 * App-wide constants for NEXUS.
 */
object Constants {
    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CHATS = "chats"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_FRIEND_REQUESTS = "friend_requests"
    const val COLLECTION_GROUPS = "groups"
    const val COLLECTION_CALLS = "calls"

    // Firebase Storage Paths
    const val STORAGE_AVATARS = "avatars"
    const val STORAGE_CHAT_IMAGES = "chat_images"
    const val STORAGE_CHAT_FILES = "chat_files"
    const val STORAGE_VOICE_MESSAGES = "voice_messages"

    // Chat Types
    const val CHAT_TYPE_DIRECT = "direct"
    const val CHAT_TYPE_GROUP = "group"

    // Message Types
    const val MESSAGE_TYPE_TEXT = "text"
    const val MESSAGE_TYPE_IMAGE = "image"
    const val MESSAGE_TYPE_FILE = "file"
    const val MESSAGE_TYPE_VOICE = "voice"
    const val MESSAGE_TYPE_VIDEO = "video"
    const val MESSAGE_TYPE_LOCATION = "location"
    const val MESSAGE_TYPE_SYSTEM = "system"
    const val MESSAGE_TYPE_CALL = "call"

    // Message Status
    const val MESSAGE_STATUS_SENT = "sent"
    const val MESSAGE_STATUS_DELIVERED = "delivered"
    const val MESSAGE_STATUS_SEEN = "seen"

    // Friend Request Status
    const val FRIEND_REQUEST_PENDING = "pending"
    const val FRIEND_REQUEST_ACCEPTED = "accepted"
    const val FRIEND_REQUEST_REJECTED = "rejected"

    // Call Types
    const val CALL_TYPE_VOICE = "voice"
    const val CALL_TYPE_VIDEO = "video"

    // Call Status
    const val CALL_STATUS_RINGING = "ringing"
    const val CALL_STATUS_ONGOING = "ongoing"
    const val CALL_STATUS_ENDED = "ended"
    const val CALL_STATUS_MISSED = "missed"

    // User Status
    const val USER_STATUS_ONLINE = "online"
    const val USER_STATUS_OFFLINE = "offline"

    // Group Roles
    const val ROLE_ADMIN = "admin"
    const val ROLE_MEMBER = "member"

    // DataStore Keys
    const val DATASTORE_SETTINGS = "nexus_settings"
    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_NOTIFICATION_ENABLED = "notification_enabled"

    // Limits
    const val MAX_GROUP_MEMBERS = 256
    const val MAX_MESSAGE_LENGTH = 5000
    const val MAX_USERNAME_LENGTH = 30
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_FILE_SIZE_MB = 100
    const val MESSAGES_PAGE_SIZE = 50
}
