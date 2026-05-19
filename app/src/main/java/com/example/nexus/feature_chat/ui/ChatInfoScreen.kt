package com.example.nexus.feature_chat.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.DateUtils
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.ui.theme.NexusError
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.OnlineGreen
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    chatId: String,
    viewModel: ChatViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onStartCall: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val nc = MaterialTheme.nexusColors
    val context = LocalContext.current
    val otherUser = viewModel?.otherUser?.collectAsState()?.value
    val sharedContentCounts = viewModel?.sharedContentCounts?.collectAsState()?.value

    var notificationsEnabled by remember { mutableStateOf(true) }
    var selectedTheme by remember { mutableIntStateOf(0) }
    var nickname by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showBlockUserDialog by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        viewModel?.loadMessages(chatId)
        viewModel?.loadSharedContentCounts(chatId)
    }

    val displayName = otherUser?.let { it.displayName.ifEmpty { it.username } } ?: "Đang tải..."
    val avatarInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val isOnline = otherUser?.status == Constants.USER_STATUS_ONLINE
    val lastSeenText = if (isOnline) {
        "Đang hoạt động"
    } else {
        otherUser?.lastSeen?.let { DateUtils.formatLastSeen(it.toDate().time) } ?: "Offline"
    }

    val themeOptions = listOf(
        "Xanh dương" to Color(0xFF3B82F6),
        "Tím" to Color(0xFF8B5CF6),
        "Xanh lá" to Color(0xFF22C55E),
        "Đỏ" to Color(0xFFEF4444),
        "Cam" to Color(0xFFF97316)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(nc.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Thông tin",
                    color = nc.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = nc.background,
                titleContentColor = nc.textPrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ══════ USER PROFILE SECTION ══════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    NexusPrimary.copy(alpha = 0.6f),
                                    Color(0xFFBB86FC).copy(alpha = 0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        avatarInitial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    displayName,
                    color = nc.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) OnlineGreen else nc.textTertiary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        lastSeenText,
                        color = if (isOnline) OnlineGreen else nc.textSecondary,
                        fontSize = 14.sp
                    )
                }

                if (!otherUser?.phone.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        otherUser!!.phone,
                        color = nc.textSecondary,
                        fontSize = 14.sp
                    )
                }

                if (!otherUser?.bio.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        otherUser!!.bio,
                        color = nc.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            // ══════ ACTION BUTTONS ROW ══════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = Icons.Default.Chat,
                    label = "Nhắn tin",
                    color = NexusPrimary,
                    onClick = { onNavigateToChat(chatId) }
                )
                ActionButton(
                    icon = Icons.Default.Call,
                    label = "Gọi",
                    color = NexusPrimary,
                    onClick = {
                        val otherId = otherUser?.uid ?: ""
                        if (otherId.isNotEmpty()) onStartCall(otherId, "voice", displayName)
                    }
                )
                ActionButton(
                    icon = Icons.Default.Videocam,
                    label = "Video",
                    color = NexusPrimary,
                    onClick = {
                        val otherId = otherUser?.uid ?: ""
                        if (otherId.isNotEmpty()) onStartCall(otherId, "video", displayName)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ══════ CHAT SETTINGS SECTION ══════
            SectionHeader(title = "Cài đặt trò chuyện")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = nc.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { notificationsEnabled = !notificationsEnabled }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = nc.iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Thông báo",
                        color = nc.textPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NexusPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = nc.textTertiary
                        )
                    )
                }
            }

            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Đổi chủ đề",
                subtitle = themeOptions[selectedTheme].first,
                onClick = { showThemeDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Edit,
                title = "Đổi biệt danh",
                subtitle = nickname.ifEmpty { null },
                onClick = { showNicknameDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ══════ SHARED CONTENT SECTION ══════
            SectionHeader(title = "Nội dung chia sẻ")

            SharedContentItem(
                icon = Icons.Default.Image,
                title = "Ảnh & Video",
                count = sharedContentCounts?.first ?: 0,
                onClick = { }
            )

            SharedContentItem(
                icon = Icons.Default.InsertDriveFile,
                title = "File",
                count = sharedContentCounts?.second ?: 0,
                onClick = { }
            )

            SharedContentItem(
                icon = Icons.Default.Link,
                title = "Liên kết",
                count = sharedContentCounts?.third ?: 0,
                onClick = { }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ══════ OTHER ACTIONS ══════
            SectionHeader(title = "Khác")

            SettingsItem(
                icon = Icons.Default.Share,
                title = "Chia sẻ liên hệ",
                subtitle = null,
                onClick = {
                    Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsItem(
                icon = Icons.Default.Search,
                title = "Tìm kiếm trong cuộc trò chuyện",
                subtitle = null,
                onClick = {
                    Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsItem(
                icon = Icons.Default.Archive,
                title = "Lưu trữ cuộc trò chuyện",
                subtitle = null,
                onClick = {
                    Toast.makeText(context, "Đã lưu trữ cuộc trò chuyện", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsItem(
                icon = Icons.Default.Block,
                title = "Chặn người dùng",
                subtitle = null,
                onClick = { showBlockUserDialog = true },
                isDestructive = true
            )

            SettingsItem(
                icon = Icons.Default.Delete,
                title = "Xóa cuộc trò chuyện",
                subtitle = null,
                onClick = {
                    Toast.makeText(context, "Đã xóa cuộc trò chuyện", Toast.LENGTH_SHORT).show()
                },
                isDestructive = true
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ══════ THEME PICKER DIALOG ══════
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = nc.surface,
            title = {
                Text("Đổi chủ đề", color = nc.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    themeOptions.forEachIndexed { index, (name, color) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedTheme = index
                                showThemeDialog = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (selectedTheme == index) {
                                            Modifier.border(3.dp, nc.textPrimary, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedTheme == index) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                name,
                                color = nc.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Đóng", color = NexusPrimary)
                }
            }
        )
    }

    // ══════ NICKNAME DIALOG ══════
    if (showNicknameDialog) {
        var nicknameInput by remember { mutableStateOf(nickname) }
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            containerColor = nc.surface,
            title = {
                Text("Đổi biệt danh", color = nc.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    placeholder = {
                        Text(
                            displayName,
                            color = nc.textTertiary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = nc.outline,
                        focusedTextColor = nc.textPrimary,
                        unfocusedTextColor = nc.textPrimary,
                        cursorColor = NexusPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) {
                    Text("Hủy", color = nc.textSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    nickname = nicknameInput
                    showNicknameDialog = false
                }) {
                    Text("Lưu", color = NexusPrimary)
                }
            }
        )
    }

    // ══════ BLOCK USER CONFIRMATION DIALOG ══════
    if (showBlockUserDialog) {
        AlertDialog(
            onDismissRequest = { showBlockUserDialog = false },
            containerColor = nc.surface,
            title = {
                Text(
                    "Chặn $displayName?",
                    color = nc.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Người này sẽ không thể nhắn tin hoặc gọi cho bạn",
                    color = nc.textSecondary,
                    fontSize = 14.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { showBlockUserDialog = false }) {
                    Text("Hủy", color = nc.textSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showBlockUserDialog = false
                    Toast.makeText(context, "Đã chặn $displayName", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Chặn", color = NexusError)
                }
            }
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = nc.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionHeader(title: String) {
    val nc = MaterialTheme.nexusColors
    Text(
        text = title,
        color = nc.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val nc = MaterialTheme.nexusColors
    val titleColor = if (isDestructive) NexusError else nc.textPrimary
    val iconColor = if (isDestructive) NexusError else nc.iconTint

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDestructive) NexusError.copy(alpha = 0.08f) else nc.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = titleColor, fontSize = 15.sp)
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, color = nc.textSecondary, fontSize = 13.sp)
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = nc.iconTintSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SharedContentItem(
    icon: ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = nc.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = nc.iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = nc.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(
                count.toString(),
                color = nc.textSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = nc.iconTintSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
