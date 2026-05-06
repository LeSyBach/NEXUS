package com.example.nexus.feature_chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Message
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

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendMessage(chatId, text.trim())
        }
    }
}
