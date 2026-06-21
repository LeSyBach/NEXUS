package com.example.nexus.feature_contact.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Resource
import com.example.nexus.feature_contact.viewmodel.ContactViewModel
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

// ══════════════════════════════════════════════════════════
//  SEARCH USER SCREEN
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(
    viewModel: ContactViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    var searchQuery by remember { mutableStateOf("") }
    val searchState = viewModel?.searchResults?.collectAsState()?.value ?: Resource.Idle
    val friendsListState = viewModel?.friendsList?.collectAsState()?.value ?: Resource.Idle
    val sentIds = viewModel?.sentRequestIds?.collectAsState()?.value ?: emptySet()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val friendIds = remember(friendsListState) {
        if (friendsListState is Resource.Success) friendsListState.data.map { it.uid }.toSet()
        else emptySet()
    }

    LaunchedEffect(viewModel) {
        viewModel?.navigateToChatEvent?.collect { chatId ->
            onNavigateToChat(chatId)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel?.sendRequestResult?.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(nc.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                }
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel?.searchUsers(it)
                    },
                    placeholder = { Text("Tìm theo số điện thoại hoặc tên...", color = nc.textSecondary, fontSize = 14.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = nc.cardBg,
                        unfocusedContainerColor = nc.cardBg,
                        cursorColor = NexusPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
            }
        },
        containerColor = nc.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                searchState is Resource.Loading -> {
                    CircularProgressIndicator(color = NexusPrimary, modifier = Modifier.align(Alignment.Center))
                }
                searchState is Resource.Success && searchState.data.isEmpty() && searchQuery.isNotBlank() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Không tìm thấy người dùng nào", color = nc.textSecondary, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Thử tìm với tên hoặc số điện thoại khác", color = nc.textTertiary, fontSize = 13.sp)
                    }
                }
                searchState is Resource.Success && searchState.data.isNotEmpty() -> {
                    LazyColumn {
                        items(searchState.data) { user ->
                            val isFriend = user.uid in friendIds
                            val isSent = user.uid in sentIds

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToProfile(user.uid) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(NexusPrimary.copy(alpha = 0.4f), nc.cardBg))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (user.avatarUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = user.avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            color = nc.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        user.displayName.ifEmpty { user.username },
                                        color = nc.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    if (user.phone.isNotEmpty()) {
                                        Text(user.phone, color = nc.textSecondary, fontSize = 12.sp)
                                    }
                                }

                                if (isFriend) {
                                    Box(
                                        modifier = Modifier
                                            .background(NexusPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Bạn bè", color = NexusPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (!isSent) viewModel?.sendFriendRequest(user.uid)
                                        },
                                        enabled = !isSent,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NexusPrimary,
                                            disabledContainerColor = nc.surfaceElevated,
                                            contentColor = nc.background,
                                            disabledContentColor = nc.textSecondary
                                        )
                                    ) {
                                        Text(
                                            if (isSent) "Đã gửi" else "Kết bạn",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                searchQuery.isBlank() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NexusPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = NexusPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tìm kiếm bạn bè", color = nc.textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Nhập số điện thoại hoặc tên người dùng", color = nc.textSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
