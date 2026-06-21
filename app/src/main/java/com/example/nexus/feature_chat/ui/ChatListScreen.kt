package com.example.nexus.feature_chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.DateUtils
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.model.User
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.navigation.Screen
import com.example.nexus.ui.components.NexusBottomBar
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel? = null,
    onNavigateToConversation: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCamera: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToTab: (String) -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    val chatsState = viewModel?.chatsState?.collectAsState()?.value ?: Resource.Idle
    val onlineFriendsState = viewModel?.onlineFriends?.collectAsState()?.value ?: emptyList()
    val currentUserId = viewModel?.currentUserId
    val currentUserState = viewModel?.currentUser?.collectAsState()?.value
    var showAddMenu by remember { mutableStateOf(false) }
    val pinnedChatIds by viewModel?.pinnedChatIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) }
    var showChatMenu by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var viewingStoryUser by remember { mutableStateOf<User?>(null) }
    var viewingMyStory by remember { mutableStateOf(false) }
    val storiesState = viewModel?.stories?.collectAsState()?.value ?: emptyMap()
    val notesState = viewModel?.notes?.collectAsState()?.value ?: emptyMap()
    val imageStoriesState = viewModel?.imageStories?.collectAsState()?.value ?: emptyMap()
    var viewingNoteUser by remember { mutableStateOf<User?>(null) }
    var viewingMyNote by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NexusBottomBar(
                currentRoute = Screen.ChatList.route,
                onNavigate = onNavigateToTab
            )
        },
        containerColor = nc.background
    ) { paddingValues ->
        if (showCreateNoteDialog) {
            CreateNoteDialog(
                user = currentUserState,
                onDismiss = { showCreateNoteDialog = false },
                onPostNote = { text, photoUrl ->
                    viewModel?.postStory(content = photoUrl ?: text, type = if (photoUrl != null) "image" else "text")
                    showCreateNoteDialog = false
                }
            )
        }

        // View friend's IMAGE STORY (avatar tap)
        if (viewingStoryUser != null) {
            val user = viewingStoryUser!!
            val storyList = imageStoriesState[user.uid]
            if (!storyList.isNullOrEmpty()) {
                ViewStoryDialog(
                    stories = storyList,
                    user = user,
                    isMyStory = false,
                    onStoryViewed = { storyId -> viewModel?.markStoryAsViewed(storyId) },
                    onReply = { replyText ->
                        val currentStory = storyList.firstOrNull()
                        if (currentStory != null) {
                            viewModel?.replyToStory(currentStory, user.displayName.ifEmpty { user.username }, replyText) { chatId ->
                                onNavigateToConversation(chatId)
                            }
                        }
                    },
                    onDismiss = { viewingStoryUser = null }
                )
            } else {
                viewingStoryUser = null
            }
        }

        // View MY IMAGE STORY (avatar tap)
        if (viewingMyStory && currentUserState != null) {
            val myStories = currentUserId?.let { imageStoriesState[it] }
            if (!myStories.isNullOrEmpty()) {
                ViewStoryDialog(
                    stories = myStories,
                    user = currentUserState,
                    isMyStory = true,
                    onDelete = { storyId ->
                        viewModel?.deleteStory(storyId)
                        viewingMyStory = false
                    },
                    onDismiss = { viewingMyStory = false }
                )
            } else {
                viewingMyStory = false
            }
        }

        // View friend's NOTE (bubble tap)
        if (viewingNoteUser != null) {
            val user = viewingNoteUser!!
            val note = notesState[user.uid]
            if (note != null) {
                ViewNoteDialog(
                    story = note,
                    user = user,
                    isMyStory = false,
                    onNoteViewed = { viewModel?.markStoryAsViewed(note.id) },
                    onReply = { replyText ->
                        viewModel?.replyToStory(note, user.displayName.ifEmpty { user.username }, replyText) { chatId ->
                            onNavigateToConversation(chatId)
                        }
                    },
                    onDismiss = { viewingNoteUser = null }
                )
            } else {
                viewingNoteUser = null
            }
        }

        // View MY NOTE (bubble tap)
        if (viewingMyNote && currentUserState != null) {
            val myNote = currentUserId?.let { notesState[it] }
            if (myNote != null) {
                ViewNoteDialog(
                    story = myNote,
                    user = currentUserState,
                    isMyStory = true,
                    onDelete = {
                        viewModel?.deleteStory(myNote.id)
                        viewingMyNote = false
                    },
                    onDismiss = { viewingMyNote = false }
                )
            } else {
                viewingMyNote = false
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEXUS",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = NexusPrimary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onNavigateToNotifications) {
                        Box {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Thong bao",
                                tint = nc.textPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                            val unreadCount = viewModel?.unreadNotificationCount?.collectAsState()?.value ?: 0
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadCount > 9) "9+" else "$unreadCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
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
                            Text("Tim kiem, AI...", color = nc.textSecondary, fontSize = 15.sp, maxLines = 1)
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
                            Icon(Icons.Default.Add, contentDescription = "Them moi", tint = nc.textPrimary)
                        }

                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false },
                            modifier = Modifier.background(nc.cardBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Them ban", color = nc.textPrimary) },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NexusPrimary) },
                                onClick = {
                                    showAddMenu = false
                                    onNavigateToSearch()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tao nhom", color = nc.textPrimary) },
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
                    text = "DANG TRUC TUYEN",
                    color = nc.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val myNote = currentUserId?.let { notesState[it] }
                        val myImageStory = currentUserId?.let { imageStoriesState[it]?.firstOrNull() }
                        MeStoryItem(
                            user = currentUserState,
                            note = myNote,
                            imageStory = myImageStory,
                            onPlusClick = onNavigateToCamera,
                            onBubbleClick = {
                                if (myNote != null) viewingMyNote = true
                                else showCreateNoteDialog = true
                            },
                            onAvatarClick = {
                                if (myImageStory != null) viewingMyStory = true
                                else onNavigateToCamera()
                            }
                        )
                    }

                    items(onlineFriendsState.size) { index ->
                        val friend = onlineFriendsState[index]
                        val name = friend.displayName.ifEmpty { friend.username }
                        val friendNote = notesState[friend.uid]
                        val friendImageStory = imageStoriesState[friend.uid]
                        val hasNote = friendNote != null
                        val hasStory = friendImageStory != null

                        val compactTime = if (friend.status != Constants.USER_STATUS_ONLINE) {
                            val lastSeen = friend.lastSeen?.toDate()?.time ?: 0L
                            if (lastSeen > 0L) {
                                val diffMs = System.currentTimeMillis() - lastSeen
                                val diffMins = diffMs / (60 * 1000)
                                val diffHours = diffMins / 60
                                when {
                                    diffMins < 60 -> "${diffMins}p"
                                    diffHours < 24 -> "${diffHours}h"
                                    else -> ""
                                }
                            } else ""
                        } else null

                        OnlineFriendItem(
                            name = name,
                            avatarUrl = friend.avatarUrl.ifEmpty { null },
                            hasNote = hasNote,
                            hasStory = hasStory,
                            noteText = friendNote?.content,
                            lastActive = compactTime,
                            onAvatarClick = {
                                if (hasStory) viewingStoryUser = friend
                                else {
                                    val directChat = (chatsState as? Resource.Success)?.data?.find { it.type == Constants.CHAT_TYPE_DIRECT && it.participants.contains(friend.uid) }
                                    if (directChat != null) onNavigateToConversation(directChat.id)
                                }
                            },
                            onBubbleClick = {
                                if (hasNote) viewingNoteUser = friend
                            },
                            onClick = {
                                val directChat = (chatsState as? Resource.Success)?.data?.find { it.type == Constants.CHAT_TYPE_DIRECT && it.participants.contains(friend.uid) }
                                if (directChat != null) {
                                    onNavigateToConversation(directChat.id)
                                }
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "TRO CHUYEN",
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
                val allChats = chatsState.data
                val activeChats = allChats.filter { chat -> currentUserId == null || !chat.archivedBy.contains(currentUserId) }
                val sortedChats = activeChats.sortedByDescending { it.id in pinnedChatIds }

                // Active chats
                items(sortedChats.size) { index ->
                    val chat = sortedChats[index]
                    val lastMessageText = chat.lastMessage?.text ?: "Chua co tin nhan"

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
                        isPinned = chat.id in pinnedChatIds,
                        onClick = { onNavigateToConversation(chat.id) },
                        onLongClick = { showChatMenu = Pair(chat.id, displayName) }
                    )
                }
            } else if (chatsState is Resource.Success && chatsState.data.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Chua co cuoc tro chuyen nao", color = nc.textSecondary, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ket ban va bat dau nhan tin!", color = NexusPrimary, fontSize = 14.sp)
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
                                viewModel?.toggleChatPin(chatId)
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
                            if (isChatPinned) "Bo ghim" else "Ghim tin nhan",
                            color = nc.textPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel?.archiveChat(chatId)
                                showChatMenu = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, tint = nc.iconTint, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Luu tru", color = nc.textPrimary)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel?.clearChatMessages(chatId)
                                showChatMenu = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = nc.errorText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Xoa cuoc tro chuyen", color = nc.errorText)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChatMenu = null }) {
                    Text("Dong", color = NexusPrimary)
                }
            }
        )
    }
}
