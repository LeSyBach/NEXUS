package com.example.nexus.feature_chat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.DateUtils
import com.example.nexus.data.model.Message
import com.example.nexus.data.model.User
import com.example.nexus.feature_chat.viewmodel.SharedMediaType
import com.example.nexus.feature_chat.viewmodel.SharedMediaViewModel
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMediaScreen(
    viewModel: SharedMediaViewModel,
    initialTab: String = "media",
    onNavigateBack: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val context = LocalContext.current

    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val showImages by viewModel.showImages.collectAsState()
    val showVideos by viewModel.showVideos.collectAsState()
    val selectedSenderId by viewModel.selectedSenderId.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val error by viewModel.error.collectAsState()
    val screenType = viewModel.screenType

    // Fullscreen viewers
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUrl by remember { mutableStateOf<String?>(null) }

    // Sender bottom sheet
    val senderSheetState = rememberModalBottomSheetState()
    var showSenderSheet by remember { mutableStateOf(false) }

    val selectedSenderName = if (selectedSenderId != null) {
        participants.find { it.uid == selectedSenderId }?.let {
            it.displayName.ifEmpty { it.username }
        } ?: "Người chia sẻ"
    } else {
        "Tất cả"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(nc.background)
    ) {
        // ══════ TOP BAR ══════
        TopAppBar(
            title = {
                Column {
                    Text(
                        screenType.label,
                        color = nc.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (totalCount > 0) {
                        Text(
                            "$totalCount mục",
                            color = nc.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = nc.background)
        )

        // ══════ FILTER BAR ══════
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(nc.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image/Video toggle chips (only for MEDIA type)
                if (screenType == SharedMediaType.MEDIA) {
                    ToggleChip(
                        selected = showImages,
                        onClick = { viewModel.toggleImages() },
                        label = { Text("Ảnh", fontSize = 13.sp) }
                    )
                    ToggleChip(
                        selected = showVideos,
                        onClick = { viewModel.toggleVideos() },
                        label = { Text("Video", fontSize = 13.sp) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // "Người chia sẻ" button — always visible
                SenderFilterChip(
                    label = selectedSenderName,
                    isActive = selectedSenderId != null,
                    onClick = {
                        viewModel.prepareSenderSheet()
                        showSenderSheet = true
                    },
                    onClear = { viewModel.clearSenderFilter() }
                )
            }
        }

        // ══════ CONTENT ══════
        Box(modifier = Modifier.fillMaxSize()) {
            if (error != null && items.isEmpty()) {
                ErrorState(
                    message = error!!,
                    onRetry = { viewModel.loadItems(reset = true) }
                )
            } else if (!isLoading && items.isEmpty()) {
                EmptyState(type = screenType)
            } else {
                when (screenType) {
                    SharedMediaType.MEDIA -> MediaGrid(
                        items = items,
                        isLoading = isLoading,
                        hasMore = hasMore,
                        onLoadMore = { viewModel.loadMore() },
                        onImageClick = { url -> fullscreenImageUrl = url },
                        onVideoClick = { url -> fullScreenVideoUrl = url }
                    )
                    SharedMediaType.FILE -> FileList(
                        items = items,
                        participants = participants,
                        isLoading = isLoading,
                        hasMore = hasMore,
                        onLoadMore = { viewModel.loadMore() },
                        onOpenFile = { url ->
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "Không thể mở file", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SharedMediaType.LINK -> LinkList(
                        items = items,
                        participants = participants,
                        isLoading = isLoading,
                        hasMore = hasMore,
                        onLoadMore = { viewModel.loadMore() },
                        onOpenLink = { url ->
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "Không thể mở liên kết", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            if (isLoading && items.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = NexusPrimary
                )
            }
        }
    }

    // ══════ SENDER FILTER BOTTOM SHEET ══════
    if (showSenderSheet) {
        SenderFilterBottomSheet(
            participants = participants,
            tempSenderId = viewModel.tempSenderId.collectAsState().value,
            onSelect = { viewModel.setTempSender(it) },
            onClear = {
                viewModel.clearSenderFilter()
                showSenderSheet = false
            },
            onApply = {
                viewModel.applySenderFilter()
                showSenderSheet = false
            },
            onDismiss = { showSenderSheet = false },
            sheetState = senderSheetState
        )
    }

    // ══════ FULLSCREEN VIEWERS ══════
    fullscreenImageUrl?.let { url ->
        FullScreenImageViewer(imageUrl = url, onDismiss = { fullscreenImageUrl = null })
    }
    fullScreenVideoUrl?.let { url ->
        FullScreenVideoPlayer(videoUrl = url, onDismiss = { fullScreenVideoUrl = null })
    }
}

// ══════════════════════════════════════════════════════════════
// TOGGLE CHIP (Image / Video)
// ══════════════════════════════════════════════════════════════

@Composable
private fun ToggleChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    val nc = MaterialTheme.nexusColors
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (selected) Modifier.border(1.5.dp, NexusPrimary, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) NexusPrimary.copy(alpha = 0.1f) else nc.surfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            ProvideTextStyle(
                value = androidx.compose.ui.text.TextStyle(
                    color = if (selected) NexusPrimary else nc.textSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            ) {
                label()
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// SENDER FILTER CHIP (with clear button)
// ══════════════════════════════════════════════════════════════

@Composable
private fun SenderFilterChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) NexusPrimary.copy(alpha = 0.12f) else nc.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = if (isActive) NexusPrimary else nc.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                color = if (isActive) NexusPrimary else nc.textPrimary,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 90.dp)
            )
            if (isActive) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Xóa lọc",
                        tint = NexusPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = nc.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// SENDER FILTER BOTTOM SHEET (Messenger style)
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SenderFilterBottomSheet(
    participants: List<User>,
    tempSenderId: String?,
    onSelect: (String?) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    val nc = MaterialTheme.nexusColors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = nc.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(nc.textTertiary.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // ═══ Top bar: title + "Xóa" ═══
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Người chia sẻ",
                    color = nc.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClear) {
                    Text(
                        "Xóa",
                        color = NexusPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ═══ "Tất cả" option ═══
            SenderRow(
                name = "Tất cả",
                avatarUrl = "",
                initial = "*",
                isSelected = tempSenderId == null,
                onClick = { onSelect(null) }
            )

            // ═══ Participant list ═══
            participants.forEach { user ->
                val name = user.displayName.ifEmpty { user.username }
                SenderRow(
                    name = name,
                    avatarUrl = user.avatarUrl,
                    initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    isSelected = tempSenderId == user.uid,
                    onClick = { onSelect(user.uid) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══ "Lưu" button ═══
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    "Lưu",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SenderRow(
    name: String,
    avatarUrl: String,
    initial: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val nc = MaterialTheme.nexusColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(nc.surfaceVariant)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(nc.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial,
                    color = nc.textSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            name,
            color = nc.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Radio circle
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.background(NexusPrimary, CircleShape)
                    } else {
                        Modifier.border(2.dp, nc.textTertiary.copy(alpha = 0.5f), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// FULLSCREEN IMAGE VIEWER
// ══════════════════════════════════════════════════════════════

@Composable
private fun FullScreenImageViewer(imageUrl: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MEDIA GRID
// ══════════════════════════════════════════════════════════════

@Composable
private fun MediaGrid(
    items: List<Message>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState.layoutInfo) {
        val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= items.size - 6 && !isLoading && hasMore) {
            onLoadMore()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { message ->
            MediaGridItem(
                message = message,
                onClick = {
                    if (message.type == Constants.MESSAGE_TYPE_VIDEO) onVideoClick(message.text)
                    else onImageClick(message.text)
                }
            )
        }
        if (isLoading && items.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NexusPrimary, strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun MediaGridItem(message: Message, onClick: () -> Unit) {
    val nc = MaterialTheme.nexusColors
    val isVideo = message.type == Constants.MESSAGE_TYPE_VIDEO

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(nc.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = message.text,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isVideo) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayCircleFilled, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// FILE LIST
// ══════════════════════════════════════════════════════════════

@Composable
private fun FileList(
    items: List<Message>,
    participants: List<User>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenFile: (String) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val listState = rememberLazyListState()

    LaunchedEffect(listState.layoutInfo) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= items.size - 6 && !isLoading && hasMore) onLoadMore()
    }

    LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 4.dp), modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { message ->
            FileListItem(message = message, participants = participants, onClick = { onOpenFile(message.text) })
        }
        if (isLoading && items.isNotEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NexusPrimary, strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun FileListItem(message: Message, participants: List<User>, onClick: () -> Unit) {
    val nc = MaterialTheme.nexusColors
    val sender = participants.find { it.uid == message.senderId }
    val senderName = sender?.let { it.displayName.ifEmpty { it.username } } ?: message.senderName
    val timeText = message.timestamp?.let { DateUtils.formatMessageTime(it.toDate().time) } ?: ""

    Surface(modifier = Modifier.fillMaxWidth(), color = nc.background) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(NexusPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = NexusPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(message.fileName.ifEmpty { "Tệp" }, color = nc.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(senderName, color = nc.textSecondary, fontSize = 12.sp)
                    Text(" · $timeText", color = nc.textTertiary, fontSize = 12.sp)
                }
                if (message.fileSize > 0) {
                    Text(formatFileSize(message.fileSize), color = nc.textTertiary, fontSize = 11.sp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// LINK LIST
// ══════════════════════════════════════════════════════════════

@Composable
private fun LinkList(
    items: List<Message>,
    participants: List<User>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val listState = rememberLazyListState()

    LaunchedEffect(listState.layoutInfo) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= items.size - 6 && !isLoading && hasMore) onLoadMore()
    }

    LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 4.dp), modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { message ->
            LinkListItem(message = message, participants = participants, onClick = {
                val url = extractUrl(message.text)
                if (url != null) onOpenLink(url)
            })
        }
        if (isLoading && items.isNotEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NexusPrimary, strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun LinkListItem(message: Message, participants: List<User>, onClick: () -> Unit) {
    val nc = MaterialTheme.nexusColors
    val sender = participants.find { it.uid == message.senderId }
    val senderName = sender?.let { it.displayName.ifEmpty { it.username } } ?: message.senderName
    val timeText = message.timestamp?.let { DateUtils.formatMessageTime(it.toDate().time) } ?: ""
    val url = extractUrl(message.text) ?: message.text

    Surface(modifier = Modifier.fillMaxWidth(), color = nc.background) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(NexusPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = NexusPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(url, color = NexusPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(message.text, color = nc.textSecondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(senderName, color = nc.textTertiary, fontSize = 12.sp)
                    Text(" · $timeText", color = nc.textTertiary, fontSize = 12.sp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// EMPTY / ERROR STATE
// ══════════════════════════════════════════════════════════════

@Composable
private fun EmptyState(type: SharedMediaType) {
    val nc = MaterialTheme.nexusColors
    val (icon, message) = when (type) {
        SharedMediaType.MEDIA -> Icons.Default.Image to "Chưa có ảnh hoặc video nào"
        SharedMediaType.FILE -> Icons.Default.InsertDriveFile to "Chưa có tệp nào"
        SharedMediaType.LINK -> Icons.Default.Link to "Chưa có liên kết nào"
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = nc.textTertiary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = nc.textSecondary, fontSize = 15.sp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val nc = MaterialTheme.nexusColors
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = nc.textTertiary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Đã xảy ra lỗi", color = nc.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(message, color = nc.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary)) {
            Text("Thử lại", color = Color.White)
        }
    }
}

// ══════════════════════════════════════════════════════════════
// UTILS
// ══════════════════════════════════════════════════════════════

private fun extractUrl(text: String): String? {
    return Regex("""https?://\S+""").find(text)?.value
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
