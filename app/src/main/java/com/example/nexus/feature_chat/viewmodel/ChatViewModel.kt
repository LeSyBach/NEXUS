package com.example.nexus.feature_chat.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.core.utils.getFileInfo
import com.example.nexus.data.firebase.AudioPlayerHelper
import com.example.nexus.data.firebase.MediaUploader
import com.example.nexus.data.firebase.PlaybackState
import com.example.nexus.data.firebase.VoiceRecorderHelper
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Message
import com.example.nexus.data.model.User
import com.example.nexus.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val mediaUploader: MediaUploader,
    private val voiceRecorderHelper: VoiceRecorderHelper,
    val audioPlayerHelper: AudioPlayerHelper
) : ViewModel() {

    private val _chatsState = MutableStateFlow<Resource<List<Chat>>>(Resource.Idle)
    val chatsState: StateFlow<Resource<List<Chat>>> = _chatsState

    private val _messagesState = MutableStateFlow<Resource<List<Message>>>(Resource.Idle)
    val messagesState: StateFlow<Resource<List<Message>>> = _messagesState

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

    private var recordTimerJob: Job? = null
    private var amplitudeJob: Job? = null

    private val userCache = mutableMapOf<String, User>()
    private var typingJob: Job? = null

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
                    _messagesState.value = result
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

    suspend fun resolveDisplayName(chat: Chat): String {
        if (chat.type == "group") {
            return chat.groupName.ifEmpty { "Nhóm" }
        }
        val myId = currentUserId ?: return chat.groupName
        val otherId = chat.participants.firstOrNull { it != myId }
        if (otherId == null) return chat.groupName

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
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(chatId, text.trim())
            } catch (_: Exception) {}
        }
    }

    fun sendImageMessage(chatId: String, uri: Uri, context: Context) {
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

    fun setTypingStatus(chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            try {
                chatRepository.setTypingStatus(chatId, isTyping)
            } catch (_: Exception) {}
        }
    }

    fun startObservingTyping(chatId: String) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val chat = chatRepository.getChatById(chatId)
                    if (chat != null) {
                        _isTyping.value = chat.typingUsers.any { it != currentUserId }
                    }
                } catch (_: Exception) {}
                delay(3000)
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
    }

    fun loadSharedContentCounts(chatId: String) {
        viewModelScope.launch {
            try {
                _sharedContentCounts.value = chatRepository.getSharedContentCounts(chatId)
            } catch (_: Exception) {}
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
}
