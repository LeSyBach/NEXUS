package com.example.nexus.feature_auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.ui.components.GlassCard
import com.example.nexus.ui.components.NexusGradientButton
import com.example.nexus.ui.components.NexusTextField
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors
import com.example.nexus.feature_auth.viewmodel.AuthViewModel
import com.example.nexus.core.utils.Resource
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginState by viewModel.loginState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val nc = MaterialTheme.nexusColors

    LaunchedEffect(loginState) {
        when (loginState) {
            is Resource.Success -> {
                viewModel.resetLoginState()
                onLoginSuccess()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar((loginState as Resource.Error).message)
                viewModel.resetLoginState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            nc.background,
                            Color(0xFF1A1A2E) // Deep purple/blue
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
        // Vòng tròn trang trí mờ mờ ở phía sau (blur effects)
        Box(
            modifier = Modifier
                .offset(x = (-80).dp, y = (-120).dp)
                .size(200.dp)
                .background(NexusPrimary.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape)
        )

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NEXUS",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = nc.textPrimary,
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Kết nối không giới hạn",
                    color = nc.textSecondary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                NexusTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = nc.textSecondary) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                NexusTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Mật khẩu",
                    isPassword = true,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = nc.textSecondary) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Quên mật khẩu?",
                    color = NexusPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 24.dp)
                        .clickable { /* Handle forgot password */ }
                )

                NexusGradientButton(
                    text = "ĐĂNG NHẬP",
                    isLoading = loginState is Resource.Loading,
                    onClick = { viewModel.login(email, password) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chưa có tài khoản? ",
                        color = nc.textSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = "Đăng ký ngay",
                        color = NexusPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.clickable { onNavigateToRegister() }
                    )
                }
            }
        }
        }  // Đóng Box
    }  // Đóng Scaffold
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val registerState by viewModel.registerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val nc = MaterialTheme.nexusColors

    LaunchedEffect(registerState) {
        when (registerState) {
            is Resource.Success -> {
                viewModel.resetRegisterState()
                onRegisterSuccess()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar((registerState as Resource.Error).message)
                viewModel.resetRegisterState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            nc.background,
                            Color(0xFF1A1A2E)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
        // Vòng tròn trang trí mờ mờ ở phía sau
        Box(
            modifier = Modifier
                .offset(x = 100.dp, y = 150.dp)
                .size(250.dp)
                .background(com.example.nexus.ui.theme.NexusSecondary.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.CircleShape)
        )

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TẠO TÀI KHOẢN",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = nc.textPrimary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Gia nhập cộng đồng NEXUS",
                    color = nc.textSecondary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                NexusTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Tên hiển thị",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = nc.textSecondary) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                NexusTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = nc.textSecondary) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                NexusTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Mật khẩu",
                    isPassword = true,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = nc.textSecondary) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                NexusTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Xác nhận mật khẩu",
                    isPassword = true,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = nc.textSecondary) },
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                NexusGradientButton(
                    text = "ĐĂNG KÝ",
                    isLoading = registerState is Resource.Loading,
                    onClick = { viewModel.register(email, username, password, confirmPassword) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đã có tài khoản? ",
                        color = nc.textSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = "Đăng nhập",
                        color = NexusPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.clickable { onNavigateBack() }
                    )
                }
            }
        }
        }  // Đóng Box
    }  // Đóng Scaffold
}
