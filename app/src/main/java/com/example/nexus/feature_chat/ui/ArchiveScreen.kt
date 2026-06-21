package com.example.nexus.feature_chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.utils.DateUtils
import com.example.nexus.core.utils.Resource
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.navigation.Screen
import com.example.nexus.ui.components.NexusBottomBar
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: ChatViewModel? = null,
    onNavigateToConversation: (String) -> Unit,
    onNavigateToTab: (String) -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    val chatsState = viewModel?.chatsState?.collectAsState()?.value ?: Resource.Idle
    val currentUserId = viewModel?.currentUserId

    Scaffold(
        bottomBar = {
            NexusBottomBar(
                currentRoute = Screen.Archive.route,
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
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    "Kho lưu trữ",
                    color = nc.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            when (chatsState) {
                is Resource.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NexusPrimary)
                    }
                }
                is Resource.Success -> {
                    val allChats = chatsState.data
                    val archivedChats = allChats.filter { chat ->
                        currentUserId != null && chat.archivedBy.contains(currentUserId)
                    }

                    if (archivedChats.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = nc.textTertiary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Không có đoạn chat nào đã lưu trữ",
                                    color = nc.textTertiary,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(archivedChats.size) { index ->
                                val chat = archivedChats[index]
                                val lastMessageText = chat.lastMessage?.text ?: "Chưa có tin nhắn"
                                val timeStr = chat.lastMessage?.timestamp?.toDate()?.let { DateUtils.formatChatTime(it.time) } ?: ""
                                var displayName by remember { mutableStateOf(chat.groupName.ifEmpty { "..." }) }
                                var avatarUrl by remember { mutableStateOf<String?>(null) }
                                LaunchedEffect(chat.id) {
                                    displayName = viewModel?.resolveDisplayName(chat) ?: chat.groupName
                                    avatarUrl = viewModel?.resolveAvatarUrl(chat)
                                }
                                val myId = viewModel?.currentUserId
                                val unreadCount = if (myId != null) (chat.lastMessage?.unreadCount?.get(myId) ?: 0L).toInt() else 0

                                ChatItem(
                                    name = displayName,
                                    avatarUrl = avatarUrl,
                                    lastMessage = lastMessageText,
                                    time = timeStr,
                                    unreadCount = unreadCount,
                                    isOnline = viewModel?.isUserOnline(chat) ?: false,
                                    isPinned = false,
                                    onClick = { onNavigateToConversation(chat.id) },
                                    onLongClick = { viewModel?.unarchiveChat(chat.id) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có tin nhắn", color = nc.textTertiary, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
