package com.example.nexus.feature_chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.model.Message
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors
import kotlinx.coroutines.launch

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
            // Header
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

            // Preview Section
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

            // Search Bar
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

            // Contact List
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
                            var avatarUrl by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(chat.id) {
                                displayName = viewModel?.resolveDisplayName(chat) ?: chat.groupName
                                avatarUrl = viewModel?.resolveAvatarUrl(chat)
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
