package com.example.nexus.feature_chat.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nexus.core.utils.Resource
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

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
            // Group Avatar + Name
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

            // Selected count
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

            // Friends List
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

            // Error toast
            if (createState is Resource.Error) {
                LaunchedEffect(createState) {
                    Toast.makeText(context, (createState as Resource.Error).message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
