package com.example.nexus.feature_chat.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.nexus.data.model.ReplyMessage
import com.example.nexus.data.model.User
import com.example.nexus.data.repository.ChatRepository
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
    private val mediaUploader: MediaUploader,
    private val voiceRecorderHelper: VoiceRecorderHelper,
    val audioPlayerHelper: AudioPlayerHelper,
    private val aiChatService: AiChatService,
    private val muteManager: MuteManager,
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
    private var typingJob: Job? = null
    private var typingDebounceJob: Job? = null

    val currentUserId: String?
        get() = chatRepository.getCurrentUserId()

    init {
        loadChats()
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
    }

    private fun loadOnlineFriends(chats: List<Chat>) {
        viewModelScope.launch {
            try {
                val myId = currentUserId ?: return@launch
                val online = mutableListOf<User>()
                for (chat in chats) {
                    if (chat.type != Constants.CHAT_TYPE_DIRECT) continue
                    val otherId = chat.participants.firstOrNull { it != myId } ?: continue
                    val cached = userCache[otherId]
                    val user = cached ?: try { chatRepository.getUserById(otherId) } catch (_: Exception) { null }
                    if (user != null) {
                        userCache[otherId] = user
                        if (user.status == Constants.USER_STATUS_ONLINE) {
                            online.add(user)
                        }
                    }
                }
                _onlineFriends.value = online
            } catch (_: Exception) {}
        }
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

    suspend fun resolveDisplayName(chat: Chat): String {
        if (chat.type == "group") {
            return chat.groupName.ifEmpty { "Nhóm" }
        }
        val myId = currentUserId ?: return chat.groupName
        val otherId = chat.participants.firstOrNull { it != myId }
        if (otherId == null) return chat.groupName

        // Check nickname set by current user for the other person
        val nickname = chat.nicknames[otherId] ?: chat.nicknames[myId]
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

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        val reply = _replyingToMessage.value?.let { msg ->
            val previewText = when (msg.type) {
                Constants.MESSAGE_TYPE_IMAGE -> "📷 Hình ảnh"
                Constants.MESSAGE_TYPE_VOICE -> "🎤 Tin nhắn thoại"
                Constants.MESSAGE_TYPE_FILE -> "📎 ${msg.fileName.ifEmpty { "Tệp" }}"
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
                chatRepository.sendMessage(chatId, text.trim(), replyTo = reply)
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
                val current = _nicknames.value.toMutableMap()
                if (nickname.isBlank()) current.remove(targetUserId)
                else current[targetUserId] = nickname
                chatRepository.updateChatNicknames(chatId, current)
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
                }
            }
        }
        viewModelScope.launch {
            muteManager.isMutedFlow(chatId).collect { muted ->
                _isMuted.value = muted
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
}
