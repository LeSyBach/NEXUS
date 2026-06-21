package com.example.nexus.feature_chat.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.PinnedMessage
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors
import kotlinx.coroutines.delay

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
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF00FF87), Color(0xFF60EFFF))
                            ),
                            shape = CircleShape
                        ) else if (hasNote) Modifier.border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
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

@Composable
internal fun MessageAvatar(initial: String, size: Int = 28, modifier: Modifier = Modifier, avatarUrl: String? = null) {
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
