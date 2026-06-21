package com.example.nexus.feature_chat.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.model.GroupMember
import com.example.nexus.data.model.User
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.feature_chat.viewmodel.GroupViewModel
import com.example.nexus.ui.theme.NexusError
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.NexusSecondary
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupInfoContent(
    chatId: String,
    viewModel: ChatViewModel?,
    groupViewModel: GroupViewModel?,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSharedMedia: (String, String) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val context = LocalContext.current
    val currentChat = viewModel?.currentChat?.collectAsState()?.value
    val group = groupViewModel?.group?.collectAsState()?.value
    val sharedContentCounts = viewModel?.sharedContentCounts?.collectAsState()?.value
    val operationState = groupViewModel?.operationState?.collectAsState()?.value
    val isAdmin = groupViewModel?.isAdmin() ?: false
    val currentUserId = groupViewModel?.currentUserId ?: ""

    val themeColorHex = viewModel?.themeColor?.collectAsState()?.value ?: ""
    val mutedState = viewModel?.isMuted?.collectAsState()?.value ?: false

    // Resolve actual User objects for members to get current displayName
    var memberUsers by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    LaunchedEffect(group?.members) {
        val members = group?.members ?: return@LaunchedEffect
        val userMap = mutableMapOf<String, User>()
        for (member in members) {
            if (member.displayName.isNotEmpty()) {
                // Already has displayName, no need to fetch
                continue
            }
            try {
                val user = viewModel?.getUserById(member.userId)
                if (user != null) {
                    userMap[member.userId] = user
                }
            } catch (_: Exception) {}
        }
        memberUsers = userMap
    }

    // Helper to get display name for a member
    fun getMemberDisplayName(member: GroupMember): String {
        // 1. Try GroupMember.displayName (populated for new members)
        if (member.displayName.isNotEmpty()) return member.displayName
        // 2. Try resolved User object
        val user = memberUsers[member.userId]
        if (user != null) return user.displayName.ifEmpty { user.username }
        // 3. Fallback to username
        return member.username.ifEmpty { "User" }
    }

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showDissolveDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showMemberMenu by remember { mutableStateOf<GroupMember?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && group?.id != null) {
            groupViewModel?.updateGroupAvatar(context, chatId, group.id, uri)
        }
    }

    val themeAccentColor = remember(themeColorHex) {
        if (themeColorHex.isNotEmpty()) {
            try { Color(android.graphics.Color.parseColor(themeColorHex)) } catch (_: Exception) { NexusPrimary }
        } else NexusPrimary
    }

    // Handle operation results
    LaunchedEffect(operationState) {
        when (operationState) {
            is Resource.Success -> {
                Toast.makeText(context, "Thành công", Toast.LENGTH_SHORT).show()
                groupViewModel?.clearOperationState()
                if (showDissolveDialog || showLeaveDialog) {
                    onNavigateBack()
                }
            }
            is Resource.Error -> {
                Toast.makeText(context, operationState.message, Toast.LENGTH_SHORT).show()
                groupViewModel?.clearOperationState()
            }
            else -> {}
        }
    }

    val groupName = currentChat?.groupName?.ifEmpty { "Nhóm" } ?: "Nhóm"
    val memberCount = group?.members?.size ?: currentChat?.participants?.size ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(nc.background)
    ) {
        TopAppBar(
            title = {
                Text("Thông tin nhóm", color = nc.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
            // ══════ GROUP PROFILE HEADER ══════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .then(
                            if (isAdmin) Modifier.clickable {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        NexusPrimary.copy(alpha = 0.6f),
                                        NexusSecondary.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = currentChat?.groupAvatarUrl?.ifEmpty { null }
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    if (isAdmin) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(nc.textPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Đổi ảnh nhóm",
                                tint = nc.background,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    groupName,
                    color = nc.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "$memberCount thành viên",
                    color = nc.textSecondary,
                    fontSize = 14.sp
                )
            }

            // ══════ ADMIN ACTIONS ══════
            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton(
                        icon = Icons.Default.PersonAdd,
                        label = "Thêm",
                        color = themeAccentColor,
                        onClick = {
                            groupViewModel?.loadFriendsForAddMember()
                            showAddMemberDialog = true
                        }
                    )
                    ActionButton(
                        icon = Icons.Default.PersonRemove,
                        label = "Xóa thành viên",
                        color = NexusError,
                        onClick = {
                            // Show member selection for kick
                            showMemberMenu = group?.members?.firstOrNull { it.userId != currentUserId }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ══════ MEMBER LIST ══════
            SectionHeader(title = "Thành viên")

            val members = group?.members ?: emptyList()
            members.forEach { member ->
                val isCurrentUser = member.userId == currentUserId
                val memberName = getMemberDisplayName(member)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = nc.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isAdmin && !isCurrentUser) {
                                showMemberMenu = member
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(nc.avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (member.avatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = member.avatarUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    memberName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = nc.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isCurrentUser) "$memberName (Bạn)" else memberName,
                                color = nc.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (isCurrentUser) FontWeight.Medium else FontWeight.Normal
                            )
                            if (member.role == Constants.ROLE_ADMIN) {
                                Text(
                                    "Quản trị viên",
                                    color = themeAccentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (isAdmin && !isCurrentUser) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Tùy chọn",
                                tint = nc.iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ══════ SHARED CONTENT ══════
            SectionHeader(title = "Nội dung chia sẻ")

            SharedContentItem(
                icon = Icons.Default.Image,
                title = "Ảnh & Video",
                count = sharedContentCounts?.first ?: 0,
                onClick = { onNavigateToSharedMedia(chatId, "media") }
            )

            SharedContentItem(
                icon = Icons.Default.InsertDriveFile,
                title = "File",
                count = sharedContentCounts?.second ?: 0,
                onClick = { onNavigateToSharedMedia(chatId, "file") }
            )

            SharedContentItem(
                icon = Icons.Default.Link,
                title = "Liên kết",
                count = sharedContentCounts?.third ?: 0,
                onClick = { onNavigateToSharedMedia(chatId, "link") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ══════ SETTINGS ══════
            SectionHeader(title = "Cài đặt")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = nc.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel?.setMuted(chatId, !mutedState) }
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
                        checked = !mutedState,
                        onCheckedChange = { enabled -> viewModel?.setMuted(chatId, !enabled) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = themeAccentColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = nc.textTertiary
                        )
                    )
                }
            }

            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Đổi chủ đề",
                subtitle = null,
                onClick = { showThemeDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ══════ LEAVE / DISSOLVE ══════
            SectionHeader(title = "Khác")

            if (isAdmin) {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Giải tán nhóm",
                    subtitle = null,
                    onClick = { showDissolveDialog = true },
                    isDestructive = true
                )
            }

            SettingsItem(
                icon = Icons.Default.ExitToApp,
                title = "Rời nhóm",
                subtitle = null,
                onClick = { showLeaveDialog = true },
                isDestructive = true
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ══════ MEMBER ACTION MENU ══════
    if (showMemberMenu != null) {
        val member = showMemberMenu!!
        val memberName = getMemberDisplayName(member)
        AlertDialog(
            onDismissRequest = { showMemberMenu = null },
            containerColor = nc.surface,
            title = {
                Text(memberName, color = nc.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    if (member.role != Constants.ROLE_ADMIN) {
                        TextButton(
                            onClick = {
                                groupViewModel?.promoteToAdmin(chatId, group?.id ?: "", member.userId, memberName)
                                showMemberMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = themeAccentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chỉ định làm quản trị viên", color = nc.textPrimary)
                        }
                    }
                    if (member.role == Constants.ROLE_ADMIN) {
                        TextButton(
                            onClick = {
                                groupViewModel?.demoteAdmin(chatId, group?.id ?: "", member.userId, memberName)
                                showMemberMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = themeAccentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hạ cấp thành viên thường", color = nc.textPrimary)
                        }
                    }
                    TextButton(
                        onClick = {
                            groupViewModel?.removeMember(chatId, group?.id ?: "", member.userId, memberName)
                            showMemberMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonRemove, contentDescription = null, tint = NexusError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xóa khỏi nhóm", color = NexusError)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemberMenu = null }) {
                    Text("Hủy", color = nc.textSecondary)
                }
            }
        )
    }

    // ══════ ADD MEMBER BOTTOM SHEET ══════
    if (showAddMemberDialog) {
        val addFriendsState = groupViewModel?.addMembersFriends?.collectAsState()?.value ?: Resource.Idle
        val addSelected = groupViewModel?.addSelectedMembers?.collectAsState()?.value ?: emptySet()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                showAddMemberDialog = false
                groupViewModel?.clearAddSelectedMembers()
            },
            sheetState = sheetState,
            containerColor = nc.background,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        showAddMemberDialog = false
                        groupViewModel?.clearAddSelectedMembers()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = nc.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Thêm thành viên",
                        color = nc.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "THÊM",
                        color = if (addSelected.isNotEmpty()) NexusPrimary else nc.textTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(enabled = addSelected.isNotEmpty()) {
                                val users = (addFriendsState as? Resource.Success)?.data
                                    ?.filter { addSelected.contains(it.uid) } ?: emptyList()
                                groupViewModel?.addMembers(chatId, group?.id ?: "", users)
                                showAddMemberDialog = false
                                groupViewModel?.clearAddSelectedMembers()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Content
                when (addFriendsState) {
                    is Resource.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NexusPrimary, strokeWidth = 2.dp)
                        }
                    }
                    is Resource.Success -> {
                        val friends = addFriendsState.data
                        if (friends.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Không có bạn bè nào để thêm", color = nc.textSecondary, fontSize = 14.sp)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                friends.forEach { friend ->
                                    val isSelected = addSelected.contains(friend.uid)
                                    val friendName = friend.displayName.ifEmpty { friend.username }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { groupViewModel?.toggleAddMember(friend.uid) }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(nc.avatarBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (friend.avatarUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = friend.avatarUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    friendName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                    color = nc.textPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            friendName,
                                            color = nc.textPrimary,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { groupViewModel?.toggleAddMember(friend.uid) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = NexusPrimary,
                                                uncheckedColor = nc.outline
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((addFriendsState as Resource.Error).message, color = nc.textSecondary)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // ══════ DISSOLVE CONFIRMATION ══════
    if (showDissolveDialog) {
        AlertDialog(
            onDismissRequest = { showDissolveDialog = false },
            containerColor = nc.surface,
            title = {
                Text("Giải tán nhóm?", color = nc.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Nhóm sẽ bị giải tán và tất cả thành viên sẽ không thể nhắn tin trong nhóm này nữa.",
                    color = nc.textSecondary,
                    fontSize = 14.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { showDissolveDialog = false }) {
                    Text("Hủy", color = nc.textSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDissolveDialog = false
                    groupViewModel?.dissolveGroup(chatId, group?.id ?: "")
                }) {
                    Text("Giải tán", color = NexusError, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ══════ LEAVE CONFIRMATION ══════
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = nc.surface,
            title = {
                Text("Rời nhóm?", color = nc.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Bạn sẽ không thể nhận tin nhắn từ nhóm này nữa.",
                    color = nc.textSecondary,
                    fontSize = 14.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Hủy", color = nc.textSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    groupViewModel?.leaveGroup(chatId, group?.id ?: "")
                }) {
                    Text("Rời nhóm", color = NexusError, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ══════ THEME PICKER DIALOG ══════
    if (showThemeDialog) {
        val themeOptions = listOf(
            "" to Color(0xFF3B82F6),
            "#3B82F6" to Color(0xFF3B82F6),
            "#8B5CF6" to Color(0xFF8B5CF6),
            "#22C55E" to Color(0xFF22C55E),
            "#EF4444" to Color(0xFFEF4444),
            "#F97316" to Color(0xFFF97316)
        )
        val themeNames = listOf("Mặc định", "Xanh dương", "Tím", "Xanh lá", "Đỏ", "Cam")
        val selectedThemeIndex = themeOptions.indexOfFirst { it.first == themeColorHex }.coerceAtLeast(0)

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = nc.surface,
            title = {
                Text("Đổi chủ đề", color = nc.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    themeOptions.forEachIndexed { index, (hex, color) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                viewModel?.updateChatTheme(chatId, hex)
                                showThemeDialog = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (selectedThemeIndex == index) {
                                            Modifier.border(3.dp, nc.textPrimary, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedThemeIndex == index) {
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
                                themeNames[index],
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
}
