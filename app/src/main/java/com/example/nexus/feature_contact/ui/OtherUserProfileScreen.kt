package com.example.nexus.feature_contact.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonAddDisabled
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.Resource
import com.example.nexus.data.model.User
import com.example.nexus.feature_contact.viewmodel.OtherUserProfileViewModel
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.NexusSecondary
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    viewModel: OtherUserProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val user by viewModel.user.collectAsState()
    val relationship by viewModel.relationship.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val nc = MaterialTheme.nexusColors

    var showMenu by remember { mutableStateOf(false) }
    var showUnfriendSheet by remember { mutableStateOf(false) }

    // Navigate to chat
    LaunchedEffect(Unit) {
        viewModel.navigateToChat.collect { chatId ->
            onNavigateToChat(chatId)
        }
    }

    // Reset action state after handling
    LaunchedEffect(actionState) {
        if (actionState is Resource.Success) {
            viewModel.resetActionState()
        }
    }

    // Unfriend bottom sheet
    if (showUnfriendSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUnfriendSheet = false },
            containerColor = nc.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    "Hủy kết bạn",
                    color = nc.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Bạn có chắc muốn hủy kết bạn với ${user?.displayName?.ifEmpty { user?.username }}? Bạn sẽ không thể nhắn tin cho người này nữa.",
                    color = nc.textSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showUnfriendSheet = false }) {
                        Text("Hủy", color = nc.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        showUnfriendSheet = false
                        viewModel.unfriend()
                    }) {
                        Text("Hủy kết bạn", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ", color = nc.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = nc.textPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = nc.surface
                        ) {
                            if (relationship == Constants.RELATION_BLOCKED) {
                                DropdownMenuItem(
                                    text = { Text("Bỏ chặn", color = NexusPrimary) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.unblockUser()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Security, contentDescription = null, tint = NexusPrimary)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Chặn người dùng", color = Color(0xFFEF4444)) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.blockUser()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Block, contentDescription = null, tint = Color(0xFFEF4444))
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = nc.background)
            )
        },
        containerColor = nc.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Avatar
            if (user?.avatarUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = user!!.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NexusPrimary, NexusSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user?.displayName?.firstOrNull()?.uppercaseChar()?.toString()
                            ?: user?.username?.firstOrNull()?.uppercaseChar()?.toString()
                            ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display name
            Text(
                user?.displayName?.ifEmpty { user?.username ?: "" } ?: "",
                color = nc.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Username
            if (user?.username?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "@${user!!.username}",
                    color = nc.textSecondary,
                    fontSize = 14.sp
                )
            }

            // Bio
            if (user?.bio?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    user!!.bio,
                    color = nc.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action buttons based on relationship
            when (relationship) {
                Constants.RELATION_BLOCKED -> {
                    // Blocked state — only show unblock
                    BlockedActions(
                        isLoading = actionState is Resource.Loading,
                        onUnblock = { viewModel.unblockUser() }
                    )
                }
                Constants.RELATION_FRIENDS -> {
                    FriendsActions(
                        isLoading = actionState is Resource.Loading,
                        onMessage = { viewModel.sendMessage() },
                        onUnfriendClick = { showUnfriendSheet = true }
                    )
                }
                Constants.RELATION_PENDING_SENT -> {
                    PendingSentActions(
                        isLoading = actionState is Resource.Loading,
                        onCancel = { viewModel.cancelFriendRequest() }
                    )
                }
                Constants.RELATION_PENDING_RECEIVED -> {
                    PendingReceivedActions(
                        isLoading = actionState is Resource.Loading,
                        onAccept = { viewModel.acceptFriendRequest() },
                        onReject = { viewModel.rejectFriendRequest() }
                    )
                }
                else -> {
                    NoneActions(
                        isLoading = actionState is Resource.Loading,
                        onSendRequest = { viewModel.sendFriendRequest() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NoneActions(isLoading: Boolean, onSendRequest: () -> Unit) {
    Button(
        onClick = onSendRequest,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gửi yêu cầu kết bạn", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PendingSentActions(isLoading: Boolean, onCancel: () -> Unit) {
    val nc = MaterialTheme.nexusColors
    OutlinedButton(
        onClick = onCancel,
        enabled = !isLoading,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = nc.textSecondary),
        border = ButtonDefaults.outlinedButtonBorder(enabled = !isLoading),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = NexusPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Outlined.PersonAddDisabled, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Thu hồi lời mời", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PendingReceivedActions(isLoading: Boolean, onAccept: () -> Unit, onReject: () -> Unit) {
    val nc = MaterialTheme.nexusColors
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        OutlinedButton(
            onClick = onReject,
            enabled = !isLoading,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = nc.textSecondary),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Từ chối", fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onAccept,
            enabled = !isLoading,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Chấp nhận", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FriendsActions(isLoading: Boolean, onMessage: () -> Unit, onUnfriendClick: () -> Unit) {
    val nc = MaterialTheme.nexusColors
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Button(
            onClick = onMessage,
            enabled = !isLoading,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Nhắn tin", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onUnfriendClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusPrimary),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = NexusPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bạn bè", fontWeight = FontWeight.SemiBold, color = NexusPrimary)
        }
    }
}

@Composable
private fun BlockedActions(isLoading: Boolean, onUnblock: () -> Unit) {
    Button(
        onClick = onUnblock,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Bỏ chặn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
