package com.example.nexus.feature_call.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.firebase.CallSignal
import com.example.nexus.data.firebase.CallSignalingService
import com.example.nexus.data.firebase.NotificationService
import com.example.nexus.data.firebase.WebRtcSignalingService
import com.example.nexus.data.webrtc.WebRtcClient
import com.example.nexus.data.webrtc.toData
import com.example.nexus.data.webrtc.toIceCandidate
import com.example.nexus.data.webrtc.toSessionDescription
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
    private val notificationService: NotificationService,
    private val webRtcClient: WebRtcClient,
    private val webRtcSignalingService: WebRtcSignalingService
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
    private var offerJob: Job? = null
    private var answerJob: Job? = null
    private var iceJob: Job? = null

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
                startWebRtc(callId, myId, signal.type == "video")
                createAndSendOffer(callId, myId)
                observeAnswer(callId)
                observeRemoteIce(callId, myId)
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

                val myId = currentUserId ?: return@launch
                startWebRtc(signal.callId, myId, signal.type == "video")
                observeOfferAndAnswer(signal.callId, myId)
                observeRemoteIce(signal.callId, myId)
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
        webRtcClient.setAudioEnabled(!_isMuted.value)
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
        webRtcClient.setSpeaker(_isSpeakerOn.value)
    }

    fun toggleVideo() {
        _isVideoEnabled.value = !_isVideoEnabled.value
        webRtcClient.setVideoEnabled(_isVideoEnabled.value)
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
        offerJob?.cancel()
        answerJob?.cancel()
        iceJob?.cancel()
        webRtcClient.release()
        viewModelScope.launch {
            try {
                _currentSignal.value?.let {
                    callSignalingService.removeCall(it.callId)
                    webRtcSignalingService.clearSession(it.callId)
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

    private fun startWebRtc(callId: String, myId: String, videoEnabled: Boolean) {
        webRtcClient.start(videoEnabled) { candidate ->
            viewModelScope.launch {
                try {
                    webRtcSignalingService.sendIceCandidate(callId, candidate.toData(myId))
                } catch (_: Exception) {}
            }
        }
    }

    private fun createAndSendOffer(callId: String, myId: String) {
        webRtcClient.createOffer { sdp ->
            viewModelScope.launch {
                try {
                    webRtcSignalingService.sendOffer(callId, sdp.toData(myId))
                } catch (_: Exception) {}
            }
        }
    }

    private fun observeOfferAndAnswer(callId: String, myId: String) {
        offerJob?.cancel()
        offerJob = viewModelScope.launch {
            webRtcSignalingService.observeOffer(callId).collect { offer ->
                if (offer != null && offer.senderId != myId) {
                    webRtcClient.setRemoteDescription(offer.toSessionDescription()) {
                        webRtcClient.createAnswer { answer ->
                            viewModelScope.launch {
                                try {
                                    webRtcSignalingService.sendAnswer(callId, answer.toData(myId))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    offerJob?.cancel()
                }
            }
        }
    }

    private fun observeAnswer(callId: String) {
        answerJob?.cancel()
        val myId = currentUserId ?: return
        answerJob = viewModelScope.launch {
            webRtcSignalingService.observeAnswer(callId).collect { answer ->
                if (answer != null && answer.senderId != myId) {
                    webRtcClient.setRemoteDescription(answer.toSessionDescription())
                    answerJob?.cancel()
                }
            }
        }
    }

    private fun observeRemoteIce(callId: String, myId: String) {
        iceJob?.cancel()
        iceJob = viewModelScope.launch {
            webRtcSignalingService.observeRemoteIceCandidates(callId, myId).collect { data ->
                webRtcClient.addIceCandidate(data.toIceCandidate())
            }
        }
    }
}

