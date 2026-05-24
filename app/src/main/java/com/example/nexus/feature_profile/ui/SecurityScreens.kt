package com.example.nexus.feature_profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.utils.Resource
import com.example.nexus.feature_profile.viewmodel.SecurityViewModel
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: SecurityViewModel,
    onNavigateBack: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val changeState by viewModel.changePasswordState.collectAsState()
    val forgotState by viewModel.forgotPasswordState.collectAsState()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle change password state
    LaunchedEffect(changeState) {
        when (changeState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Đổi mật khẩu thành công!")
                viewModel.resetChangePasswordState()
                onNavigateBack()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar((changeState as Resource.Error).message)
                viewModel.resetChangePasswordState()
            }
            else -> {}
        }
    }

    // Handle forgot password state
    LaunchedEffect(forgotState) {
        when (forgotState) {
            is Resource.Success -> {
                showForgotDialog = true
                viewModel.resetForgotPasswordState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar((forgotState as Resource.Error).message)
                viewModel.resetForgotPasswordState()
            }
            else -> {}
        }
    }

    // Forgot password success dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = nc.surface,
            title = {
                Text("Kiểm tra email", color = nc.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Chúng tôi đã gửi email đặt lại mật khẩu đến ${viewModel.userEmail ?: "email của bạn"}. Vui lòng kiểm tra hộp thư và làm theo hướng dẫn.",
                    color = nc.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Đã hiểu", color = NexusPrimary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đổi mật khẩu", color = nc.textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = nc.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = nc.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Info card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(nc.cardBg, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = NexusPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Để đổi mật khẩu, bạn cần nhập mật khẩu hiện tại. Nếu không nhớ, hãy dùng \"Quên mật khẩu cũ?\"",
                        color = nc.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Old Password
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("Mật khẩu hiện tại", color = nc.textSecondary) },
                singleLine = true,
                visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showOldPassword = !showOldPassword }) {
                        Icon(
                            if (showOldPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = nc.iconTintSecondary
                        )
                    }
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
                modifier = Modifier.fillMaxWidth()
            )

            // Forgot password link
            Text(
                "Quên mật khẩu cũ?",
                color = NexusPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 8.dp, start = 4.dp)
                    .clickable {
                        viewModel.userEmail?.let {
                            viewModel.forgotPassword(it)
                        }
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // New Password
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Mật khẩu mới", color = nc.textSecondary) },
                singleLine = true,
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = nc.iconTintSecondary
                        )
                    }
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Xác nhận mật khẩu mới", color = nc.textSecondary) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Submit button
            Button(
                onClick = { viewModel.changePassword(oldPassword, newPassword, confirmPassword) },
                enabled = changeState !is Resource.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexusPrimary,
                    disabledContainerColor = nc.textSecondary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (changeState is Resource.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("ĐỔI MẬT KHẨU", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}
