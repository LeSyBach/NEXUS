package com.example.nexus.feature_call.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.firebase.CallSignal
import com.example.nexus.data.firebase.CallSignalingService
import com.example.nexus.data.firebase.NotificationService
import com.example.nexus.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class CallState {
    IDLE, OUTGOING, INCOMING, CONNECTED, ENDED
}

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callSignalingService: CallSignalingService,
    private val chatRepository: ChatRepository,
    private val notificationService: NotificationService
) : ViewModel() {

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private val _currentSignal = MutableStateFlow<CallSignal?>(null)
    val currentSignal: StateFlow<CallSignal?> = _currentSignal

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration

    private var durationJob: Job? = null
    private var statusJob: Job? = null

    val currentUserId: String?
        get() = chatRepository.getCurrentUserId()

    fun startCall(receiverId: String, receiverName: String, type: String) {
        viewModelScope.launch {
            try {
                val myId = currentUserId ?: return@launch
                val currentUser = chatRepository.getUserById(myId)
                val callId = UUID.randomUUID().toString()
                val signal = CallSignal(
                    callId = callId,
                    callerId = myId,
                    callerName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                    callerAvatar = currentUser?.avatarUrl ?: "",
                    receiverId = receiverId,
                    receiverName = receiverName,
                    type = type,
                    status = "ringing"
                )
                _currentSignal.value = signal
                _callState.value = CallState.OUTGOING
                _isVideoEnabled.value = type == "video"

                callSignalingService.initiateCall(signal)
                notificationService.sendCallNotification(
                    receiverId = receiverId,
                    callerName = signal.callerName,
                    callId = callId,
                    callType = type
                )
                observeCallStatus(callId)
            } catch (_: Exception) {}
        }
    }

    fun handleIncomingCall(signal: CallSignal) {
        _currentSignal.value = signal
        _callState.value = CallState.INCOMING
        _isVideoEnabled.value = signal.type == "video"
    }

    fun acceptCall() {
        viewModelScope.launch {
            try {
                val signal = _currentSignal.value ?: return@launch
                callSignalingService.acceptCall(signal.callId)
                _callState.value = CallState.CONNECTED
                startDurationTimer()
            } catch (_: Exception) {}
        }
    }

    fun rejectCall() {
        viewModelScope.launch {
            try {
                val signal = _currentSignal.value ?: return@launch
                callSignalingService.rejectCall(signal.callId)
                _callState.value = CallState.ENDED
                cleanup()
            } catch (_: Exception) {}
        }
    }

    fun endCall() {
        viewModelScope.launch {
            try {
                val signal = _currentSignal.value ?: return@launch
                callSignalingService.endCall(signal.callId)
                _callState.value = CallState.ENDED
                cleanup()
            } catch (_: Exception) {}
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleVideo() {
        _isVideoEnabled.value = !_isVideoEnabled.value
    }

    private fun observeCallStatus(callId: String) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            callSignalingService.observeCallStatus(callId).collect { status ->
                when (status) {
                    "ongoing" -> {
                        if (_callState.value != CallState.CONNECTED) {
                            _callState.value = CallState.CONNECTED
                            startDurationTimer()
                        }
                    }
                    "rejected", "ended", "missed" -> {
                        _callState.value = CallState.ENDED
                        cleanup()
                    }
                }
            }
        }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        _callDuration.value = 0
        durationJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _callDuration.value++
            }
        }
    }

    private fun cleanup() {
        durationJob?.cancel()
        statusJob?.cancel()
        viewModelScope.launch {
            try {
                _currentSignal.value?.let {
                    callSignalingService.removeCall(it.callId)
                }
            } catch (_: Exception) {}
        }
    }

    fun resetState() {
        cleanup()
        _callState.value = CallState.IDLE
        _currentSignal.value = null
        _callDuration.value = 0
        _isMuted.value = false
        _isSpeakerOn.value = false
        _isVideoEnabled.value = true
    }

    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun observeIncomingCalls(userId: String) {
        viewModelScope.launch {
            try {
                callSignalingService.observeIncomingCalls(userId).collect { signal ->
                    if (signal != null && _callState.value == CallState.IDLE) {
                        handleIncomingCall(signal)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
