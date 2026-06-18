package com.example.nexus.feature_chat.ui

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.util.Patterns
import android.widget.Toast
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.DateUtils
import com.example.nexus.core.utils.Resource
import com.example.nexus.core.utils.createTempImageUri
import com.example.nexus.core.utils.createTempVideoUri
import com.example.nexus.core.utils.toReadableFileSize
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Message
import com.example.nexus.data.model.PinnedMessage
import com.example.nexus.data.model.ReplyMessage
import com.example.nexus.data.model.User
import com.example.nexus.data.firebase.PlaybackState
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.feature_chat.viewmodel.UploadState
import com.example.nexus.feature_chat.viewmodel.VoiceRecordingState
import com.example.nexus.feature_chat.viewmodel.AiSummaryState
import com.example.nexus.navigation.Screen
import com.example.nexus.ui.components.NexusBottomBar

import com.example.nexus.ui.theme.NexusColors
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
                                LaunchedEffect(chat.id) {
                                    displayName = viewModel?.resolveDisplayName(chat) ?: chat.groupName
                                }
                                val myId = viewModel?.currentUserId
                                val unreadCount = if (myId != null) (chat.lastMessage?.unreadCount?.get(myId) ?: 0L).toInt() else 0

                                ChatItem(
                                    name = displayName,
                                    avatarUrl = viewModel?.resolveAvatarUrl(chat),
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

@Composable
fun ViewNoteDialog(
    story: com.example.nexus.data.model.Story,
    user: User,
    isMyStory: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onNoteViewed: (() -> Unit)? = null,
    onReply: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val nc = MaterialTheme.nexusColors

    LaunchedEffect(story.id) {
        if (!isMyStory) onNoteViewed?.invoke()
    }

    val timeAgo = remember(story.createdAt) {
        val diff = System.currentTimeMillis() - (story.createdAt?.toDate()?.time ?: 0L)
        val mins = diff / 60000
        when {
            mins < 1 -> "Vừa xong"
            mins < 60 -> "${mins}p trước"
            mins < 1440 -> "${mins / 60}h trước"
            else -> "${mins / 1440} ngày trước"
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(vertical = 48.dp, horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (user.avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(nc.avatarBg), contentAlignment = Alignment.Center) {
                                Text(user.displayName.ifEmpty { user.username }.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = nc.textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(if (isMyStory) "Ghi chú của bạn" else user.displayName.ifEmpty { user.username }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(timeAgo, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Content - Note style (centered text with avatar below)
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .background(nc.surfaceVariant, RoundedCornerShape(24.dp))
                                .padding(horizontal = 32.dp, vertical = 24.dp)
                        ) {
                            Text(
                                text = story.content,
                                color = nc.textPrimary,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 28.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Avatar below note (Messenger-style)
                        if (user.avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action
                if (isMyStory) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = "Views", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${story.viewedBy.size} người đã xem", color = Color.Gray)
                        }
                        IconButton(onClick = { onDelete?.invoke() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = nc.errorText)
                        }
                    }
                } else {
                    var replyText by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Trả lời ghi chú...", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onReply?.invoke(replyText.trim())
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.background(if (replyText.isNotBlank()) Color(0xFF00C6FF) else Color.DarkGray, CircleShape).size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ViewStoryDialog(
    stories: List<com.example.nexus.data.model.Story>,
    user: User,
    isMyStory: Boolean = false,
    onDelete: ((String) -> Unit)? = null,
    onStoryViewed: ((String) -> Unit)? = null,
    onReply: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    var currentIndex by remember { mutableIntStateOf(0) }
    val story = stories.getOrNull(currentIndex) ?: return

    // Mark story as viewed
    LaunchedEffect(story.id) {
        if (!isMyStory) onStoryViewed?.invoke(story.id)
    }

    // Auto-advance timer (5 seconds per story)
    LaunchedEffect(currentIndex) {
        kotlinx.coroutines.delay(5000)
        if (currentIndex < stories.size - 1) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    val timeAgo = remember(story.createdAt) {
        val diff = System.currentTimeMillis() - (story.createdAt?.toDate()?.time ?: 0L)
        val mins = diff / 60000
        when {
            mins < 1 -> "Vừa xong"
            mins < 60 -> "${mins}p trước"
            mins < 1440 -> "${mins / 60}h trước"
            else -> "${mins / 1440} ngày trước"
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Story image full screen
            if (story.content.isNotEmpty()) {
                AsyncImage(
                    model = story.content,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Tap zones for prev/next
            Box(modifier = Modifier.fillMaxSize()) {
                // Left tap zone - previous
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (currentIndex > 0) currentIndex--
                            }
                        )
                )
                // Right tap zone - next
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .align(Alignment.TopEnd)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (currentIndex < stories.size - 1) currentIndex++
                                else onDismiss()
                            }
                        )
                )
            }

            // Gradient overlay top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )

            // Top header with progress bars
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // Segmented progress bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (i in stories.indices) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        i < currentIndex -> Color.White
                                        i == currentIndex -> Color.White
                                        else -> Color.White.copy(alpha = 0.3f)
                                    }
                                )
                        ) {
                            if (i == currentIndex) {
                                // Animated progress for current story
                                val progress = remember { androidx.compose.animation.core.Animatable(0f) }
                                LaunchedEffect(story.id) {
                                    progress.snapTo(0f)
                                    progress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 5000,
                                            easing = androidx.compose.animation.core.LinearEasing
                                        )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress.value)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (user.avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(nc.avatarBg), contentAlignment = Alignment.Center) {
                                Text(user.displayName.ifEmpty { user.username }.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = nc.textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(user.displayName.ifEmpty { user.username }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(timeAgo, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Caption overlay bottom
            if (!story.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Text(
                        text = story.caption,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bottom actions
            if (isMyStory) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = "Views", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${story.viewedBy.size} người đã xem", color = Color.White)
                    }
                    IconButton(onClick = { onDelete?.invoke(story.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = nc.errorText)
                    }
                }
            } else {
                // Reply input for other's story
                var replyText by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Trả lời tin...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReply?.invoke(replyText.trim())
                                onDismiss()
                            }
                        },
                        modifier = Modifier.background(if (replyText.isNotBlank()) Color(0xFF00C6FF) else Color.DarkGray, CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateNoteDialog(
    user: com.example.nexus.data.model.User?,
    onDismiss: () -> Unit,
    onPostNote: (String, String?) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    var noteText by remember { mutableStateOf("") }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Text("Ghi chú mới", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                
                Button(
                    onClick = { onPostNote(noteText, null) },
                    enabled = noteText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF222222),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Chia sẻ", fontWeight = FontWeight.Bold)
                }
            }

            // Center Content
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bubble input
                Box(
                    modifier = Modifier
                        .background(Color(0xFF333333), RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .widthIn(min = 180.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = noteText,
                        onValueChange = { if (it.length <= 60) noteText = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00C6FF)),
                        decorationBox = { innerTextField ->
                            if (noteText.isEmpty()) {
                                Text("| Chia sẻ suy nghĩ...", color = Color.Gray, fontSize = 18.sp)
                            } else {
                                innerTextField()
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(nc.avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (user != null && user.avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initial = user?.displayName?.ifEmpty { user.username }?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                        Text(initial, color = nc.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

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
                                contentDescription = "Thông báo",
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

                        val compactTime = if (friend.status != com.example.nexus.core.utils.Constants.USER_STATUS_ONLINE) {
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
                                    val directChat = (chatsState as? Resource.Success)?.data?.find { it.type == com.example.nexus.core.utils.Constants.CHAT_TYPE_DIRECT && it.participants.contains(friend.uid) }
                                    if (directChat != null) onNavigateToConversation(directChat.id)
                                }
                            },
                            onBubbleClick = {
                                if (hasNote) viewingNoteUser = friend
                            },
                            onClick = {
                                val directChat = (chatsState as? Resource.Success)?.data?.find { it.type == com.example.nexus.core.utils.Constants.CHAT_TYPE_DIRECT && it.participants.contains(friend.uid) }
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
                val allChats = chatsState.data
                val activeChats = allChats.filter { chat -> currentUserId == null || !chat.archivedBy.contains(currentUserId) }
                val sortedChats = activeChats.sortedByDescending { it.id in pinnedChatIds }

                // Active chats
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
                        avatarUrl = viewModel?.resolveAvatarUrl(chat),
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
                            if (isChatPinned) "Bỏ ghim" else "Ghim tin nhắn",
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
                        Text("Lưu trữ", color = nc.textPrimary)
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
fun MeStoryItem(
    user: User?,
    note: com.example.nexus.data.model.Story?,
    imageStory: com.example.nexus.data.model.Story?,
    onPlusClick: () -> Unit,
    onBubbleClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val hasNote = note != null
    val hasStory = imageStory != null
    val bubbleText = note?.content?.takeIf { it.isNotBlank() } ?: "Chia sẻ ghi chú"

    Box(
        modifier = Modifier.width(72.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(68.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (hasStory) 2.dp else 4.dp)
                    .then(
                        if (hasStory) Modifier.border(
                            width = 2.5.dp,
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF00FF87), Color(0xFF60EFFF))
                            ),
                            shape = CircleShape
                        ) else if (hasNote) Modifier.border(
                            width = 2.5.dp,
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                            ),
                            shape = CircleShape
                        ) else Modifier
                    )
                    .padding(if (hasStory || hasNote) 3.dp else 0.dp)
                    .clip(CircleShape)
                    .background(nc.avatarBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAvatarClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (user != null && user.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initial = user?.displayName?.ifEmpty { user.username }?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(initial, color = nc.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Nút + (To hơn)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(nc.surfaceVariant)
                    .border(2.dp, nc.background, CircleShape)
                    .align(Alignment.BottomEnd)
                    .clickable(onClick = onPlusClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Instant Camera", tint = nc.textPrimary, modifier = Modifier.size(20.dp))
            }
        }

        // Bubble (Placed using layout to avoid affecting parent bounds)
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(androidx.compose.ui.unit.Constraints())
                    layout(0, 0) {
                        placeable.placeRelative(
                            x = -placeable.width / 2 - 12.dp.roundToPx(),
                            y = -placeable.height / 2
                        )
                    }
                }
                .zIndex(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBubbleClick
                )
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 68.dp)
                    .background(nc.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = bubbleText,
                    color = nc.textPrimary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun OnlineFriendItem(
    name: String,
    avatarUrl: String? = null,
    isMe: Boolean = false,
    hasNote: Boolean = false,
    hasStory: Boolean = false,
    noteText: String? = null,
    lastActive: String? = null,
    onAvatarClick: () -> Unit = {},
    onBubbleClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier.size(68.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (hasStory || hasNote) 2.dp else 4.dp)
                    .then(
                        if (hasStory) Modifier.border(
                            width = 2.5.dp,
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF00FF87), Color(0xFF60EFFF))
                            ),
                            shape = CircleShape
                        ) else if (hasNote) Modifier.border(
                            width = 2.5.dp,
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                            ),
                            shape = CircleShape
                        ) else Modifier
                    )
                    .padding(if (hasStory || hasNote) 3.dp else 0.dp)
                    .clip(CircleShape)
                    .background(nc.avatarBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (hasStory) onAvatarClick()
                            else if (hasNote) onBubbleClick()
                            else onClick()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(initial, color = nc.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isMe) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(nc.surfaceVariant)
                        .border(2.dp, nc.background, CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note", tint = nc.textPrimary, modifier = Modifier.size(16.dp))
                }
            } else {
                if (lastActive == null) {
                    // Online Green Dot
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .border(2.dp, nc.background, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                } else if (lastActive.isNotEmpty()) {
                    // Offline but recently active (e.g., "5p")
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(8.dp))
                            .background(nc.surfaceVariant)
                            .border(2.dp, nc.background, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(lastActive, color = nc.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (hasNote && !noteText.isNullOrBlank()) {
                    // Note indicator bubble with actual text - clickable
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-2).dp)
                            .widthIn(max = 68.dp)
                            .background(nc.surfaceVariant, RoundedCornerShape(12.dp))
                            .border(2.dp, nc.background, RoundedCornerShape(12.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBubbleClick
                            )
                    ) {
                        Text(
                            text = noteText,
                            color = nc.textPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            color = nc.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
    name: String,
    avatarUrl: String? = null,
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
                    if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initial = name.firstOrNull()?.toString() ?: "?"
                        Text(text = initial, color = nc.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
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

    // ── AI State ──
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

    // Load more when scrolling near the top of chat (oldest messages).
    // With reverseLayout=true, the list is inverted:
    //   - visual bottom = index 0 (newest)
    //   - visual top = highest index (oldest)
    // User scrolls UP → sees higher indices → triggers load more.
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            // lastVisibleItemIndex = item at the TOP of viewport (highest visible index)
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val canScrollForward = listState.canScrollForward
            Triple(total, lastVisible, canScrollForward)
        }.collect { (total, _, canScrollForward) ->
            // Trigger when user can't scroll further toward older messages (top of chat)
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

    // Mark as seen once when entering the chat (not on every message update to avoid infinite loop)
    LaunchedEffect(chatId) {
        kotlinx.coroutines.delay(1000) // Wait for messages to load first
        viewModel?.markMessagesAsSeen(chatId)
    }

    LaunchedEffect(chatId) {
        viewModel?.startObservingTyping(chatId)
    }

    // Handle action from ChatInfoScreen navigation
    LaunchedEffect(initialAction) {
        when (initialAction) {
            "search" -> viewModel?.startSearch()
            "share_contact" -> viewModel?.openContactPicker()
        }
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
    val isSearchActive = viewModel?.isSearchActive?.collectAsState()?.value ?: false
    val searchQuery = viewModel?.searchQuery?.collectAsState()?.value ?: ""
    val searchResults = viewModel?.searchResults?.collectAsState()?.value ?: emptyList()
    val currentSearchIndex = viewModel?.currentSearchIndex?.collectAsState()?.value ?: -1
    var showMessageMenu by remember { mutableStateOf<Pair<String, Message>?>(null) }
    val showContactPicker = viewModel?.showContactPicker?.collectAsState()?.value ?: false

    // Auto-scroll to current search result
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

    // Resolve sender display names and avatars for group messages
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

    // Load group members for @mention popup
    LaunchedEffect(isGroup, currentChat) {
        if (!isGroup) return@LaunchedEffect
        val participantIds = currentChat?.participants ?: return@LaunchedEffect
        val users = viewModel?.getUsersByIds(participantIds) ?: return@LaunchedEffect
        mentionMembers = users.filter { it.uid != currentUserId }
    }

    // ── Scroll state for hiding floating button ──
    val isScrollingUp = listState.isScrollInProgress && listState.firstVisibleItemIndex > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(nc.background)
            .imePadding()
    ) {
        // ── Pattern Background ──
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
            // ── Top Bar ──
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

        // ── Search Overlay ──
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

        // ── Offline banner ──
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

        // ── Messages ──
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
                        // Upload progress bubble (appears at bottom since reverseLayout=true)
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

                                // Only show status on the newest message, and only if I sent it
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
                                            // Find story owner from stories/notes state and open viewer
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

                            // Loading indicator for older messages (at bottom of reversed list = top of chat)
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

        // ── Smart Reply Bar ──
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
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
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

        // Message action dialog — Messenger style
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
                        // Emoji reaction row
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

                        // Reply
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

                        // Copy
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

                        // Forward
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

                        // Download (only for media/file messages)
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

                        // Recall (only for own messages)
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

                        // Delete
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

            // Try image story first
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
                // Story expired or not found
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
                        Icon(Icons.Default.Pause, contentDescription = "Tạm dừng", tint = sentBubbleColor, modifier = Modifier.size(24.dp))
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
                            .background(sentBubbleColor)
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
                        Icon(Icons.Default.Refresh, contentDescription = "Ghi lại", tint = sentBubbleColor, modifier = Modifier.size(22.dp))
                    }

                    // Continue (play/resume preview)
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
                                    .background(if (i <= activeBar) sentBubbleColor else nc.divider)
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

        // ── Floating AI Summarize Button ──
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
                    // Gradient icon circle
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00C6FF), // Cyan
                                        Color(0xFF0072FF)  // Blue
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

    // ── Reaction Detail Sheet ──
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

    // ── Emoji Picker ──
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

    // ── AI Summary Bottom Sheet (single sheet, content changes with state) ──
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

    // ── Contact Picker Bottom Sheet (share current chat partner to another conversation) ──
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

                // Show who is being shared
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

@Composable
private fun StoryReplyBubble(
    message: Message?,
    text: String,
    isMe: Boolean,
    sentBubbleBrush: Brush,
    bubbleShape: RoundedCornerShape,
    nc: NexusColors,
    reactions: Map<String, String>,
    onLongClick: (() -> Unit)?,
    onForward: (() -> Unit)?,
    onReactionsClick: ((Map<String, String>, String) -> Unit)?,
    onStoryReplyClick: ((String) -> Unit)?
) {
    val isNoteReply = message?.type == Constants.MESSAGE_TYPE_NOTE_REPLY
    val storyId = message?.storyId ?: ""
    val storyContent = message?.storyContent ?: ""
    val storyCaption = message?.storyCaption ?: ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isMe) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
                    .clickable { onForward?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Chuyển tiếp",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp).graphicsLayer(scaleX = -1f)
                )
            }
        }
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .then(if (reactions.isNotEmpty()) Modifier.padding(bottom = 12.dp) else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (storyId.isNotEmpty()) onStoryReplyClick?.invoke(storyId) },
                        onLongClick = onLongClick
                    )
                    .then(if (isMe) Modifier.background(sentBubbleBrush, bubbleShape) else Modifier.background(nc.receivedBubble, bubbleShape))
                    .padding(4.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable { if (storyId.isNotEmpty()) onStoryReplyClick?.invoke(storyId) }
                            .padding(12.dp)
                    ) {
                        if (isNoteReply) {
                            Column {
                                Text(
                                    text = "Ghi chú",
                                    color = nc.textTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = storyContent,
                                    color = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                                    fontSize = 14.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (storyContent.isNotEmpty()) {
                                    AsyncImage(
                                        model = storyContent,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tin",
                                        color = nc.textTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (storyCaption.isNotEmpty()) {
                                        Text(
                                            text = storyCaption,
                                            color = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = text,
                        color = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            if (reactions.isNotEmpty()) {
                val displayEmoji = reactions.values.groupBy { e: String -> e }.maxByOrNull { entry -> entry.value.size }?.key ?: reactions.values.first()
                val count = reactions.size
                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 10.dp)
                            .clickable { onReactionsClick?.invoke(reactions, message?.id ?: "") }
                            .background(nc.background, RoundedCornerShape(10.dp))
                            .border(1.dp, nc.divider, RoundedCornerShape(10.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(displayEmoji, fontSize = 14.sp)
                            if (count > 1) {
                                Text(text = count.toString(), color = nc.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 1.dp))
                            }
                        }
                    }
                }
            }
        }
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
                    .clickable { onForward?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Chuyển tiếp",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp).graphicsLayer(scaleX = -1f)
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
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
    isOriginalRecalled: Boolean = false,
    avatarInitial: String = "",
    showAvatar: Boolean = false,
    senderName: String = "",
    avatarUrl: String? = null,
    messageType: String = Constants.MESSAGE_TYPE_TEXT,
    duration: Long = 0,
    message: Message? = null,
    currentUserId: String? = null,
    isSending: Boolean = false,
    isSearchHighlight: Boolean = false,
    sentBubbleColor: Color = MaterialTheme.nexusColors.sentBubble,
    onLongClick: (() -> Unit)? = null,
    onReply: (() -> Unit)? = null,
    onReact: ((String) -> Unit)? = null,
    onReactionsClick: ((Map<String, String>, String) -> Unit)? = null,
    onQuoteClick: ((String) -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    onVideoClick: (() -> Unit)? = null,
    onContactClick: ((String) -> Unit)? = null,
    onStoryReplyClick: ((String) -> Unit)? = null
) {
    val nc = MaterialTheme.nexusColors
    val avatarSize = 28
    val sentBubbleBrush = remember(sentBubbleColor, nc.isLight) {
        val targetColor = if (nc.isLight) Color.White else Color.Black
        val argb1 = sentBubbleColor.toArgb()
        val argb2 = androidx.core.graphics.ColorUtils.blendARGB(argb1, targetColor.toArgb(), 0.15f)
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(sentBubbleColor, Color(argb2))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSearchHighlight) {
                    Modifier
                        .background(NexusPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.5.dp, NexusPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                } else Modifier
            )
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

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isMe) {
                if (showAvatar) {
                    MessageAvatar(initial = avatarInitial, size = avatarSize, modifier = Modifier.align(Alignment.Bottom), avatarUrl = avatarUrl)
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

            // ── Stacking group: reply above, bubble below (overlapping) ──
            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                // ── Sender name for group chats ──
                if (senderName.isNotEmpty() && !isMe && showAvatar) {
                    val senderColors = listOf(
                        Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF22C55E),
                        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFEC4899),
                        Color(0xFF14B8A6), Color(0xFFF59E0B)
                    )
                    val nameColor = senderColors[senderName.hashCode().and(0x7FFFFFFF) % senderColors.size]
                    Text(
                        text = senderName,
                        color = nameColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                    )
                }

                // ── Forwarded header ──
                val forwardedFrom = message?.forwardedFrom
                if (forwardedFrom != null && !isRecalled) {
                    val mediaLabel = when (messageType) {
                        Constants.MESSAGE_TYPE_IMAGE -> "hình ảnh"
                        Constants.MESSAGE_TYPE_VIDEO -> "video"
                        Constants.MESSAGE_TYPE_VOICE -> "tin nhắn thoại"
                        Constants.MESSAGE_TYPE_FILE -> "tệp tin"
                        else -> "tin nhắn"
                    }
                    val headerText = if (isMe) "Bạn đã chuyển tiếp một $mediaLabel"
                    else "$forwardedFrom đã chuyển tiếp một $mediaLabel"
                    Text(
                        text = headerText,
                        color = nc.textSecondary,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // ── Reply section (rendered ABOVE the bubble) ──
                val replyTo = message?.replyTo
                if (replyTo != null && !isRecalled) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .padding(horizontal = if (messageType == Constants.MESSAGE_TYPE_IMAGE) 4.dp else 0.dp),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Reply,
                                contentDescription = null,
                                tint = nc.textTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            val headerText = if (isOriginalRecalled) {
                                "Bạn đã trả lời một tin nhắn bị gỡ"
                            } else if (replyTo.senderId == currentUserId) {
                                "Bạn đã trả lời chính mình"
                            } else {
                                "Bạn đã trả lời ${replyTo.senderName}"
                            }
                            Text(
                                text = headerText,
                                color = nc.textTertiary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val previewText = if (isOriginalRecalled) {
                            "Tin nhắn đã bị thu hồi"
                        } else when (replyTo.type) {
                            Constants.MESSAGE_TYPE_IMAGE -> "📷 Hình ảnh"
                            Constants.MESSAGE_TYPE_VIDEO -> "🎬 Video"
                            Constants.MESSAGE_TYPE_VOICE -> "🎤 Tin nhắn thoại"
                            Constants.MESSAGE_TYPE_FILE -> "📎 Tệp"
                            Constants.MESSAGE_TYPE_CONTACT -> "👤 Liên hệ"
                            Constants.MESSAGE_TYPE_STORY_REPLY -> "📸 Trả lời tin"
                            Constants.MESSAGE_TYPE_NOTE_REPLY -> "📝 Phản hồi ghi chú"
                            else -> replyTo.text
                        }
                        Box(
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .clickable(enabled = !isOriginalRecalled && onQuoteClick != null && replyTo.messageId.isNotEmpty()) {
                                    onQuoteClick?.invoke(replyTo.messageId)
                                }
                                .background(nc.surfaceVariant, RoundedCornerShape(18.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = previewText,
                                color = nc.textSecondary,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ── Main bubble (zIndex overlaps the reply quote) ──
                val hasReply = message?.replyTo != null && !isRecalled
                val reactions = message?.reactions ?: emptyMap()
                Box(modifier = if (hasReply) Modifier.offset(y = (-8).dp).zIndex(1f) else Modifier) {
                    if (isRecalled) {
                        // ── Recalled bubble: frozen, border-only, italic ──
                        Box(
                            modifier = Modifier
                                .background(nc.surfaceVariant.copy(alpha = 0.4f), bubbleShape)
                                .border(1.dp, nc.divider, bubbleShape)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isMe) "Bạn đã thu hồi một tin nhắn" else "${message?.senderName ?: "Đối phương"} đã thu hồi một tin nhắn",
                                color = nc.textTertiary,
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    } else if (messageType == Constants.MESSAGE_TYPE_IMAGE && !isRecalled) {
                        // ── Image bubble: overlay reaction badge + forward button ──
                        @Composable
                        fun ForwardBtn() {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(nc.cardBg)
                                    .clickable { onForward?.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "Chuyển tiếp",
                                    tint = nc.iconTint,
                                    modifier = Modifier.size(18.dp).graphicsLayer(scaleX = -1f)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isMe) ForwardBtn()
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 210.dp)
                                    .then(if (reactions.isNotEmpty()) Modifier.padding(bottom = 12.dp) else Modifier)
                            ) {
                                // Layer 1: main bubble
                                Box(
                                    modifier = Modifier
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {},
                                            onLongClick = onLongClick
                                        )
                                        .clip(bubbleShape)
                                        .then(if (isMe) Modifier.background(sentBubbleBrush, bubbleShape) else Modifier.background(nc.receivedBubble, bubbleShape))
                                ) {
                                    AsyncImage(
                                        model = text,
                                        contentDescription = "Hình ảnh",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth().clip(bubbleShape)
                                    )
                                }
                                // Layer 2: reaction badge overlay
                                if (reactions.isNotEmpty()) {
                                    val displayEmoji = reactions.values.groupBy { e: String -> e }.maxByOrNull { entry -> entry.value.size }?.key ?: reactions.values.first()
                                    val count = reactions.size
                                    Box(modifier = Modifier.matchParentSize()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = 2.dp, y = 10.dp)
                                                .clickable { onReactionsClick?.invoke(reactions, message?.id ?: "") }
                                                .background(nc.background, RoundedCornerShape(10.dp))
                                                .border(1.dp, nc.divider, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(displayEmoji, fontSize = 14.sp)
                                                if (count > 1) {
                                                    Text(text = count.toString(), color = nc.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 1.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!isMe) ForwardBtn()
                        }
                    } else if (messageType == Constants.MESSAGE_TYPE_VIDEO && !isRecalled) {
                        // ── Video bubble: thumbnail + inline playback + forward button ──
                        val context = LocalContext.current
                        val reactions = message?.reactions ?: emptyMap()
                        val videoDurationSec = message?.duration ?: 0L
                        val durationText = if (videoDurationSec > 0) {
                            val mins = videoDurationSec / 60
                            val secs = videoDurationSec % 60
                            "%d:%02d".format(mins, secs)
                        } else ""

                        var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
                        var isInlinePlaying by remember { mutableStateOf(false) }

                        // Extract first frame as thumbnail
                        LaunchedEffect(text) {
                            try {
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(text, HashMap())
                                thumbnailBitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                retriever.release()
                            } catch (_: Exception) {}
                        }

                        // ExoPlayer for inline playback
                        val inlineExoPlayer = remember(text) {
                            ExoPlayer.Builder(context).build().apply {
                                setMediaItem(MediaItem.fromUri(text))
                                // Defer prepare() until user actually plays to prevent memory leaks
                            }
                        }
                        DisposableEffect(text) {
                            val listener = object : androidx.media3.common.Player.Listener {
                                override fun onPlaybackStateChanged(playbackState: Int) {
                                    if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                        isInlinePlaying = false
                                    }
                                }
                            }
                            inlineExoPlayer.addListener(listener)
                            onDispose {
                                inlineExoPlayer.removeListener(listener)
                                try { inlineExoPlayer.release() } catch (_: Exception) {}
                            }
                        }

                        @Composable
                        fun ForwardBtn() {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A2A2A))
                                    .clickable { onForward?.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "Chuyển tiếp",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp).graphicsLayer(scaleX = -1f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isMe) ForwardBtn()
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 210.dp)
                                    .then(if (reactions.isNotEmpty()) Modifier.padding(bottom = 12.dp) else Modifier)
                            ) {
                                // Layer 1: main bubble
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 210.dp)
                                        .heightIn(max = 280.dp)
                                        .wrapContentSize()
                                        .clip(bubbleShape)
                                        .background(color = nc.cardBg, shape = bubbleShape)
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                if (!isInlinePlaying) onVideoClick?.invoke()
                                            },
                                            onLongClick = onLongClick
                                        )
                                ) {
                                    if (isInlinePlaying) {
                                        // ── Inline playback — click to go full-screen ──
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 210.dp)
                                                .heightIn(max = 280.dp)
                                                .clip(bubbleShape)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { onVideoClick?.invoke() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AndroidView(
                                                factory = { ctx ->
                                                    PlayerView(ctx).apply {
                                                        player = inlineExoPlayer
                                                        useController = false
                                                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(bubbleShape)
                                            )
                                            LaunchedEffect(Unit) {
                                                inlineExoPlayer.prepare()
                                                inlineExoPlayer.play()
                                            }
                                        }
                                    } else {
                                        // ── Thumbnail view ──
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 210.dp)
                                                .heightIn(max = 280.dp)
                                                .clip(bubbleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val bmp = thumbnailBitmap
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Video thumbnail",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .widthIn(max = 210.dp)
                                                        .heightIn(max = 280.dp)
                                                        .clip(bubbleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(nc.cardBg)
                                                )
                                            }
                                            // Center play button — triggers inline playback
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.85f))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { isInlinePlaying = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Phát video",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                            // Duration badge bottom-right
                                            if (durationText.isNotEmpty()) {
                                                Text(
                                                    text = durationText,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(8.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                // Layer 2: reaction badge overlay
                                if (reactions.isNotEmpty()) {
                                    val displayEmoji = reactions.values.groupBy { e: String -> e }.maxByOrNull { entry -> entry.value.size }?.key ?: reactions.values.first()
                                    val count = reactions.size
                                    Box(modifier = Modifier.matchParentSize()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = 2.dp, y = 10.dp)
                                                .clickable { onReactionsClick?.invoke(reactions, message?.id ?: "") }
                                                .background(nc.background, RoundedCornerShape(10.dp))
                                                .border(1.dp, nc.divider, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(displayEmoji, fontSize = 14.sp)
                                                if (count > 1) {
                                                    Text(text = count.toString(), color = nc.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 1.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!isMe) ForwardBtn()
                        }
                    } else if (messageType == Constants.MESSAGE_TYPE_VOICE && !isRecalled) {
                        // ── Voice bubble: overlay reaction badge ──
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

                        @Composable
                        fun ForwardBtn() {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A2A2A))
                                    .clickable { onForward?.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "Chuyển tiếp",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp).graphicsLayer(scaleX = -1f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isMe) ForwardBtn()
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .then(if (reactions.isNotEmpty()) Modifier.padding(bottom = 12.dp) else Modifier)
                            ) {
                                // Layer 1: main bubble
                                Box(
                                    modifier = Modifier
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {},
                                            onLongClick = onLongClick
                                        )
                                        .then(if (isMe) Modifier.background(sentBubbleBrush, bubbleShape) else Modifier.background(nc.receivedBubble, bubbleShape))
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isMe) nc.sentBubbleText.copy(alpha = 0.15f) else nc.receivedBubbleText.copy(alpha = 0.15f))
                                                    .clickable {
                                                        if (isPrepared) {
                                                            if (voiceIsPlaying) { voicePlayer.pause(); voiceIsPlaying = false }
                                                            else { voicePlayer.start(); voiceIsPlaying = true }
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

                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .pointerInput(Unit) {
                                                        detectDragGestures { change, _ ->
                                                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                                            if (isPrepared) { voicePlayer.seekTo((fraction * voiceDurationMs).toInt()); voiceProgress = fraction }
                                                        }
                                                    }
                                                    .pointerInput(Unit) {
                                                        detectTapGestures { offset ->
                                                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                                            if (isPrepared) { voicePlayer.seekTo((fraction * voiceDurationMs).toInt()); voiceProgress = fraction }
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

                                        Text(
                                            text = if (voiceIsPlaying) "$positionText / $durationText" else durationText,
                                            color = if (isMe) nc.sentBubbleText.copy(alpha = 0.7f) else nc.receivedBubbleText.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                // Layer 2: reaction badge overlay
                                if (reactions.isNotEmpty()) {
                                    val displayEmoji = reactions.values.groupBy { e: String -> e }.maxByOrNull { entry -> entry.value.size }?.key ?: reactions.values.first()
                                    val count = reactions.size
                                    Box(modifier = Modifier.matchParentSize()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = 2.dp, y = 10.dp)
                                                .clickable { onReactionsClick?.invoke(reactions, message?.id ?: "") }
                                                .background(nc.background, RoundedCornerShape(10.dp))
                                                .border(1.dp, nc.divider, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(displayEmoji, fontSize = 14.sp)
                                                if (count > 1) {
                                                    Text(text = count.toString(), color = nc.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 1.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!isMe) ForwardBtn()
                        }
                    } else if (messageType == Constants.MESSAGE_TYPE_FILE && !isRecalled) {
                        // ── File bubble: overlay reaction badge + forward button ──
                        val context = LocalContext.current
                        val fileUrl = text
                        val fileName = message?.fileName ?: "File"
                        val fileSize = message?.fileSize ?: 0L

                        @Composable
                        fun ForwardBtn() {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A2A2A))
                                    .clickable { onForward?.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "Chuyển tiếp",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp).graphicsLayer(scaleX = -1f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isMe) ForwardBtn()
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 220.dp)
                                    .then(if (reactions.isNotEmpty()) Modifier.padding(bottom = 12.dp) else Modifier)
                            ) {
                                // Layer 1: main bubble
                                Box(
                                    modifier = Modifier
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
                                        .then(if (isMe) Modifier.background(sentBubbleBrush, bubbleShape) else Modifier.background(nc.receivedBubble, bubbleShape))
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
                                            Text(text = fileName, color = if (isMe) nc.sentBubbleText else nc.receivedBubbleText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            if (fileSize > 0) {
                                                Text(text = fileSize.toReadableFileSize(), color = if (isMe) nc.sentBubbleText.copy(alpha = 0.6f) else nc.receivedBubbleText.copy(alpha = 0.6f), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                                // Layer 2: reaction badge overlay
                                if (reactions.isNotEmpty()) {
                                    val displayEmoji = reactions.values.groupBy { e: String -> e }.maxByOrNull { entry -> entry.value.size }?.key ?: reactions.values.first()
                                    val count = reactions.size
                                    Box(modifier = Modifier.matchParentSize()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = 2.dp, y = 10.dp)
                                                .clickable { onReactionsClick?.invoke(reactions, message?.id ?: "") }
                                                .background(nc.background, RoundedCornerShape(10.dp))
                                                .border(1.dp, nc.divider, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(displayEmoji, fontSize = 14.sp)
                                                if (count > 1) {
                                                    Text(text = count.toString(), color = nc.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 1.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!isMe) ForwardBtn()
                        }
                    } else if (messageType == Constants.MESSAGE_TYPE_CONTACT && !isRecalled) {
                        // ── Contact bubble ──
                        val contactUserId = message?.contactUserId ?: ""
                        val contactName = message?.contactName ?: ""
                        val contactPhone = message?.contactPhone ?: ""
                        val contactAvatarUrl = message?.contactAvatarUrl ?: ""

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isMe) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2A2A2A))
                                        .clickable { onForward?.invoke() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = "Chuyển tiếp",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp).graphicsLayer(scaleX = -1f)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .then(if (reactions.isNotEmpty()) Modifier.padding(bottom = 12.dp) else Modifier)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { if (contactUserId.isNotEmpty()) onContactClick?.invoke(contactUserId) },
                                            onLongClick = onLongClick
                                        )
                                        .then(if (isMe) Modifier.background(sentBubbleBrush, bubbleShape) else Modifier.background(nc.receivedBubble, bubbleShape))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(nc.avatarBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (contactAvatarUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = contactAvatarUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = contactName,
                                                color = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (contactPhone.isNotEmpty()) {
                                                Text(
                                                    text = contactPhone,
                                                    color = if (isMe) nc.sentBubbleText.copy(alpha = 0.7f) else nc.receivedBubbleText.copy(alpha = 0.7f),
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Text(
                                                text = "Liên hệ",
                                                color = if (isMe) nc.sentBubbleText.copy(alpha = 0.5f) else nc.receivedBubbleText.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                if (reactions.isNotEmpty()) {
                                    val displayEmoji = reactions.values.groupBy { e: String -> e }.maxByOrNull { entry -> entry.value.size }?.key ?: reactions.values.first()
                                    val count = reactions.size
                                    Box(modifier = Modifier.matchParentSize()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = 2.dp, y = 10.dp)
                                                .clickable { onReactionsClick?.invoke(reactions, message?.id ?: "") }
                                                .background(nc.background, RoundedCornerShape(10.dp))
                                                .border(1.dp, nc.divider, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(displayEmoji, fontSize = 14.sp)
                                                if (count > 1) {
                                                    Text(text = count.toString(), color = nc.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 1.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!isMe) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2A2A2A))
                                        .clickable { onForward?.invoke() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = "Chuyển tiếp",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp).graphicsLayer(scaleX = -1f)
                                    )
                                }
                            }
                        }
                    } else if ((messageType == Constants.MESSAGE_TYPE_STORY_REPLY || messageType == Constants.MESSAGE_TYPE_NOTE_REPLY) && !isRecalled) {
                        StoryReplyBubble(
                            message = message,
                            text = text,
                            isMe = isMe,
                            sentBubbleBrush = sentBubbleBrush,
                            bubbleShape = bubbleShape,
                            nc = nc,
                            reactions = reactions,
                            onLongClick = onLongClick,
                            onForward = onForward,
                            onReactionsClick = onReactionsClick,
                            onStoryReplyClick = onStoryReplyClick
                        )
                    } else {
                        // ── Text bubble: overlay reaction badge ──
                        val isUrl = remember(text) { Patterns.WEB_URL.matcher(text.trim()).matches() }
                        @Composable
                        fun ForwardBtn() {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A2A2A))
                                    .clickable { onForward?.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "Chuyển tiếp",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp).graphicsLayer(scaleX = -1f)
                                )
                            }
                        }
                        if (isUrl) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isMe) ForwardBtn()
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .then(if (reactions.isNotEmpty()) Modifier.padding(bottom = 12.dp) else Modifier)
                                ) {
                                    // Layer 1: main bubble
                                    Box(
                                        modifier = Modifier
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {},
                                                onLongClick = onLongClick
                                            )
                                            .then(if (isMe) Modifier.background(sentBubbleBrush, bubbleShape) else Modifier.background(nc.receivedBubble, bubbleShape))
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = text,
                                            color = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp
                                        )
                                    }
                                    // Layer 2: reaction badge overlay
                                    if (reactions.isNotEmpty()) {
                                        val displayEmoji = reactions.values.groupBy { e: String -> e }.maxByOrNull { entry -> entry.value.size }?.key ?: reactions.values.first()
                                        val count = reactions.size
                                        Box(modifier = Modifier.matchParentSize()) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .offset(x = 2.dp, y = 10.dp)
                                                    .clickable { onReactionsClick?.invoke(reactions, message?.id ?: "") }
                                                    .background(nc.background, RoundedCornerShape(10.dp))
                                                    .border(1.dp, nc.divider, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(displayEmoji, fontSize = 14.sp)
                                                    if (count > 1) {
                                                        Text(text = count.toString(), color = nc.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 1.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!isMe) ForwardBtn()
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .then(if (reactions.isNotEmpty()) Modifier.padding(bottom = 12.dp) else Modifier)
                            ) {
                                // Layer 1: main bubble
                                Box(
                                    modifier = Modifier
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {},
                                            onLongClick = onLongClick
                                        )
                                        .then(if (isMe) Modifier.background(sentBubbleBrush, bubbleShape) else Modifier.background(nc.receivedBubble, bubbleShape))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = text,
                                        color = if (isMe) nc.sentBubbleText else nc.receivedBubbleText,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                                // Layer 2: reaction badge overlay
                                if (reactions.isNotEmpty()) {
                                    val displayEmoji = reactions.values.groupBy { e: String -> e }.maxByOrNull { entry -> entry.value.size }?.key ?: reactions.values.first()
                                    val count = reactions.size
                                    Box(modifier = Modifier.matchParentSize()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = 2.dp, y = 10.dp)
                                                .clickable { onReactionsClick?.invoke(reactions, message?.id ?: "") }
                                                .background(nc.background, RoundedCornerShape(10.dp))
                                                .border(1.dp, nc.divider, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(displayEmoji, fontSize = 14.sp)
                                                if (count > 1) {
                                                    Text(text = count.toString(), color = nc.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 1.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isMe) Spacer(modifier = Modifier.width(4.dp))
        }

        val timeStartPadding = if (!isMe) avatarSize.dp + 6.dp else 0.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = timeStartPadding, end = if (isMe) 4.dp else 0.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = time, color = nc.textTertiary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
            if (isMe && isSending) {
                Text(
                    text = "Đang gửi...",
                    color = nc.textTertiary,
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else if (isMe && status.isNotEmpty()) {
                Text(
                    text = when(status) { "seen" -> "Đã xem"; "delivered" -> "Đã nhận"; "recalled" -> ""; else -> "Đã gửi" },
                    color = if (status == "seen") sentBubbleColor else nc.textTertiary,
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
    avatarUrl: String? = null,
    sentBubbleColor: Color = MaterialTheme.nexusColors.sentBubble,
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
                    MessageAvatar(initial = avatarInitial, size = avatarSize, avatarUrl = avatarUrl)
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FullScreenVideoPlayer(videoUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val nc = MaterialTheme.nexusColors

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }

    DisposableEffect(videoUrl) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    totalDuration = exoPlayer.duration
                }
            }
        }
        exoPlayer.addListener(listener)
        val progressScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val job = progressScope.launch {
            while (true) {
                delay(500)
                if (exoPlayer.isPlaying) {
                    currentPosition = exoPlayer.currentPosition
                    totalDuration = exoPlayer.duration
                }
            }
        }
        onDispose {
            job.cancel()
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    Dialog(
        onDismissRequest = {
            exoPlayer.pause()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { controlsVisible = !controlsVisible }
        ) {
            // Video player — centered, fit aspect ratio, rounded corners
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            // Controls overlay
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    exoPlayer.pause()
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.LibraryAdd, contentDescription = "Lưu", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    try {
                                        val downloadManager = context.getSystemService(DownloadManager::class.java)
                                        val request = DownloadManager.Request(Uri.parse(videoUrl))
                                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "NEXUS_video.mp4")
                                        downloadManager.enqueue(request)
                                    } catch (_: Exception) {}
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Tải xuống", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Thêm", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }

                    // Center play/pause (when paused)
                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                                .clickable { exoPlayer.play() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Phát",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Bottom bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Slider(
                            value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
                            onValueChange = { fraction ->
                                val newPos = (fraction * totalDuration).toLong()
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        val remainingMs = (totalDuration - currentPosition).coerceAtLeast(0)
                        val remSecs = remainingMs / 1000
                        val remMins = remSecs / 60
                        val remSecsDisplay = remSecs % 60
                        Text(
                            text = "-%d:%02d".format(remMins, remSecsDisplay),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardMessageBottomSheet(
    message: Message,
    viewModel: ChatViewModel?,
    onDismiss: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val chatsState = viewModel?.chatsState?.collectAsState()?.value ?: Resource.Idle
    val sentChats = remember { mutableStateListOf<String>() }
    var messageText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = nc.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = nc.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Gửi đến",
                    color = nc.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "TẠO NHÓM",
                    color = NexusPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // ── Preview Section ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thumbnail
                val isMedia = message.type == Constants.MESSAGE_TYPE_IMAGE ||
                    message.type == Constants.MESSAGE_TYPE_VIDEO
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(nc.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMedia) {
                        AsyncImage(
                            model = message.text,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    } else if (message.type == Constants.MESSAGE_TYPE_FILE) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = nc.textSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.fileName.ifEmpty { "File" },
                                color = nc.textSecondary,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = message.text.take(80),
                            color = nc.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Message input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(nc.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (messageText.isEmpty()) {
                        Text("Soạn tin nhắn...", color = nc.textTertiary, fontSize = 15.sp)
                    }
                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = nc.textPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(NexusPrimary)
                    )
                }
            }

            // ── Search Bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A2A2A))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = nc.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text("Tìm kiếm", color = nc.textTertiary, fontSize = 15.sp)
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = nc.textPrimary, fontSize = 15.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(NexusPrimary)
                        )
                    }
                }
            }

            // ── Contact List ──
            when (chatsState) {
                is Resource.Success -> {
                    val chats = chatsState.data.filter { chat ->
                        if (searchQuery.isBlank()) true
                        else chat.groupName.contains(searchQuery, ignoreCase = true) ||
                            chat.participants.any { it.contains(searchQuery, ignoreCase = true) }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(chats.size) { index ->
                            val chat = chats[index]
                            var displayName by remember { mutableStateOf(chat.groupName.ifEmpty { "..." }) }
                            LaunchedEffect(chat.id) {
                                displayName = viewModel?.resolveDisplayName(chat) ?: chat.groupName
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(nc.avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val avatarUrl = viewModel?.resolveAvatarUrl(chat)
                                    if (!avatarUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                                        Text(
                                            text = initial,
                                            color = nc.textPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                // Name
                                Text(
                                    text = displayName,
                                    color = nc.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Send / Sent button
                                val isSent = chat.id in sentChats
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSent) nc.surfaceVariant
                                            else NexusPrimary
                                        )
                                        .clickable(enabled = !isSent) {
                                            viewModel?.let { vm ->
                                                scope.launch {
                                                    val result = vm.forwardMessage(chat.id, message)
                                                    if (result is Resource.Success) {
                                                        sentChats.add(chat.id)
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSent) "ĐÃ GỬI" else "GỬI",
                                        color = if (isSent) nc.textTertiary else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NexusPrimary)
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Không có cuộc trò chuyện", color = nc.textSecondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageAvatar(initial: String, size: Int = 28, modifier: Modifier = Modifier, avatarUrl: String? = null) {
    val nc = MaterialTheme.nexusColors
    val safeInitial = initial.ifBlank { "?" }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(nc.avatarBg),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = safeInitial,
                color = nc.textPrimary,
                fontSize = (size / 2.2f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    var dotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            dotCount = (dotCount + 1) % 4
        }
    }
    val dots = ".".repeat(dotCount)
    Text(
        text = "Đang nhập$dots",
        color = Color(0xFF22C55E),
        fontSize = 12.sp
    )
}

@Composable
fun UploadProgressBubble(
    imageUri: Uri,
    isMe: Boolean,
    sentBubbleColor: Color = MaterialTheme.nexusColors.sentBubble
) {
    val nc = MaterialTheme.nexusColors
    val context = LocalContext.current
    val mimeType = context.contentResolver.getType(imageUri) ?: ""
    val isVideo = mimeType.startsWith("video")

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
                        color = if (isMe) sentBubbleColor else nc.receivedBubble,
                        shape = bubbleShape
                    )
            ) {
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(bubbleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1A1A1A))
                        )
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                } else {
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
fun SystemMessageBubble(
    text: String,
    time: String,
    showDateSeparator: Boolean = false,
    dateSeparatorText: String = ""
) {
    val nc = MaterialTheme.nexusColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showDateSeparator && dateSeparatorText.isNotEmpty()) {
            Text(
                text = dateSeparatorText,
                color = nc.textTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = time,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PinnedMessageBar(
    pinnedMessage: PinnedMessage,
    onClick: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nc = MaterialTheme.nexusColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = nc.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pinnedMessage.senderName,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = pinnedMessage.text,
                    color = nc.textPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onUnpin, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Bỏ ghim",
                    tint = nc.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: com.example.nexus.feature_chat.viewmodel.GroupViewModel? = null,
    onNavigateBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val context = LocalContext.current
    val friendsState = viewModel?.friendsList?.collectAsState()?.value ?: Resource.Idle
    val selectedMembers = viewModel?.selectedMembers?.collectAsState()?.value ?: emptySet()
    val groupName = viewModel?.groupName?.collectAsState()?.value ?: ""
    val createState = viewModel?.createGroupState?.collectAsState()?.value ?: Resource.Idle
    val avatarUri = viewModel?.groupAvatarUri?.collectAsState()?.value

    LaunchedEffect(Unit) {
        viewModel?.loadFriends()
    }

    LaunchedEffect(createState) {
        if (createState is Resource.Success) {
            onGroupCreated(createState.data)
            viewModel?.clearCreateState()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel?.setGroupAvatarUri(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Tạo nhóm", color = nc.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                    }
                },
                actions = {
                    val canCreate = groupName.isNotBlank() && selectedMembers.isNotEmpty()
                    TextButton(
                        onClick = { viewModel?.createGroup(context) },
                        enabled = canCreate && createState !is Resource.Loading
                    ) {
                        if (createState is Resource.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = NexusPrimary
                            )
                        } else {
                            Text(
                                "Tạo",
                                color = if (canCreate) NexusPrimary else nc.textTertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = nc.background,
                    titleContentColor = nc.textPrimary
                )
            )
        },
        containerColor = nc.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Group Avatar + Name ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(nc.avatarBg)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Chọn ảnh",
                            tint = nc.textSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { viewModel?.setGroupName(it) },
                    placeholder = {
                        Text("Nhập tên nhóm", color = nc.textTertiary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = nc.outline,
                        focusedTextColor = nc.textPrimary,
                        unfocusedTextColor = nc.textPrimary,
                        cursorColor = NexusPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
            }

            // ── Selected count ──
            if (selectedMembers.isNotEmpty()) {
                Text(
                    text = "Đã chọn ${selectedMembers.size} người",
                    color = NexusPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(nc.divider))

            // ── Friends List ──
            when (friendsState) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NexusPrimary, strokeWidth = 2.dp)
                    }
                }
                is Resource.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(friendsState.data.size) { index ->
                            val friend = friendsState.data[index]
                            val isSelected = selectedMembers.contains(friend.uid)
                            val friendName = friend.displayName.ifEmpty { friend.username }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel?.toggleMember(friend.uid) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
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
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = friendName,
                                    color = nc.textPrimary,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel?.toggleMember(friend.uid) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = NexusPrimary,
                                        uncheckedColor = nc.outline
                                    )
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (friendsState as Resource.Error).message,
                            color = nc.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {}
            }

            // ── Error toast ──
            if (createState is Resource.Error) {
                LaunchedEffect(createState) {
                    Toast.makeText(context, (createState as Resource.Error).message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
