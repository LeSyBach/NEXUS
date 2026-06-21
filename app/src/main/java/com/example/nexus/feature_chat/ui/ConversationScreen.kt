package com.example.nexus.feature_chat.ui

import android.Manifest
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.DateUtils
import com.example.nexus.core.utils.Resource
import com.example.nexus.core.utils.createTempImageUri
import com.example.nexus.core.utils.createTempVideoUri
import com.example.nexus.data.model.Message
import com.example.nexus.data.model.PinnedMessage
import com.example.nexus.data.model.User
import com.example.nexus.data.firebase.PlaybackState
import com.example.nexus.data.firebase.NexusMessagingService
import com.example.nexus.feature_chat.viewmodel.AiSummaryState
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.feature_chat.viewmodel.UploadState
import com.example.nexus.feature_chat.viewmodel.VoiceRecordingState
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatId: String,
    viewModel: ChatViewModel? = null,
    initialAction: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (String) -> Unit,
    onStartCall: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateToProfile: (String) -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    var messageText by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val messagesState = viewModel?.messagesState?.collectAsState()?.value ?: Resource.Idle
    val currentUserId = viewModel?.currentUserId
    val currentChat = viewModel?.currentChat?.collectAsState()?.value
    val otherUser = viewModel?.otherUser?.collectAsState()?.value
    val listState = rememberLazyListState()

    // Theme color for sent message bubbles (realtime from Firestore)
    val themeColorHex = viewModel?.themeColor?.collectAsState()?.value ?: ""
    val sentBubbleColor = remember(themeColorHex, nc.sentBubble) {
        if (themeColorHex.isNotEmpty()) {
            try { Color(android.graphics.Color.parseColor(themeColorHex)) } catch (_: Exception) { nc.sentBubble }
        } else nc.sentBubble
    }

    val uploadState = viewModel?.uploadState?.collectAsState()?.value ?: UploadState.Idle
    val pendingImageUri = viewModel?.pendingImageUri?.collectAsState()?.value
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel?.sendMediaMessage(chatId, uri, context)
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraOptions by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            viewModel?.sendMediaMessage(chatId, tempCameraUri!!, context)
        }
        tempCameraUri = null
    }

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success && tempVideoUri != null) {
            viewModel?.sendMediaMessage(chatId, tempVideoUri!!, context)
        }
        tempVideoUri = null
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
    val replyingToMessage = viewModel?.replyingToMessage?.collectAsState()?.value
    var reactionsSheetState by remember { mutableStateOf<Pair<Map<String, String>, String>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val showScrollToBottom = remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    // AI State
    val aiSummaryState = viewModel?.aiSummaryState?.collectAsState()?.value ?: AiSummaryState.Idle
    val smartReplies = viewModel?.smartReplies?.collectAsState()?.value ?: emptyList()
    val unreadFromOthers = remember {
        derivedStateOf {
            val messages = (messagesState as? Resource.Success)?.data ?: emptyList()
            messages.count { msg ->
                msg.senderId != currentUserId && !msg.seenBy.contains(currentUserId)
            }
        }
    }
    val totalMessagesFromOthers = remember {
        derivedStateOf {
            val messages = (messagesState as? Resource.Success)?.data ?: emptyList()
            messages.count { it.senderId != currentUserId }
        }
    }

    // Pagination state
    val isLoadingMore = viewModel?.isLoadingMoreMessages?.collectAsState()?.value ?: false

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val canScrollForward = listState.canScrollForward
            Triple(total, lastVisible, canScrollForward)
        }.collect { (total, _, canScrollForward) ->
            if (total > 0 && !canScrollForward) {
                viewModel?.loadMoreMessages(chatId)
            }
        }
    }

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

    LaunchedEffect(chatId) {
        delay(1000)
        viewModel?.markMessagesAsSeen(chatId)
    }

    LaunchedEffect(chatId) {
        viewModel?.startObservingTyping(chatId)
    }

    LaunchedEffect(initialAction) {
        when (initialAction) {
            "search" -> viewModel?.startSearch()
            "share_contact" -> viewModel?.openContactPicker()
        }
    }

    DisposableEffect(chatId) {
        NexusMessagingService.activeChatId = chatId
        onDispose {
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
    val isSearchActive = viewModel?.isSearchActive?.collectAsState()?.value ?: false
    val searchQuery = viewModel?.searchQuery?.collectAsState()?.value ?: ""
    val searchResults = viewModel?.searchResults?.collectAsState()?.value ?: emptyList()
    val currentSearchIndex = viewModel?.currentSearchIndex?.collectAsState()?.value ?: -1
    var showMessageMenu by remember { mutableStateOf<Pair<String, Message>?>(null) }
    val showContactPicker = viewModel?.showContactPicker?.collectAsState()?.value ?: false

    LaunchedEffect(currentSearchIndex, searchResults) {
        if (currentSearchIndex in searchResults.indices) {
            val messageIndex = searchResults[currentSearchIndex]
            listState.animateScrollToItem(messageIndex)
        }
    }
    var showFullScreenVideo by remember { mutableStateOf<String?>(null) }
    var messageToForward by remember { mutableStateOf<Message?>(null) }
    var storyReplyViewUserId by remember { mutableStateOf<String?>(null) }
    var storyReplyViewStoryId by remember { mutableStateOf<String?>(null) }
    var storyReplyViewUser by remember { mutableStateOf<User?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val otherId = otherUser?.uid ?: ""

    // Pin & Mention state
    val pinnedMessage = viewModel?.pinnedMessage?.collectAsState()?.value
    var showMentionPopup by remember { mutableStateOf(false) }
    var mentionMembers by remember { mutableStateOf<List<User>>(emptyList()) }
    val selectedMentions = remember { mutableStateListOf<String>() }

    val isGroup = currentChat?.type == Constants.CHAT_TYPE_GROUP
    val nicknames = viewModel?.nicknames?.collectAsState()?.value ?: emptyMap()
    val displayName = if (isGroup) {
        currentChat?.groupName?.ifEmpty { "Nhóm" } ?: "Nhóm"
    } else {
        val nicknameForOther = if (otherId.isNotEmpty()) nicknames[otherId] else null
        nicknameForOther?.takeIf { it.isNotBlank() }
            ?: otherUser?.let { it.displayName.ifEmpty { it.username } }
            ?: "Đang tải..."
    }
    val statusText = if (isGroup) {
        "${currentChat?.participants?.size ?: 0} thành viên"
    } else {
        if (otherUser?.status == Constants.USER_STATUS_ONLINE) "Đang hoạt động"
        else otherUser?.lastSeen?.let { DateUtils.getRelativeTimeSpan(it.toDate().time) } ?: ""
    }
    val avatarInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    var senderNameMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var senderAvatarMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(isGroup, messagesState) {
        if (!isGroup) return@LaunchedEffect
        val messages = (messagesState as? Resource.Success)?.data ?: return@LaunchedEffect
        val senderIds = messages.filter { it.senderId != currentUserId }
            .map { it.senderId }.distinct()
        if (senderIds.isEmpty()) return@LaunchedEffect
        val users = viewModel?.getUsersByIds(senderIds) ?: return@LaunchedEffect
        senderNameMap = users.associate { it.uid to it.displayName.ifEmpty { it.username } }
        senderAvatarMap = users.associate { it.uid to (it.avatarUrl.ifEmpty { "" }) }
    }

    LaunchedEffect(isGroup, currentChat) {
        if (!isGroup) return@LaunchedEffect
        val participantIds = currentChat?.participants ?: return@LaunchedEffect
        val users = viewModel?.getUsersByIds(participantIds) ?: return@LaunchedEffect
        mentionMembers = users.filter { it.uid != currentUserId }
    }

    val isScrollingUp = listState.isScrollInProgress && listState.firstVisibleItemIndex > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(nc.background)
            .imePadding()
    ) {
        // Pattern Background
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val dotRadius = 2f
            val spacing = 70f
            val dotColor = nc.textSecondary.copy(alpha = 0.06f)
            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(color = dotColor, radius = dotRadius, center = androidx.compose.ui.geometry.Offset(x, y))
                    y += spacing
                }
                x += spacing
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(nc.surface.copy(alpha = 0.85f))
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
                val avatarUrl = if (isGroup) currentChat?.groupAvatarUrl?.ifEmpty { null } else otherUser?.avatarUrl?.ifEmpty { null }
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(avatarInitial, color = nc.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
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
                    TypingIndicator()
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
                Icon(Icons.Default.Call, contentDescription = "Call", tint = sentBubbleColor, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = {
                val otherId = otherUser?.uid ?: ""
                if (otherId.isNotEmpty()) onStartCall(otherId, "video", displayName)
            }) {
                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = sentBubbleColor, modifier = Modifier.size(22.dp))
            }
        }

        // Divider
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(nc.divider))

        // Search Overlay
        AnimatedVisibility(
            visible = isSearchActive,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(nc.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel?.clearSearch() }) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng tìm kiếm", tint = nc.textPrimary, modifier = Modifier.size(22.dp))
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel?.updateSearchQuery(it) },
                    placeholder = { Text("Tìm kiếm tin nhắn...", color = nc.textTertiary, fontSize = 14.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = nc.outline,
                        focusedTextColor = nc.textPrimary,
                        unfocusedTextColor = nc.textPrimary,
                        cursorColor = NexusPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (searchResults.isNotEmpty()) {
                    Text(
                        "${currentSearchIndex + 1}/${searchResults.size}",
                        color = nc.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { viewModel?.navigateToPreviousResult() }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Kết quả trước", tint = nc.textPrimary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { viewModel?.navigateToNextResult() }) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Kết quả sau", tint = nc.textPrimary, modifier = Modifier.size(22.dp))
                    }
                } else if (searchQuery.isNotEmpty()) {
                    Text(
                        "0 kết quả",
                        color = nc.textTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Offline banner
        val isOffline = viewModel?.isOffline?.collectAsState()?.value ?: false
        AnimatedVisibility(
            visible = isOffline,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(nc.errorText)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có kết nối Internet",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Messages
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Pinned message bar
            pinnedMessage?.let { pinned ->
                val messages = (messagesState as? Resource.Success)?.data ?: emptyList()
                val pinnedIndex = messages.indexOfFirst { it.id == pinned.messageId }
                PinnedMessageBar(
                    pinnedMessage = pinned,
                    onClick = {
                        if (pinnedIndex >= 0) {
                            coroutineScope.launch { listState.animateScrollToItem(pinnedIndex) }
                        }
                    },
                    onUnpin = { viewModel?.unpinMessage(chatId) },
                    modifier = Modifier.align(Alignment.TopCenter).zIndex(1f)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (messagesState) {
                    is Resource.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = sentBubbleColor, strokeWidth = 2.dp)
                            }
                        }
                    }
                    is Resource.Success -> {
                        if (uploadState is UploadState.Uploading && pendingImageUri != null) {
                            item {
                                UploadProgressBubble(
                                    imageUri = pendingImageUri,
                                    isMe = true,
                                    sentBubbleColor = sentBubbleColor
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
                                                .background(sentBubbleColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.ChatBubbleOutline,
                                                contentDescription = null,
                                                tint = sentBubbleColor.copy(alpha = 0.6f),
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
                                val resolvedSenderName = if (isGroup && !isMe) {
                                    senderNameMap[msg.senderId] ?: msg.senderName.ifEmpty { displayName }
                                } else msg.senderName
                                val senderInitial = if (isMe) {
                                    ""
                                } else {
                                    val baseName = resolvedSenderName.ifEmpty { displayName }
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

                                val showStatus = isMe && index == 0 && msg.status != "recalled"

                                if (msg.type == Constants.MESSAGE_TYPE_SYSTEM) {
                                    SystemMessageBubble(
                                        text = msg.text,
                                        time = timeStr,
                                        showDateSeparator = showDateSeparator,
                                        dateSeparatorText = msg.timestamp?.toDate()?.let { DateUtils.formatDateSeparator(it.time) } ?: ""
                                    )
                                } else if (msg.type == Constants.MESSAGE_TYPE_CALL) {
                                    CallHistoryBubble(
                                        message = msg,
                                        isMe = isMe,
                                        time = timeStr,
                                        showDateSeparator = showDateSeparator,
                                        dateSeparatorText = msg.timestamp?.toDate()?.let { DateUtils.formatDateSeparator(it.time) } ?: "",
                                        avatarInitial = senderInitial,
                                        showAvatar = !isMe && isLastFromSender,
                                        avatarUrl = if (!isMe) senderAvatarMap[msg.senderId]?.ifEmpty { null } else null,
                                        sentBubbleColor = sentBubbleColor,
                                        onStartCall = {
                                            if (otherId.isNotEmpty()) onStartCall(otherId, msg.text, displayName)
                                        }
                                    )
                                } else {
                                    val isOriginalRecalled = if (msg.replyTo != null) {
                                        messagesState.data.find { it.id == msg.replyTo!!.messageId }?.status == "recalled"
                                    } else false

                                    val isHighlight = searchResults.isNotEmpty() && currentSearchIndex in searchResults.indices && index == searchResults[currentSearchIndex]

                                    MessageBubble(
                                        text = msg.text,
                                        isMe = isMe,
                                        time = timeStr,
                                        status = if (showStatus) msg.status else "",
                                        showDateSeparator = showDateSeparator,
                                        dateSeparatorText = msg.timestamp?.toDate()?.let { DateUtils.formatDateSeparator(it.time) } ?: "",
                                        isRecalled = msg.status == "recalled",
                                        isOriginalRecalled = isOriginalRecalled,
                                        avatarInitial = senderInitial,
                                        showAvatar = !isMe && isLastFromSender,
                                        senderName = if (isGroup && !isMe) resolvedSenderName else "",
                                        avatarUrl = if (!isMe) senderAvatarMap[msg.senderId]?.ifEmpty { null } else null,
                                        messageType = msg.type,
                                        duration = msg.duration,
                                        message = msg,
                                        currentUserId = currentUserId,
                                        isSending = msg.isSending,
                                        isSearchHighlight = isHighlight,
                                        sentBubbleColor = sentBubbleColor,
                                        onLongClick = { showMessageMenu = Pair(chatId, msg) },
                                        onReply = {
                                            viewModel?.setReplyingMessage(msg)
                                            coroutineScope.launch { listState.animateScrollToItem(0) }
                                        },
                                        onReact = { emoji -> viewModel?.toggleReaction(chatId, msg.id, emoji) },
                                        onReactionsClick = { reactions, msgId -> reactionsSheetState = Pair(reactions, msgId) },
                                        onQuoteClick = { quoteId ->
                                            val messages = (messagesState as? Resource.Success)?.data
                                            if (messages != null) {
                                                val index = messages.indexOfFirst { it.id == quoteId }
                                                if (index >= 0) {
                                                    coroutineScope.launch { listState.animateScrollToItem(index) }
                                                }
                                            }
                                        },
                                        onForward = { messageToForward = msg },
                                        onVideoClick = {
                                            showFullScreenVideo = msg.text
                                        },
                                        onContactClick = { contactUserId ->
                                            onNavigateToProfile(contactUserId)
                                        },
                                        onStoryReplyClick = { storyId ->
                                            val allStories = (viewModel?.stories?.value ?: emptyMap()) + (viewModel?.notes?.value ?: emptyMap())
                                            val storyOwnerId = allStories.entries.find { it.value.id == storyId }?.key
                                            if (storyOwnerId != null) {
                                                storyReplyViewUserId = storyOwnerId
                                                storyReplyViewStoryId = storyId
                                                coroutineScope.launch {
                                                    val users = viewModel?.getUsersByIds(listOf(storyOwnerId))
                                                    storyReplyViewUser = users?.firstOrNull()
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            if (isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = sentBubbleColor,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
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

            // Scroll-to-bottom FAB
            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollToBottom.value,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp),
                    shape = CircleShape,
                    color = nc.surfaceVariant,
                    shadowElevation = 6.dp,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.clickable {
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Cuộn xuống",
                            tint = nc.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(nc.divider))

        // Smart Reply Bar
        AnimatedVisibility(
            visible = smartReplies.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(nc.background)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(smartReplies.size) { index ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, sentBubbleColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .background(nc.cardBg)
                            .clickable {
                                val reply = smartReplies[index]
                                viewModel?.dismissSmartReplies()
                                viewModel?.sendMessage(chatId, reply)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = smartReplies[index],
                            fontSize = 13.sp,
                            color = nc.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Reply preview bar
        if (replyingToMessage != null) {
            val replyPreviewText = when (replyingToMessage.type) {
                Constants.MESSAGE_TYPE_IMAGE -> "📷 Hình ảnh"
                Constants.MESSAGE_TYPE_VIDEO -> "🎬 Video"
                Constants.MESSAGE_TYPE_VOICE -> "🎤 Tin nhắn thoại"
                Constants.MESSAGE_TYPE_FILE -> "📎 ${replyingToMessage.fileName.ifEmpty { "Tệp" }}"
                Constants.MESSAGE_TYPE_STORY_REPLY -> "📸 Trả lời tin"
                Constants.MESSAGE_TYPE_NOTE_REPLY -> "📝 Phản hồi ghi chú"
                Constants.MESSAGE_TYPE_CONTACT -> "👤 ${replyingToMessage.contactName.ifEmpty { "Liên hệ" }}"
                else -> replyingToMessage.text
            }
            val replyHeaderText = if (replyingToMessage.senderId == currentUserId) {
                "Bạn đã trả lời chính mình"
            } else {
                "Bạn đã trả lời ${replyingToMessage.senderName}"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(nc.background)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(nc.cardBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .background(sentBubbleColor.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = replyHeaderText,
                            color = sentBubbleColor.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = replyPreviewText,
                            color = nc.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { viewModel?.setReplyingMessage(null) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Hủy", tint = nc.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Message action dialog
        showMessageMenu?.let { (chatIdForMenu, msg) ->
            AlertDialog(
                onDismissRequest = { showMessageMenu = null },
                containerColor = nc.surfaceElevated,
                title = null,
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val quickEmojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
                            for (emoji in quickEmojis) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            viewModel?.toggleReaction(chatIdForMenu, msg.id, emoji)
                                            showMessageMenu = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 24.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = nc.divider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel?.setReplyingMessage(msg)
                                    showMessageMenu = null
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = null, tint = nc.iconTint, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Trả lời", color = nc.textPrimary, fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(msg.text))
                                    showMessageMenu = null
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = nc.iconTint, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Sao chép", color = nc.textPrimary, fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    messageToForward = msg
                                    showMessageMenu = null
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Forward, contentDescription = null, tint = nc.iconTint, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Chuyển tiếp", color = nc.textPrimary, fontSize = 15.sp)
                        }

                        if (msg.type == Constants.MESSAGE_TYPE_IMAGE ||
                            msg.type == Constants.MESSAGE_TYPE_VIDEO ||
                            msg.type == Constants.MESSAGE_TYPE_FILE ||
                            msg.type == Constants.MESSAGE_TYPE_VOICE) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val downloadManager = context.getSystemService(DownloadManager::class.java)
                                            val request = DownloadManager.Request(Uri.parse(msg.text))
                                            val fileName = msg.fileName.ifEmpty { msg.type }
                                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                            downloadManager.enqueue(request)
                                        } catch (_: Exception) {}
                                        showMessageMenu = null
                                    }
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = nc.iconTint, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Text("Tải xuống", color = nc.textPrimary, fontSize = 15.sp)
                            }
                        }

                        if (msg.senderId == currentUserId) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel?.recallMessage(chatIdForMenu, msg.id)
                                        showMessageMenu = null
                                    }
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null, tint = sentBubbleColor, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Text("Thu hồi", color = nc.textPrimary, fontSize = 15.sp)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel?.deleteMessage(chatIdForMenu, msg.id)
                                    showMessageMenu = null
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = nc.errorText, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Xóa", color = nc.errorText, fontSize = 15.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }

        showFullScreenVideo?.let { url ->
            FullScreenVideoPlayer(videoUrl = url, onDismiss = { showFullScreenVideo = null })
        }

        // Story viewer from story reply click
        if (storyReplyViewUserId != null && storyReplyViewStoryId != null) {
            val storyOwnerId = storyReplyViewUserId!!
            val targetStoryId = storyReplyViewStoryId!!
            val allImageStories = viewModel?.imageStories?.collectAsState()?.value ?: emptyMap()
            val allNotes = viewModel?.notes?.collectAsState()?.value ?: emptyMap()

            val imageStoryList = allImageStories[storyOwnerId]
            val noteStory = allNotes[storyOwnerId]

            if (imageStoryList != null && imageStoryList.any { it.id == targetStoryId }) {
                val owner = storyReplyViewUser ?: User(uid = storyOwnerId, displayName = "Người dùng")
                ViewStoryDialog(
                    stories = imageStoryList,
                    user = owner,
                    isMyStory = storyOwnerId == currentUserId,
                    onDismiss = { storyReplyViewUserId = null; storyReplyViewStoryId = null; storyReplyViewUser = null }
                )
            } else if (noteStory != null && noteStory.id == targetStoryId) {
                val owner = storyReplyViewUser ?: User(uid = storyOwnerId, displayName = "Người dùng")
                ViewNoteDialog(
                    story = noteStory,
                    user = owner,
                    isMyStory = storyOwnerId == currentUserId,
                    onDismiss = { storyReplyViewUserId = null; storyReplyViewStoryId = null; storyReplyViewUser = null }
                )
            } else {
                storyReplyViewUserId = null
                storyReplyViewStoryId = null
                storyReplyViewUser = null
            }
        }

        messageToForward?.let { msg ->
            ForwardMessageBottomSheet(
                message = msg,
                viewModel = viewModel,
                onDismiss = { messageToForward = null }
            )
        }

        when (voiceState) {
            is VoiceRecordingState.Recording -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(nc.background)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { viewModel?.cancelVoicePreview() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hủy", tint = Color(0xFFFF3B30), modifier = Modifier.size(22.dp))
                    }

                    IconButton(
                        onClick = { viewModel?.stopVoiceRecording() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Tạm dừng", tint = sentBubbleColor, modifier = Modifier.size(24.dp))
                    }

                    val mins = voiceRecordTimeSec / 60
                    val secs = voiceRecordTimeSec % 60
                    Text(
                        text = String.format("%d:%02d", mins, secs),
                        color = nc.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )

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

                    IconButton(
                        onClick = { viewModel?.sendVoiceDirectly(chatId, context) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(sentBubbleColor)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            is VoiceRecordingState.Previewing -> {
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
                    IconButton(
                        onClick = { viewModel?.cancelVoicePreview() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFFF3B30), modifier = Modifier.size(22.dp))
                    }

                    IconButton(
                        onClick = { viewModel?.reRecordVoice(context) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Ghi lại", tint = sentBubbleColor, modifier = Modifier.size(22.dp))
                    }

                    IconButton(
                        onClick = { viewModel?.toggleVoicePreview(context) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Tạm dừng" else "Tiếp tục",
                            tint = sentBubbleColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

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
                                    .background(if (i <= activeBar) sentBubbleColor else nc.divider)
                            )
                        }
                    }

                    Text(
                        text = if (playbackState.isPlaying) positionText else durationText,
                        color = nc.textSecondary,
                        fontSize = 11.sp
                    )

                    IconButton(
                        onClick = { viewModel?.sendVoicePreview(chatId, context) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(sentBubbleColor)
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
                        Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Gửi file", tint = sentBubbleColor, modifier = Modifier.size(24.dp))
                    }
                    Box {
                        IconButton(
                            onClick = { showCameraOptions = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = sentBubbleColor, modifier = Modifier.size(22.dp))
                        }
                        DropdownMenu(
                            expanded = showCameraOptions,
                            onDismissRequest = { showCameraOptions = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Chụp ảnh") },
                                onClick = {
                                    showCameraOptions = false
                                    val uri = context.createTempImageUri()
                                    tempCameraUri = uri
                                    takePictureLauncher.launch(uri)
                                },
                                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Quay video") },
                                onClick = {
                                    showCameraOptions = false
                                    val uri = context.createTempVideoUri()
                                    tempVideoUri = uri
                                    videoCaptureLauncher.launch(uri)
                                },
                                leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(20.dp)) }
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Gallery", tint = sentBubbleColor, modifier = Modifier.size(22.dp))
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
                        Icon(Icons.Default.Mic, contentDescription = "Mic", tint = sentBubbleColor, modifier = Modifier.size(24.dp))
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
                                    onValueChange = {
                                        messageText = it
                                        if (it.isNotEmpty()) {
                                            viewModel?.startTyping(chatId)
                                            viewModel?.dismissSmartReplies()
                                        }
                                    },
                                    textStyle = TextStyle(color = nc.textPrimary, fontSize = 15.sp),
                                    cursorBrush = SolidColor(sentBubbleColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { state ->
                                            if (state.isFocused) {
                                                if (messageText.isNotEmpty()) viewModel?.startTyping(chatId)
                                            } else {
                                                viewModel?.stopTyping(chatId)
                                            }
                                        }
                                )
                            }
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    showEmojiPicker = !showEmojiPicker
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji", tint = sentBubbleColor, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (messageText.isNotEmpty()) {
                                viewModel?.sendMessage(chatId, messageText)
                                messageText = ""
                                showEmojiPicker = false
                            } else {
                                viewModel?.sendMessage(chatId, "👍")
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        val isSendEnabled = messageText.isNotEmpty()
                        Icon(
                            if (isSendEnabled) Icons.AutoMirrored.Filled.Send else Icons.Default.ThumbUp,
                            contentDescription = if (isSendEnabled) "Send" else "Like",
                            tint = sentBubbleColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
        } // end Column

        // Floating AI Summarize Button
        AnimatedVisibility(
            visible = totalMessagesFromOthers.value >= 5,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(nc.cardBg.copy(alpha = 0.9f))
                    .clickable { viewModel?.summarizeMessages() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00C6FF),
                                        Color(0xFF0072FF)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Tóm tắt đoạn chat",
                        color = nc.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } // end Box

    // Reaction Detail Sheet
    reactionsSheetState?.let { (reactions, msgId) ->
        val emojiCounts = reactions.values.groupBy { it }.mapValues { it.value.size }
        var reactionUsers by remember { mutableStateOf<List<com.example.nexus.data.model.User>>(emptyList()) }

        LaunchedEffect(reactions) {
            reactionUsers = viewModel?.getUsersByIds(reactions.keys.toList()) ?: emptyList()
        }

        ModalBottomSheet(
            onDismissRequest = { reactionsSheetState = null },
            containerColor = nc.surfaceElevated,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Cảm xúc",
                    color = nc.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                for ((emoji, count) in emojiCounts) {
                    Text(
                        text = "$emoji $count",
                        color = nc.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val usersWithEmoji = reactions.entries.filter { it.value == emoji }.map { it.key }
                    for (userId in usersWithEmoji) {
                        val user = reactionUsers.find { it.uid == userId }
                        val userName = user?.displayName?.ifEmpty { user.username } ?: userId
                        val isMe = userId == currentUserId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isMe) {
                                    if (isMe) {
                                        viewModel?.toggleReaction(chatId, msgId, emoji)
                                        reactionsSheetState = null
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(nc.avatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                val userAvatarUrl = user?.avatarUrl?.ifEmpty { null }
                                if (userAvatarUrl != null) {
                                    AsyncImage(
                                        model = userAvatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        color = nc.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMe) "Bạn" else userName,
                                    color = nc.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isMe) {
                                    Text(
                                        text = "Nhấn để gỡ",
                                        color = nc.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(emoji, fontSize = 22.sp)
                        }
                    }

                    if (emojiCounts.keys.last() != emoji) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Emoji Picker
    if (showEmojiPicker) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            containerColor = nc.surfaceElevated,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Emoji",
                    color = nc.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
                val emojis = listOf(
                    "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
                    "🙂", "😉", "😊", "😇", "🥰", "😍", "🤩", "😘",
                    "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭",
                    "😎", "🥳", "😏", "😢", "😭", "😤", "😡", "🥺",
                    "😱", "😳", "🤯", "😴", "🤮", "👻", "💀", "☠️",
                    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
                    "👍", "👎", "👏", "🙌", "🤝", "🙏", "✌️", "🤞",
                    "🔥", "💯", "✨", "🎉", "🎊", "💐", "🌹", "⭐"
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    messageText += emoji
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }

    // AI Summary Bottom Sheet
    if (aiSummaryState !is AiSummaryState.Idle) {
        ModalBottomSheet(
            onDismissRequest = { viewModel?.dismissSummary() },
            containerColor = nc.surfaceElevated,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = sentBubbleColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tóm tắt cuộc trò chuyện",
                        color = nc.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                when (aiSummaryState) {
                    is AiSummaryState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = sentBubbleColor, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Đang tạo tóm tắt...", color = nc.textSecondary, fontSize = 14.sp)
                        }
                    }
                    is AiSummaryState.Success -> {
                        Text(
                            text = (aiSummaryState as AiSummaryState.Success).summary,
                            color = nc.textPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                    is AiSummaryState.Error -> {
                        Text(
                            text = (aiSummaryState as AiSummaryState.Error).message,
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    // Contact Picker Bottom Sheet
    if (showContactPicker) {
        val chatsForSharing = viewModel?.getChatsForSharing()?.filter { it.chatId != chatId } ?: emptyList()
        val contactToShare = otherUser

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel?.dismissContactPicker() },
            sheetState = sheetState,
            containerColor = nc.background,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
            ) {
                Text(
                    text = "Chia sẻ liên hệ",
                    color = nc.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                if (contactToShare != null) {
                    val shareName = contactToShare.displayName.ifEmpty { contactToShare.username }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(nc.surface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(nc.avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (contactToShare.avatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = contactToShare.avatarUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    shareName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = nc.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Đang chia sẻ:", color = nc.textSecondary, fontSize = 12.sp)
                            Text(shareName, color = nc.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "Gửi đến:",
                    color = nc.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                if (chatsForSharing.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Không có cuộc trò chuyện nào", color = nc.textSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(chatsForSharing.size) { index ->
                            val targetChat = chatsForSharing[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (contactToShare != null) {
                                            viewModel?.sendContactMessage(
                                                chatId = targetChat.chatId,
                                                contactUserId = contactToShare.uid,
                                                contactName = contactToShare.displayName.ifEmpty { contactToShare.username },
                                                contactPhone = contactToShare.phone,
                                                contactAvatarUrl = contactToShare.avatarUrl
                                            )
                                        }
                                        viewModel?.dismissContactPicker()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(nc.avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!targetChat.avatarUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = targetChat.avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            targetChat.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            color = nc.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    targetChat.displayName,
                                    color = nc.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
