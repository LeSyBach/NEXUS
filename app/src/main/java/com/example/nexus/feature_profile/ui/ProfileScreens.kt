package com.example.nexus.feature_profile.ui

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onLogout: () -> Unit,
    onNavigateToTab: (String) -> Unit = {}
) {
    val user by (viewModel?.user?.collectAsState() ?: remember { mutableStateOf(null) })

    Scaffold(
        bottomBar = {
            NexusBottomBar(
                currentRoute = Screen.Profile.route,
                onNavigate = onNavigateToTab
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header gradient banner ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF5A55FF).copy(alpha = 0.6f), DarkBackground)
                        )
                    ),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = "HỒ SƠ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        brush = Brush.linearGradient(colors = listOf(GradientStart, GradientEnd))
                    ),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
                )
            }

            // ── Avatar + Info Card ──
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-24).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkCard)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF5A55FF), Color(0xFF00E5FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = user?.displayName?.firstOrNull()
                            ?: user?.username?.firstOrNull()
                            ?: 'U'
                        Text(
                            initial.uppercaseChar().toString(),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            user?.displayName?.ifEmpty { user?.username ?: "" } ?: "...",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            user?.phone?.ifEmpty { "Chưa cập nhật SĐT" } ?: "...",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        if (!user?.bio.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(user?.bio ?: "", color = Color(0xFF00E5FF), fontSize = 13.sp)
                        }
                    }

                    IconButton(
                        onClick = onNavigateToEdit,
                        modifier = Modifier.background(DarkBackground, CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa hồ sơ", tint = NexusPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Menu section ──
            Text(
                "TÙY CHỌN",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
            )

            ProfileMenuItem(Icons.Outlined.Settings, "Cài đặt & Quyền riêng tư") {}
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(14.dp))
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

    var displayName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

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
        if (updateSuccess) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chỉnh sửa hồ sơ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
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

                // Avatar preview
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF5A55FF), Color(0xFF00E5FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Display Name field
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Tên hiển thị", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = NexusPrimary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NexusPrimary,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
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
                    label = { Text("Số điện thoại", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = NexusPrimary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NexusPrimary,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
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
                    label = { Text("Giới thiệu bản thân", color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = NexusPrimary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusPrimary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NexusPrimary,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
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
                        disabledContainerColor = Color.Gray
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
