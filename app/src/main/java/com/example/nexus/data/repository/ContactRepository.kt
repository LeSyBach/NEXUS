package com.example.nexus.data.repository

import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.firebase.NotificationService
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.FriendRequest
import com.example.nexus.data.model.User
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val authService: AuthService,
    private val notificationService: NotificationService
) {

    fun getCurrentUserId(): String? = authService.currentUserId

    // ── Search ──────────────────────────────────────────────────────
    suspend fun searchUsers(query: String): Resource<List<User>> {
        return try {
            val userId = getCurrentUserId() ?: return Resource.Error("Not logged in")
            val results = if (query.all { it.isDigit() }) {
                firestoreService.searchUsersByPhone(query)
            } else {
                firestoreService.searchUsersByUsername(query)
            }
            Resource.Success(results.filter { it.uid != userId })
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi khi tìm kiếm")
        }
    }

    // ── Send friend request ──────────────────────────────────────────
    suspend fun sendFriendRequest(toUserId: String): Resource<Unit> {
        return try {
            val fromUserId = getCurrentUserId() ?: return Resource.Error("Not logged in")

            val existing = firestoreService.checkExistingFriendRequest(fromUserId, toUserId)
            if (existing != null) {
                return Resource.Error("Đã gửi lời mời kết bạn rồi")
            }

            val reverse = firestoreService.checkExistingFriendRequest(toUserId, fromUserId)
            if (reverse != null) {
                return Resource.Error("Người này đã gửi lời mời kết bạn cho bạn rồi")
            }

            val currentUser = firestoreService.getUser(fromUserId)
            if (currentUser != null && toUserId in currentUser.friends) {
                return Resource.Error("Đã là bạn bè rồi")
            }

            val fromUser = currentUser
            val toUser   = firestoreService.getUser(toUserId)

            val request = FriendRequest(
                fromUserId   = fromUserId,
                fromUsername = fromUser?.username ?: "User",
                toUserId     = toUserId,
                toUsername   = toUser?.username ?: "User",
                status       = Constants.FRIEND_REQUEST_PENDING,
                timestamp    = Timestamp.now()
            )
            val requestId = firestoreService.sendFriendRequest(request)

            // Gửi push notification cho người nhận
            val displayName = fromUser?.displayName?.ifEmpty { fromUser.username } ?: fromUser?.username ?: "User"
            notificationService.sendFriendRequestNotification(
                receiverId = toUserId,
                senderName = displayName,
                senderId = fromUserId,
                requestId = requestId
            )

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi khi gửi lời mời")
        }
    }

    // ── Observe received friend requests (real-time) ─────────────────
    fun observeReceivedRequests(): Flow<Resource<List<FriendRequest>>> = flow {
        emit(Resource.Loading)
        val userId = getCurrentUserId()
        if (userId == null) {
            emit(Resource.Error("Not logged in"))
            return@flow
        }
        firestoreService.observeReceivedFriendRequests(userId).collect { requests ->
            emit(Resource.Success(requests))
        }
    }.catch { emit(Resource.Error(it.message ?: "Lỗi tải lời mời")) }

    // ── Accept / Reject request ──────────────────────────────────────
    suspend fun respondToRequest(
        requestId: String,
        accept: Boolean,
        fromUserId: String
    ): Resource<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Resource.Error("Not logged in")
            if (accept) {
                // acceptFriendRequest also auto-creates the Chat document
                firestoreService.acceptFriendRequest(requestId, fromUserId, currentUserId)
            } else {
                firestoreService.rejectFriendRequest(requestId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi phản hồi")
        }
    }

    // ── Friends list ─────────────────────────────────────────────────
    suspend fun getFriendsList(): Resource<List<User>> {
        return try {
            val userId = getCurrentUserId() ?: return Resource.Error("Not logged in")
            val currentUser = firestoreService.getUser(userId)
                ?: return Resource.Error("Không tìm thấy user")

            val friendsList = mutableListOf<User>()
            for (friendId in currentUser.friends) {
                val friend = firestoreService.getUser(friendId)
                if (friend != null) friendsList.add(friend)
            }
            Resource.Success(friendsList)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi tải danh bạ")
        }
    }

    // ── Get (or create) direct chat ──────────────────────────────────
    suspend fun getDirectChatId(friendId: String): String? {
        val userId = getCurrentUserId() ?: return null

        // Try to find existing direct chat
        val existing = firestoreService.findDirectChat(userId, friendId)
        if (existing != null) return existing.id

        // Create a new one if not found (handles old friend pairs accepted before the fix)
        val currentUser = firestoreService.getUser(userId)
        val friendUser  = firestoreService.getUser(friendId)
        val newChat = Chat(
            type         = Constants.CHAT_TYPE_DIRECT,
            participants = listOf(userId, friendId),
            groupName    = "${currentUser?.username ?: "User"} & ${friendUser?.username ?: "User"}",
            updatedAt    = Timestamp.now(),
            createdAt    = Timestamp.now()
        )
        return firestoreService.createChat(newChat)
    }

    // ── Observe sent requests ────────────────────────────────────────
    fun observeSentRequests(): Flow<Resource<List<FriendRequest>>> = flow {
        emit(Resource.Loading)
        val userId = getCurrentUserId()
        if (userId == null) {
            emit(Resource.Error("Not logged in"))
            return@flow
        }
        firestoreService.observeSentFriendRequests(userId).collect { requests ->
            emit(Resource.Success(requests))
        }
    }.catch { emit(Resource.Error(it.message ?: "Lỗi tải yêu cầu đã gửi")) }

    suspend fun getSentRequestTargetIds(): Set<String> {
        return try {
            val userId = getCurrentUserId() ?: return emptySet()
            firestoreService.getSentRequestTargetIds(userId)
        } catch (e: Exception) {
            emptySet()
        }
    }

    // ── Observe relationship with another user ─────────────────────
    fun observeRelationship(targetUserId: String): Flow<String> = flow {
        val userId = getCurrentUserId() ?: run {
            emit(Constants.RELATION_NONE)
            return@flow
        }

        // Observe current user's document for real-time relationship changes
        firestoreService.observeUser(userId).collect { currentUser ->
            if (currentUser == null) {
                emit(Constants.RELATION_NONE)
                return@collect
            }

            // Check blocked first
            if (targetUserId in currentUser.blockedUsers) {
                emit(Constants.RELATION_BLOCKED)
                return@collect
            }

            // Check friends
            if (targetUserId in currentUser.friends) {
                emit(Constants.RELATION_FRIENDS)
                return@collect
            }

            // Check friend requests in both directions
            val sentRequest = firestoreService.checkExistingFriendRequest(userId, targetUserId)
            if (sentRequest != null) {
                emit(Constants.RELATION_PENDING_SENT)
                return@collect
            }

            val receivedRequest = firestoreService.checkExistingFriendRequest(targetUserId, userId)
            if (receivedRequest != null) {
                emit(Constants.RELATION_PENDING_RECEIVED)
                return@collect
            }

            emit(Constants.RELATION_NONE)
        }
    }.catch { emit(Constants.RELATION_NONE) }

    // ── Block user ─────────────────────────────────────────────────
    suspend fun blockUser(targetUserId: String): Resource<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Resource.Error("Not logged in")
            // Remove from friends first if applicable
            val currentUser = firestoreService.getUser(userId)
            if (currentUser != null && targetUserId in currentUser.friends) {
                firestoreService.removeFriend(userId, targetUserId)
            }
            firestoreService.blockUser(userId, targetUserId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi khi chặn người dùng")
        }
    }

    // ── Unblock user ───────────────────────────────────────────────
    suspend fun unblockUser(targetUserId: String): Resource<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Resource.Error("Not logged in")
            firestoreService.unblockUser(userId, targetUserId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi khi bỏ chặn")
        }
    }

    // ── Unfriend ───────────────────────────────────────────────────
    suspend fun unfriend(targetUserId: String): Resource<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Resource.Error("Not logged in")
            firestoreService.removeFriend(userId, targetUserId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi khi hủy kết bạn")
        }
    }

    // ── Cancel sent friend request ─────────────────────────────────
    suspend fun cancelFriendRequest(targetUserId: String): Resource<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Resource.Error("Not logged in")
            firestoreService.cancelFriendRequest(userId, targetUserId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi khi thu hồi lời mời")
        }
    }
}
