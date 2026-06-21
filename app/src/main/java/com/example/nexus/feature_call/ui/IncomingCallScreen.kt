package com.example.nexus.feature_call.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.feature_call.viewmodel.CallViewModel

@Composable
fun IncomingCallScreen(
    callId: String,
    viewModel: CallViewModel? = null,
    onNavigateBack: () -> Unit,
    onCallAccepted: (callType: String) -> Unit
) {
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

    // Ripple Pulse mượt mà chuẩn Messenger
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Cần quyền truy cập") },
            text = { Text(if (isVideo) "Để thực hiện cuộc gọi video, ứng dụng cần quyền truy cập Micro và Camera." else "Để thực hiện cuộc gọi, ứng dụng cần quyền truy cập Micro.") },
            confirmButton = { TextButton(onClick = { showPermissionRationale = false; permissions.requestPermissions() }) { Text("Cấp quyền") } },
            dismissButton = { TextButton(onClick = { showPermissionRationale = false }) { Text("Hủy") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(callGradient)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                // Sóng Pulse
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }
                        .border(2.dp, Color.White, CircleShape)
                )
                // Avatar chính
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    AvatarCircle(initial = initial, size = 160, avatarUrl = avatarUrl)
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = displayName,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isVideo) "Cuộc gọi video đến..." else "Cuộc gọi thoại đến...",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Accept / Reject buttons
            Row(
                modifier = Modifier.padding(bottom = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CallControlButton(
                        icon = { Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp)) },
                        backgroundColor = Color(0xFFEF4444),
                        size = 72,
                        onClick = { viewModel?.rejectCall(); onNavigateBack() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Từ chối", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CallControlButton(
                        icon = { Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(32.dp)) },
                        backgroundColor = Color(0xFF22C55E),
                        size = 72,
                        onClick = {
                            if (permissions.allGranted) {
                                viewModel?.acceptCall(callId)
                                onCallAccepted(if (isVideo) "video" else "voice")
                            } else {
                                pendingAccept = true
                                permissions.requestPermissions()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Chấp nhận", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
