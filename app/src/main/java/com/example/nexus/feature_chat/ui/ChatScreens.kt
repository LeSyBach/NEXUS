package com.example.nexus.feature_chat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.DateUtils
import com.example.nexus.core.utils.Resource
import com.example.nexus.core.utils.createTempImageUri
import com.example.nexus.core.utils.toReadableFileSize
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Message
import com.example.nexus.data.firebase.PlaybackState
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.feature_chat.viewmodel.UploadState
import com.example.nexus.feature_chat.viewmodel.VoiceRecordingState
import com.example.nexus.navigation.Screen
import com.example.nexus.ui.components.NexusBottomBar

import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors
import com.example.nexus.data.firebase.NexusMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.nexus.ui.theme.NexusSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel? = null,
    onNavigateToConversation: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToTab: (String) -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    val chatsState = viewModel?.chatsState?.collectAsState()?.value ?: Resource.Idle
    val onlineFriendsState = viewModel?.onlineFriends?.collectAsState()?.value ?: emptyList()
    var showAddMenu by remember { mutableStateOf(false) }
    var pinnedChatIds by remember { mutableStateOf(setOf<String>()) }
    var showChatMenu by remember { mutableStateOf<Pair<String, String>?>(null) }
    Scaffold(
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = onNavigateToCreateGroup,
//                containerColor = NexusPrimary,
//                contentColor = nc.sentBubbleText,
//                shape = RoundedCornerShape(16.dp),
//                modifier = Modifier.size(64.dp)
//            ) {
//                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "New Chat", modifier = Modifier.size(32.dp))
//            }
//        },
        bottomBar = {
            NexusBottomBar(
                currentRoute = Screen.ChatList.route,
                onNavigate = onNavigateToTab
            )
        },
        containerColor = nc.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Text(
                    text = "NEXUS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = NexusPrimary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(nc.searchBg, RoundedCornerShape(24.dp))
                            .clickable { onNavigateToSearch() }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = nc.textSecondary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Tìm kiếm, AI...", color = nc.textSecondary, fontSize = 15.sp, maxLines = 1)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Box {
                        IconButton(
                            onClick = { showAddMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(nc.cardBg, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm mới", tint = nc.textPrimary)
                        }

                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false },
                            modifier = Modifier.background(nc.cardBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Thêm bạn", color = nc.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NexusPrimary) },
                                onClick = {
                                    showAddMenu = false
                                    onNavigateToSearch()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tạo nhóm", color = nc.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = NexusPrimary) },
                                onClick = {
                                    showAddMenu = false
                                    onNavigateToCreateGroup()
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ĐANG TRỰC TUYẾN",
                    color = nc.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .border(1.dp, nc.outline, CircleShape)
                                    .clickable { onNavigateToSearch() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = nc.textSecondary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Mới", color = nc.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    items(onlineFriendsState.size) { index ->
                        val friend = onlineFriendsState[index]
                        val name = friend.displayName.ifEmpty { friend.username }
                        OnlineFriendItem(name = name)
                    }
                }
            }

            item {
                Text(
                    text = "TRÒ CHUYỆN",
                    color = nc.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 12.dp)
                )
            }

            if (chatsState is Resource.Loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NexusPrimary)
                    }
                }
            } else if (chatsState is Resource.Success && chatsState.data.isNotEmpty()) {
                val sortedChats = chatsState.data.sortedByDescending { it.id in pinnedChatIds }
                items(sortedChats.size) { index ->
                    val chat = sortedChats[index]
                    val lastMessageText = chat.lastMessage?.text ?: "Chưa có tin nhắn"

                    val timeStr = chat.lastMessage?.timestamp?.toDate()?.let { DateUtils.formatChatTime(it.time) } ?: ""

                    var displayName by remember { mutableStateOf(chat.groupName.ifEmpty { "..." }) }
                    LaunchedEffect(chat.id) {
                        displayName = viewModel?.resolveDisplayName(chat) ?: chat.groupName
                    }

                    val myId = viewModel?.currentUserId
                    val unreadCount = if (myId != null) (chat.lastMessage?.unreadCount?.get(myId) ?: 0L).toInt() else 0

                    ChatItem(
                        name = displayName,
                        lastMessage = lastMessageText,
                        time = timeStr,
                        unreadCount = unreadCount,
                        isOnline = false,
                        isPinned = chat.id in pinnedChatIds,
                        onClick = { onNavigateToConversation(chat.id) },
                        onLongClick = { showChatMenu = Pair(chat.id, displayName) }
                    )
                }
            } else if (chatsState is Resource.Success && chatsState.data.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Chưa có cuộc trò chuyện nào", color = nc.textSecondary, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Kết bạn và bắt đầu nhắn tin!", color = NexusPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    showChatMenu?.let { (chatId, chatName) ->
        val isChatPinned = chatId in pinnedChatIds
        AlertDialog(
            onDismissRequest = { showChatMenu = null },
            containerColor = nc.surfaceElevated,
            title = { Text(chatName, color = nc.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pinnedChatIds = if (isChatPinned) {
                                    pinnedChatIds - chatId
                                } else {
                                    if (pinnedChatIds.size < 3) pinnedChatIds + chatId
                                    else pinnedChatIds
                                }
                                showChatMenu = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            tint = if (isChatPinned) nc.textTertiary else NexusPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (isChatPinned) "Bỏ ghim" else "Ghim tin nhắn",
                            color = nc.textPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showChatMenu = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, tint = nc.iconTint, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Lưu trữ", color = nc.textPrimary)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showChatMenu = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = nc.errorText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Xóa cuộc trò chuyện", color = nc.errorText)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChatMenu = null }) {
                    Text("Đóng", color = NexusPrimary)
                }
            }
        )
    }
}

@Composable
fun OnlineFriendItem(name: String) {
    val nc = MaterialTheme.nexusColors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(listOf(NexusPrimary, NexusSecondary)),
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(nc.avatarBg),
                contentAlignment = Alignment.Center
            ) {
                val initial = name.firstOrNull()?.toString() ?: "?"
                Text(initial, color = nc.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(2.dp, nc.background, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, color = nc.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
    name: String,
    lastMessage: String,
    time: String,
    unreadCount: Int,
    isOnline: Boolean,
    isPinned: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    val isUnread = unreadCount > 0
    val previewText = if (isUnread) {
        val capped = if (unreadCount > 9) "9+" else unreadCount.toString()
        "$capped tin nhắn mới"
    } else {
        lastMessage
    }
    val previewWithTime = if (time.isNotEmpty()) "$previewText · $time" else previewText

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isUnread) Modifier.background(nc.unreadBadge.copy(alpha = 0.06f))
                else Modifier
            )
    ) {
        if (isUnread) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(nc.unreadBadge)
                    .align(Alignment.CenterStart)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(56.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isUnread) Modifier.border(2.5.dp, nc.unreadBadge, CircleShape)
                            else Modifier
                        )
                        .padding(if (isUnread) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .background(nc.avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = name.firstOrNull()?.toString() ?: "?"
                    Text(text = initial, color = nc.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        color = if (isUnread) nc.unreadMessageText else nc.textPrimary,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Đã ghim",
                            tint = nc.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = previewWithTime,
                        color = if (isUnread) nc.unreadMessageText else nc.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(nc.unreadBadge)
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  CONVERSATION SCREEN
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatId: String,
    viewModel: ChatViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (String) -> Unit,
    onStartCall: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val nc = MaterialTheme.nexusColors
    var messageText by remember { mutableStateOf("") }
    val messagesState = viewModel?.messagesState?.collectAsState()?.value ?: Resource.Idle
    val currentUserId = viewModel?.currentUserId
    val currentChat = viewModel?.currentChat?.collectAsState()?.value
    val otherUser = viewModel?.otherUser?.collectAsState()?.value
    val listState = rememberLazyListState()

    val uploadState = viewModel?.uploadState?.collectAsState()?.value ?: UploadState.Idle
    val pendingImageUri = viewModel?.pendingImageUri?.collectAsState()?.value
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel?.sendImageMessage(chatId, uri, context)
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            viewModel?.sendImageMessage(chatId, tempCameraUri!!, context)
        }
        tempCameraUri = null
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel?.sendFileMessage(chatId, uri, context)
        }
    }

    val voiceState = viewModel?.voiceState?.collectAsState()?.value ?: VoiceRecordingState.Idle
    val voiceRecordTimeSec = viewModel?.voiceRecordTimeSec?.collectAsState()?.value ?: 0L
    val voiceAmplitudes = viewModel?.voiceAmplitudes?.collectAsState()?.value ?: emptyList()
    val playbackState = viewModel?.audioPlayerHelper?.state?.collectAsState()?.value ?: PlaybackState()

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            viewModel?.startVoiceRecording(context)
        }
    }

    LaunchedEffect(chatId) {
        viewModel?.loadMessages(chatId)
    }

    // Mark as seen once when entering the chat (not on every message update to avoid infinite loop)
    LaunchedEffect(chatId) {
        kotlinx.coroutines.delay(1000) // Wait for messages to load first
        viewModel?.markMessagesAsSeen(chatId)
    }

    LaunchedEffect(chatId) {
        viewModel?.startObservingTyping(chatId)
    }

    // Suppress push notification khi đang xem cuộc trò chuyện này
    DisposableEffect(chatId) {
        NexusMessagingService.activeChatId = chatId
        onDispose {
            // Chỉ clear nếu vẫn đang là chat này (không clear nếu đã navigate sang chat khác)
            if (NexusMessagingService.activeChatId == chatId) {
                NexusMessagingService.activeChatId = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel?.stopObservingTyping()
        }
    }

    val isOtherTyping = viewModel?.isTyping?.collectAsState()?.value ?: false
    var showMessageMenu by remember { mutableStateOf<Pair<String, Message>?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val otherId = otherUser?.uid ?: ""

    val isGroup = currentChat?.type == Constants.CHAT_TYPE_GROUP
    val displayName = if (isGroup) {
        currentChat?.groupName?.ifEmpty { "Nhóm" } ?: "Nhóm"
    } else {
        otherUser?.let { it.displayName.ifEmpty { it.username } } ?: "Đang tải..."
    }
    val statusText = if (isGroup) {
        "${currentChat?.participants?.size ?: 0} thành viên"
    } else {
        if (otherUser?.status == Constants.USER_STATUS_ONLINE) "Đang hoạt động"
        else otherUser?.lastSeen?.let { DateUtils.formatLastSeen(it.toDate().time) } ?: ""
    }
    val avatarInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(nc.background)
            .imePadding()
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(nc.background)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(nc.avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(avatarInitial, color = nc.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToGroupInfo(chatId) }
            ) {
                Text(
                    displayName,
                    color = nc.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isOtherTyping) {
                    Text("Đang nhập...", color = Color(0xFF22C55E), fontSize = 12.sp)
                } else if (statusText.isNotEmpty()) {
                    Text(
                        statusText,
                        color = if (!isGroup && otherUser?.status == Constants.USER_STATUS_ONLINE) Color(0xFF22C55E) else nc.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = {
                val otherId = otherUser?.uid ?: ""
                if (otherId.isNotEmpty()) onStartCall(otherId, "voice", displayName)
            }) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = NexusPrimary, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = {
                val otherId = otherUser?.uid ?: ""
                if (otherId.isNotEmpty()) onStartCall(otherId, "video", displayName)
            }) {
                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = NexusPrimary, modifier = Modifier.size(22.dp))
            }
        }

        // Divider
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(nc.divider))

        // ── Messages ──
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (messagesState) {
                is Resource.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NexusPrimary, strokeWidth = 2.dp)
                        }
                    }
                }
                is Resource.Success -> {
                    // Upload progress bubble (appears at bottom since reverseLayout=true)
                    if (uploadState is UploadState.Uploading && pendingImageUri != null) {
                        item {
                            UploadProgressBubble(
                                imageUri = pendingImageUri,
                                isMe = true
                            )
                        }
                    }

                    if (messagesState.data.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(NexusPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = NexusPrimary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Chưa có tin nhắn nào", color = nc.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Gửi tin nhắn để bắt đầu trò chuyện!", color = nc.textSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(messagesState.data.size) { index ->
                            val msg = messagesState.data[index]
                            val isMe = msg.senderId == currentUserId
                            val timeStr = msg.timestamp?.toDate()?.let { DateUtils.formatMessageTime(it.time) } ?: ""
                            val senderInitial = if (isMe) {
                                ""
                            } else {
                                val baseName = msg.senderName.ifEmpty { displayName }
                                baseName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                            }
                            val isLastFromSender = if (!isMe) {
                                val prevMsg = messagesState.data.getOrNull(index - 1)
                                prevMsg == null || prevMsg.senderId != msg.senderId
                            } else {
                                false
                            }

                            val showDateSeparator = if (index < messagesState.data.size - 1) {
                                val currDate = msg.timestamp?.toDate()
                                val nextDate = messagesState.data[index + 1].timestamp?.toDate()
                                currDate != null && nextDate != null &&
                                    java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(currDate) !=
                                    java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(nextDate)
                            } else if (index == messagesState.data.size - 1) {
                                true
                            } else false

                            // Only show status on the newest message, and only if I sent it
                            val showStatus = isMe && index == 0 && msg.status != "recalled"

                            if (msg.type == Constants.MESSAGE_TYPE_CALL) {
                                CallHistoryBubble(
                                    message = msg,
                                    isMe = isMe,
                                    time = timeStr,
                                    showDateSeparator = showDateSeparator,
                                    dateSeparatorText = msg.timestamp?.toDate()?.let { DateUtils.formatDateSeparator(it.time) } ?: "",
                                    avatarInitial = senderInitial,
                                    showAvatar = !isMe && isLastFromSender,
                                    onStartCall = {
                                        if (otherId.isNotEmpty()) onStartCall(otherId, msg.text, displayName)
                                    }
                                )
                            } else {
                                MessageBubble(
                                    text = msg.text,
                                    isMe = isMe,
                                    time = timeStr,
                                    status = if (showStatus) msg.status else "",
                                    showDateSeparator = showDateSeparator,
                                    dateSeparatorText = msg.timestamp?.toDate()?.let { DateUtils.formatDateSeparator(it.time) } ?: "",
                                    isRecalled = msg.status == "recalled",
                                    avatarInitial = senderInitial,
                                    showAvatar = !isMe && isLastFromSender,
                                    messageType = msg.type,
                                    duration = msg.duration,
                                    message = msg,
                                    onLongClick = { showMessageMenu = Pair(chatId, msg) }
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Lỗi tải tin nhắn", color = nc.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
                else -> {}
            }
        }

        // Message action dialog
        showMessageMenu?.let { (chatIdForMenu, msg) ->
            AlertDialog(
                onDismissRequest = { showMessageMenu = null },
                containerColor = nc.surfaceElevated,
                title = { Text("Tin nhắn", color = nc.textPrimary) },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                clipboardManager.setText(AnnotatedString(msg.text))
                                showMessageMenu = null
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = nc.iconTint, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Sao chép", color = nc.textPrimary)
                        }

                        if (msg.senderId == currentUserId) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel?.recallMessage(chatIdForMenu, msg.id)
                                    showMessageMenu = null
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null, tint = NexusPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Thu hồi", color = nc.textPrimary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel?.deleteMessage(chatIdForMenu, msg.id)
                                showMessageMenu = null
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = nc.errorText, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Xóa", color = nc.errorText)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showMessageMenu = null }) {
                        Text("Đóng", color = NexusPrimary)
                    }
                }
            )
        }

        // ── Input Area ──
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(nc.divider))

        when (voiceState) {
            is VoiceRecordingState.Recording -> {
                // Recording: Trash | Pause | Timer | Waveform | Send
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(nc.background)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Trash - cancel recording
                    IconButton(
                        onClick = { viewModel?.cancelVoicePreview() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hủy", tint = Color(0xFFFF3B30), modifier = Modifier.size(22.dp))
                    }

                    // Pause (→ preview)
                    IconButton(
                        onClick = { viewModel?.stopVoiceRecording() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Tạm dừng", tint = NexusPrimary, modifier = Modifier.size(24.dp))
                    }

                    // Timer
                    val mins = voiceRecordTimeSec / 60
                    val secs = voiceRecordTimeSec % 60
                    Text(
                        text = String.format("%d:%02d", mins, secs),
                        color = nc.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )

                    // Waveform bars
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val bars = if (voiceAmplitudes.size >= 25) voiceAmplitudes.takeLast(25)
                        else List(25 - voiceAmplitudes.size) { 2 } + voiceAmplitudes
                        for (amp in bars) {
                            val height = (amp / 100f * 28).dp.coerceAtLeast(2.dp)
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(height)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFFF3B30).copy(alpha = 0.7f))
                            )
                        }
                    }

                    // Send directly
                    IconButton(
                        onClick = { viewModel?.sendVoiceDirectly(chatId, context) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NexusPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            is VoiceRecordingState.Previewing -> {
                // Preview mode: delete | re-record | continue | waveform seek | send
                val preview = voiceState as VoiceRecordingState.Previewing
                val currentPosMs = playbackState.currentPositionMs
                val previewProgress = playbackState.progress
                val mins = preview.durationSec / 60
                val secs = preview.durationSec % 60
                val durationText = String.format("%d:%02d", mins, secs)
                val posMins = currentPosMs / 1000 / 60
                val posSecs = (currentPosMs / 1000) % 60
                val positionText = String.format("%d:%02d", posMins, posSecs)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(nc.background)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Delete
                    IconButton(
                        onClick = { viewModel?.cancelVoicePreview() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFFF3B30), modifier = Modifier.size(22.dp))
                    }

                    // Re-record
                    IconButton(
                        onClick = { viewModel?.reRecordVoice(context) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Ghi lại", tint = NexusPrimary, modifier = Modifier.size(22.dp))
                    }

                    // Continue (play/resume preview)
                    IconButton(
                        onClick = { viewModel?.toggleVoicePreview(context) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Tạm dừng" else "Tiếp tục",
                            tint = NexusPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Waveform seek (horizontal drag)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                    viewModel?.seekVoicePreview(fraction)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    viewModel?.seekVoicePreview(fraction)
                                }
                            },
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val barCount = 28
                        val bars = if (voiceAmplitudes.size >= barCount) voiceAmplitudes.takeLast(barCount)
                        else List(barCount - voiceAmplitudes.size) { 2 } + voiceAmplitudes
                        val activeBar = (previewProgress * barCount).toInt().coerceIn(0, barCount - 1)
                        for (i in 0 until barCount) {
                            val amp = bars[i]
                            val height = (amp / 100f * 28).dp.coerceAtLeast(2.dp)
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(height)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (i <= activeBar) NexusPrimary else nc.divider)
                            )
                        }
                    }

                    Text(
                        text = if (playbackState.isPlaying) positionText else durationText,
                        color = nc.textSecondary,
                        fontSize = 11.sp
                    )

                    // Send button
                    IconButton(
                        onClick = { viewModel?.sendVoicePreview(chatId, context) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NexusPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            else -> {
                // Normal input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(nc.background)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Gửi file", tint = NexusPrimary, modifier = Modifier.size(24.dp))
                    }
                    IconButton(
                        onClick = {
                            val uri = context.createTempImageUri()
                            tempCameraUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = NexusPrimary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Gallery", tint = NexusPrimary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel?.startVoiceRecording(context)
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Mic", tint = NexusPrimary, modifier = Modifier.size(24.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(nc.cardBg, RoundedCornerShape(22.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (messageText.isEmpty()) {
                                    Text("Aa", color = nc.textSecondary, fontSize = 15.sp)
                                }
                                BasicTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    textStyle = TextStyle(color = nc.textPrimary, fontSize = 15.sp),
                                    cursorBrush = SolidColor(NexusPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(
                                onClick = { },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji", tint = NexusPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (messageText.isNotEmpty()) {
                                viewModel?.sendMessage(chatId, messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        val isSendEnabled = messageText.isNotEmpty()
                        Icon(
                            if (isSendEnabled) Icons.AutoMirrored.Filled.Send else Icons.Default.ThumbUp,
                            contentDescription = if (isSendEnabled) "Send" else "Like",
                            tint = NexusPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    text: String,
    isMe: Boolean,
    time: String,
    status: String = "",
    showDateSeparator: Boolean = false,
    dateSeparatorText: String = "",
    isRecalled: Boolean = false,
    avatarInitial: String = "",
    showAvatar: Boolean = false,
    messageType: String = Constants.MESSAGE_TYPE_TEXT,
    duration: Long = 0,
    message: Message? = null,
    onLongClick: (() -> Unit)? = null
) {
    val nc = MaterialTheme.nexusColors
    val avatarSize = 28
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (showDateSeparator && dateSeparatorText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dateSeparatorText,
                    color = nc.textTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(
                            nc.divider,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isMe) {
                if (showAvatar) {
                    MessageAvatar(
                        initial = avatarInitial,
                        size = avatarSize,
                        modifier = Modifier.align(Alignment.Bottom)
                    )
                } else {
                    Spacer(modifier = Modifier.size(avatarSize.dp).align(Alignment.Bottom))
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            val bubbleShape = RoundedCornerShape(
                topStart = if (isMe) 18.dp else 4.dp,
                topEnd = if (isMe) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            )

            if (messageType == Constants.MESSAGE_TYPE_IMAGE && !isRecalled) {
                // Image bubble
                Box(
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = onLongClick
                        )
                        .clip(bubbleShape)
                        .background(
                            color = if (isMe) nc.sentBubble else nc.receivedBubble,
                            shape = bubbleShape
                        )
                ) {
                    AsyncImage(
                        model = text,
                        contentDescription = "Hình ảnh",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(bubbleShape)
                    )
                }
            } else if (messageType == Constants.MESSAGE_TYPE_VOICE && !isRecalled) {
                // Voice bubble with real playback
                val context = LocalContext.current
                var voiceIsPlaying by remember { mutableStateOf(false) }
                var voicePositionMs by remember { mutableStateOf(0L) }
                var voiceDurationMs by remember { mutableStateOf(duration * 1000L) }
                var voiceProgress by remember { mutableStateOf(0f) }
                val voicePlayer = remember { MediaPlayer() }
                var isPrepared by remember { mutableStateOf(false) }

                DisposableEffect(text) {
                    voicePlayer.setOnPreparedListener { mp ->
                        voiceDurationMs = mp.duration.toLong()
                        isPrepared = true
                    }
                    voicePlayer.setOnCompletionListener {
                        voiceIsPlaying = false
                        voicePositionMs = 0L
                        voiceProgress = 0f
                    }
                    voicePlayer.setDataSource(text)
                    voicePlayer.prepareAsync()

                    val progressScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                    val job = progressScope.launch {
                        while (true) {
                            delay(100)
                            if (voicePlayer.isPlaying) {
                                voicePositionMs = voicePlayer.currentPosition.toLong()
                                voiceDurationMs = voicePlayer.duration.toLong()
                                voiceProgress = if (voiceDurationMs > 0) voicePositionMs.toFloat() / voiceDurationMs else 0f
                            }
                        }
                    }

                    onDispose {
                        job.cancel()
                        try {
                            if (voicePlayer.isPlaying) voicePlayer.stop()
                            voicePlayer.release()
                        } catch (_: Exception) {}
                    }
                }

                val minutes = duration / 60
                val seconds = duration % 60
                val durationText = if (minutes > 0) "${minutes}:${String.format("%02d", seconds)}" else "0:${String.format("%02d", seconds)}"
                val posMins = voicePositionMs / 1000 / 60
                val posSecs = (voicePositionMs / 1000) % 60
                val positionText = String.format("%d:%02d", posMins, posSecs)

                Box(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = onLongClick
                        )
                        .background(
                            color = if (isMe) nc.sentBubble else nc.receivedBubble,
                            shape = bubbleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Play/Pause button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isMe) nc.sentBubbleText.copy(alpha = 0.15f) else nc.receivedBubbleText.copy(alpha = 0.15f))
                                    .clickable {
                                        if (isPrepared) {
                                            if (voiceIsPlaying) {
                                                voicePlayer.pause()
                                                voiceIsPlaying = false
                                            } else {
                                                voicePlayer.start()
                                                voiceIsPlaying = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (voiceIsPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (voiceIsPlaying) "Dừng" else "Phát",
                                    tint = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Waveform bars (horizontal drag to seek)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, _ ->
                                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                            if (isPrepared) {
                                                voicePlayer.seekTo((fraction * voiceDurationMs).toInt())
                                                voiceProgress = fraction
                                            }
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                            if (isPrepared) {
                                                voicePlayer.seekTo((fraction * voiceDurationMs).toInt())
                                                voiceProgress = fraction
                                            }
                                        }
                                    },
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val barCount = 20
                                val barColor = if (isMe) nc.sentBubbleText.copy(alpha = 0.5f) else nc.receivedBubbleText.copy(alpha = 0.5f)
                                val barColorActive = if (isMe) nc.sentBubbleText else nc.receivedBubbleText
                                val heights = listOf(8, 14, 20, 16, 10, 18, 22, 14, 8, 16, 20, 12, 18, 14, 10, 22, 16, 8, 14, 20)
                                val activeBar = (voiceProgress * barCount).toInt().coerceIn(0, barCount - 1)
                                for (i in 0 until barCount) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(heights[i % heights.size].dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (i <= activeBar) barColorActive else barColor)
                                    )
                                }
                            }
                        }

                        // Time display
                        Text(
                            text = if (voiceIsPlaying) "$positionText / $durationText" else durationText,
                            color = if (isMe) nc.sentBubbleText.copy(alpha = 0.7f) else nc.receivedBubbleText.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            } else if (messageType == Constants.MESSAGE_TYPE_FILE && !isRecalled) {
                // File bubble
                val context = LocalContext.current
                val fileUrl = text
                val fileName = message?.fileName ?: "File"
                val fileSize = message?.fileSize ?: 0L

                Box(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(fileUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    android.widget.Toast.makeText(context, "Không thể mở file", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLongClick = onLongClick
                        )
                        .background(
                            color = if (isMe) nc.sentBubble else nc.receivedBubble,
                            shape = bubbleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMe) nc.sentBubbleText.copy(alpha = 0.12f) else nc.receivedBubbleText.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fileName,
                                color = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (fileSize > 0) {
                                Text(
                                    text = fileSize.toReadableFileSize(),
                                    color = if (isMe) nc.sentBubbleText.copy(alpha = 0.6f) else nc.receivedBubbleText.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Text bubble
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = onLongClick
                        )
                        .background(
                            color = if (isMe) nc.sentBubble else nc.receivedBubble,
                            shape = bubbleShape
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isRecalled) "Tin nhắn đã được thu hồi" else text,
                        color = if (isRecalled) nc.textTertiary else if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontStyle = if (isRecalled) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                    )
                }
            }

            if (isMe) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        val timeStartPadding = if (!isMe) avatarSize.dp + 6.dp else 0.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = timeStartPadding, end = if (isMe) 4.dp else 0.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = time,
                color = nc.textTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            if (isMe && status.isNotEmpty()) {
                Text(
                    text = when(status) {
                        "seen" -> "Đã xem"
                        "delivered" -> "Đã nhận"
                        "recalled" -> ""
                        else -> "Đã gửi"
                    },
                    color = if (status == "seen") NexusPrimary else nc.textTertiary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CallHistoryBubble(
    message: Message,
    isMe: Boolean,
    time: String,
    showDateSeparator: Boolean = false,
    dateSeparatorText: String = "",
    avatarInitial: String = "",
    showAvatar: Boolean = false,
    onStartCall: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val isDark = isSystemInDarkTheme()
    val avatarSize = 28

    val isVideo = message.text == "video"
    val isMissed = message.duration == 0L
    val callLabel = if (isVideo) "Cuộc gọi video" else "Cuộc gọi thoại"
    val titleText = if (isMissed) "Đã bỏ lỡ $callLabel" else callLabel
    val subtitleText = if (isMissed) {
        time
    } else {
        val mins = message.duration / 60
        val secs = message.duration % 60
        if (mins > 0) "${mins} phút ${secs} giây" else "${secs} giây"
    }

    val bubbleColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)
    val buttonColor = if (isDark) Color(0xFF383838) else Color(0xFFD0D0D0)
    val primaryText = if (isDark) Color(0xFFE4E6E9) else Color(0xFF1A1A1A)
    val secondaryText = if (isDark) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val iconTint = if (isDark) Color(0xFFB0B3B8) else Color(0xFF555555)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (showDateSeparator && dateSeparatorText.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dateSeparatorText,
                    color = nc.textTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(nc.divider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }

        // Row: avatar (receiver only) + bubble
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isMe) {
                if (showAvatar) {
                    MessageAvatar(initial = avatarInitial, size = avatarSize)
                } else {
                    Spacer(modifier = Modifier.size(avatarSize.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Bubble — wraps to info row width, button fills that width
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMissed && !isMe) Color(0xFFFF3B30) else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMissed && !isMe) {
                            Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(8.dp))
                        } else if (isMissed && isMe) {
                            Icon(Icons.Default.Call, null, tint = iconTint, modifier = Modifier.size(15.dp))
                            Icon(Icons.Default.Close, null, tint = iconTint, modifier = Modifier.size(8.dp))
                        } else {
                            Icon(
                                if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                null, tint = iconTint, modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = titleText,
                            color = if (isMissed && !isMe) Color(0xFFFF3B30) else primaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = subtitleText, color = secondaryText, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // "Gọi lại"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(buttonColor)
                        .clickable { onStartCall() }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            null, tint = primaryText, modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Gọi lại", color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (isMe) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        // Timestamp
        val timePadStart = if (!isMe) avatarSize.dp + 6.dp else 0.dp
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = timePadStart, end = if (isMe) 4.dp else 0.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Text(
                text = time,
                color = nc.textTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun MessageAvatar(initial: String, size: Int = 28, modifier: Modifier = Modifier) {
    val nc = MaterialTheme.nexusColors
    val safeInitial = initial.ifBlank { "?" }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(nc.avatarBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = safeInitial,
            color = nc.textPrimary,
            fontSize = (size / 2.2f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UploadProgressBubble(
    imageUri: Uri,
    isMe: Boolean
) {
    val nc = MaterialTheme.nexusColors
    val bubbleShape = RoundedCornerShape(
        topStart = if (isMe) 18.dp else 4.dp,
        topEnd = if (isMe) 4.dp else 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 18.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .clip(bubbleShape)
                    .background(
                        color = if (isMe) nc.sentBubble else nc.receivedBubble,
                        shape = bubbleShape
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        alpha = 0.5f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(bubbleShape)
                    )
                    CircularProgressIndicator(
                        color = NexusPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            if (isMe) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (isMe) 4.dp else 0.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Text(
                text = "Đang tải lên...",
                color = nc.textTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    Box(modifier = Modifier.fillMaxSize().background(nc.background), contentAlignment = Alignment.Center) {
        Button(onClick = { onGroupCreated("new_group_id") }) {
            Text("Create Group Screen")
        }
    }
}
