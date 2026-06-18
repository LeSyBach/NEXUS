package com.example.nexus.feature_admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.data.model.SystemNotification
import com.example.nexus.feature_admin.viewmodel.AdminViewModel
import com.example.nexus.ui.theme.nexusColors

// Colors for notification types
private val CyanAccent = Color(0xFF00C6FF)
private val PurpleAccent = Color(0xFFA855F7)
private val GreenAccent = Color(0xFF22C55E)
private val OrangeAccent = Color(0xFFF97316)
private val RedAccent = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val notifications by viewModel.systemNotifications.collectAsState()
    val readNotificationIds by viewModel.readNotificationIds.collectAsState()
    var selectedNotification by remember { mutableStateOf<SystemNotification?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông báo", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = nc.background,
                    titleContentColor = nc.textPrimary,
                    navigationIconContentColor = nc.textPrimary
                )
            )
        },
        containerColor = nc.background
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Chưa có thông báo nào",
                        color = nc.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Thông báo mới sẽ xuất hiện tại đây",
                        color = nc.textTertiary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(notifications) { notification ->
                    val isRead = notification.id in readNotificationIds
                    NotificationItem(
                        notification = notification,
                        isRead = isRead,
                        onClick = {
                            viewModel.markAsRead(notification.id)
                            selectedNotification = notification
                        }
                    )
                }
            }
        }
    }

    // Detail dialog
    selectedNotification?.let { notif ->
        NotificationDetailDialog(
            notification = notif,
            onDismiss = { selectedNotification = null }
        )
    }
}

@Composable
private fun NotificationItem(
    notification: SystemNotification,
    isRead: Boolean,
    onClick: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val timeAgo = remember(notification.created_at) {
        formatTimeAgo(notification.created_at)
    }

    // Determine notification icon and color based on content
    val icon: ImageVector
    val accentColor: Color
    val result = remember(notification.title) {
        when {
            notification.title.contains("khóa", ignoreCase = true) ||
            notification.title.contains("cấm", ignoreCase = true) ->
                Icons.Default.Warning to RedAccent
            notification.title.contains("mở khóa", ignoreCase = true) ||
            notification.title.contains("thông báo", ignoreCase = true) ->
                Icons.Default.Info to CyanAccent
            else -> Icons.Default.Campaign to PurpleAccent
        }
    }
    icon = result.first
    accentColor = result.second

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isRead) Modifier.background(nc.cardBg)
                else Modifier.background(
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.08f),
                            nc.cardBg
                        )
                    )
                )
            )
            .then(
                if (!isRead) Modifier.border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = if (isRead) 0.05f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isRead) nc.textTertiary else accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        color = if (isRead) nc.textSecondary else nc.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeAgo,
                        color = nc.textTertiary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notification.body,
                    color = if (isRead) nc.textTertiary else nc.textSecondary,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            // Unread indicator dot
            if (!isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
private fun NotificationDetailDialog(
    notification: SystemNotification,
    onDismiss: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val timeAgo = remember(notification.created_at) {
        formatTimeAgo(notification.created_at)
    }
    val fullTime = remember(notification.created_at) {
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault())
            val date = sdf.parse(notification.created_at)
            val outFormat = java.text.SimpleDateFormat("HH:mm - dd/MM/yyyy", java.util.Locale.getDefault())
            outFormat.format(date!!)
        } catch (e: Exception) {
            notification.created_at
        }
    }

    // Determine notification icon and color
    val icon: ImageVector
    val accentColor: Color
    val result = remember(notification.title) {
        when {
            notification.title.contains("khóa", ignoreCase = true) ||
            notification.title.contains("cấm", ignoreCase = true) ->
                Icons.Default.Warning to RedAccent
            notification.title.contains("mở khóa", ignoreCase = true) ||
            notification.title.contains("thông báo", ignoreCase = true) ->
                Icons.Default.Info to CyanAccent
            else -> Icons.Default.Campaign to PurpleAccent
        }
    }
    icon = result.first
    accentColor = result.second

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = nc.cardBg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = notification.title,
                        color = nc.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "$timeAgo • $fullTime",
                        color = nc.textTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Text(
                text = notification.body,
                color = nc.textSecondary,
                fontSize = 15.sp,
                lineHeight = 24.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = CyanAccent, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

private fun formatTimeAgo(createdAt: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault())
        val date = sdf.parse(createdAt)
        val diff = System.currentTimeMillis() - (date?.time ?: 0L)
        val mins = diff / 60000
        when {
            mins < 1 -> "Vừa xong"
            mins < 60 -> "${mins}p trước"
            mins < 1440 -> "${mins / 60}h trước"
            mins < 10080 -> "${mins / 1440} ngày trước"
            else -> {
                val outFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                outFormat.format(date!!)
            }
        }
    } catch (e: Exception) {
        ""
    }
}
