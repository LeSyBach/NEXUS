package com.example.nexus.feature_chat.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.MuteManager
import com.example.nexus.core.utils.NetworkMonitor
import com.example.nexus.core.utils.Resource
import com.example.nexus.core.utils.getFileInfo
import com.example.nexus.data.firebase.AiChatService
import com.example.nexus.data.firebase.AudioPlayerHelper
import com.example.nexus.data.firebase.MediaUploader
import com.example.nexus.data.firebase.PlaybackState
import com.example.nexus.data.firebase.VoiceRecorderHelper
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Message
import com.google.firebase.Timestamp
import com.example.nexus.data.model.PinnedMessage
import com.example.nexus.data.model.ReplyMessage
import com.example.nexus.data.model.User
import com.example.nexus.data.repository.ChatRepository
import com.example.nexus.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UploadState {
    data object Idle : UploadState()
    data class Uploading(val progress: Float = 0f) : UploadState()
    data object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

sealed class VoiceRecordingState {
    data object Idle : VoiceRecordingState()
    data object Recording : VoiceRecordingState()
    data class Previewing(
        val localUri: Uri,
        val durationSec: Long
    ) : VoiceRecordingState()
}

sealed class AiSummaryState {
    data object Idle : AiSummaryState()
    data object Loading : AiSummaryState()
    data class Success(val summary: String) : AiSummaryState()
    data class Error(val message: String) : AiSummaryState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val mediaUploader: MediaUploader,
    private val voiceRecorderHelper: VoiceRecorderHelper,
    val audioPlayerHelper: AudioPlayerHelper,
    private val aiChatService: AiChatService,
    private val muteManager: MuteManager,
    private val chatPreferencesManager: com.example.nexus.core.utils.ChatPreferencesManager,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _chatsState = MutableStateFlow<Resource<List<Chat>>>(Resource.Idle)
    val chatsState: StateFlow<Resource<List<Chat>>> = _chatsState

    private val _messagesState = MutableStateFlow<Resource<List<Message>>>(Resource.Idle)
    val messagesState: StateFlow<Resource<List<Message>>> = _messagesState

    val isOffline: StateFlow<Boolean> = networkMonitor.isConnected
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat

    private val _otherUser = MutableStateFlow<User?>(null)
    val otherUser: StateFlow<User?> = _otherUser

    private val _onlineFriends = MutableStateFlow<List<User>>(emptyList())
    val onlineFriends: StateFlow<List<User>> = _onlineFriends

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _stories = MutableStateFlow<Map<String, com.example.nexus.data.model.Story>>(emptyMap())
    val stories: StateFlow<Map<String, com.example.nexus.data.model.Story>> = _stories

    private val _notes = MutableStateFlow<Map<String, com.example.nexus.data.model.Story>>(emptyMap())
    val notes: StateFlow<Map<String, com.example.nexus.data.model.Story>> = _notes

    private val _imageStories = MutableStateFlow<Map<String, List<com.example.nexus.data.model.Story>>>(emptyMap())
    val imageStories: StateFlow<Map<String, List<com.example.nexus.data.model.Story>>> = _imageStories

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    private val _sharedContentCounts = MutableStateFlow(Triple(0, 0, 0))
    val sharedContentCounts: StateFlow<Triple<Int, Int, Int>> = _sharedContentCounts

    private val _themeColor = MutableStateFlow("")
    val themeColor: StateFlow<String> = _themeColor

    private val _nicknames = MutableStateFlow<Map<String, String>>(emptyMap())
    val nicknames: StateFlow<Map<String, String>> = _nicknames

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _relationship = MutableStateFlow(Constants.RELATION_NONE)
    val relationship: StateFlow<String> = _relationship

    private val _blockResult = MutableSharedFlow<Boolean>()
    val blockResult = _blockResult.asSharedFlow()

    private val _clearChatSuccess = MutableSharedFlow<Boolean>()
    val clearChatSuccess = _clearChatSuccess.asSharedFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _pendingImageUri = MutableStateFlow<Uri?>(null)
    val pendingImageUri: StateFlow<Uri?> = _pendingImageUri

    private val _voiceState = MutableStateFlow<VoiceRecordingState>(VoiceRecordingState.Idle)
    val voiceState: StateFlow<VoiceRecordingState> = _voiceState

    private val _voiceRecordTimeSec = MutableStateFlow(0L)
    val voiceRecordTimeSec: StateFlow<Long> = _voiceRecordTimeSec

    private val _voiceAmplitudes = MutableStateFlow<List<Int>>(emptyList())
    val voiceAmplitudes: StateFlow<List<Int>> = _voiceAmplitudes

    private val _replyingToMessage = MutableStateFlow<Message?>(null)
    val replyingToMessage: StateFlow<Message?> = _replyingToMessage

    private val _aiSummaryState = MutableStateFlow<AiSummaryState>(AiSummaryState.Idle)
    val aiSummaryState: StateFlow<AiSummaryState> = _aiSummaryState

    private val _smartReplies = MutableStateFlow<List<String>>(emptyList())
    val smartReplies: StateFlow<List<String>> = _smartReplies

    private val _pinnedMessage = MutableStateFlow<PinnedMessage?>(null)
    val pinnedMessage: StateFlow<PinnedMessage?> = _pinnedMessage

    // Search in conversation
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<Int>>(emptyList())
    val searchResults: StateFlow<List<Int>> = _searchResults

    private val _pinnedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedChatIds: StateFlow<Set<String>> = _pinnedChatIds

    private val _currentSearchIndex = MutableStateFlow(-1)
    val currentSearchIndex: StateFlow<Int> = _currentSearchIndex

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    private val _showContactPicker = MutableStateFlow(false)
    val showContactPicker: StateFlow<Boolean> = _showContactPicker

    private var _olderMessages = MutableStateFlow<List<Message>>(emptyList())
    private var _isLoadingMoreMessages = MutableStateFlow(false)
    val isLoadingMoreMessages: StateFlow<Boolean> = _isLoadingMoreMessages
    private var _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages

    private var smartReplyJob: Job? = null
    private var lastProcessedMessageCount = 0

    private var recordTimerJob: Job? = null
    private var amplitudeJob: Job? = null

    private val userCache = mutableMapOf<String, User>()

    fun getDirectChatUsers(): List<User> {
        val myId = currentUserId ?: return emptyList()
        val chats = (_chatsState.value as? Resource.Success)?.data ?: return emptyList()
        return chats
            .filter { it.type == Constants.CHAT_TYPE_DIRECT }
            .mapNotNull { chat ->
                val otherId = chat.participants.firstOrNull { it != myId } ?: return@mapNotNull null
                userCache[otherId]
            }
    }

    data class ChatToShare(val chatId: String, val displayName: String, val avatarUrl: String?)

    fun getChatsForSharing(): List<ChatToShare> {
        val myId = currentUserId ?: return emptyList()
        val chats = (_chatsState.value as? Resource.Success)?.data ?: return emptyList()
        return chats
            .filter { it.type == Constants.CHAT_TYPE_DIRECT }
            .mapNotNull { chat ->
                val otherId = chat.participants.firstOrNull { it != myId } ?: return@mapNotNull null
                val user = userCache[otherId]
                val name = user?.displayName?.ifEmpty { user.username } ?: chat.groupName.ifEmpty { "Chat" }
                ChatToShare(chat.id, name, user?.avatarUrl?.ifEmpty { null })
            }
    }
    private var typingJob: Job? = null
    private var typingDebounceJob: Job? = null
    private var otherUserJob: Job? = null

    val currentUserId: String?
        get() = chatRepository.getCurrentUserId()

    init {
        loadChats()
        loadStories()
        currentUserId?.let { uid ->
            viewModelScope.launch {
                chatRepository.observeUser(uid).collect { _currentUser.value = it }
            }
        }
    }

    private fun loadStories() {
        viewModelScope.launch {
            chatRepository.observeAllActiveStories().collect { storyList ->
                val storyMap = mutableMapOf<String, com.example.nexus.data.model.Story>()
                val noteMap = mutableMapOf<String, com.example.nexus.data.model.Story>()
                val imageMap = mutableMapOf<String, MutableList<com.example.nexus.data.model.Story>>()
                for (story in storyList) {
                    // Keep most recent per user in main map
                    val existing = storyMap[story.userId]
                    if (existing == null || (story.createdAt?.toDate()?.time ?: 0) > (existing.createdAt?.toDate()?.time ?: 0)) {
                        storyMap[story.userId] = story
                    }
                    // Split by type
                    if (story.type == "image") {
                        imageMap.getOrPut(story.userId) { mutableListOf() }.add(story)
                    } else {
                        val existingNote = noteMap[story.userId]
                        if (existingNote == null || (story.createdAt?.toDate()?.time ?: 0) > (existingNote.createdAt?.toDate()?.time ?: 0)) {
                            noteMap[story.userId] = story
                        }
                    }
                }
                // Sort each user's image stories by time (newest first)
                for (entry in imageMap) {
                    entry.value.sortByDescending { it.createdAt?.toDate()?.time ?: 0 }
                }
                _stories.value = storyMap
                _notes.value = noteMap
                _imageStories.value = imageMap
            }
        }
    }

    fun postStory(content: String, type: String = "text") {
        viewModelScope.launch {
            chatRepository.createStory(content, type)
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            chatRepository.deleteStory(storyId)
        }
    }

    fun markStoryAsViewed(storyId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            chatRepository.markStoryAsViewed(storyId, uid)
        }
    }

    fun uploadAndPostStory(context: android.content.Context, uri: android.net.Uri, caption: String? = null) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                _uploadState.value = UploadState.Uploading()
                try {
                    val imageUrl = mediaUploader.upload(context, uri)
                    android.util.Log.d("ChatViewModel", "Upload result: imageUrl=$imageUrl")
                    if (imageUrl != null) {
                        val result = chatRepository.createStory(imageUrl, "image", caption)
                        android.util.Log.d("ChatViewModel", "CreateStory result: $result, userId=$currentUserId")
                        _uploadState.value = UploadState.Success
                    } else {
                        android.util.Log.e("ChatViewModel", "Image upload failed - returned null")
                        _uploadState.value = UploadState.Error("Upload failed")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "uploadAndPostStory error", e)
                    _uploadState.value = UploadState.Error(e.message ?: "Unknown error")
                } finally {
                    kotlinx.coroutines.delay(2000)
                    _uploadState.value = UploadState.Idle
                }
            }
        }
    }

    private fun loadChats() {
        viewModelScope.launch {
            try {
                chatRepository.observeChats().collect { result ->
                    _chatsState.value = result
                    if (result is Resource.Success) {
                        loadOnlineFriends(result.data)
                    }
                }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            currentUserId?.let { uid ->
                chatPreferencesManager.getPinnedChatIdsFlow(uid).collect { pinned ->
                    _pinnedChatIds.value = pinned
                }
            }
        }
    }

    fun toggleChatPin(chatId: String) {
        viewModelScope.launch {
            currentUserId?.let { uid ->
                chatPreferencesManager.toggleChatPin(uid, chatId)
            }
        }
    }

    private var userObserverJobs = mutableListOf<Job>()

    private fun loadOnlineFriends(chats: List<Chat>) {
        // Cancel previous observers
        userObserverJobs.forEach { it.cancel() }
        userObserverJobs.clear()

        viewModelScope.launch {
            try {
                val myId = currentUserId ?: return@launch
                val otherIds = chats
                    .filter { it.type == Constants.CHAT_TYPE_DIRECT }
                    .mapNotNull { it.participants.firstOrNull { id -> id != myId } }
                    .distinct()

                // Observe each participant in real-time
                for (otherId in otherIds) {
                    val job = launch {
                        chatRepository.observeUser(otherId).collect { user ->
                            if (user != null) {
                                userCache[otherId] = user
                                // Refresh online friends list
                                refreshOnlineFriends()
                            }
                        }
                    }
                    userObserverJobs.add(job)
                }
            } catch (_: Exception) {}
        }
    }

    private fun refreshOnlineFriends() {
        val myId = currentUserId ?: return
        val now = System.currentTimeMillis()
        val recentFriends = userCache.values.filter { user ->
            if (user.uid == myId) return@filter false
            if (user.status == Constants.USER_STATUS_ONLINE) return@filter true
            val lastSeen = user.lastSeen?.toDate()?.time ?: 0L
            val hoursSinceLastSeen = (now - lastSeen) / (1000 * 60 * 60)
            hoursSinceLastSeen < 24
        }.sortedByDescending { it.lastSeen }
        _onlineFriends.value = recentFriends
    }

    fun loadMessages(chatId: String) {
        _messagesState.value = Resource.Loading
        _otherUser.value = null
        _currentChat.value = null

        observeChatRealtime(chatId)

        viewModelScope.launch {
            try {
                val chat = chatRepository.getChatById(chatId)
                _currentChat.value = chat
                if (chat != null) {
                    val myId = currentUserId
                    val otherId = chat.participants.firstOrNull { it != myId }
                    if (otherId != null) {
                        val user = try { chatRepository.getUserById(otherId) } catch (_: Exception) { null }
                        _otherUser.value = user
                        if (user != null) userCache[otherId] = user

                        // Observe otherUser in real-time for status changes
                        observeOtherUserRealtime(otherId)
                        observeRelationship(otherId)
                    }
                }
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            try {
                chatRepository.observeMessages(chatId).collect { result ->
                    if (result is Resource.Success) {
                        val older = _olderMessages.value
                        if (older.isEmpty()) {
                            _messagesState.value = result
                        } else {
                            val combined = (older + result.data)
                                .distinctBy { it.id }
                                .sortedByDescending { it.timestamp }
                            _messagesState.value = Resource.Success(combined)
                        }
                        val messages = (_messagesState.value as? Resource.Success)?.data ?: emptyList()
                        if (messages.isNotEmpty() && messages.size > lastProcessedMessageCount) {
                            val latest = messages.first() // DESC order
                            if (latest.senderId != currentUserId && lastProcessedMessageCount > 0) {
                                requestSmartReplies()
                            }
                        }
                        lastProcessedMessageCount = messages.size
                    } else {
                        _messagesState.value = result
                    }
                }
            } catch (_: Exception) {
                _messagesState.value = Resource.Error("Lỗi tải tin nhắn")
            }
        }
    }

    fun loadOtherUser(chat: Chat) {
        val myId = currentUserId ?: return
        val otherId = chat.participants.firstOrNull { it != myId } ?: return
        viewModelScope.launch {
            try {
                val user = chatRepository.getUserById(otherId)
                _otherUser.value = user
                if (user != null) userCache[otherId] = user
            } catch (_: Exception) {}
        }
    }

    fun loadOtherUserByChatId(chatId: String) {
        viewModelScope.launch {
            try {
                val chat = chatRepository.getChatById(chatId)
                if (chat != null) loadOtherUser(chat)
            } catch (_: Exception) {}
        }
    }

    fun loadMoreMessages(chatId: String) {
        val currentMessages = (_messagesState.value as? Resource.Success)?.data
        if (currentMessages.isNullOrEmpty()) {
            android.util.Log.d("PAGINATION_VM", "No current messages, skipping")
            return
        }
        if (_isLoadingMoreMessages.value) {
            android.util.Log.d("PAGINATION_VM", "Already loading, skipping")
            return
        }
        if (!_hasMoreMessages.value) {
            android.util.Log.d("PAGINATION_VM", "No more messages, skipping")
            return
        }

        // Get the timestamp of the oldest message (last in DESC list)
        val lastTimestamp = currentMessages.lastOrNull()?.timestamp ?: return
        android.util.Log.d("PAGINATION_VM", "Loading more messages, currentCount=${currentMessages.size}, lastTimestamp=$lastTimestamp")

        _isLoadingMoreMessages.value = true
        viewModelScope.launch {
            try {
                val older = chatRepository.loadMoreMessages(chatId, lastTimestamp)
                android.util.Log.d("PAGINATION_VM", "Loaded ${older.size} older messages")
                if (older.size < Constants.MESSAGES_PAGE_SIZE) {
                    _hasMoreMessages.value = false
                }
                if (older.isNotEmpty()) {
                    _olderMessages.value = _olderMessages.value + older
                    val listenerData = (_messagesState.value as? Resource.Success)?.data ?: emptyList()
                    val combined = (_olderMessages.value + listenerData)
                        .distinctBy { it.id }
                        .sortedByDescending { it.timestamp }
                    _messagesState.value = Resource.Success(combined)
                } else {
                    _hasMoreMessages.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("PAGINATION_VM", "Error loading more", e)
            } finally {
                _isLoadingMoreMessages.value = false
            }
        }
    }

    suspend fun getUserById(userId: String): User? {
        return chatRepository.getUserById(userId)
    }

    suspend fun resolveDisplayName(chat: Chat): String {
        if (chat.type == "group") {
            return chat.groupName.ifEmpty { "Nhóm" }
        }
        val myId = currentUserId ?: return chat.groupName
        val otherId = chat.participants.firstOrNull { it != myId }
        if (otherId == null) return chat.groupName

        // Check nickname set by current user for the other person
        val nickname = chat.nicknames[otherId]
        if (!nickname.isNullOrBlank()) return nickname

        userCache[otherId]?.let { user ->
            return user.displayName.ifEmpty { user.username }
        }

        return try {
            val user = chatRepository.getUserById(otherId)
            if (user != null) {
                userCache[otherId] = user
                user.displayName.ifEmpty { user.username }
            } else chat.groupName.ifEmpty { "Cuộc trò chuyện" }
        } catch (_: Exception) {
            chat.groupName.ifEmpty { "Cuộc trò chuyện" }
        }
    }

    fun resolveAvatarUrl(chat: Chat): String? {
        if (chat.type == "group") return chat.groupAvatarUrl.ifEmpty { null }
        val myId = currentUserId ?: return null
        val otherId = chat.participants.firstOrNull { it != myId } ?: return null
        return userCache[otherId]?.avatarUrl?.ifEmpty { null }
    }

    fun isUserOnline(chat: Chat): Boolean {
        if (chat.type == "group") return false
        val myId = currentUserId ?: return false
        val otherId = chat.participants.firstOrNull { it != myId } ?: return false
        return userCache[otherId]?.status == Constants.USER_STATUS_ONLINE
    }

    fun sendMessage(chatId: String, text: String, mentions: List<String> = emptyList()) {
        if (text.isBlank()) return
        val reply = _replyingToMessage.value?.let { msg ->
            val previewText = when (msg.type) {
                Constants.MESSAGE_TYPE_IMAGE -> "📷 Hình ảnh"
                Constants.MESSAGE_TYPE_VOICE -> "🎤 Tin nhắn thoại"
                Constants.MESSAGE_TYPE_FILE -> "📎 ${msg.fileName.ifEmpty { "Tệp" }}"
                Constants.MESSAGE_TYPE_CONTACT -> "👤 ${msg.contactName.ifEmpty { "Liên hệ" }}"
                else -> msg.text
            }
            ReplyMessage(
                messageId = msg.id,
                text = previewText,
                senderId = msg.senderId,
                senderName = msg.senderName,
                type = msg.type
            )
        }
        _replyingToMessage.value = null
        stopTyping(chatId)

        viewModelScope.launch {
            try {
                chatRepository.sendMessage(chatId, text.trim(), replyTo = reply, mentions = mentions)
            } catch (_: Exception) {}
        }
    }

    fun setReplyingMessage(message: Message?) {
        _replyingToMessage.value = message
    }

    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        viewModelScope.launch {
            try {
                chatRepository.toggleReaction(chatId, messageId, emoji)
            } catch (_: Exception) {}
        }
    }

    suspend fun forwardMessage(targetChatId: String, message: Message): Resource<Unit> {
        return chatRepository.forwardMessage(targetChatId, message)
    }

    suspend fun getUsersByIds(userIds: List<String>): List<User> {
        return userIds.mapNotNull { id ->
            userCache[id] ?: try {
                val user = chatRepository.getUserById(id)
                if (user != null) userCache[id] = user
                user
            } catch (_: Exception) { null }
        }
    }

    fun sendImageMessage(chatId: String, uri: Uri, context: Context) {
        stopTyping(chatId)
        _pendingImageUri.value = uri
        _uploadState.value = UploadState.Uploading()

        viewModelScope.launch {
            try {
                val imageUrl = mediaUploader.upload(context, uri)
                if (imageUrl != null) {
                    chatRepository.sendImageMessage(chatId, imageUrl)
                    _uploadState.value = UploadState.Success
                } else {
                    _uploadState.value = UploadState.Error("Tải ảnh lên thất bại")
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Lỗi tải ảnh")
            } finally {
                _pendingImageUri.value = null
                delay(500)
                _uploadState.value = UploadState.Idle
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
        _pendingImageUri.value = null
    }

    fun sendMediaMessage(chatId: String, uri: Uri, context: Context) {
        stopTyping(chatId)
        val mimeType = mediaUploader.getMimeType(context, uri) ?: ""
        val isVideo = mimeType.startsWith("video")

        _pendingImageUri.value = uri
        _uploadState.value = UploadState.Uploading()

        viewModelScope.launch {
            try {
                val resourceType = if (isVideo) "video" else "image"
                val mediaUrl = mediaUploader.upload(context, uri, resourceType = resourceType)
                if (mediaUrl != null) {
                    if (isVideo) {
                        chatRepository.sendVideoMessage(chatId, mediaUrl)
                    } else {
                        chatRepository.sendImageMessage(chatId, mediaUrl)
                    }
                    _uploadState.value = UploadState.Success
                } else {
                    _uploadState.value = UploadState.Error("Tải media thất bại")
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Lỗi tải media")
            } finally {
                _pendingImageUri.value = null
                delay(500)
                _uploadState.value = UploadState.Idle
            }
        }
    }

    fun startVoiceRecording(context: Context) {
        voiceRecorderHelper.startRecording(context)
        _voiceState.value = VoiceRecordingState.Recording
        _voiceRecordTimeSec.value = 0L
        _voiceAmplitudes.value = emptyList()
        recordTimerJob?.cancel()
        recordTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _voiceRecordTimeSec.value++
            }
        }
        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch {
            while (isActive) {
                delay(80)
                val amp = voiceRecorderHelper.getMaxAmplitude()
                val normalized = (amp / 32767f * 100).toInt().coerceIn(0, 100)
                val current = _voiceAmplitudes.value.toMutableList()
                current.add(normalized)
                if (current.size > 30) current.removeAt(0)
                _voiceAmplitudes.value = current
            }
        }
    }

    fun stopVoiceRecording() {
        recordTimerJob?.cancel()
        recordTimerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null
        val result = voiceRecorderHelper.stopRecording() ?: run {
            _voiceState.value = VoiceRecordingState.Idle
            return
        }
        val (audioUri, durationSec) = result
        _voiceState.value = VoiceRecordingState.Previewing(localUri = audioUri, durationSec = durationSec)
    }

    fun toggleVoicePreview(context: Context) {
        val state = _voiceState.value
        if (state is VoiceRecordingState.Previewing) {
            val playerState = audioPlayerHelper.state.value
            if (!playerState.isPlaying && playerState.currentPositionMs == 0L) {
                audioPlayerHelper.load(context, state.localUri)
            }
            audioPlayerHelper.togglePlayPause()
        }
    }

    fun seekVoicePreview(fraction: Float) {
        audioPlayerHelper.seekTo(fraction)
    }

    fun cancelVoicePreview() {
        audioPlayerHelper.stop()
        audioPlayerHelper.release()
        val state = _voiceState.value
        if (state is VoiceRecordingState.Previewing) {
            val file = java.io.File(state.localUri.path ?: "")
            file.delete()
        }
        _voiceState.value = VoiceRecordingState.Idle
        _voiceRecordTimeSec.value = 0L
        _voiceAmplitudes.value = emptyList()
    }

    fun reRecordVoice(context: Context) {
        audioPlayerHelper.stop()
        audioPlayerHelper.release()
        val state = _voiceState.value
        if (state is VoiceRecordingState.Previewing) {
            val file = java.io.File(state.localUri.path ?: "")
            file.delete()
        }
        _voiceState.value = VoiceRecordingState.Idle
        startVoiceRecording(context)
    }

    fun sendVoicePreview(chatId: String, context: Context) {
        stopTyping(chatId)
        val state = _voiceState.value
        if (state !is VoiceRecordingState.Previewing) return

        audioPlayerHelper.stop()
        audioPlayerHelper.release()

        _uploadState.value = UploadState.Uploading()
        viewModelScope.launch {
            try {
                val voiceUrl = mediaUploader.upload(context, state.localUri, resourceType = "video")
                if (voiceUrl != null) {
                    chatRepository.sendVoiceMessage(chatId, voiceUrl, state.durationSec)
                    _uploadState.value = UploadState.Success
                    val file = java.io.File(state.localUri.path ?: "")
                    file.delete()
                } else {
                    _uploadState.value = UploadState.Error("Tải voice lên thất bại")
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Lỗi tải voice")
            } finally {
                _voiceState.value = VoiceRecordingState.Idle
                _voiceRecordTimeSec.value = 0L
                delay(500)
                _uploadState.value = UploadState.Idle
            }
        }
    }

    fun sendVoiceDirectly(chatId: String, context: Context) {
        stopTyping(chatId)
        recordTimerJob?.cancel()
        recordTimerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null
        val result = voiceRecorderHelper.stopRecording() ?: run {
            _voiceState.value = VoiceRecordingState.Idle
            return
        }
        val (audioUri, durationSec) = result
        _voiceState.value = VoiceRecordingState.Idle
        _voiceRecordTimeSec.value = 0L
        _voiceAmplitudes.value = emptyList()

        _uploadState.value = UploadState.Uploading()
        viewModelScope.launch {
            try {
                val voiceUrl = mediaUploader.upload(context, audioUri, resourceType = "video")
                if (voiceUrl != null) {
                    chatRepository.sendVoiceMessage(chatId, voiceUrl, durationSec)
                    _uploadState.value = UploadState.Success
                    val file = java.io.File(audioUri.path ?: "")
                    file.delete()
                } else {
                    _uploadState.value = UploadState.Error("Tải voice lên thất bại")
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Lỗi tải voice")
            } finally {
                delay(500)
                _uploadState.value = UploadState.Idle
            }
        }
    }

    fun sendFileMessage(chatId: String, uri: Uri, context: Context) {
        stopTyping(chatId)
        val fileInfo = context.getFileInfo(uri) ?: return
        if (fileInfo.fileSizeBytes > Constants.MAX_FILE_SIZE_MB * 1024 * 1024) {
            _uploadState.value = UploadState.Error("File quá lớn (tối đa ${Constants.MAX_FILE_SIZE_MB}MB)")
            return
        }

        _uploadState.value = UploadState.Uploading()
        viewModelScope.launch {
            try {
                val fileUrl = mediaUploader.upload(context, uri, resourceType = "raw")
                if (fileUrl != null) {
                    chatRepository.sendFileMessage(chatId, fileUrl, fileInfo.fileName, fileInfo.fileSizeBytes)
                    _uploadState.value = UploadState.Success
                } else {
                    _uploadState.value = UploadState.Error("Tải file lên thất bại")
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Lỗi tải file")
            } finally {
                delay(500)
                _uploadState.value = UploadState.Idle
            }
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(chatId, messageId)
            } catch (_: Exception) {}
        }
    }

    fun recallMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.recallMessage(chatId, messageId)
            } catch (_: Exception) {}
        }
    }

    fun markMessagesAsSeen(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsSeen(chatId)
            } catch (_: Exception) {}
        }
    }

    fun startTyping(chatId: String) {
        // Send typing=true once
        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch {
            try {
                chatRepository.setTypingStatus(chatId, true)
            } catch (_: Exception) {}
        }
        // Auto-stop after 3 seconds of inactivity
        typingDebounceJob = viewModelScope.launch {
            delay(3000L)
            try {
                chatRepository.setTypingStatus(chatId, false)
            } catch (_: Exception) {}
        }
    }

    fun stopTyping(chatId: String) {
        typingDebounceJob?.cancel()
        typingDebounceJob = null
        viewModelScope.launch {
            try {
                chatRepository.setTypingStatus(chatId, false)
            } catch (_: Exception) {}
        }
    }

    fun startObservingTyping(chatId: String) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            chatRepository.observeTypingUsers(chatId).collect { typingUsers ->
                _isTyping.value = typingUsers.any { it != currentUserId }
            }
        }
    }

    fun stopObservingTyping() {
        typingJob?.cancel()
        typingJob = null
        _isTyping.value = false
    }

    fun clearConversationState() {
        stopObservingTyping()
        otherUserJob?.cancel()
        otherUserJob = null
        relationshipJob?.cancel()
        relationshipJob = null
        cancelVoicePreview()
        _currentChat.value = null
        _otherUser.value = null
        _messagesState.value = Resource.Idle
        _isTyping.value = false
        _replyingToMessage.value = null
        _aiSummaryState.value = AiSummaryState.Idle
        _smartReplies.value = emptyList()
        smartReplyJob?.cancel()
        lastProcessedMessageCount = 0
        _olderMessages.value = emptyList()
        _isLoadingMoreMessages.value = false
        _hasMoreMessages.value = true
        _themeColor.value = ""
        _nicknames.value = emptyMap()
        _isMuted.value = false
        _pinnedMessage.value = null
        _relationship.value = Constants.RELATION_NONE
        clearSearch()
        _showContactPicker.value = false
    }

    fun loadSharedContentCounts(chatId: String) {
        viewModelScope.launch {
            try {
                _sharedContentCounts.value = chatRepository.getSharedContentCounts(chatId)
            } catch (_: Exception) {}
        }
    }

    fun updateChatTheme(chatId: String, color: String) {
        viewModelScope.launch {
            try {
                chatRepository.updateChatTheme(chatId, color)
            } catch (_: Exception) {}
        }
    }

    fun updateChatNickname(chatId: String, targetUserId: String, nickname: String) {
        viewModelScope.launch {
            try {
                if (nickname.isBlank()) {
                    chatRepository.removeChatNickname(chatId, targetUserId)
                } else {
                    chatRepository.setChatNickname(chatId, targetUserId, nickname)
                }
            } catch (_: Exception) {}
        }
    }

    fun setMuted(chatId: String, muted: Boolean) {
        viewModelScope.launch {
            try {
                muteManager.setMuted(chatId, muted)
            } catch (_: Exception) {}
        }
    }

    fun observeChatRealtime(chatId: String) {
        viewModelScope.launch {
            chatRepository.observeChat(chatId).collect { chat ->
                if (chat != null) {
                    _currentChat.value = chat
                    _themeColor.value = chat.themeColor
                    _nicknames.value = chat.nicknames
                    _pinnedMessage.value = chat.pinnedMessage
                }
            }
        }
        viewModelScope.launch {
            muteManager.isMutedFlow(chatId).collect { muted ->
                _isMuted.value = muted
            }
        }
    }

    private fun observeOtherUserRealtime(userId: String) {
        otherUserJob?.cancel()
        otherUserJob = viewModelScope.launch {
            chatRepository.observeUser(userId).collect { user ->
                if (user != null) {
                    _otherUser.value = user
                    userCache[userId] = user
                }
            }
        }
    }

    fun clearChatMessages(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.clearChatMessages(chatId)
                _clearChatSuccess.emit(true)
            } catch (_: Exception) {
                _clearChatSuccess.emit(false)
            }
        }
    }

    fun pinMessage(chatId: String, message: Message) {
        viewModelScope.launch {
            try {
                chatRepository.pinMessage(chatId, message)
            } catch (_: Exception) {}
        }
    }

    fun unpinMessage(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.unpinMessage(chatId)
            } catch (_: Exception) {}
        }
    }

    // ── AI Features ──

    fun summarizeMessages() {
        val messages = (_messagesState.value as? Resource.Success)?.data
        if (messages.isNullOrEmpty()) return

        _aiSummaryState.value = AiSummaryState.Loading
        viewModelScope.launch {
            try {
                val summary = aiChatService.summarizeMessages(messages, currentUserId)
                _aiSummaryState.value = AiSummaryState.Success(summary)
            } catch (e: Exception) {
                _aiSummaryState.value = AiSummaryState.Error(
                    e.message ?: "Không thể tạo tóm tắt"
                )
            }
        }
    }

    fun dismissSummary() {
        _aiSummaryState.value = AiSummaryState.Idle
    }

    fun requestSmartReplies() {
        smartReplyJob?.cancel()
        smartReplyJob = viewModelScope.launch {
            delay(500)
            val messages = (_messagesState.value as? Resource.Success)?.data
            if (messages.isNullOrEmpty()) {
                _smartReplies.value = emptyList()
                return@launch
            }
            val latestMessage = messages.first() // DESC order
            if (latestMessage.senderId == currentUserId) {
                _smartReplies.value = emptyList()
                return@launch
            }
            try {
                val replies = aiChatService.getSmartReplies(messages, currentUserId)
                _smartReplies.value = replies
            } catch (_: Exception) {
                _smartReplies.value = emptyList()
            }
        }
    }

    fun dismissSmartReplies() {
        smartReplyJob?.cancel()
        _smartReplies.value = emptyList()
    }

    // ── Block / Unblock ──

    fun blockUser(targetUserId: String) {
        viewModelScope.launch {
            val result = contactRepository.blockUser(targetUserId)
            _blockResult.emit(result is Resource.Success)
        }
    }

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            val result = contactRepository.unblockUser(targetUserId)
            _blockResult.emit(result is Resource.Success)
        }
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            try { chatRepository.archiveChat(chatId) } catch (_: Exception) {}
        }
    }

    fun unarchiveChat(chatId: String) {
        viewModelScope.launch {
            try { chatRepository.unarchiveChat(chatId) } catch (_: Exception) {}
        }
    }

    fun sendContactMessage(
        chatId: String,
        contactUserId: String,
        contactName: String,
        contactPhone: String,
        contactAvatarUrl: String
    ) {
        viewModelScope.launch {
            try {
                chatRepository.sendContactMessage(chatId, contactUserId, contactName, contactPhone, contactAvatarUrl)
            } catch (_: Exception) {}
        }
    }

    private var relationshipJob: Job? = null

    private fun observeRelationship(targetUserId: String) {
        relationshipJob?.cancel()
        relationshipJob = viewModelScope.launch {
            contactRepository.observeRelationship(targetUserId).collect { relation ->
                _relationship.value = relation
            }
        }
    }

    // ── Search in conversation ──

    fun startSearch() {
        _isSearchActive.value = true
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _currentSearchIndex.value = -1
            return
        }
        val messages = (_messagesState.value as? Resource.Success)?.data ?: emptyList()
        val matchingIndices = messages.indices.filter { index ->
            messages[index].text.contains(query, ignoreCase = true)
        }
        _searchResults.value = matchingIndices
        _currentSearchIndex.value = if (matchingIndices.isNotEmpty()) 0 else -1
    }

    fun navigateToNextResult() {
        val results = _searchResults.value
        if (results.isEmpty()) return
        _currentSearchIndex.value = (_currentSearchIndex.value + 1) % results.size
    }

    fun navigateToPreviousResult() {
        val results = _searchResults.value
        if (results.isEmpty()) return
        _currentSearchIndex.value = if (_currentSearchIndex.value <= 0) results.size - 1 else _currentSearchIndex.value - 1
    }

    fun clearSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _currentSearchIndex.value = -1
    }

    // ── Contact Picker ──

    fun openContactPicker() {
        _showContactPicker.value = true
    }

    fun dismissContactPicker() {
        _showContactPicker.value = false
    }
}
