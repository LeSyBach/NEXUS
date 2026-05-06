package com.example.nexus.feature_chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.BasicTextField
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.core.utils.Resource
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.navigation.Screen
import com.example.nexus.ui.components.NexusBottomBar
import com.example.nexus.ui.theme.DarkBackground
import com.example.nexus.ui.theme.DarkCard
import com.example.nexus.ui.theme.GradientEnd
import com.example.nexus.ui.theme.GradientStart
import com.example.nexus.ui.theme.NexusPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel? = null,
    onNavigateToConversation: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToTab: (String) -> Unit = {}
) {
    val chatsState = viewModel?.chatsState?.collectAsState()?.value ?: Resource.Idle
    var showAddMenu by remember { mutableStateOf(false) }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateGroup,
                containerColor = Color(0xFF5A55FF),
                contentColor = Color.White,
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
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. HEADER: Logo NEXUS
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

            // 2. SEARCH BAR & THÊM MỚI (+)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thanh Tìm kiếm
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E1E2A), RoundedCornerShape(24.dp))
                            .clickable { onNavigateToSearch() }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Tìm kiếm, AI...", color = Color.Gray, fontSize = 15.sp, maxLines = 1)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Nút Dấu Cộng (+) và Dropdown Menu
                    Box {
                        IconButton(
                            onClick = { showAddMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(DarkCard, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm mới", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Thêm bạn", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NexusPrimary) },
                                onClick = {
                                    showAddMenu = false
                                    onNavigateToSearch()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tạo nhóm", color = Color.White) },
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

            // 3. ONLINE FRIENDS: Trực tuyến (Ngang)
            item {
                Text(
                    text = "ĐANG TRỰC TUYẾN",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Nút Tạo mới / Thêm bạn (Mới)
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .border(1.dp, Color.DarkGray, CircleShape)
                                    .clickable { onNavigateToSearch() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Gray, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Mới", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    // Danh sách bạn bè trực tuyến
                    val onlineFriends = listOf("Anh", "Lê", "Nguyễn", "Trần", "Phạm")
                    items(onlineFriends.size) { index ->
                        OnlineFriendItem(name = onlineFriends[index])
                    }
                }
            }

            // 4. TIÊU ĐỀ TRÒ CHUYỆN
            item {
                Text(
                    text = "TRÒ CHUYỆN",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 12.dp)
                )
            }

            // 5. CHAT LIST (Dọc)
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
                    
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val timeStr = chat.lastMessage?.timestamp?.toDate()?.let { timeFormat.format(it) } ?: ""

                    ChatItem(
                        name = "Chat Group ${chat.id.take(4)}", // Tạm thời dùng ID, sẽ thay bằng tên thực sau
                        lastMessage = lastMessageText,
                        time = timeStr,
                        unreadCount = 0,
                        isOnline = false,
                        onClick = { onNavigateToConversation(chat.id) }
                    )
                }
            } else {
                // Hiển thị Mock Data nếu chưa có dữ liệu thật (để duy trì UI đẹp)
                items(5) { index ->
                    ChatItem(
                        name = if (index == 0) "Anh Em Sài Gòn" else if (index == 1) "Nguyễn Văn Hiếu" else "Người dùng $index",
                        lastMessage = if (index == 0) "Tối nay 7h lẩu bò nha ae!" else if (index == 1) "Bạn: Ko niet" else "Đã gửi một nhãn dán",
                        time = if (index == 1) "19:48" else "",
                        unreadCount = if (index == 0) 2 else 0,
                        isOnline = index % 3 == 0,
                        onClick = { onNavigateToConversation("mock_chat_$index") }
                    )
                }
            }
        }
    }
}

@Composable
fun OnlineFriendItem(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp)
        ) {
            // Avatar với viền Gradient cực ngầu
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
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(name.first().toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            
            // Chấm Online xanh lá góc phải dưới
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(2.dp, DarkBackground, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar trong danh sách chat
        Box(modifier = Modifier.size(60.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = name.first().toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            
            if (isOnline) {
                // Chấm Online xanh lá
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .border(2.dp, DarkBackground, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (time.isNotEmpty()) {
                    Text(
                        text = time,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = lastMessage,
                color = if (unreadCount > 0) Color.White else Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
            )
        }
        
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5A55FF)), // Nền thông báo xanh tím
                contentAlignment = Alignment.Center
            ) {
                Text(text = unreadCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------------------------------------------------
// Tạm giữ các hàm giữ chỗ cũ cho màn hình khác trong luồng Chat
// ---------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatId: String,
    viewModel: ChatViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val messagesState = viewModel?.messagesState?.collectAsState()?.value ?: Resource.Idle
    val currentUserId = viewModel?.currentUserId

    LaunchedEffect(chatId) {
        viewModel?.loadMessages(chatId)
    }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Tên & Trạng thái
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToGroupInfo(chatId) }
                ) {
                    Text("Anh Em Sài Gòn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Đang hoạt động", color = Color(0xFF22C55E), fontSize = 12.sp)
                }
                
                IconButton(onClick = { /* Gọi thoại */ }) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = NexusPrimary)
                }
                IconButton(onClick = { /* Gọi Video */ }) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video", tint = NexusPrimary)
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Thêm Media */ }) {
                    Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Add", tint = NexusPrimary)
                }
                IconButton(onClick = { /* Chụp ảnh */ }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = NexusPrimary)
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkCard, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (messageText.isEmpty()) {
                        Text("Nhắn tin...", color = Color.Gray, fontSize = 16.sp)
                    }
                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                if (messageText.isNotEmpty()) {
                    IconButton(onClick = { 
                        viewModel?.sendMessage(chatId, messageText)
                        messageText = "" 
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = NexusPrimary)
                    }
                } else {
                    IconButton(onClick = { /* Voice message */ }) {
                        Icon(Icons.Default.Mic, contentDescription = "Mic", tint = NexusPrimary)
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            reverseLayout = true,
            contentPadding = PaddingValues(16.dp)
        ) {
            if (messagesState is Resource.Loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NexusPrimary)
                    }
                }
            } else if (messagesState is Resource.Success && messagesState.data.isNotEmpty()) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                items(messagesState.data.size) { index ->
                    val msg = messagesState.data[index]
                    val isMe = msg.senderId == currentUserId
                    val timeStr = msg.timestamp?.toDate()?.let { timeFormat.format(it) } ?: ""
                    MessageBubble(text = msg.text, isMe = isMe, time = timeStr)
                }
            } else {
                // MOCK DATA (Hiển thị tạm nếu chưa có tin nhắn thật)
                item { MessageBubble(text = "Ok nha bạn!", isMe = true, time = "19:50") }
                item { MessageBubble(text = "Nhớ mang theo laptop nhé.", isMe = false, time = "19:49") }
                item { MessageBubble(text = "Tối nay 7h lẩu bò nha ae!", isMe = false, time = "19:48") }
                item { MessageBubble(text = "Chào mọi người", isMe = true, time = "19:45") }
            }
        }
    }
}

@Composable
fun MessageBubble(text: String, isMe: Boolean, time: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = if (isMe) Brush.linearGradient(listOf(Color(0xFF5A55FF), Color(0xFF00E5FF))) 
                            else Brush.linearGradient(listOf(DarkCard, DarkCard)),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(text = text, color = Color.White, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = time, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = { onGroupCreated("new_group_id") }) {
            Text("Create Group Screen")
        }
    }
}
