package com.example.nexus.feature_chat.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.Patterns
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.toReadableFileSize
import com.example.nexus.data.model.Message
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun StoryReplyBubble(
    message: Message?,
    text: String,
    isMe: Boolean,
    sentBubbleBrush: Brush,
    bubbleShape: RoundedCornerShape,
    nc: com.example.nexus.ui.theme.NexusColors,
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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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
        Brush.linearGradient(
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

            // Stacking group: reply above, bubble below (overlapping)
            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                // Sender name for group chats
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

                // Forwarded header
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

                // Reply section (rendered ABOVE the bubble)
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
                            Constants.MESSAGE_TYPE_VIDEO -> "🎥 Video"
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

                // Main bubble (zIndex overlaps the reply quote)
                val hasReply = message?.replyTo != null && !isRecalled
                val reactions = message?.reactions ?: emptyMap()
                Box(modifier = if (hasReply) Modifier.offset(y = (-8).dp).zIndex(1f) else Modifier) {
                    if (isRecalled) {
                        // Recalled bubble: frozen, border-only, italic
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
                        // Image bubble
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
                        // Video bubble
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

                        LaunchedEffect(text) {
                            try {
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(text, HashMap())
                                thumbnailBitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                retriever.release()
                            } catch (_: Exception) {}
                        }

                        val inlineExoPlayer = remember(text) {
                            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                setMediaItem(androidx.media3.common.MediaItem.fromUri(text))
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
                                                    androidx.media3.ui.PlayerView(ctx).apply {
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
                        // Voice bubble
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
                        // File bubble
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
                                Box(
                                    modifier = Modifier
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fileUrl))
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
                        // Contact bubble
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
                        // Text bubble
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
