package com.example.nexus.feature_contact.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.utils.Resource
import com.example.nexus.feature_contact.viewmodel.ContactViewModel
import com.example.nexus.ui.components.NexusBottomBar
import com.example.nexus.navigation.Screen
import com.example.nexus.ui.theme.DarkBackground
import com.example.nexus.ui.theme.DarkCard
import com.example.nexus.ui.theme.GradientEnd
import com.example.nexus.ui.theme.GradientStart
import com.example.nexus.ui.theme.NexusPrimary

// ══════════════════════════════════════════════════════════
//  CONTACT LIST SCREEN – Main "Danh Bạ" tab
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    viewModel: ContactViewModel? = null,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFriendRequests: () -> Unit,
    onNavigateToTab: (String) -> Unit
) {
    val friendsListState = viewModel?.friendsList?.collectAsState()?.value ?: Resource.Idle

    // Listen for startChatWithFriend navigation events
    LaunchedEffect(viewModel) {
        viewModel?.navigateToChatEvent?.collect { chatId ->
            onNavigateToChat(chatId)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = NexusPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Thêm bạn")
            }
        },
        bottomBar = {
            NexusBottomBar(
                currentRoute = Screen.Contacts.route,
                onNavigate = onNavigateToTab
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DANH BẠ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        brush = Brush.linearGradient(colors = listOf(GradientStart, GradientEnd))
                    ),
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = onNavigateToFriendRequests,
                    modifier = Modifier.background(DarkCard, CircleShape)
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Lời mời kết bạn", tint = Color.White)
                }
            }

            // ── Friends list ──
            when (friendsListState) {
                is Resource.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NexusPrimary)
                    }
                }
                is Resource.Success -> {
                    if (friendsListState.data.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Bạn chưa có người bạn nào.", color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Bấm nút + để thêm bạn ngay!", color = NexusPrimary)
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(friendsListState.data) { friend ->
                                ContactItem(
                                    name = friend.displayName.ifEmpty { friend.username },
                                    phone = friend.phone,
                                    onClick = { viewModel?.startChatWithFriend(friend.uid) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bạn chưa có người bạn nào.", color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bấm nút + để thêm bạn ngay!", color = NexusPrimary)
                        }
                    }
                }
            }
        }
    }
}

// ── Single contact row ──
@Composable
fun ContactItem(name: String, phone: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NexusPrimary.copy(alpha = 0.6f), DarkCard))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(phone, color = Color.Gray, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.Chat, contentDescription = "Nhắn tin", tint = NexusPrimary.copy(alpha = 0.7f))
    }
}

// ══════════════════════════════════════════════════════════
//  SEARCH USER SCREEN
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(
    viewModel: ContactViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchState = viewModel?.searchResults?.collectAsState()?.value ?: Resource.Idle
    val friendsListState = viewModel?.friendsList?.collectAsState()?.value ?: Resource.Idle
    val focusManager = LocalFocusManager.current

    // Track sent requests locally for instant feedback
    val sentRequests = remember { mutableStateListOf<String>() }

    // Friends set for O(1) lookup
    val friendIds = remember(friendsListState) {
        if (friendsListState is Resource.Success) friendsListState.data.map { it.uid }.toSet()
        else emptySet()
    }

    // Navigate to chat when viewModel fires the event
    LaunchedEffect(viewModel) {
        viewModel?.navigateToChatEvent?.collect { chatId ->
            onNavigateToChat(chatId)
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel?.searchUsers(it)
                    },
                    placeholder = { Text("Tìm theo số điện thoại hoặc tên...", color = Color.Gray, fontSize = 14.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        cursorColor = NexusPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                searchState is Resource.Loading -> {
                    CircularProgressIndicator(color = NexusPrimary, modifier = Modifier.align(Alignment.Center))
                }
                searchState is Resource.Success && searchState.data.isEmpty() && searchQuery.isNotBlank() -> {
                    Text(
                        "Không tìm thấy người dùng nào",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                searchState is Resource.Success && searchState.data.isNotEmpty() -> {
                    LazyColumn {
                        items(searchState.data) { user ->
                            val isFriend = user.uid in friendIds
                            val isSent   = user.uid in sentRequests

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(NexusPrimary.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            user.displayName.ifEmpty { user.username },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(user.phone, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }

                                if (isFriend) {
                                    // Already friends → show "Nhắn tin" button
                                    IconButton(
                                        onClick = { viewModel?.startChatWithFriend(user.uid) },
                                        modifier = Modifier.background(NexusPrimary.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = "Nhắn tin", tint = NexusPrimary)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (!isSent) {
                                                viewModel?.sendFriendRequest(user.uid)
                                                sentRequests.add(user.uid)
                                            }
                                        },
                                        enabled = !isSent,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NexusPrimary,
                                            disabledContainerColor = Color.DarkGray
                                        )
                                    ) {
                                        Text(if (isSent) "Đã gửi" else "Kết bạn", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                searchQuery.isBlank() -> {
                    Text(
                        "Gõ số điện thoại hoặc tên để tìm kiếm...",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  FRIEND REQUESTS SCREEN
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    viewModel: ContactViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val requestsState = viewModel?.friendRequests?.collectAsState()?.value ?: Resource.Idle

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lời mời kết bạn", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (requestsState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(color = NexusPrimary, modifier = Modifier.align(Alignment.Center))
                }
                is Resource.Success -> {
                    if (requestsState.data.isEmpty()) {
                        Text(
                            "Không có lời mời kết bạn nào",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn {
                            items(requestsState.data) { request ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(NexusPrimary.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                request.fromUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                request.fromUsername.ifEmpty { "Người dùng" },
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text("Muốn kết bạn với bạn", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }

                                    Row {
                                        // Accept
                                        IconButton(
                                            onClick = {
                                                viewModel?.respondToRequest(request.id, true, request.fromUserId)
                                            },
                                            modifier = Modifier.background(Color(0xFF22C55E).copy(alpha = 0.2f), CircleShape)
                                        ) {
                                            Icon(Icons.Outlined.Check, contentDescription = "Đồng ý", tint = Color(0xFF22C55E))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Reject
                                        IconButton(
                                            onClick = {
                                                viewModel?.respondToRequest(request.id, false, request.fromUserId)
                                            },
                                            modifier = Modifier.background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape)
                                        ) {
                                            Icon(Icons.Outlined.Close, contentDescription = "Từ chối", tint = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text("Không có lời mời kết bạn nào", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
