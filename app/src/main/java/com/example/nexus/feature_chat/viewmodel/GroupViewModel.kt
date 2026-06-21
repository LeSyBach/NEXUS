package com.example.nexus.feature_chat.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.firebase.MediaUploader
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Group
import com.example.nexus.data.model.User
import com.example.nexus.data.repository.ChatRepository
import com.example.nexus.data.repository.ContactRepository
import com.example.nexus.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val mediaUploader: MediaUploader
) : ViewModel() {

    // ── Group Creation State ──
    private val _friendsList = MutableStateFlow<Resource<List<User>>>(Resource.Idle)
    val friendsList: StateFlow<Resource<List<User>>> = _friendsList

    private val _selectedMembers = MutableStateFlow<Set<String>>(emptySet())
    val selectedMembers: StateFlow<Set<String>> = _selectedMembers

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName

    private val _groupAvatarUri = MutableStateFlow<Uri?>(null)
    val groupAvatarUri: StateFlow<Uri?> = _groupAvatarUri

    private val _createGroupState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val createGroupState: StateFlow<Resource<String>> = _createGroupState

    // ── Group Info State ──
    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group

    private val _chat = MutableStateFlow<Chat?>(null)
    val chat: StateFlow<Chat?> = _chat

    private val _operationState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val operationState: StateFlow<Resource<Unit>> = _operationState

    private val _addMembersFriends = MutableStateFlow<Resource<List<User>>>(Resource.Idle)
    val addMembersFriends: StateFlow<Resource<List<User>>> = _addMembersFriends

    private val _addSelectedMembers = MutableStateFlow<Set<String>>(emptySet())
    val addSelectedMembers: StateFlow<Set<String>> = _addSelectedMembers

    val currentUserId: String?
        get() = chatRepository.getCurrentUserId()

    fun loadFriends() {
        viewModelScope.launch {
            _friendsList.value = Resource.Loading
            _friendsList.value = contactRepository.getFriendsList()
        }
    }

    fun toggleMember(userId: String) {
        val current = _selectedMembers.value.toMutableSet()
        if (current.contains(userId)) current.remove(userId) else current.add(userId)
        _selectedMembers.value = current
    }

    fun setGroupName(name: String) {
        _groupName.value = name
    }

    fun setGroupAvatarUri(uri: Uri?) {
        _groupAvatarUri.value = uri
    }

    fun createGroup(context: Context) {
        val name = _groupName.value.trim()
        if (name.isBlank()) {
            _createGroupState.value = Resource.Error("Vui lòng nhập tên nhóm")
            return
        }
        val members = _selectedMembers.value.toList()
        if (members.isEmpty()) {
            _createGroupState.value = Resource.Error("Vui lòng chọn ít nhất 1 thành viên")
            return
        }

        _createGroupState.value = Resource.Loading
        viewModelScope.launch {
            try {
                var avatarUrl = ""
                val uri = _groupAvatarUri.value
                if (uri != null) {
                    avatarUrl = mediaUploader.upload(context, uri) ?: ""
                }
                val result = groupRepository.createGroup(name, avatarUrl, members)
                _createGroupState.value = result
            } catch (e: Exception) {
                _createGroupState.value = Resource.Error(e.message ?: "Lỗi tạo nhóm")
            }
        }
    }

    fun clearCreateState() {
        _createGroupState.value = Resource.Idle
        _selectedMembers.value = emptySet()
        _groupName.value = ""
        _groupAvatarUri.value = null
    }

    // ── Group Info Operations ──

    fun loadGroup(chatId: String) {
        viewModelScope.launch {
            val chatDoc = chatRepository.getChatById(chatId)
            _chat.value = chatDoc
            if (chatDoc != null) {
                val groupId = findGroupId(chatId)
                if (groupId != null) {
                    viewModelScope.launch {
                        groupRepository.observeGroup(groupId).collect { group ->
                            _group.value = group
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            chatRepository.observeChat(chatId).collect { chat ->
                _chat.value = chat
            }
        }
    }

    private suspend fun findGroupId(chatId: String): String? {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        return try {
            val snapshot = db.collection("groups")
                .whereEqualTo("chatId", chatId)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    fun isAdmin(): Boolean {
        val userId = currentUserId ?: return false
        return _chat.value?.adminIds?.contains(userId) == true
    }

    fun loadFriendsForAddMember() {
        viewModelScope.launch {
            _addMembersFriends.value = Resource.Loading
            val result = contactRepository.getFriendsList()
            if (result is Resource.Success) {
                val currentMembers = _group.value?.members?.map { it.userId } ?: emptyList()
                val filtered = result.data.filter { it.uid !in currentMembers }
                _addMembersFriends.value = Resource.Success(filtered)
            } else {
                _addMembersFriends.value = result
            }
        }
    }

    fun toggleAddMember(userId: String) {
        val current = _addSelectedMembers.value.toMutableSet()
        if (current.contains(userId)) current.remove(userId) else current.add(userId)
        _addSelectedMembers.value = current
    }

    fun clearAddSelectedMembers() {
        _addSelectedMembers.value = emptySet()
    }

    fun addMembers(chatId: String, groupId: String, users: List<User>) {
        _operationState.value = Resource.Loading
        viewModelScope.launch {
            _operationState.value = groupRepository.addGroupMembers(chatId, groupId, users)
        }
    }

    fun removeMember(chatId: String, groupId: String, userId: String, username: String) {
        _operationState.value = Resource.Loading
        viewModelScope.launch {
            _operationState.value = groupRepository.kickMember(chatId, groupId, userId, username)
        }
    }

    fun promoteToAdmin(chatId: String, groupId: String, userId: String, username: String) {
        _operationState.value = Resource.Loading
        viewModelScope.launch {
            _operationState.value = groupRepository.promoteToAdmin(chatId, groupId, userId, username)
        }
    }

    fun demoteAdmin(chatId: String, groupId: String, userId: String, username: String) {
        _operationState.value = Resource.Loading
        viewModelScope.launch {
            _operationState.value = groupRepository.demoteAdmin(chatId, groupId, userId, username)
        }
    }

    fun leaveGroup(chatId: String, groupId: String) {
        _operationState.value = Resource.Loading
        viewModelScope.launch {
            _operationState.value = groupRepository.leaveGroup(chatId, groupId)
        }
    }

    fun dissolveGroup(chatId: String, groupId: String) {
        _operationState.value = Resource.Loading
        viewModelScope.launch {
            _operationState.value = groupRepository.dissolveGroup(chatId, groupId)
        }
    }

    fun clearOperationState() {
        _operationState.value = Resource.Idle
    }

    fun updateGroupAvatar(context: Context, chatId: String, groupId: String, uri: Uri) {
        _operationState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val avatarUrl = mediaUploader.upload(context, uri)
                if (avatarUrl != null) {
                    groupRepository.updateGroupAvatar(chatId, groupId, avatarUrl)
                    _operationState.value = Resource.Success(Unit)
                } else {
                    _operationState.value = Resource.Error("Tải ảnh lên thất bại")
                }
            } catch (e: Exception) {
                _operationState.value = Resource.Error(e.message ?: "Lỗi cập nhật avatar")
            }
        }
    }
}
