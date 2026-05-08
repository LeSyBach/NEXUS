package com.example.nexus.feature_chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.DateUtils
import com.example.nexus.core.utils.Resource
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.navigation.Screen
import com.example.nexus.ui.components.NexusBottomBar
import com.example.nexus.ui.theme.GradientEnd
import com.example.nexus.ui.theme.GradientStart
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

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
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateGroup,
                containerColor = Color(0xFF5A55FF),
                contentColor = nc.sentBubbleText,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "New Chat", modifier = Modifier.size(32.dp))
            }
        },
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
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        )
                    ),
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
                items(chatsState.data.size) { index ->
                    val chat = chatsState.data[index]
                    val lastMessageText = chat.lastMessage?.text ?: "Chưa có tin nhắn"

                    val timeStr = chat.lastMessage?.timestamp?.toDate()?.let { DateUtils.formatChatTime(it.time) } ?: ""

                    var displayName by remember { mutableStateOf(chat.groupName.ifEmpty { "..." }) }
                    LaunchedEffect(chat.id) {
                        displayName = viewModel?.resolveDisplayName(chat) ?: chat.groupName
                    }

                    val myId = viewModel?.currentUserId
                    val unreadCount = if (myId != null) chat.lastMessage?.unreadCount?.get(myId) ?: 0 else 0

                    ChatItem(
                        name = displayName,
                        lastMessage = lastMessageText,
                        time = timeStr,
                        unreadCount = unreadCount,
                        isOnline = false,
                        onClick = { onNavigateToConversation(chat.id) }
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
                        brush = Brush.linearGradient(listOf(Color(0xFFBB86FC), Color(0xFF00E5FF))), 
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(nc.avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(name.first().toString(), color = nc.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun ChatItem(
    name: String,
    lastMessage: String,
    time: String,
    unreadCount: Int,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val isUnread = unreadCount > 0

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
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(56.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NexusPrimary.copy(alpha = 0.4f), nc.cardBg))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = name.first().toString(), color = nc.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                    if (time.isNotEmpty()) {
                        Text(
                            text = time,
                            color = if (isUnread) nc.unreadTimeText else nc.textSecondary,
                            fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lastMessage,
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
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(nc.unreadBadge),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = nc.unreadBadgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
    onNavigateToGroupInfo: (String) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    var messageText by remember { mutableStateOf("") }
    val messagesState = viewModel?.messagesState?.collectAsState()?.value ?: Resource.Idle
    val currentUserId = viewModel?.currentUserId
    val currentChat = viewModel?.currentChat?.collectAsState()?.value
    val otherUser = viewModel?.otherUser?.collectAsState()?.value
    val listState = rememberLazyListState()

    LaunchedEffect(chatId) {
        viewModel?.loadMessages(chatId)
    }

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
                    .background(Brush.linearGradient(listOf(NexusPrimary.copy(alpha = 0.6f), nc.cardBg))),
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
                if (statusText.isNotEmpty()) {
                    Text(
                        statusText,
                        color = if (!isGroup && otherUser?.status == Constants.USER_STATUS_ONLINE) Color(0xFF22C55E) else nc.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = { }) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = NexusPrimary, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { }) {
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

                            val showDateSeparator = if (index < messagesState.data.size - 1) {
                                val currDate = msg.timestamp?.toDate()
                                val nextDate = messagesState.data[index + 1].timestamp?.toDate()
                                currDate != null && nextDate != null &&
                                    java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(currDate) !=
                                    java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(nextDate)
                            } else if (index == messagesState.data.size - 1) {
                                true
                            } else false

                            MessageBubble(
                                text = msg.text,
                                isMe = isMe,
                                time = timeStr,
                                showDateSeparator = showDateSeparator,
                                dateSeparatorText = msg.timestamp?.toDate()?.let { DateUtils.formatDateSeparator(it.time) } ?: ""
                            )
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

        // ── Input Bar ──
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(nc.divider))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(nc.background)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = { },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Add", tint = NexusPrimary, modifier = Modifier.size(24.dp))
            }
            IconButton(
                onClick = { },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = NexusPrimary, modifier = Modifier.size(22.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(nc.cardBg, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (messageText.isEmpty()) {
                    Text("Nhắn tin...", color = nc.textSecondary, fontSize = 15.sp)
                }
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    textStyle = TextStyle(color = nc.textPrimary, fontSize = 15.sp),
                    cursorBrush = SolidColor(NexusPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            AnimatedVisibility(visible = messageText.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(
                    onClick = {
                        viewModel?.sendMessage(chatId, messageText)
                        messageText = ""
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NexusPrimary, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = nc.sentBubbleText, modifier = Modifier.size(20.dp))
                }
            }

            AnimatedVisibility(visible = messageText.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic", tint = NexusPrimary, modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
fun MessageBubble(
    text: String,
    isMe: Boolean,
    time: String,
    showDateSeparator: Boolean = false,
    dateSeparatorText: String = ""
) {
    val nc = MaterialTheme.nexusColors
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
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            brush = if (isMe) Brush.linearGradient(listOf(Color(0xFF5A55FF), Color(0xFF3B82F6)))
                                    else SolidColor(nc.receivedBubble),
                            shape = RoundedCornerShape(
                                topStart = if (isMe) 18.dp else 4.dp,
                                topEnd = if (isMe) 4.dp else 18.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 18.dp
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(text = text, color = if (isMe) nc.sentBubbleText else nc.receivedBubbleText, fontSize = 15.sp, lineHeight = 20.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = time,
                    color = nc.textTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (isMe) {
                Spacer(modifier = Modifier.width(4.dp))
            }
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
