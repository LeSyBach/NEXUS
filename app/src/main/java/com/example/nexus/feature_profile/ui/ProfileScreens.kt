package com.example.nexus.feature_profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nexus.feature_profile.viewmodel.ProfileViewModel
import com.example.nexus.navigation.Screen
import com.example.nexus.ui.components.NexusBottomBar
import com.example.nexus.ui.theme.*

// ══════════════════════════════════════════════════════════
//  PROFILE SCREEN
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel? = null,
    onNavigateToEdit: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit,
    onNavigateToTab: (String) -> Unit = {}
) {
    val user by (viewModel?.user?.collectAsState() ?: remember { mutableStateOf(null) })
    val nc = MaterialTheme.nexusColors

    Scaffold(
        bottomBar = {
            NexusBottomBar(
                currentRoute = Screen.Profile.route,
                onNavigate = onNavigateToTab
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
            // ── Header gradient banner ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(NexusPrimary.copy(alpha = 0.6f), nc.background)
                        )
                    ),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = "HỒ SƠ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = NexusPrimary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
                )
            }

            // ── Avatar (centered, large) ──
            Box(
                modifier = Modifier
                    .offset(y = (-48).dp)
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = user?.avatarUrl
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(3.dp, nc.background, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NexusPrimary, NexusSecondary)))
                            .border(3.dp, nc.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = user?.displayName?.firstOrNull()
                            ?: user?.username?.firstOrNull()
                            ?: 'U'
                        Text(
                            initial.uppercaseChar().toString(),
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── User Info ──
            Column(
                modifier = Modifier
                    .offset(y = (-24).dp)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    user?.displayName?.ifEmpty { user?.username ?: "" } ?: "...",
                    color = nc.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    user?.email?.ifEmpty { "" } ?: "",
                    color = nc.textSecondary,
                    fontSize = 14.sp
                )

                if (!user?.bio.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        user?.bio ?: "",
                        color = NexusPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Edit Profile button
                Button(
                    onClick = onNavigateToEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexusPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chỉnh sửa hồ sơ", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Menu section ──
            Text(
                "TÙY CHỌN",
                color = nc.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
            )

            ProfileMenuItem(Icons.Outlined.Settings, "Cài đặt & Quyền riêng tư") { onNavigateToSettings() }
            ProfileMenuItem(Icons.Outlined.HelpOutline, "Trợ giúp & Hỗ trợ") {}
            ProfileMenuItem(Icons.Outlined.Info, "Giới thiệu về NEXUS") {}

            Spacer(modifier = Modifier.height(24.dp))

            // ── Logout button ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable {
                        viewModel?.logout()
                        onLogout()
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Đăng xuất", tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Đăng xuất",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    val nc = MaterialTheme.nexusColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = nc.iconTintSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = nc.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = nc.iconTintSecondary, modifier = Modifier.size(14.dp))
    }
}

// ══════════════════════════════════════════════════════════
//  EDIT PROFILE SCREEN
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val user by (viewModel?.user?.collectAsState() ?: remember { mutableStateOf(null) })
    val isLoading by (viewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) })
    val updateSuccess by (viewModel?.updateSuccess?.collectAsState() ?: remember { mutableStateOf(false) })
    val avatarError by (viewModel?.avatarUploadError?.collectAsState() ?: remember { mutableStateOf(null) })
    val nc = MaterialTheme.nexusColors

    var displayName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedAvatarUri = it
            viewModel?.setPendingAvatar(it)
        }
    }

    // Pre-fill when user data arrives
    LaunchedEffect(user) {
        user?.let {
            displayName = it.displayName.ifEmpty { it.username }
            phone = it.phone
            bio = it.bio
        }
    }

    // Navigate back after successful update
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            viewModel?.resetUpdateSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chỉnh sửa hồ sơ",
                        color = nc.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = nc.background)
            )
        },
        containerColor = nc.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── Avatar with camera overlay ──
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Avatar image
                    when {
                        selectedAvatarUri != null -> {
                            AsyncImage(
                                model = selectedAvatarUri,
                                contentDescription = "Avatar đã chọn",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(3.dp, NexusPrimary.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                        !user?.avatarUrl.isNullOrEmpty() -> {
                            AsyncImage(
                                model = user?.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(3.dp, nc.surface, CircleShape)
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(NexusPrimary, NexusSecondary))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                                    color = Color.White,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Camera icon overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NexusPrimary)
                            .border(2.dp, nc.background, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Đổi ảnh đại diện",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Avatar error message
                if (avatarError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        avatarError ?: "",
                        color = nc.errorText,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Display Name field
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Tên hiển thị", color = nc.textSecondary) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = NexusPrimary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = nc.textSecondary.copy(alpha = 0.3f),
                        focusedTextColor = nc.textPrimary,
                        unfocusedTextColor = nc.textPrimary,
                        cursorColor = NexusPrimary,
                        focusedContainerColor = nc.cardBg,
                        unfocusedContainerColor = nc.cardBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại", color = nc.textSecondary) },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = NexusPrimary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = nc.textSecondary.copy(alpha = 0.3f),
                        focusedTextColor = nc.textPrimary,
                        unfocusedTextColor = nc.textPrimary,
                        cursorColor = NexusPrimary,
                        focusedContainerColor = nc.cardBg,
                        unfocusedContainerColor = nc.cardBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bio field
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Giới thiệu bản thân", color = nc.textSecondary) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = NexusPrimary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = nc.textSecondary.copy(alpha = 0.3f),
                        focusedTextColor = nc.textPrimary,
                        unfocusedTextColor = nc.textPrimary,
                        cursorColor = NexusPrimary,
                        focusedContainerColor = nc.cardBg,
                        unfocusedContainerColor = nc.cardBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Save button
                Button(
                    onClick = { viewModel?.updateProfile(displayName, phone, bio) },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexusPrimary,
                        disabledContainerColor = nc.textSecondary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "LƯU THAY ĐỔI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
