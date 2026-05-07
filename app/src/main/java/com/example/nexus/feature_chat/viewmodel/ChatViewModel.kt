package com.example.nexus.feature_chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    /** The other participant's info for a direct chat */
    private val _otherUser = MutableStateFlow<User?>(null)
    val otherUser: StateFlow<User?> = _otherUser

    /** Cached map: participantId → User for resolving chat display names */
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
            }
        }
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.observeMessages(chatId).collect { result ->
                _messagesState.value = result
            }
        }
    }

    /** Load the other participant's User object for a direct chat */
    fun loadOtherUser(chat: Chat) {
        val myId = currentUserId ?: return
        val otherId = chat.participants.firstOrNull { it != myId } ?: return
        viewModelScope.launch {
            val user = chatRepository.getUserById(otherId)
            _otherUser.value = user
        }
    }

    /** For a direct chat, load the other user by chatId */
    fun loadOtherUserByChatId(chatId: String) {
        viewModelScope.launch {
            val chat = chatRepository.getChatById(chatId)
            if (chat != null) loadOtherUser(chat)
        }
    }

    /** Resolve the display name for a chat.
     *  For direct chats: show the other person's name.
     *  For group chats: show groupName. */
    suspend fun resolveDisplayName(chat: Chat): String {
        if (chat.type == "group") {
            return chat.groupName.ifEmpty { "Nhóm" }
        }
        val myId = currentUserId ?: return chat.groupName
        val otherId = chat.participants.firstOrNull { it != myId }
        if (otherId == null) return chat.groupName

        // Check cache first
        userCache[otherId]?.let { user ->
            return user.displayName.ifEmpty { user.username }
        }

        // Load and cache
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
}
