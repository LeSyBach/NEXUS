package com.example.nexus.feature_auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    val loginState by viewModel.loginState.collectAsState()
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val nc = MaterialTheme.nexusColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Google Sign-In via Credential Manager
    val credentialManager = remember { CredentialManager.create(context) }
    val serverClientId = context.getString(com.example.nexus.R.string.web_client_id)

    fun launchGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                viewModel.googleSignIn(idToken)
            } catch (e: GetCredentialException) {
                snackbarHostState.showSnackbar("Đăng nhập Google bị hủy hoặc thất bại")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: "Đăng nhập Google thất bại")
            }
        }
    }

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

    LaunchedEffect(forgotPasswordState) {
        when (forgotPasswordState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Đã gửi email khôi phục! Kiểm tra hộp thư của bạn.")
                showForgotPasswordDialog = false
                forgotEmail = ""
                viewModel.resetForgotPasswordState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar((forgotPasswordState as Resource.Error).message)
                viewModel.resetForgotPasswordState()
            }
            else -> {}
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                viewModel.resetForgotPasswordState()
            },
            containerColor = nc.surface,
            title = {
                Text("Quên mật khẩu", color = nc.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Nhập email để nhận link đặt lại mật khẩu.",
                        color = nc.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    NexusTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = "Email",
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = nc.textSecondary) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.forgotPassword(forgotEmail) },
                    enabled = forgotPasswordState !is Resource.Loading
                ) {
                    if (forgotPasswordState is Resource.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NexusPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("GỬI", color = NexusPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotPasswordDialog = false
                    viewModel.resetForgotPasswordState()
                }) {
                    Text("HỦY", color = nc.textSecondary)
                }
            }
        )
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
                        colors = if (nc.isLight) {
                            listOf(nc.background, Color(0xFFE0E7F1))
                        } else {
                            listOf(nc.background, Color(0xFF1A1A2E))
                        }
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
                        .clickable {
                            forgotEmail = email  // Pre-fill with entered email
                            showForgotPasswordDialog = true
                        }
                )

                NexusGradientButton(
                    text = "ĐĂNG NHẬP",
                    isLoading = loginState is Resource.Loading,
                    onClick = { viewModel.login(email, password) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(nc.textSecondary.copy(alpha = 0.3f)))
                    Text(
                        text = " HOẶC ",
                        color = nc.textSecondary,
                        fontSize = 12.sp
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(nc.textSecondary.copy(alpha = 0.3f)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(
                            width = 1.dp,
                            color = nc.textSecondary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = loginState !is Resource.Loading) { launchGoogleSignIn() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "G",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đăng nhập với Google",
                            color = nc.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

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
                        colors = if (nc.isLight) {
                            listOf(nc.background, Color(0xFFE0E7F1))
                        } else {
                            listOf(nc.background, Color(0xFF1A1A2E))
                        }
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
