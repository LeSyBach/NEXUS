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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SettingsSystemDaydream
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
import com.example.nexus.core.utils.Resource
import com.example.nexus.core.utils.SavedAccount
import com.example.nexus.feature_profile.viewmodel.SettingsViewModel
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.NexusSecondary
import com.example.nexus.ui.theme.nexusColors
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToAddAccount: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSwitchAccount: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val switchState by viewModel.switchAccountState.collectAsState()
    val deleteState by viewModel.deleteAccountState.collectAsState()
    val nc = MaterialTheme.nexusColors

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Handle switch account success
    LaunchedEffect(switchState) {
        if (switchState is Resource.Success) {
            viewModel.resetSwitchAccountState()
            viewModel.refreshAccounts()
            onSwitchAccount()
        }
    }

    // Handle delete account success
    LaunchedEffect(deleteState) {
        if (deleteState is Resource.Success) {
            viewModel.resetDeleteAccountState()
            onLogout()
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = nc.surface,
            title = { Text("Xóa tài khoản?", color = nc.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Tài khoản của bạn sẽ bị xóa sau 30 ngày. Bạn có thể đăng nhập lại trong thời gian này để khôi phục.",
                    color = nc.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.requestDeleteAccount()
                }) {
                    Text("XÓA TÀI KHOẢN", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("HỦY", color = nc.textSecondary)
                }
            }
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = nc.surface,
            title = { Text("Đăng xuất?", color = nc.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn đăng xuất?", color = nc.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) {
                    Text("ĐĂNG XUẤT", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("HỦY", color = nc.textSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt", color = nc.textPrimary, fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // ── GIAO DIỆN ──
            SectionHeader("GIAO DIỆN")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(nc.cardBg)
            ) {
                ThemeOptionItem(
                    icon = Icons.Outlined.SettingsSystemDaydream,
                    title = "Giao diện hệ thống",
                    isSelected = uiState.useSystemTheme,
                    onClick = { viewModel.updateTheme(useSystemTheme = true) }
                )
                HorizontalDivider(color = nc.divider, modifier = Modifier.padding(horizontal = 16.dp))
                ThemeOptionItem(
                    icon = Icons.Outlined.LightMode,
                    title = "Chế độ sáng",
                    isSelected = !uiState.useSystemTheme && !uiState.isDarkMode,
                    onClick = { viewModel.updateTheme(useSystemTheme = false, isDarkMode = false) }
                )
                HorizontalDivider(color = nc.divider, modifier = Modifier.padding(horizontal = 16.dp))
                ThemeOptionItem(
                    icon = Icons.Outlined.DarkMode,
                    title = "Chế độ tối",
                    isSelected = !uiState.useSystemTheme && uiState.isDarkMode,
                    onClick = { viewModel.updateTheme(useSystemTheme = false, isDarkMode = true) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── BẢO MẬT ──
            SectionHeader("BẢO MẬT")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(nc.cardBg)
            ) {
                // Biometric toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = uiState.biometricAvailable) {
                            viewModel.toggleAppLock(!uiState.isAppLockEnabled)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = NexusPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Khóa vân tay / Khuôn mặt", color = nc.textPrimary, fontSize = 16.sp)
                        if (!uiState.biometricAvailable) {
                            Text("Thiết bị không hỗ trợ", color = nc.textTertiary, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = uiState.isAppLockEnabled,
                        onCheckedChange = { viewModel.toggleAppLock(it) },
                        enabled = uiState.biometricAvailable,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NexusPrimary
                        )
                    )
                }

                HorizontalDivider(color = nc.divider, modifier = Modifier.padding(horizontal = 16.dp))

                // Change password
                SettingsMenuItem(
                    icon = Icons.Default.Key,
                    title = "Đổi mật khẩu",
                    onClick = onNavigateToChangePassword
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── TÀI KHOẢN ──
            SectionHeader("CHUYỂN TÀI KHOẢN")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(nc.cardBg)
            ) {
                // Loading overlay when switching
                if (switchState is Resource.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NexusPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Đang chuyển tài khoản...", color = nc.textSecondary, fontSize = 14.sp)
                        }
                    }
                    HorizontalDivider(color = nc.divider, modifier = Modifier.padding(horizontal = 16.dp))
                }

                // Saved accounts list
                uiState.savedAccounts.forEachIndexed { index, account ->
                    AccountItem(
                        account = account,
                        isCurrent = account.email == uiState.currentAccountEmail,
                        isSwitching = switchState is Resource.Loading,
                        onSwitch = {
                            viewModel.switchAccount(account.email, account.encryptedPassword)
                        },
                        onRemove = {
                            viewModel.removeAccount(account.email)
                        }
                    )
                    if (index < uiState.savedAccounts.lastIndex) {
                        HorizontalDivider(color = nc.divider, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                // Add account button
                HorizontalDivider(color = nc.divider, modifier = Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAddAccount() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NexusPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = NexusPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Thêm tài khoản",
                        color = NexusPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── ĐĂNG XUẤT ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { showLogoutDialog = true }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng xuất", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── VÙNG NGUY HIỂM ──
            SectionHeader("VÙNG NGUY HIỂM")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.05f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .clickable { showDeleteDialog = true }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Xóa tài khoản", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tài khoản sẽ bị xóa sau 30 ngày", color = nc.textTertiary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val nc = MaterialTheme.nexusColors
    Text(
        title,
        color = nc.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun ThemeOptionItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = nc.iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = nc.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = NexusPrimary,
                unselectedColor = nc.textSecondary
            )
        )
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NexusPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = nc.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = nc.iconTintSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun AccountItem(
    account: SavedAccount,
    isCurrent: Boolean,
    isSwitching: Boolean = false,
    onSwitch: () -> Unit,
    onRemove: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCurrent && !isSwitching, onClick = onSwitch)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (account.avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = account.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NexusPrimary, NexusSecondary))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    account.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                account.displayName.ifEmpty { account.email },
                color = nc.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(account.email, color = nc.textSecondary, fontSize = 12.sp)
        }

        if (isCurrent) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Tài khoản hiện tại",
                tint = NexusPrimary,
                modifier = Modifier.size(22.dp)
            )
        } else {
            TextButton(
                onClick = onSwitch,
                enabled = !isSwitching
            ) {
                Text("Chuyển", color = if (isSwitching) nc.textTertiary else NexusPrimary, fontSize = 13.sp)
            }
        }
    }
}
