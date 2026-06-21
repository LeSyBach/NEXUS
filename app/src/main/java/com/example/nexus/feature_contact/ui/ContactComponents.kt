package com.example.nexus.feature_contact.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.model.FriendRequest
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

@Composable
fun ContactItem(name: String, status: String, isOnline: Boolean, avatarUrl: String? = null, onClick: () -> Unit) {
    val nc = MaterialTheme.nexusColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(50.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NexusPrimary.copy(alpha = 0.4f), nc.cardBg))),
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
                        name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = nc.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .border(2.dp, nc.background, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = nc.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (status.isNotEmpty()) {
                Text(
                    status,
                    color = if (isOnline) Color(0xFF22C55E) else nc.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.Chat,
            contentDescription = "Nhắn tin",
            tint = NexusPrimary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun ReceivedRequestsTab(
    state: Resource<List<FriendRequest>>,
    processingId: String?,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nc = MaterialTheme.nexusColors
    Box(modifier = modifier) {
        when (state) {
            is Resource.Loading -> {
                CircularProgressIndicator(color = NexusPrimary, modifier = Modifier.align(Alignment.Center), strokeWidth = 2.dp)
            }
            is Resource.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.PersonAddAlt,
                        title = "Chưa có lời mời nào",
                        subtitle = "Khi ai đó gửi lời mời kết bạn, nó sẽ hiển thị ở đây"
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(state.data) { request ->
                            val isProcessing = processingId == request.id
                            ReceivedRequestItem(
                                request = request,
                                isProcessing = isProcessing,
                                onAccept = { onAccept(request.id, request.fromUserId) },
                                onReject = { onReject(request.id, request.fromUserId) }
                            )
                        }
                    }
                }
            }
            is Resource.Error -> {
                Text(
                    state.message,
                    color = nc.textSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                EmptyState(
                    icon = Icons.Default.PersonAddAlt,
                    title = "Chưa có lời mời nào",
                    subtitle = "Khi ai đó gửi lời mời kết bạn, nó sẽ hiển thị ở đây"
                )
            }
        }
    }
}

@Composable
internal fun SentRequestsTab(
    state: Resource<List<FriendRequest>>,
    onCancel: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val nc = MaterialTheme.nexusColors
    Box(modifier = modifier) {
        when (state) {
            is Resource.Loading -> {
                CircularProgressIndicator(color = NexusPrimary, modifier = Modifier.align(Alignment.Center), strokeWidth = 2.dp)
            }
            is Resource.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.HowToReg,
                        title = "Chưa gửi lời mời nào",
                        subtitle = "Tìm kiếm và gửi lời mời kết bạn để bắt đầu"
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(state.data) { request ->
                            SentRequestItem(
                                request = request,
                                onCancel = { onCancel(request.toUserId) },
                                onClick = { onNavigateToProfile(request.toUserId) }
                            )
                        }
                    }
                }
            }
            is Resource.Error -> {
                Text(state.message, color = nc.textSecondary, modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                EmptyState(
                    icon = Icons.Outlined.HowToReg,
                    title = "Chưa gửi lời mời nào",
                    subtitle = "Tìm kiếm và gửi lời mời kết bạn để bắt đầu"
                )
            }
        }
    }
}

@Composable
internal fun ReceivedRequestItem(
    request: FriendRequest,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NexusPrimary.copy(alpha = 0.4f), nc.cardBg))),
            contentAlignment = Alignment.Center
        ) {
            if (request.fromAvatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = request.fromAvatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    request.fromUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = nc.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                request.fromUsername.ifEmpty { "Người dùng" },
                color = nc.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text("Muốn kết bạn với bạn", color = nc.textSecondary, fontSize = 12.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onReject,
                enabled = !isProcessing,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFFEF4444))
                } else {
                    Icon(Icons.Outlined.Close, contentDescription = "Từ chối", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                }
            }
            IconButton(
                onClick = onAccept,
                enabled = !isProcessing,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF22C55E).copy(alpha = 0.15f), CircleShape)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF22C55E))
                } else {
                    Icon(Icons.Outlined.Check, contentDescription = "Đồng ý", tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun SentRequestItem(
    request: FriendRequest,
    onCancel: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NexusPrimary.copy(alpha = 0.4f), nc.cardBg))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                request.toUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = nc.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                request.toUsername.ifEmpty { "Người dùng" },
                color = nc.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text("Đã gửi lời mời kết bạn", color = nc.textSecondary, fontSize = 12.sp)
        }
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Hủy yêu cầu",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val nc = MaterialTheme.nexusColors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(NexusPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = NexusPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = nc.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            subtitle,
            color = nc.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}
