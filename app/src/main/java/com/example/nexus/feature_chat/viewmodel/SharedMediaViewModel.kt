package com.example.nexus.feature_chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.Message
import com.example.nexus.data.model.User
import com.example.nexus.data.repository.ChatRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SharedMediaType(val label: String, val key: String) {
    MEDIA("Ảnh & Video", "media"),
    FILE("File", "file"),
    LINK("Liên kết", "link");

    companion object {
        fun fromKey(key: String): SharedMediaType =
            entries.find { it.key == key } ?: MEDIA
    }
}

@HiltViewModel
class SharedMediaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val chatId: String = savedStateHandle.get<String>("chatId") ?: ""
    val screenType: SharedMediaType = SharedMediaType.fromKey(
        savedStateHandle.get<String>("initialTab") ?: "media"
    )

    private val _items = MutableStateFlow<List<Message>>(emptyList())
    val items: StateFlow<List<Message>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore

    // Media sub-filter: true = show images, true = show videos (both can be true)
    private val _showImages = MutableStateFlow(true)
    val showImages: StateFlow<Boolean> = _showImages

    private val _showVideos = MutableStateFlow(true)
    val showVideos: StateFlow<Boolean> = _showVideos

    // Sender filter: null = all senders
    private val _selectedSenderId = MutableStateFlow<String?>(null)
    val selectedSenderId: StateFlow<String?> = _selectedSenderId

    // Temp selection used inside the bottom sheet (not applied until "Lưu")
    private val _tempSenderId = MutableStateFlow<String?>(null)
    val tempSenderId: StateFlow<String?> = _tempSenderId

    private val _participants = MutableStateFlow<List<User>>(emptyList())
    val participants: StateFlow<List<User>> = _participants

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var lastTimestamp: Timestamp? = null

    init {
        loadParticipants()
        loadItems(reset = true)
    }

    private fun loadParticipants() {
        viewModelScope.launch {
            try {
                val chat = chatRepository.getChatById(chatId) ?: return@launch
                val users = chat.participants.mapNotNull { uid ->
                    try { chatRepository.getUserById(uid) } catch (_: Exception) { null }
                }
                _participants.value = users
            } catch (_: Exception) {}
        }
    }

    // ── Media sub-filter toggles ──

    fun toggleImages() {
        // Don't allow deselecting both
        if (_showImages.value && !_showVideos.value) return
        _showImages.value = !_showImages.value
        loadItems(reset = true)
    }

    fun toggleVideos() {
        // Don't allow deselecting both
        if (_showVideos.value && !_showImages.value) return
        _showVideos.value = !_showVideos.value
        loadItems(reset = true)
    }

    // ── Sender filter (bottom sheet) ──

    fun prepareSenderSheet() {
        _tempSenderId.value = _selectedSenderId.value
    }

    fun setTempSender(senderId: String?) {
        _tempSenderId.value = senderId
    }

    fun applySenderFilter() {
        _selectedSenderId.value = _tempSenderId.value
        loadItems(reset = true)
    }

    fun clearSenderFilter() {
        _tempSenderId.value = null
        _selectedSenderId.value = null
        loadItems(reset = true)
    }

    // ── Data loading ──

    fun loadItems(reset: Boolean = false) {
        if (_isLoading.value) return
        if (!reset && !_hasMore.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            if (reset) {
                _items.value = emptyList()
                lastTimestamp = null
                _hasMore.value = true
            }

            try {
                val types = when (screenType) {
                    SharedMediaType.MEDIA -> buildList {
                        if (_showImages.value) add(Constants.MESSAGE_TYPE_IMAGE)
                        if (_showVideos.value) add(Constants.MESSAGE_TYPE_VIDEO)
                    }
                    SharedMediaType.FILE -> listOf(Constants.MESSAGE_TYPE_FILE)
                    SharedMediaType.LINK -> emptyList()
                }
                val filterLinks = screenType == SharedMediaType.LINK

                val (result, rawCursor) = chatRepository.getSharedMedia(
                    chatId = chatId,
                    types = types,
                    filterLinks = filterLinks,
                    senderId = _selectedSenderId.value,
                    limit = 30,
                    lastTimestamp = if (reset) null else lastTimestamp
                )

                if (reset) {
                    _items.value = result
                } else {
                    _items.value = _items.value + result
                }

                lastTimestamp = rawCursor
                _hasMore.value = rawCursor != null
            } catch (e: Exception) {
                _error.value = e.message ?: "Không thể tải dữ liệu"
            } finally {
                _isLoading.value = false
            }

            loadTotalCount()
        }
    }

    private fun loadTotalCount() {
        viewModelScope.launch {
            try {
                val counts = chatRepository.getSharedContentCounts(chatId)
                _totalCount.value = when (screenType) {
                    SharedMediaType.MEDIA -> counts.first
                    SharedMediaType.FILE -> counts.second
                    SharedMediaType.LINK -> counts.third
                }
            } catch (_: Exception) {}
        }
    }

    fun loadMore() {
        loadItems(reset = false)
    }
}
