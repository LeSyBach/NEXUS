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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    /**
     * Observes the current user's friends list in real-time.
     * When any friend's status changes (online/offline), the list is re-emitted.
     */
    fun observeFriendsList(): Flow<Resource<List<User>>> {
        val userId = getCurrentUserId()
        if (userId == null) return flow { emit(Resource.Error("Not logged in")) }

        return firestoreService.observeUser(userId).flatMapLatest { currentUser ->
            if (currentUser == null || currentUser.friends.isEmpty()) {
                flowOf(Resource.Success(emptyList()))
            } else {
                combineUserFlows(currentUser.friends.map { firestoreService.observeUser(it) })
            }
        }.catch { emit(Resource.Error(it.message ?: "Lỗi tải danh bạ")) }
    }

    /**
     * Combines a dynamic list of Flow<User?> into Flow<Resource<List<User>>>.
     * Uses channelFlow to safely emit from multiple coroutines.
     */
    private fun combineUserFlows(flows: List<Flow<User?>>): Flow<Resource<List<User>>> = channelFlow {
        if (flows.isEmpty()) {
            send(Resource.Success(emptyList()))
            return@channelFlow
        }
        val results = arrayOfNulls<User>(flows.size)
        flows.forEachIndexed { index, flow ->
            launch {
                flow.collect { user ->
                    results[index] = user
                    send(Resource.Success(results.filterNotNull().toList()))
                }
            }
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
    fun observeRelationship(targetUserId: String): Flow<String> {
        val userId = getCurrentUserId()
            ?: return flow { emit(Constants.RELATION_NONE) }

        return channelFlow {
            var currentUser: User? = null
            var hasPendingSent = false
            var hasPendingReceived = false

            fun emitRelation() {
                val user = currentUser ?: run {
                    trySend(Constants.RELATION_NONE); return
                }
                trySend(when {
                    targetUserId in user.blockedUsers -> Constants.RELATION_BLOCKED
                    targetUserId in user.friends -> Constants.RELATION_FRIENDS
                    hasPendingSent -> Constants.RELATION_PENDING_SENT
                    hasPendingReceived -> Constants.RELATION_PENDING_RECEIVED
                    else -> Constants.RELATION_NONE
                })
            }

            // 1. Observe current user document (friends, blocked)
            launch {
                firestoreService.observeUser(userId).collect { user ->
                    currentUser = user
                    emitRelation()
                }
            }

            // 2. Observe sent requests from me → target (real-time)
            launch {
                firestoreService.observeFriendRequestExists(userId, targetUserId).collect { exists ->
                    hasPendingSent = exists
                    emitRelation()
                }
            }

            // 3. Observe received requests from target → me (real-time)
            launch {
                firestoreService.observeFriendRequestExists(targetUserId, userId).collect { exists ->
                    hasPendingReceived = exists
                    emitRelation()
                }
            }
        }.catch { emit(Constants.RELATION_NONE) }
    }

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
