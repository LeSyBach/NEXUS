package com.example.nexus.feature_call.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.firebase.CallService
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.tasks.await
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
    @ApplicationContext private val appContext: Context,
    private val callSignalingService: CallSignalingService,
    private val chatRepository: ChatRepository,
    private val notificationService: NotificationService,
    private val webRtcClient: WebRtcClient,
    private val webRtcSignalingService: WebRtcSignalingService
) : ViewModel() {

    companion object {
        private const val TAG = "CallViewModel"
    }

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

    private val _remoteVideoTrack = MutableStateFlow<org.webrtc.VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<org.webrtc.VideoTrack?> = _remoteVideoTrack

    private val _localVideoTrack = MutableStateFlow<org.webrtc.VideoTrack?>(null)
    val localVideoTrack: StateFlow<org.webrtc.VideoTrack?> = _localVideoTrack

    val eglContext: org.webrtc.EglBase.Context
        get() = webRtcClient.eglContext

    private var durationJob: Job? = null
    private var statusJob: Job? = null
    private var offerJob: Job? = null
    private var answerJob: Job? = null
    private var iceJob: Job? = null
    private var isCleaningUp = false
    private var callHistorySaved = false

    val currentUserId: String?
        get() = chatRepository.getCurrentUserId()

    fun startCall(receiverId: String, receiverName: String, type: String, callIdOverride: String? = null) {
        viewModelScope.launch {
            try {
                val myId = currentUserId ?: return@launch
                val currentUser = chatRepository.getUserById(myId)
                val receiverUser = chatRepository.getUserById(receiverId)
                val callId = callIdOverride ?: UUID.randomUUID().toString()
                val signal = CallSignal(
                    callId = callId,
                    callerId = myId,
                    callerName = currentUser?.displayName?.ifEmpty { currentUser.username } ?: "User",
                    callerAvatar = currentUser?.avatarUrl ?: "",
                    receiverId = receiverId,
                    receiverName = receiverName,
                    receiverAvatar = receiverUser?.avatarUrl ?: "",
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
                    callType = type,
                    callerId = myId
                )
                startWebRtc(callId, myId, signal.type == "video")
                createAndSendOffer(callId, myId)
                observeAnswer(callId)
                observeRemoteIce(callId, myId)
                observeCallStatus(callId)
            } catch (e: Exception) {
                Log.e(TAG, "startCall failed", e)
            }
        }
    }

    fun handleIncomingCall(signal: CallSignal) {
        _currentSignal.value = signal
        _callState.value = CallState.INCOMING
        _isVideoEnabled.value = signal.type == "video"
    }

    /**
     * Load call signal from RTDB by callId.
     * Suspend function — must be called from a coroutine.
     * Used when navigating to IncomingCallScreen or OngoingCallScreen.
     */
    suspend fun loadCallSignal(callId: String) {
        try {
            val snapshot = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("calls")
                .child(callId)
                .get()
                .await()

            val signal = snapshot.getValue(CallSignal::class.java)
            if (signal != null) {
                Log.d(TAG, "loadCallSignal: caller=${signal.callerName}, type=${signal.type}")
                _currentSignal.value = signal
                if (_callState.value == CallState.IDLE) {
                    _callState.value = CallState.INCOMING
                }
                _isVideoEnabled.value = signal.type == "video"
            } else {
                Log.w(TAG, "loadCallSignal: signal not found for callId=$callId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadCallSignal failed", e)
        }
    }

    /**
     * Called when the user accepted the call from the FCM notification.
     * Loads the call signal from RTDB and starts WebRTC as the receiver.
     */
    fun acceptCallFromNotification(callId: String, callType: String) {
        viewModelScope.launch {
            try {
                val myId = currentUserId ?: return@launch
                Log.d(TAG, "acceptCallFromNotification: callId=$callId, type=$callType")

                // Load signal from RTDB
                val snapshot = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("calls")
                    .child(callId)
                    .get()
                    .await()

                val signal = snapshot.getValue(CallSignal::class.java)
                if (signal != null) {
                    Log.d(TAG, "Signal loaded: caller=${signal.callerName}, type=${signal.type}")
                    _currentSignal.value = signal
                    _isVideoEnabled.value = signal.type == "video"
                    _callState.value = CallState.CONNECTED
                    startDurationTimer()
                    startCallService(signal)
                    startWebRtc(callId, myId, signal.type == "video")
                    observeOfferAndAnswer(callId, myId)
                    observeRemoteIce(callId, myId)
                    observeCallStatus(callId)
                } else {
                    Log.w(TAG, "Call signal not found in RTDB for callId=$callId")
                    _callState.value = CallState.CONNECTED
                    // Still observe status in case signal appears later
                    observeCallStatus(callId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "acceptCallFromNotification failed", e)
                _callState.value = CallState.CONNECTED
            }
        }
    }

    fun acceptCall(callId: String? = null) {
        viewModelScope.launch {
            try {
                var signal = _currentSignal.value
                if (signal == null && callId != null) {
                    // Signal not loaded yet — load from RTDB first (await completion)
                    loadCallSignal(callId)
                    signal = _currentSignal.value
                }
                if (signal == null) {
                    Log.e(TAG, "acceptCall: signal is null, cannot proceed")
                    return@launch
                }
                callSignalingService.acceptCall(signal.callId)
                _callState.value = CallState.CONNECTED
                _isVideoEnabled.value = signal.type == "video"
                startDurationTimer()
                startCallService(signal)

                val myId = currentUserId ?: return@launch
                startWebRtc(signal.callId, myId, signal.type == "video")
                observeOfferAndAnswer(signal.callId, myId)
                observeRemoteIce(signal.callId, myId)
                observeCallStatus(signal.callId)
            } catch (e: Exception) {
                Log.e(TAG, "acceptCall failed", e)
            }
        }
    }

    fun rejectCall() {
        viewModelScope.launch {
            try {
                val signal = _currentSignal.value ?: return@launch
                callSignalingService.rejectCall(signal.callId)
                _callState.value = CallState.ENDED
                cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "rejectCall failed", e)
            }
        }
    }

    fun endCall() {
        viewModelScope.launch {
            try {
                val signal = _currentSignal.value ?: return@launch
                callSignalingService.endCall(signal.callId)
                _callState.value = CallState.ENDED
                cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "endCall failed", e)
            }
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

    fun flipCamera() {
        webRtcClient.flipCamera()
    }

    // ════════════════════════════════════════════════════════════════
    // CALL STATUS OBSERVER
    // ════════════════════════════════════════════════════════════════

    private fun observeCallStatus(callId: String) {
        statusJob?.cancel()
        Log.d(TAG, "observeCallStatus: callId=$callId")
        statusJob = viewModelScope.launch {
            callSignalingService.observeCallStatus(callId).collect { status ->
                Log.d(TAG, "Call status changed: $status")
                when (status) {
                    "ongoing" -> {
                        if (_callState.value != CallState.CONNECTED) {
                            _callState.value = CallState.CONNECTED
                            startDurationTimer()
                            _currentSignal.value?.let { startCallService(it) }
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

    // ════════════════════════════════════════════════════════════════
    // CALL SERVICE (Foreground Service)
    // ════════════════════════════════════════════════════════════════

    private fun startCallService(signal: CallSignal) {
        try {
            val participantName = if (signal.callerId == currentUserId) {
                signal.receiverName
            } else {
                signal.callerName
            }
            CallService.startService(
                appContext,
                signal.callId,
                signal.type,
                participantName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start CallService", e)
        }
    }

    private fun stopCallService() {
        try {
            CallService.stopService(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop CallService", e)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // CLEANUP
    // ════════════════════════════════════════════════════════════════

    private fun cleanup() {
        if (isCleaningUp) return
        isCleaningUp = true
        Log.d(TAG, "cleanup called")

        // Save call history message before tearing down (only caller saves to avoid duplicates)
        val signal = _currentSignal.value
        val myId = currentUserId
        if (signal != null && myId == signal.callerId && !callHistorySaved) {
            callHistorySaved = true
            val duration = _callDuration.value
            val callStatus = if (duration > 0) "ended" else "missed"
            viewModelScope.launch {
                try {
                    val chatId = chatRepository.findChatIdByParticipants(signal.receiverId)
                    if (chatId != null) {
                        chatRepository.sendCallHistoryMessage(chatId, signal.type, duration, callStatus)
                        Log.d(TAG, "Call history saved: type=${signal.type}, duration=$duration, status=$callStatus")
                    } else {
                        Log.w(TAG, "Chat not found for call history")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save call history", e)
                }
            }
        }

        durationJob?.cancel()
        statusJob?.cancel()
        offerJob?.cancel()
        answerJob?.cancel()
        iceJob?.cancel()
        webRtcClient.release()
        stopCallService()
        viewModelScope.launch {
            try {
                _currentSignal.value?.let {
                    callSignalingService.removeCall(it.callId)
                    webRtcSignalingService.clearSession(it.callId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "cleanup failed", e)
            }
            isCleaningUp = false
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
        _remoteVideoTrack.value = null
        _localVideoTrack.value = null
        isCleaningUp = false
        callHistorySaved = false
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
            } catch (e: Exception) {
                Log.e(TAG, "observeIncomingCalls failed", e)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // WEBRTC ORCHESTRATION
    // ════════════════════════════════════════════════════════════════

    private suspend fun startWebRtc(callId: String, myId: String, videoEnabled: Boolean) {
        Log.d(TAG, "startWebRtc: callId=$callId, video=$videoEnabled")
        webRtcClient.start(videoEnabled) { candidate ->
            Log.d(TAG, "ICE candidate callback fired")
            viewModelScope.launch {
                try {
                    webRtcSignalingService.sendIceCandidate(callId, candidate.toData(myId))
                } catch (e: Exception) {
                    Log.e(TAG, "sendIceCandidate failed", e)
                }
            }
        }
        // Listen for remote video track
        webRtcClient.onRemoteVideoTrack = { track ->
            Log.d(TAG, "Remote video track received in ViewModel")
            _remoteVideoTrack.value = track
        }
        // Expose local video track for preview (poll until camera is ready)
        if (videoEnabled) {
            viewModelScope.launch {
                repeat(20) { // up to 2 seconds
                    val track = webRtcClient.localVideoTrack
                    if (track != null) {
                        _localVideoTrack.value = track
                        Log.d(TAG, "Local video track ready")
                        return@launch
                    }
                    delay(100)
                }
                Log.w(TAG, "Local video track not ready after 2s")
            }
        }
    }

    private fun createAndSendOffer(callId: String, myId: String) {
        Log.d(TAG, "createAndSendOffer: callId=$callId")
        webRtcClient.createOffer { sdp ->
            Log.d(TAG, "Offer created, sending to RTDB...")
            viewModelScope.launch {
                try {
                    webRtcSignalingService.sendOffer(callId, sdp.toData(myId))
                    Log.d(TAG, "Offer sent successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "sendOffer failed", e)
                }
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
                                } catch (e: Exception) {
                                    Log.e(TAG, "sendAnswer failed", e)
                                }
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
