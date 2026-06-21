package com.example.nexus.feature_call.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.nexus.feature_call.viewmodel.CallState
import com.example.nexus.feature_call.viewmodel.CallViewModel
import com.example.nexus.ui.theme.nexusColors
import kotlinx.coroutines.delay

// Messenger-style dark gradient
internal val callGradient = Brush.verticalGradient(
    listOf(Color(0xFF1E1E2A), Color(0xFF12121A), Color(0xFF0A0A0F))
)

@Composable
fun OutgoingCallScreen(
    viewModel: CallViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val signal = viewModel?.currentSignal?.collectAsState()?.value
    val displayName = signal?.receiverName?.ifEmpty { null } ?: "Người dùng"
    val avatarUrl = signal?.receiverAvatar?.ifEmpty { null }
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    val isVideo = signal?.type == "video"
    val localTrack = viewModel?.localVideoTrack?.collectAsState()?.value
    val eglContext = viewModel?.eglContext

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isVideo) {
            if (localTrack != null && eglContext != null) {
                LocalVideoRenderer(videoTrack = localTrack, eglContext = eglContext, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }

            Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))))

            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = displayName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Đang gọi...", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
            }

            CallControlButton(
                icon = { Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp)) },
                backgroundColor = Color(0xFFEF4444),
                size = 72,
                onClick = { viewModel?.rejectCall(); onNavigateBack() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(callGradient))

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(160.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }.border(2.dp, Color.White, CircleShape))
                    Box(modifier = Modifier.size(160.dp).shadow(16.dp, CircleShape).clip(CircleShape).border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)) {
                        AvatarCircle(initial = initial, size = 160, avatarUrl = avatarUrl)
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
                Text(text = displayName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Đang gọi...", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)

                Spacer(modifier = Modifier.weight(1f))

                CallControlButton(
                    icon = { Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp)) },
                    backgroundColor = Color(0xFFEF4444),
                    size = 72,
                    onClick = { viewModel?.rejectCall(); onNavigateBack() },
                    modifier = Modifier.padding(bottom = 64.dp)
                )
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
        CallState.INCOMING -> IncomingCallScreen(callId = callId, viewModel = viewModel, onNavigateBack = onNavigateBack, onCallAccepted = { _ -> })
        CallState.CONNECTED -> OngoingCallScreen(callId = callId, callType = callType, viewModel = viewModel, onNavigateBack = onNavigateBack)
        CallState.ENDED -> {
            LaunchedEffect(Unit) { delay(1500); viewModel?.resetState(); onNavigateBack() }
            Box(modifier = Modifier.fillMaxSize().background(callGradient), contentAlignment = Alignment.Center) {
                Text("Cuộc gọi đã kết thúc", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
        CallState.IDLE -> LaunchedEffect(Unit) { onNavigateBack() }
    }
}

// ─── Shared composables used across call screens ───

@Composable
internal fun AvatarCircle(initial: String, size: Int, avatarUrl: String? = null) {
    val nc = MaterialTheme.nexusColors
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(nc.avatarBg),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(text = initial, color = nc.textPrimary, fontSize = (size / 2.5).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun CallControlButton(
    icon: @Composable () -> Unit,
    backgroundColor: Color,
    size: Int = 56,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Hiệu ứng scale nhún nhẹ khi chạm vào giống Messenger
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(size.dp)
            .shadow(if (isPressed) 2.dp else 8.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

