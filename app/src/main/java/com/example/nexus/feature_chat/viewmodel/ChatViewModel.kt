package com.example.nexus.feature_chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
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

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
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
