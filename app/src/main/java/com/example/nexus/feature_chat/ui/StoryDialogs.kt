package com.example.nexus.feature_chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.nexus.data.model.User
import com.example.nexus.ui.theme.nexusColors

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
                        Brush.verticalGradient(
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
                            Brush.verticalGradient(
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
