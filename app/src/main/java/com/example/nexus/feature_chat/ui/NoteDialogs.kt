package com.example.nexus.feature_chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nexus.data.model.User
import com.example.nexus.ui.theme.nexusColors

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
