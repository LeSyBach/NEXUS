package com.example.nexus.feature_call.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.nexus.feature_call.viewmodel.CallState
import com.example.nexus.feature_call.viewmodel.CallViewModel
import com.example.nexus.ui.theme.nexusColors
import kotlinx.coroutines.delay
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import kotlin.math.roundToInt

// Messenger-style dark gradient
private val callGradient = Brush.verticalGradient(
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
)

@Composable
fun CallHistoryScreen(
    onNavigateToCall: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Button(onClick = { onNavigateToCall("dummy_call_id") }) {
            Text("Call History Screen")
        }
    }
}

@Composable
fun OngoingCallScreen(
    callId: String,
    callType: String = "voice",
    viewModel: CallViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val nc = MaterialTheme.nexusColors

    // Load signal from RTDB if not already loaded (e.g. when coming from IncomingCallScreen)
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

    // Determine the other person based on who initiated the call
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isVideo) {
            // ─── VIDEO CALL ───
            val remoteVideoTrack = viewModel?.remoteVideoTrack?.collectAsState()?.value
            val localTrack = viewModel?.localVideoTrack?.collectAsState()?.value
            val eglContext = viewModel?.eglContext

            // Remote video full screen
            val currentRemote = remoteVideoTrack
            if (currentRemote != null && eglContext != null) {
                RemoteVideoRenderer(
                    videoTrack = currentRemote,
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

            // Dark overlay on top for name/duration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
            )

            // Top: name + duration
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isConnected) {
                    Text(
                        text = viewModel?.formatDuration(callDuration) ?: "00:00",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                } else {
                    Text(
                        text = "Đang kết nối...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }

            // Draggable local video preview (PiP)
            if (currentRemote != null && localTrack != null && eglContext != null) {
                DraggableLocalPreview(
                    videoTrack = localTrack,
                    eglContext = eglContext
                )
            }

            // Bottom controls
            VideoCallControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                isMuted = isMuted,
                isSpeakerOn = isSpeakerOn,
                isVideoEnabled = isVideoEnabled,
                onToggleMute = { viewModel?.toggleMute() },
                onToggleSpeaker = { viewModel?.toggleSpeaker() },
                onToggleVideo = { viewModel?.toggleVideo() },
                onEndCall = { viewModel?.endCall() }
            )
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

                // Avatar with subtle border
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    AvatarCircle(initial = initial, size = 128, avatarUrl = avatarUrl?.ifEmpty { null })
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isConnected) {
                    Text(
                        text = viewModel?.formatDuration(callDuration) ?: "00:00",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        text = "Đang kết nối...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cuộc gọi thoại",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Controls
                Row(
                    modifier = Modifier.padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlButton(
                        icon = {
                            Icon(
                                if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        backgroundColor = Color.White.copy(alpha = 0.15f),
                        onClick = { viewModel?.toggleMute() }
                    )
                    CallControlButton(
                        icon = {
                            Icon(
                                if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        backgroundColor = Color.White.copy(alpha = 0.15f),
                        onClick = { viewModel?.toggleSpeaker() }
                    )
                    CallControlButton(
                        icon = {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        backgroundColor = Color(0xFFEF4444),
                        size = 64,
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
    val previewWidthPx = with(density) { 110.dp.toPx() }
    val previewHeightPx = with(density) { 146.dp.toPx() }

    // Start at bottom-right corner
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - previewWidthPx - with(density) { 16.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx - previewHeightPx - with(density) { 140.dp.toPx() }) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(110.dp, 146.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(
                        0f,
                        screenWidthPx - previewWidthPx
                    )
                    offsetY = (offsetY + dragAmount.y).coerceIn(
                        0f,
                        screenHeightPx - previewHeightPx
                    )
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
    onEndCall: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CallControlButton(
            icon = {
                Icon(
                    if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            },
            backgroundColor = Color.White.copy(alpha = 0.2f),
            onClick = onToggleMute
        )
        CallControlButton(
            icon = {
                Icon(
                    if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            },
            backgroundColor = Color.White.copy(alpha = 0.2f),
            onClick = onToggleSpeaker
        )
        CallControlButton(
            icon = {
                Icon(
                    if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            },
            backgroundColor = Color.White.copy(alpha = 0.2f),
            onClick = onToggleVideo
        )
        CallControlButton(
            icon = {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            },
            backgroundColor = Color(0xFFEF4444),
            size = 64,
            onClick = onEndCall
        )
    }
}

@Composable
fun IncomingCallScreen(
    callId: String,
    viewModel: CallViewModel? = null,
    onNavigateBack: () -> Unit,
    onCallAccepted: (callType: String) -> Unit
) {
    val nc = MaterialTheme.nexusColors

    // Always load signal from RTDB when screen opens
    LaunchedEffect(callId) {
        viewModel?.loadCallSignal(callId)
    }

    val signal = viewModel?.currentSignal?.collectAsState()?.value

    val displayName = signal?.callerName?.ifEmpty { null } ?: "Đang tải..."
    val avatarUrl = signal?.callerAvatar?.ifEmpty { null }
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    val isVideo = signal?.type == "video"

    var pendingAccept by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    val permissions = rememberCallPermissions(needCamera = isVideo) {
        if (pendingAccept) {
            pendingAccept = false
            viewModel?.acceptCall(callId)
            onCallAccepted(if (isVideo) "video" else "voice")
        }
    }

    LaunchedEffect(permissions.shouldShowRationale) {
        if (permissions.shouldShowRationale && pendingAccept) {
            showPermissionRationale = true
            pendingAccept = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Cần quyền truy cập") },
            text = {
                Text(
                    if (isVideo) "Để thực hiện cuộc gọi video, ứng dụng cần quyền truy cập Micro và Camera."
                    else "Để thực hiện cuộc gọi, ứng dụng cần quyền truy cập Micro."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    permissions.requestPermissions()
                }) {
                    Text("Cấp quyền")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(callGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Avatar with pulse ring
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(152.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    AvatarCircle(initial = initial, size = 140, avatarUrl = avatarUrl)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = displayName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isVideo) "Cuộc gọi video đến" else "Cuộc gọi thoại đến",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Accept / Reject buttons
            Row(
                modifier = Modifier.padding(bottom = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reject
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .clickable {
                                viewModel?.rejectCall()
                                onNavigateBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Từ chối", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }

                // Accept
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .clickable {
                                if (permissions.allGranted) {
                                    viewModel?.acceptCall(callId)
                                    onCallAccepted(if (isVideo) "video" else "voice")
                                } else {
                                    pendingAccept = true
                                    permissions.requestPermissions()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Chấp nhận", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun OutgoingCallScreen(
    viewModel: CallViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val signal = viewModel?.currentSignal?.collectAsState()?.value

    val displayName = signal?.receiverName?.ifEmpty { null } ?: "Người dùng"
    val avatarUrl = signal?.receiverAvatar?.ifEmpty { null }
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    val isVideo = signal?.type == "video"
    val localTrack = viewModel?.localVideoTrack?.collectAsState()?.value
    val eglContext = viewModel?.eglContext

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isVideo) {
            // Video: show local preview as background
            if (localTrack != null && eglContext != null) {
                LocalVideoRenderer(
                    videoTrack = localTrack,
                    eglContext = eglContext,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }

            // Dark overlay on top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Đang gọi...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    modifier = Modifier.graphicsLayer { this.alpha = alpha }
                )
            }

            // Cancel button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .clickable {
                        viewModel?.rejectCall()
                        onNavigateBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // Voice call outgoing
            Box(modifier = Modifier.fillMaxSize().background(callGradient))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    AvatarCircle(initial = initial, size = 128, avatarUrl = avatarUrl)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Đang gọi...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    modifier = Modifier.graphicsLayer { this.alpha = alpha }
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .padding(bottom = 64.dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .clickable {
                            viewModel?.rejectCall()
                            onNavigateBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallRouter(
    callId: String,
    callType: String = "voice",
    viewModel: CallViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val callState = viewModel?.callState?.collectAsState()?.value ?: CallState.IDLE

    when (callState) {
        CallState.OUTGOING -> OutgoingCallScreen(viewModel = viewModel, onNavigateBack = onNavigateBack)
        CallState.INCOMING -> IncomingCallScreen(
            callId = callId,
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            onCallAccepted = { _ -> }
        )
        CallState.CONNECTED -> OngoingCallScreen(
            callId = callId,
            callType = callType,
            viewModel = viewModel,
            onNavigateBack = onNavigateBack
        )
        CallState.ENDED -> {
            LaunchedEffect(Unit) {
                delay(1500)
                viewModel?.resetState()
                onNavigateBack()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(callGradient),
                contentAlignment = Alignment.Center
            ) {
                Text("Cuộc gọi đã kết thúc", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp)
            }
        }
        CallState.IDLE -> {
            LaunchedEffect(Unit) { onNavigateBack() }
        }
    }
}

@Composable
private fun AvatarCircle(initial: String, size: Int, avatarUrl: String? = null) {
    val nc = MaterialTheme.nexusColors
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(nc.avatarBg),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                color = nc.textPrimary,
                fontSize = (size / 3).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CallControlButton(
    icon: @Composable () -> Unit,
    backgroundColor: Color,
    size: Int = 56,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun <T> rememberStaticState(value: T) =
    remember { mutableStateOf(value) }

@Composable
fun RemoteVideoRenderer(
    videoTrack: org.webrtc.VideoTrack,
    eglContext: EglBase.Context,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                setMirror(false)
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
