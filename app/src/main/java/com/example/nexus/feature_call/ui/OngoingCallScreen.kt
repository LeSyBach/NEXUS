package com.example.nexus.feature_call.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.nexus.feature_call.viewmodel.CallState
import com.example.nexus.feature_call.viewmodel.CallViewModel
import kotlinx.coroutines.delay
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import kotlin.math.roundToInt

@Composable
fun OngoingCallScreen(
    callId: String,
    callType: String = "voice",
    viewModel: CallViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val signal = viewModel?.currentSignal?.collectAsState()?.value
    LaunchedEffect(callId) {
        if (signal == null || signal.callId != callId) {
            viewModel?.loadCallSignal(callId)
        }
    }

    val isMuted by viewModel?.isMuted?.collectAsState() ?: rememberStaticState(false)
    val isSpeakerOn by viewModel?.isSpeakerOn?.collectAsState() ?: rememberStaticState(false)
    val isVideoEnabled by viewModel?.isVideoEnabled?.collectAsState() ?: rememberStaticState(false)
    val callDuration by viewModel?.callDuration?.collectAsState() ?: rememberStaticState(0L)
    val callState by viewModel?.callState?.collectAsState() ?: rememberStaticState(CallState.IDLE)
    val currentUserId = viewModel?.currentUserId

    val isCaller = signal?.callerId == currentUserId
    val displayName = if (isCaller) {
        signal?.receiverName?.ifEmpty { null } ?: "Người dùng"
    } else {
        signal?.callerName?.ifEmpty { null } ?: "Người dùng"
    }
    val avatarUrl = if (isCaller) signal?.receiverAvatar else signal?.callerAvatar
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    val isVideo = callType == "video"
    val isConnected = callState == CallState.CONNECTED

    var showControls by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (isVideo) showControls = !showControls }
    ) {
        if (isVideo) {
            // ─── VIDEO CALL ───
            val remoteVideoTrack = viewModel?.remoteVideoTrack?.collectAsState()?.value
            val localTrack = viewModel?.localVideoTrack?.collectAsState()?.value
            val eglContext = viewModel?.eglContext

            // Remote video
            if (remoteVideoTrack != null && eglContext != null) {
                RemoteVideoRenderer(
                    videoTrack = remoteVideoTrack,
                    eglContext = eglContext,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (localTrack != null && eglContext != null) {
                LocalVideoRenderer(
                    videoTrack = localTrack,
                    eglContext = eglContext,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }

            // Top Overlay & Info
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (isConnected) {
                            Text(
                                text = viewModel?.formatDuration(callDuration) ?: "00:00",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp
                            )
                        } else {
                            Text(
                                text = "Đang kết nối...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // PiP Video (Always visible)
            if (remoteVideoTrack != null && localTrack != null && eglContext != null) {
                DraggableLocalPreview(
                    videoTrack = localTrack,
                    eglContext = eglContext
                )
            }

            // Bottom Controls Floating Island
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp) // Tránh gesture bar
            ) {
                VideoCallControls(
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    isVideoEnabled = isVideoEnabled,
                    onToggleMute = { viewModel?.toggleMute() },
                    onToggleSpeaker = { viewModel?.toggleSpeaker() },
                    onToggleVideo = { viewModel?.toggleVideo() },
                    onFlipCamera = { viewModel?.flipCamera() },
                    onEndCall = { viewModel?.endCall() }
                )
            }
        } else {
            // ─── VOICE CALL ───
            Box(modifier = Modifier.fillMaxSize().background(callGradient))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Avatar lớn hơn, đổ bóng sâu
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    AvatarCircle(initial = initial, size = 160, avatarUrl = avatarUrl?.ifEmpty { null })
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isConnected) {
                    Text(
                        text = viewModel?.formatDuration(callDuration) ?: "00:00",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Đang kết nối...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Cuộc gọi thoại",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Voice Call Controls
                Row(
                    modifier = Modifier.padding(bottom = 64.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlButton(
                        icon = {
                            Icon(
                                if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        backgroundColor = Color.White.copy(alpha = 0.15f),
                        size = 60,
                        onClick = { viewModel?.toggleMute() }
                    )
                    CallControlButton(
                        icon = {
                            Icon(
                                if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        backgroundColor = Color.White.copy(alpha = 0.15f),
                        size = 60,
                        onClick = { viewModel?.toggleSpeaker() }
                    )
                    CallControlButton(
                        icon = {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        backgroundColor = Color(0xFFEF4444),
                        size = 72, // Nút End Call lớn
                        onClick = { viewModel?.endCall() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableLocalPreview(
    videoTrack: org.webrtc.VideoTrack,
    eglContext: EglBase.Context
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    // Tỷ lệ 9:16 chuẩn
    val previewWidthPx = with(density) { 100.dp.toPx() }
    val previewHeightPx = with(density) { 170.dp.toPx() }

    var offsetX by remember { mutableFloatStateOf(screenWidthPx - previewWidthPx - with(density) { 16.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx - previewHeightPx - with(density) { 160.dp.toPx() }) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(100.dp, 170.dp)
            // Đổ bóng góc vuông
            .shadow(12.dp, androidx.compose.ui.graphics.RectangleShape)
            // Viền góc vuông
            .border(1.5.dp, Color.White.copy(alpha = 0.4f), androidx.compose.ui.graphics.RectangleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - previewWidthPx)
                    offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx - previewHeightPx)
                }
            }
    ) {
        LocalVideoRenderer(
            videoTrack = videoTrack,
            eglContext = eglContext,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun VideoCallControls(
    modifier: Modifier = Modifier,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideoEnabled: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    // Floating Island style
    Row(
        modifier = modifier
            .background(Color(0xFF1E1E1E).copy(alpha = 0.75f), RoundedCornerShape(36.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CallControlButton(
            icon = { Icon(Icons.Default.FlipCameraAndroid, null, tint = Color.White, modifier = Modifier.size(24.dp)) },
            backgroundColor = Color.White.copy(alpha = 0.15f),
            size = 52,
            onClick = onFlipCamera
        )
        CallControlButton(
            icon = { Icon(if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, null, tint = Color.White, modifier = Modifier.size(24.dp)) },
            backgroundColor = Color.White.copy(alpha = 0.15f),
            size = 52,
            onClick = onToggleVideo
        )
        CallControlButton(
            icon = { Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(24.dp)) },
            backgroundColor = Color.White.copy(alpha = 0.15f),
            size = 52,
            onClick = onToggleMute
        )
        CallControlButton(
            icon = { Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp)) },
            backgroundColor = Color(0xFFEF4444),
            size = 64, // Nút End Call lớn nhất
            onClick = onEndCall
        )
    }
}

@Composable
fun RemoteVideoRenderer(videoTrack: org.webrtc.VideoTrack, eglContext: EglBase.Context, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                setMirror(false)
                videoTrack.addSink(this)
            }
        },
        update = { view -> videoTrack.addSink(view) },
        modifier = modifier
    )
}

@Composable
fun LocalVideoRenderer(
    videoTrack: org.webrtc.VideoTrack,
    eglContext: EglBase.Context,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setMirror(true)
                videoTrack.addSink(this)
            }
        },
        update = { view ->
            videoTrack.addSink(view)
        },
        modifier = modifier
    )
}

@Composable
private fun <T> rememberStaticState(value: T) = remember { mutableStateOf(value) }
