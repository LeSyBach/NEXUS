package com.example.nexus.data.webrtc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import javax.inject.Singleton

@Singleton
class WebRtcClient(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WebRtcClient"

        private const val STUN_URL_1 = "stun:stun.l.google.com:19302"
        private const val STUN_URL_2 = "stun:stun1.l.google.com:19302"
        private const val TURN_URL_UDP = "turn:free.expressturn.com:3478"
        private const val TURN_URL_TCP = "turn:free.expressturn.com:3478?transport=tcp"
        private const val TURN_URL_TLS = "turns:free.expressturn.com:443?transport=tls"
        private const val TURN_USERNAME = "000000002094554570"
        private const val TURN_PASSWORD = "2yRsQwo76z2J4PU2FU9m/E/THm4="
    }

    private val eglBase: EglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null

    private val cameraEventsHandler = object : CameraVideoCapturer.CameraEventsHandler {
        override fun onCameraError(errorDescription: String) {
            Log.e(TAG, "Camera error: $errorDescription")
        }

        override fun onCameraDisconnected() {
            Log.w(TAG, "Camera disconnected")
        }

        override fun onCameraFreezed(errorDescription: String) {
            Log.w(TAG, "Camera freezed: $errorDescription")
        }

        override fun onCameraOpening(cameraName: String) {
            Log.d(TAG, "Camera opening: $cameraName")
        }

        override fun onFirstFrameAvailable() {
            Log.d(TAG, "Camera first frame available")
        }

        override fun onCameraClosed() {
            Log.d(TAG, "Camera closed")
        }
    }

    private var onIceCandidate: ((IceCandidate) -> Unit)? = null
    var onRemoteVideoTrack: ((VideoTrack) -> Unit)? = null
    var localVideoTrack: VideoTrack? = null
        private set
    val eglContext: EglBase.Context get() = eglBase.eglBaseContext
    private var isVideoCall = false

    fun start(isVideoEnabled: Boolean, onIceCandidate: (IceCandidate) -> Unit) {
        this.onIceCandidate = onIceCandidate
        this.isVideoCall = isVideoEnabled
        Log.d(TAG, "start() called, video=$isVideoEnabled, peerConn=${peerConnection != null}")
        if (peerConnection == null) {
            peerConnection = createPeerConnection()
            Log.d(TAG, "createPeerConnection returned: ${peerConnection != null}")
            try {
                setupMediaTransceivers(isVideoEnabled)
            } catch (e: Exception) {
                Log.e(TAG, "setupMediaTransceivers FAILED", e)
            }
            setAudioModeForCall()
            configureAudioRouting(isVideoEnabled)
            Log.d(TAG, "start() complete, peerConn=${peerConnection != null}")
        } else {
            Log.w(TAG, "PeerConnection already exists, skipping setup")
        }
    }

    fun createOffer(onSdpReady: (SessionDescription) -> Unit) {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints()
        Log.d(TAG, "Creating offer...")
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "Offer created: ${sdp.type}, SDP length=${sdp.description.length}")
                // Log audio/video m-lines
                val hasAudio = sdp.description.contains("m=audio")
                val hasVideo = sdp.description.contains("m=video")
                Log.d(TAG, "Offer SDP: hasAudio=$hasAudio, hasVideo=$hasVideo")
                pc.setLocalDescription(SimpleSdpObserver(), sdp)
                onSdpReady(sdp)
            }
        }, constraints)
    }

    fun createAnswer(onSdpReady: (SessionDescription) -> Unit) {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints()
        Log.d(TAG, "Creating answer...")
        pc.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "Answer created: ${sdp.type}, SDP length=${sdp.description.length}")
                val hasAudio = sdp.description.contains("m=audio")
                val hasVideo = sdp.description.contains("m=video")
                Log.d(TAG, "Answer SDP: hasAudio=$hasAudio, hasVideo=$hasVideo")
                pc.setLocalDescription(SimpleSdpObserver(), sdp)
                onSdpReady(sdp)
            }
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription, onComplete: (() -> Unit)? = null) {
        val hasAudio = sdp.description.contains("m=audio")
        val hasVideo = sdp.description.contains("m=video")
        Log.d(TAG, "Setting remote description: ${sdp.type}, hasAudio=$hasAudio, hasVideo=$hasVideo")
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully")
                onComplete?.invoke()
            }
            override fun onSetFailure(error: String) {
                Log.e(TAG, "Remote description set failed: $error")
            }
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        Log.d(TAG, "Adding ICE candidate: ${candidate.sdpMid}")
        peerConnection?.addIceCandidate(candidate)
    }

    fun setAudioEnabled(enabled: Boolean) {
        audioTrack?.setEnabled(enabled)
    }

    fun setVideoEnabled(enabled: Boolean) {
        videoTrack?.setEnabled(enabled)
    }

    fun setSpeaker(enabled: Boolean) {
        audioManager.isSpeakerphoneOn = enabled
        Log.d(TAG, "Speaker: $enabled")
    }

    fun release() {
        if (peerConnection == null) {
            Log.d(TAG, "Already released, skipping")
            return
        }
        Log.d(TAG, "Releasing WebRTC resources")
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {
        }
        videoCapturer?.dispose()
        videoCapturer = null
        localVideoTrack = null
        videoTrack?.dispose()
        videoTrack = null
        videoSource?.dispose()
        videoSource = null
        audioTrack?.dispose()
        audioTrack = null
        audioSource?.dispose()
        audioSource = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        onIceCandidate = null
        onRemoteVideoTrack = null
        resetAudioMode()
    }

    // ════════════════════════════════════════════════════════════════
    // PEER CONNECTION FACTORY
    // ════════════════════════════════════════════════════════════════

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    // ════════════════════════════════════════════════════════════════
    // PEER CONNECTION + OBSERVER
    // ════════════════════════════════════════════════════════════════

    private fun createPeerConnection(): PeerConnection? {
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(TAG, "Local ICE candidate: ${candidate.sdpMid}")
                onIceCandidate?.invoke(candidate)
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Log.d(TAG, "ICE connection state: $newState")
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED -> Log.d(TAG, "ICE CONNECTED — audio/video should work")
                    PeerConnection.IceConnectionState.COMPLETED -> Log.d(TAG, "ICE COMPLETED — all media flowing")
                    PeerConnection.IceConnectionState.FAILED -> Log.e(TAG, "ICE FAILED — check TURN servers")
                    PeerConnection.IceConnectionState.DISCONNECTED -> Log.w(TAG, "ICE DISCONNECTED")
                    else -> {}
                }
            }

            override fun onAddStream(stream: MediaStream) {
                Log.d(TAG, "onAddStream: ${stream.audioTracks.size} audio, ${stream.videoTracks.size} video")
                stream.audioTracks.forEach { track ->
                    track.setEnabled(true)
                    Log.d(TAG, "Remote audio enabled from onAddStream: ${track.id()}, enabled=${track.enabled()}")
                }
                stream.videoTracks.forEach { track ->
                    track.setEnabled(true)
                    Log.d(TAG, "Remote video track received from onAddStream: ${track.id()}")
                    onRemoteVideoTrack?.invoke(track)
                }
            }

            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
                Log.d(TAG, "onAddTrack: ${streams.size} streams, kind=${receiver.track()?.kind()}")
                streams.forEach { stream ->
                    stream.audioTracks.forEach { track ->
                        track.setEnabled(true)
                        Log.d(TAG, "Remote audio enabled via onAddTrack: ${track.id()}")
                    }
                    stream.videoTracks.forEach { track ->
                        track.setEnabled(true)
                        Log.d(TAG, "Remote video track via onAddTrack: ${track.id()}")
                        onRemoteVideoTrack?.invoke(track)
                    }
                }
                val track = receiver.track()
                when (track) {
                    is AudioTrack -> {
                        track.setEnabled(true)
                        Log.d(TAG, "Remote audio enabled from receiver.track(): ${track.id()}")
                    }
                    is VideoTrack -> {
                        track.setEnabled(true)
                        Log.d(TAG, "Remote video from receiver.track(): ${track.id()}")
                        onRemoteVideoTrack?.invoke(track)
                    }
                }
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState) {
                Log.d(TAG, "Signaling state: $newState")
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) = Unit
            override fun onRemoveStream(stream: MediaStream) = Unit
            override fun onDataChannel(channel: org.webrtc.DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
        }

        // Try with all ICE servers (STUN + TURN) first
        val allServers = buildIceServers(includeTurn = true)
        Log.d(TAG, "Trying with ${allServers.size} ICE servers (STUN + TURN)")
        allServers.forEach { server -> Log.d(TAG, "  ICE server: ${server.uri}") }

        var pc = peerConnectionFactory.createPeerConnection(allServers, observer)
        if (pc != null) {
            Log.d(TAG, "PeerConnection created successfully with STUN + TURN")
            return pc
        }

        // TURN servers may be invalid/expired — fall back to STUN only
        Log.w(TAG, "PeerConnection creation failed with TURN servers, falling back to STUN only")
        val stunOnly = buildIceServers(includeTurn = false)
        Log.d(TAG, "Trying with ${stunOnly.size} STUN-only servers")
        stunOnly.forEach { server -> Log.d(TAG, "  ICE server: ${server.uri}") }

        pc = peerConnectionFactory.createPeerConnection(stunOnly, observer)
        if (pc == null) {
            Log.e(TAG, "createPeerConnection returned NULL even with STUN only! PeerConnectionFactory may be broken.")
        } else {
            Log.d(TAG, "PeerConnection created successfully with STUN only")
        }
        return pc
    }

    // ════════════════════════════════════════════════════════════════
    // MEDIA SETUP (addTransceiver for proper SDP)
    // ════════════════════════════════════════════════════════════════

    private fun setupMediaTransceivers(videoEnabled: Boolean) {
        val pc = peerConnection
        if (pc == null) {
            Log.e(TAG, "setupMediaTransceivers: peerConnection is NULL!")
            return
        }
        Log.d(TAG, "setupMediaTransceivers: video=$videoEnabled, pc=$pc")

        try {
            // Audio — create source + track, then add via addTrack
            Log.d(TAG, "Creating audio source...")
            val audioConstraints = MediaConstraints()
            audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
            Log.d(TAG, "Audio source created: ${audioSource != null}")

            audioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)
            Log.d(TAG, "Audio track created: ${audioTrack != null}")

            val audioSender = pc.addTrack(audioTrack!!)
            Log.d(TAG, "Audio track added to PC, sender=${audioSender != null}, kind=${audioSender?.track()?.kind()}")
        } catch (e: Exception) {
            Log.e(TAG, "Audio setup FAILED", e)
        }

        // Video — if video call, create track and add to peer connection
        if (videoEnabled) {
            try {
                if (!hasCameraPermission()) {
                    Log.w(TAG, "Camera permission not granted; skipping video track")
                    return
                }
                Log.d(TAG, "Creating video capturer...")
                val capturer = createVideoCapturer()
                if (capturer != null) {
                    videoCapturer = capturer
                    Log.d(TAG, "Video capturer created, initializing...")
                    val surfaceTextureHelper = SurfaceTextureHelper.create("video_capture", eglBase.eglBaseContext)
                    videoSource = peerConnectionFactory.createVideoSource(capturer.isScreencast)
                    capturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
                    try {
                        capturer.startCapture(640, 480, 30)
                        Log.d(TAG, "Video capture started")
                    } catch (e: Exception) {
                        Log.e(TAG, "startCapture failed", e)
                    }
                    videoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource)
                    localVideoTrack = videoTrack
                    val videoSender = pc.addTrack(videoTrack!!)
                    Log.d(TAG, "Video track added to PC, sender=${videoSender != null}")
                } else {
                    Log.w(TAG, "No camera available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Video setup FAILED", e)
            }
        }

        Log.d(TAG, "setupMediaTransceivers complete. Transceivers: ${pc.transceivers.size}")
        pc.transceivers.forEachIndexed { i, t ->
            Log.d(TAG, "  [$i] kind=${t.mediaType}, dir=${t.direction}, curDir=${t.currentDirection}")
        }
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val camera2Capturer = if (Camera2Enumerator.isSupported(context)) {
            createCapturerFromEnumerator(Camera2Enumerator(context), "Camera2")
        } else {
            null
        }
        if (camera2Capturer != null) return camera2Capturer
        return createCapturerFromEnumerator(Camera1Enumerator(false), "Camera1")
    }

    private fun createCapturerFromEnumerator(
        enumerator: CameraEnumerator,
        label: String
    ): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        val frontCamera = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        val backCamera = deviceNames.firstOrNull { enumerator.isBackFacing(it) }
        val chosen = frontCamera ?: backCamera
        Log.d(TAG, "$label camera: $chosen (front=$frontCamera)")
        return chosen?.let {
            try {
                enumerator.createCapturer(it, cameraEventsHandler)
            } catch (e: Exception) {
                Log.e(TAG, "$label createCapturer failed", e)
                null
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    // ════════════════════════════════════════════════════════════════
    // ICE SERVERS
    // ════════════════════════════════════════════════════════════════

    private fun buildIceServers(includeTurn: Boolean = true): List<PeerConnection.IceServer> {
        val servers = mutableListOf<PeerConnection.IceServer>()

        // STUN servers for NAT discovery
        servers.add(PeerConnection.IceServer.builder(STUN_URL_1).createIceServer())
        servers.add(PeerConnection.IceServer.builder(STUN_URL_2).createIceServer())

        if (!includeTurn) return servers

        // TURN servers for relay (when STUN fails)
        val hasCredentials = TURN_USERNAME.isNotEmpty() && TURN_PASSWORD.isNotEmpty()
        Log.d(TAG, "TURN credentials: hasCredentials=$hasCredentials, username=${TURN_USERNAME.take(5)}...")

        try {
            if (TURN_URL_UDP.isNotEmpty()) {
                val builder = PeerConnection.IceServer.builder(TURN_URL_UDP)
                if (hasCredentials) { builder.setUsername(TURN_USERNAME); builder.setPassword(TURN_PASSWORD) }
                servers.add(builder.createIceServer())
            }
            if (TURN_URL_TCP.isNotEmpty()) {
                val builder = PeerConnection.IceServer.builder(TURN_URL_TCP)
                if (hasCredentials) { builder.setUsername(TURN_USERNAME); builder.setPassword(TURN_PASSWORD) }
                servers.add(builder.createIceServer())
            }
            if (TURN_URL_TLS.isNotEmpty()) {
                val builder = PeerConnection.IceServer.builder(TURN_URL_TLS)
                if (hasCredentials) { builder.setUsername(TURN_USERNAME); builder.setPassword(TURN_PASSWORD) }
                servers.add(builder.createIceServer())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build TURN servers", e)
        }

        Log.d(TAG, "Total ICE servers: ${servers.size}")
        return servers
    }

    // ════════════════════════════════════════════════════════════════
    // AUDIO ROUTING
    // ════════════════════════════════════════════════════════════════

    private fun setAudioModeForCall() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = false
        audioManager.isSpeakerphoneOn = false
        Log.d(TAG, "Audio mode: MODE_IN_COMMUNICATION")
    }

    private fun resetAudioMode() {
        audioManager.isSpeakerphoneOn = false
        audioManager.isMicrophoneMute = false
        audioManager.mode = AudioManager.MODE_NORMAL
        Log.d(TAG, "Audio mode: MODE_NORMAL")
    }

    private fun configureAudioRouting(isVideo: Boolean) {
        if (hasBluetoothAudio()) {
            audioManager.isBluetoothScoOn = true
            audioManager.startBluetoothSco()
            audioManager.isSpeakerphoneOn = false
            Log.d(TAG, "Audio → Bluetooth")
        } else if (isVideo) {
            audioManager.isSpeakerphoneOn = true
            Log.d(TAG, "Audio → Speaker (video)")
        } else {
            audioManager.isSpeakerphoneOn = false
            Log.d(TAG, "Audio → Earpiece (voice)")
        }
    }

    private fun hasBluetoothAudio(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            return devices.any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
            }
        }
        @Suppress("DEPRECATION")
        return audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
    }
}

open class SimpleSdpObserver : org.webrtc.SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String) {
        android.util.Log.e("SimpleSdpObserver", "SDP create failed: $error")
    }
    override fun onSetFailure(error: String) {
        android.util.Log.e("SimpleSdpObserver", "SDP set failed: $error")
    }
}

