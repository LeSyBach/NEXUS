package com.example.nexus.feature_contact.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.model.FriendRequest
import com.example.nexus.data.model.User
import com.example.nexus.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtherUserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contactRepository: ContactRepository,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val targetUserId: String = savedStateHandle.get<String>("targetUserId") ?: ""

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _actionState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val actionState: StateFlow<Resource<Unit>> = _actionState

    private val _navigateToChat = MutableSharedFlow<String>()
    val navigateToChat: SharedFlow<String> = _navigateToChat

    val relationship: StateFlow<String> = contactRepository.observeRelationship(targetUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Constants.RELATION_NONE
        )

    // Store the pending received request info for accept/reject
    private var pendingReceivedRequest: FriendRequest? = null

    init {
        observeUser()
        loadPendingRequest()
    }

    private fun observeUser() {
        viewModelScope.launch {
            firestoreService.observeUser(targetUserId).collect { user ->
                _user.value = user
            }
        }
    }

    private fun loadPendingRequest() {
        viewModelScope.launch {
            val currentUserId = contactRepository.getCurrentUserId() ?: return@launch
            val request = firestoreService.checkExistingFriendRequest(targetUserId, currentUserId)
            pendingReceivedRequest = request
        }
    }

    fun sendMessage() {
        viewModelScope.launch {
            val chatId = contactRepository.getDirectChatId(targetUserId)
            if (chatId != null) {
                _navigateToChat.emit(chatId)
            }
        }
    }

    fun sendFriendRequest() {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = contactRepository.sendFriendRequest(targetUserId)
            _actionState.value = result
        }
    }

    fun cancelFriendRequest() {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = contactRepository.cancelFriendRequest(targetUserId)
            _actionState.value = result
        }
    }

    fun acceptFriendRequest() {
        viewModelScope.launch {
            val request = pendingReceivedRequest ?: return@launch
            _actionState.value = Resource.Loading
            val result = contactRepository.respondToRequest(request.id, true, request.fromUserId)
            _actionState.value = result
            if (result is Resource.Success) {
                pendingReceivedRequest = null
            }
        }
    }

    fun rejectFriendRequest() {
        viewModelScope.launch {
            val request = pendingReceivedRequest ?: return@launch
            _actionState.value = Resource.Loading
            val result = contactRepository.respondToRequest(request.id, false, request.fromUserId)
            _actionState.value = result
            if (result is Resource.Success) {
                pendingReceivedRequest = null
            }
        }
    }

    fun unfriend() {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = contactRepository.unfriend(targetUserId)
            _actionState.value = result
        }
    }

    fun blockUser() {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = contactRepository.blockUser(targetUserId)
            _actionState.value = result
        }
    }

    fun unblockUser() {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = contactRepository.unblockUser(targetUserId)
            _actionState.value = result
        }
    }

    fun resetActionState() {
        _actionState.value = Resource.Idle
    }
}
