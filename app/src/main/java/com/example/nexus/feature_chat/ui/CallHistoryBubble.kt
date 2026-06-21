package com.example.nexus.feature_chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.data.model.Message
import com.example.nexus.ui.theme.nexusColors

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
