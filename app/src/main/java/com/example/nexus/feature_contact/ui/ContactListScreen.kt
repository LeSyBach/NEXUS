package com.example.nexus.feature_contact.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.DateUtils
import com.example.nexus.core.utils.Resource
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
                                val isOnline = friend.status == Constants.USER_STATUS_ONLINE
                                val statusText = if (isOnline) {
                                    "Đang hoạt động"
                                } else {
                                    friend.lastSeen?.let { DateUtils.getRelativeTimeSpan(it.toDate().time) } ?: ""
                                }
                                ContactItem(
                                    name = friend.displayName.ifEmpty { friend.username },
                                    status = statusText,
                                    isOnline = isOnline,
                                    avatarUrl = friend.avatarUrl.ifEmpty { null },
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
