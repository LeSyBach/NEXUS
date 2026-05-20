package com.example.nexus.feature_call.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.nexus.feature_call.viewmodel.CallState
import com.example.nexus.feature_call.viewmodel.CallViewModel
import com.example.nexus.ui.theme.nexusColors
import kotlinx.coroutines.delay
import android.view.SurfaceHolder
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallHistoryScreen(
    onNavigateToCall: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = { onNavigateToCall("dummy_call_id") }) {
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
    val signal = viewModel?.currentSignal?.collectAsState()?.value
    val isMuted by viewModel?.isMuted?.collectAsState() ?: rememberStaticState(false)
    val isSpeakerOn by viewModel?.isSpeakerOn?.collectAsState() ?: rememberStaticState(false)
    val isVideoEnabled by viewModel?.isVideoEnabled?.collectAsState() ?: rememberStaticState(false)
    val callDuration by viewModel?.callDuration?.collectAsState() ?: rememberStaticState(0L)
    val callState by viewModel?.callState?.collectAsState() ?: rememberStaticState(CallState.IDLE)

    val displayName = signal?.callerName ?: "Người dùng"
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    val isVideo = callType == "video"
    val isConnected = callState == CallState.CONNECTED

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background layer
        if (isVideo) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(nc.background, nc.surface.copy(alpha = 0.8f)))))
        }
        if (isVideo) {
            // ─── VIDEO CALL LAYOUT ───
            val remoteVideoTrack = viewModel?.remoteVideoTrack?.collectAsState()?.value
            val localTrack = viewModel?.localVideoTrack?.collectAsState()?.value
            val eglContext = viewModel?.eglContext

            // Remote video or local preview as background
            val currentRemote = remoteVideoTrack
            if (currentRemote != null && eglContext != null) {
                RemoteVideoRenderer(
                    videoTrack = currentRemote,
                    eglContext = eglContext,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (localTrack != null && eglContext != null) {
                // No remote yet — show local video as full background
                LocalVideoRenderer(
                    videoTrack = localTrack,
                    eglContext = eglContext,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top overlay — name + duration
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

            // Small local video preview at bottom-right (when remote is showing)
            if (currentRemote != null && localTrack != null && eglContext != null) {
                LocalVideoRenderer(
                    videoTrack = localTrack,
                    eglContext = eglContext,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 120.dp)
                        .size(110.dp, 146.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlButton(
                    icon = {
                        Icon(
                            if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    onClick = { viewModel?.toggleMute() }
                )
                CallControlButton(
                    icon = {
                        Icon(
                            if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (isSpeakerOn) "Speaker off" else "Speaker on",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    onClick = { viewModel?.toggleSpeaker() }
                )
                CallControlButton(
                    icon = {
                        Icon(
                            if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = if (isVideoEnabled) "Video off" else "Video on",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    onClick = { viewModel?.toggleVideo() }
                )
                CallControlButton(
                    icon = {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "End call",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    backgroundColor = Color(0xFFEF4444),
                    size = 64,
                    onClick = { viewModel?.endCall() }
                )
            }
        } else {
            // ─── VOICE CALL LAYOUT ───
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                AvatarCircle(initial = initial, size = 120)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = displayName,
                    color = nc.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isConnected) {
                    Text(
                        text = viewModel?.formatDuration(callDuration) ?: "00:00",
                        color = nc.textSecondary,
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        text = "Đang kết nối...",
                        color = nc.textTertiary,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cuộc gọi thoại",
                    color = nc.textTertiary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlButton(
                        icon = {
                            Icon(
                                if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        backgroundColor = nc.surfaceVariant,
                        onClick = { viewModel?.toggleMute() }
                    )
                    CallControlButton(
                        icon = {
                            Icon(
                                if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = if (isSpeakerOn) "Speaker off" else "Speaker on",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        backgroundColor = nc.surfaceVariant,
                        onClick = { viewModel?.toggleSpeaker() }
                    )
                    CallControlButton(
                        icon = {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = "End call",
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
fun IncomingCallScreen(
    callId: String,
    viewModel: CallViewModel? = null,
    onNavigateBack: () -> Unit,
    onCallAccepted: (callType: String) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val signal = viewModel?.currentSignal?.collectAsState()?.value

    val displayName = signal?.callerName ?: "Người dùng"
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    val isVideo = signal?.type == "video"

    // Permission handling
    var pendingAccept by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    val permissions = rememberCallPermissions(needCamera = isVideo) {
        // All permissions granted — proceed with accept
        if (pendingAccept) {
            pendingAccept = false
            viewModel?.acceptCall()
            onCallAccepted(if (isVideo) "video" else "voice")
        }
    }

    // If permissions were requested and still not granted, show rationale
    LaunchedEffect(permissions.shouldShowRationale) {
        if (permissions.shouldShowRationale && pendingAccept) {
            showPermissionRationale = true
            pendingAccept = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Permission rationale dialog
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(nc.background, nc.surface.copy(alpha = 0.8f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            AvatarCircle(initial = initial, size = 140)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = displayName,
                color = nc.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isVideo) "Cuộc gọi video đến" else "Cuộc gọi thoại đến",
                color = nc.textTertiary,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.padding(bottom = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            contentDescription = "Reject",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Từ chối", color = nc.textSecondary, fontSize = 14.sp)
                }

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
                                    viewModel?.acceptCall()
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
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Chấp nhận", color = nc.textSecondary, fontSize = 14.sp)
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

    val displayName = signal?.callerName ?: "Người dùng"
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(nc.background, nc.surface.copy(alpha = 0.8f))))
    ) {
        if (isVideo) {
            if (localTrack != null && eglContext != null) {
                LocalVideoRenderer(
                    videoTrack = localTrack,
                    eglContext = eglContext,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }

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
                    contentDescription = "Cancel",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                AvatarCircle(initial = initial, size = 120)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = displayName,
                    color = nc.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Đang gọi...",
                    color = nc.textSecondary,
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
                        contentDescription = "Cancel",
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
    val nc = MaterialTheme.nexusColors
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
                    .background(nc.background),
                contentAlignment = Alignment.Center
            ) {
                Text("Cuộc gọi đã kết thúc", color = Color.Gray, fontSize = 18.sp)
            }
        }
        CallState.IDLE -> {
            LaunchedEffect(Unit) { onNavigateBack() }
        }
    }
}

@Composable
private fun AvatarCircle(initial: String, size: Int) {
    val nc = MaterialTheme.nexusColors
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(nc.avatarBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = nc.textPrimary,
            fontSize = (size / 3).sp,
            fontWeight = FontWeight.Bold
        )
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
                // Wait for surface to be created before adding sink
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        videoTrack.addSink(this@apply)
                    }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        videoTrack.removeSink(this@apply)
                    }
                })
            }
        },
        update = { _ -> },
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
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        videoTrack.addSink(this@apply)
                    }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        videoTrack.removeSink(this@apply)
                    }
                })
            }
        },
        update = { _ -> },
        modifier = modifier
    )
}