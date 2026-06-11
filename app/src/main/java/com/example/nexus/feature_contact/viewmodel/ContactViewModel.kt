package com.example.nexus.feature_contact.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.model.FriendRequest
import com.example.nexus.data.model.User
import com.example.nexus.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<Resource<List<User>>>(Resource.Idle)
    val searchResults: StateFlow<Resource<List<User>>> = _searchResults

    private val _friendRequests = MutableStateFlow<Resource<List<FriendRequest>>>(Resource.Idle)
    val friendRequests: StateFlow<Resource<List<FriendRequest>>> = _friendRequests

    private val _sentRequests = MutableStateFlow<Resource<List<FriendRequest>>>(Resource.Idle)
    val sentRequests: StateFlow<Resource<List<FriendRequest>>> = _sentRequests

    private val _friendsList = MutableStateFlow<Resource<List<User>>>(Resource.Idle)
    val friendsList: StateFlow<Resource<List<User>>> = _friendsList

    private val _sentRequestIds = MutableStateFlow<Set<String>>(emptySet())
    val sentRequestIds: StateFlow<Set<String>> = _sentRequestIds

    private val _sendRequestResult = MutableSharedFlow<String>()
    val sendRequestResult = _sendRequestResult.asSharedFlow()

    private val _respondResult = MutableSharedFlow<String>()
    val respondResult = _respondResult.asSharedFlow()

    private val _navigateToChatEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToChatEvent = _navigateToChatEvent.asSharedFlow()

    init {
        loadFriendRequests()
        loadSentRequestsList()
        loadFriendsList()
        loadSentRequestIds()
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = Resource.Idle
            return
        }
        viewModelScope.launch {
            _searchResults.value = Resource.Loading
            _searchResults.value = contactRepository.searchUsers(query.trim())
        }
    }

    fun sendFriendRequest(toUserId: String) {
        viewModelScope.launch {
            val result = contactRepository.sendFriendRequest(toUserId)
            if (result is Resource.Success) {
                _sentRequestIds.value = _sentRequestIds.value + toUserId
            } else if (result is Resource.Error) {
                _sendRequestResult.emit(result.message)
            }
        }
    }

    private fun loadFriendRequests() {
        viewModelScope.launch {
            contactRepository.observeReceivedRequests().collect { result ->
                _friendRequests.value = result
            }
        }
    }

    private fun loadSentRequestsList() {
        viewModelScope.launch {
            contactRepository.observeSentRequests().collect { result ->
                _sentRequests.value = result
            }
        }
    }

    private fun loadSentRequestIds() {
        viewModelScope.launch {
            val ids = contactRepository.getSentRequestTargetIds()
            _sentRequestIds.value = ids
        }
    }

    fun respondToRequest(requestId: String, accept: Boolean, fromUserId: String) {
        viewModelScope.launch {
            val result = contactRepository.respondToRequest(requestId, accept, fromUserId)
            if (result is Resource.Error) {
                _respondResult.emit(result.message)
            }
            // Real-time listener will auto-update friends list on accept
        }
    }

    fun loadFriendsList() {
        viewModelScope.launch {
            contactRepository.observeFriendsList().collect { result ->
                _friendsList.value = result
            }
        }
    }

    fun cancelFriendRequest(targetUserId: String) {
        viewModelScope.launch {
            val result = contactRepository.cancelFriendRequest(targetUserId)
            if (result is Resource.Success) {
                // Update local state immediately for instant UI feedback
                _sentRequestIds.value = _sentRequestIds.value - targetUserId
                val current = _sentRequests.value
                if (current is Resource.Success) {
                    _sentRequests.value = Resource.Success(
                        current.data.filter { it.toUserId != targetUserId }
                    )
                }
            } else if (result is Resource.Error) {
                _sendRequestResult.emit(result.message)
            }
        }
    }

    fun startChatWithFriend(friendId: String) {
        viewModelScope.launch {
            val chatId = contactRepository.getDirectChatId(friendId)
            if (chatId != null) {
                _navigateToChatEvent.emit(chatId)
            }
        }
    }

    val receivedRequestCount: Int
        get() = (friendRequests.value as? Resource.Success)?.data?.size ?: 0
}
