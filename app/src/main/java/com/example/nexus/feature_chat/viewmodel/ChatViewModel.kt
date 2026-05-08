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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val userCache = mutableMapOf<String, User>()

    val currentUserId: String?
        get() = chatRepository.getCurrentUserId()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            chatRepository.observeChats().collect { result ->
                _chatsState.value = result
                if (result is Resource.Success) {
                    loadOnlineFriends(result.data)
                }
            }
        }
    }

    private fun loadOnlineFriends(chats: List<Chat>) {
        viewModelScope.launch {
            val myId = currentUserId ?: return@launch
            val online = mutableListOf<User>()
            for (chat in chats) {
                if (chat.type != Constants.CHAT_TYPE_DIRECT) continue
                val otherId = chat.participants.firstOrNull { it != myId } ?: continue
                val cached = userCache[otherId]
                val user = cached ?: chatRepository.getUserById(otherId)
                if (user != null) {
                    userCache[otherId] = user
                    if (user.status == Constants.USER_STATUS_ONLINE) {
                        online.add(user)
                    }
                }
            }
            _onlineFriends.value = online
        }
    }

    fun loadMessages(chatId: String) {
        _messagesState.value = Resource.Loading
        _otherUser.value = null
        viewModelScope.launch {
            val chat = chatRepository.getChatById(chatId)
            _currentChat.value = chat
            if (chat != null) {
                val myId = currentUserId
                val otherId = chat.participants.firstOrNull { it != myId }
                if (otherId != null) {
                    val user = chatRepository.getUserById(otherId)
                    _otherUser.value = user
                    if (user != null) userCache[otherId] = user
                }
            }
        }
        viewModelScope.launch {
            chatRepository.observeMessages(chatId).collect { result ->
                _messagesState.value = result
            }
        }
    }

    fun loadOtherUser(chat: Chat) {
        val myId = currentUserId ?: return
        val otherId = chat.participants.firstOrNull { it != myId } ?: return
        viewModelScope.launch {
            val user = chatRepository.getUserById(otherId)
            _otherUser.value = user
            if (user != null) userCache[otherId] = user
        }
    }

    fun loadOtherUserByChatId(chatId: String) {
        viewModelScope.launch {
            val chat = chatRepository.getChatById(chatId)
            if (chat != null) loadOtherUser(chat)
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

        val user = chatRepository.getUserById(otherId)
        if (user != null) {
            userCache[otherId] = user
            return user.displayName.ifEmpty { user.username }
        }
        return chat.groupName.ifEmpty { "Cuộc trò chuyện" }
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendMessage(chatId, text.trim())
        }
    }

    fun clearConversationState() {
        _currentChat.value = null
        _otherUser.value = null
        _messagesState.value = Resource.Idle
    }
}
