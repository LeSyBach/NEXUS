package com.example.nexus.feature_contact.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.model.FriendRequest
import com.example.nexus.feature_contact.viewmodel.ContactViewModel
import com.example.nexus.navigation.Screen
import com.example.nexus.ui.components.NexusBottomBar
import com.example.nexus.ui.theme.GradientEnd
import com.example.nexus.ui.theme.GradientStart
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    viewModel: ContactViewModel? = null,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFriendRequests: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToTab: (String) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val friendsListState = viewModel?.friendsList?.collectAsState()?.value ?: Resource.Idle
    val receivedRequests = viewModel?.friendRequests?.collectAsState()?.value
    val requestCount = (receivedRequests as? Resource.Success)?.data?.size ?: 0

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
                contentColor = nc.background,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Thêm bạn", modifier = Modifier.size(24.dp))
            }
        },
        bottomBar = {
            NexusBottomBar(
                currentRoute = Screen.Contacts.route,
                onNavigate = onNavigateToTab
            )
        },
        containerColor = nc.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
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
            }

            // Lời mời kết bạn button
            Surface(
                onClick = onNavigateToFriendRequests,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = nc.surfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NexusPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PersonAddAlt,
                            contentDescription = null,
                            tint = NexusPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Lời mời kết bạn",
                            color = nc.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        if (requestCount > 0) {
                            Text(
                                "$requestCount lời mời mới",
                                color = NexusPrimary,
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                "Xem lời mời đã gửi và nhận",
                                color = nc.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                    if (requestCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(NexusPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                requestCount.toString(),
                                color = nc.background,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = nc.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(rotationZ = 180f)
                    )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Danh sách bạn bè
            when (friendsListState) {
                is Resource.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NexusPrimary, strokeWidth = 2.dp)
                    }
                }
                is Resource.Success -> {
                    if (friendsListState.data.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(NexusPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PersonAddAlt,
                                        contentDescription = null,
                                        tint = NexusPrimary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Chưa có bạn bè nào", color = nc.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Bấm nút + để tìm và thêm bạn!", color = nc.textSecondary, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Text(
                            text = "${friendsListState.data.size} bạn bè",
                            color = nc.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                        )
                        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(friendsListState.data) { friend ->
                                ContactItem(
                                    name = friend.displayName.ifEmpty { friend.username },
                                    status = if (friend.status == Constants.USER_STATUS_ONLINE) "Đang hoạt động" else "",
                                    isOnline = friend.status == Constants.USER_STATUS_ONLINE,
                                    onClick = { onNavigateToProfile(friend.uid) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có bạn bè nào", color = nc.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(name: String, status: String, isOnline: Boolean, onClick: () -> Unit) {
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
                Text(
                    name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = nc.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
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
                Text(status, color = Color(0xFF22C55E), fontSize = 12.sp)
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

// ══════════════════════════════════════════════════════════
//  SEARCH USER SCREEN
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(
    viewModel: ContactViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    var searchQuery by remember { mutableStateOf("") }
    val searchState = viewModel?.searchResults?.collectAsState()?.value ?: Resource.Idle
    val friendsListState = viewModel?.friendsList?.collectAsState()?.value ?: Resource.Idle
    val sentIds = viewModel?.sentRequestIds?.collectAsState()?.value ?: emptySet()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val friendIds = remember(friendsListState) {
        if (friendsListState is Resource.Success) friendsListState.data.map { it.uid }.toSet()
        else emptySet()
    }

    LaunchedEffect(viewModel) {
        viewModel?.navigateToChatEvent?.collect { chatId ->
            onNavigateToChat(chatId)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel?.sendRequestResult?.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(nc.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                }
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel?.searchUsers(it)
                    },
                    placeholder = { Text("Tìm theo số điện thoại hoặc tên...", color = nc.textSecondary, fontSize = 14.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = nc.cardBg,
                        unfocusedContainerColor = nc.cardBg,
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
        containerColor = nc.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                searchState is Resource.Loading -> {
                    CircularProgressIndicator(color = NexusPrimary, modifier = Modifier.align(Alignment.Center))
                }
                searchState is Resource.Success && searchState.data.isEmpty() && searchQuery.isNotBlank() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Không tìm thấy người dùng nào", color = nc.textSecondary, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Thử tìm với tên hoặc số điện thoại khác", color = nc.textTertiary, fontSize = 13.sp)
                    }
                }
                searchState is Resource.Success && searchState.data.isNotEmpty() -> {
                    LazyColumn {
                        items(searchState.data) { user ->
                            val isFriend = user.uid in friendIds
                            val isSent = user.uid in sentIds

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToProfile(user.uid) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
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
                                        user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        color = nc.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        user.displayName.ifEmpty { user.username },
                                        color = nc.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    if (user.phone.isNotEmpty()) {
                                        Text(user.phone, color = nc.textSecondary, fontSize = 12.sp)
                                    }
                                }

                                if (isFriend) {
                                    Box(
                                        modifier = Modifier
                                            .background(NexusPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Bạn bè", color = NexusPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (!isSent) viewModel?.sendFriendRequest(user.uid)
                                        },
                                        enabled = !isSent,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NexusPrimary,
                                            disabledContainerColor = nc.surfaceElevated,
                                            contentColor = nc.background,
                                            disabledContentColor = nc.textSecondary
                                        )
                                    ) {
                                        Text(
                                            if (isSent) "Đã gửi" else "Kết bạn",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                searchQuery.isBlank() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NexusPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = NexusPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tìm kiếm bạn bè", color = nc.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Nhập số điện thoại hoặc tên người dùng", color = nc.textSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  FRIEND REQUESTS SCREEN – with tabs: ĐÃ NHẬN / ĐÃ GỬI
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    viewModel: ContactViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val receivedState = viewModel?.friendRequests?.collectAsState()?.value ?: Resource.Idle
    val sentState = viewModel?.sentRequests?.collectAsState()?.value ?: Resource.Idle
    val snackbarHostState = remember { SnackbarHostState() }
    var processingId by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel?.respondResult?.collect { message ->
            processingId = null
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Lời mời kết bạn", color = nc.textPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = nc.background)
                )
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = nc.background,
                    contentColor = NexusPrimary,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = NexusPrimary
                            )
                        }
                    },
                    divider = {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(nc.divider))
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            val receivedCount = (receivedState as? Resource.Success)?.data?.size ?: 0
                            Text(
                                if (receivedCount > 0) "Đã nhận ($receivedCount)" else "Đã nhận",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = NexusPrimary,
                        unselectedContentColor = nc.textSecondary
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            val sentCount = (sentState as? Resource.Success)?.data?.size ?: 0
                            Text(
                                if (sentCount > 0) "Đã gửi ($sentCount)" else "Đã gửi",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = NexusPrimary,
                        unselectedContentColor = nc.textSecondary
                    )
                }
            }
        },
        containerColor = nc.background
    ) { paddingValues ->
        when (selectedTab) {
            0 -> ReceivedRequestsTab(
                state = receivedState,
                processingId = processingId,
                onAccept = { requestId, fromUserId ->
                    processingId = requestId
                    viewModel?.respondToRequest(requestId, true, fromUserId)
                },
                onReject = { requestId, fromUserId ->
                    processingId = requestId
                    viewModel?.respondToRequest(requestId, false, fromUserId)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            1 -> SentRequestsTab(
                state = sentState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ReceivedRequestsTab(
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
private fun SentRequestsTab(
    state: Resource<List<FriendRequest>>,
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
                            SentRequestItem(request = request)
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
private fun ReceivedRequestItem(
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
            Text(
                request.fromUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = nc.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
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
private fun SentRequestItem(request: FriendRequest) {
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
        Box(
            modifier = Modifier
                .background(NexusPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text("Đã gửi", color = NexusPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
