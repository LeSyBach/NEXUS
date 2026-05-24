package com.example.nexus

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.nexus.navigation.NexusNavGraph
import com.example.nexus.ui.theme.NEXUSTheme
import com.example.nexus.ui.theme.NexusPrimary
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.FirebaseAuth
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    @Inject
    lateinit var themeManager: com.example.nexus.core.utils.ThemeManager

    @Inject
    lateinit var biometricManager: com.example.nexus.core.utils.BiometricManager

    private var isAuthenticated by mutableStateOf(true)

    // State để trigger recompose khi intent mới đến (app đang mở)
    private val pendingIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        askNotificationPermission()
        pendingIntentState.value = intent

        // Check biometric on first launch
        checkBiometricAndAuthenticate()

        setContent {
            val isDarkMode by themeManager.isDarkModeFlow.collectAsState(initial = null)
            val useSystemTheme by themeManager.useSystemThemeFlow.collectAsState(initial = true)

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = if (useSystemTheme) isSystemDark else (isDarkMode ?: isSystemDark)

            NEXUSTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    val navController = rememberNavController()
                    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

                    // Observe pending intent (từ notification khi app đang mở hoặc khởi động)
                    val currentIntent by pendingIntentState
                    LaunchedEffect(currentIntent) {
                        val activeIntent = currentIntent ?: return@LaunchedEffect
                        val navigateTo = activeIntent.getStringExtra("navigateTo")
                        when (navigateTo) {
                            "conversation" -> {
                                val cid = activeIntent.getStringExtra("chatId")
                                if (!cid.isNullOrEmpty()) {
                                    navController.navigate(
                                        com.example.nexus.navigation.Screen.Conversation.createRoute(cid)
                                    )
                                }
                            }
                            "incoming_call" -> {
                                val callId = activeIntent.getStringExtra("callId")
                                if (!callId.isNullOrEmpty()) {
                                    navController.navigate(
                                        com.example.nexus.navigation.Screen.IncomingCall.createRoute(callId)
                                    ) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                            "ongoing_call" -> {
                                // From call accept action in notification
                                val callId = activeIntent.getStringExtra("callId") ?: return@LaunchedEffect
                                val callType = activeIntent.getStringExtra("callType") ?: "voice"
                                val callerName = activeIntent.getStringExtra("callerName") ?: ""
                                navController.navigate(
                                    com.example.nexus.navigation.Screen.OngoingCall.createRoute(
                                        callId = callId,
                                        callType = callType,
                                        receiverId = "",  // already accepted, no need to initiate
                                        receiverName = callerName
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            }
                            "friend_requests" -> {
                                // Điều hướng đến màn hình lời mời kết bạn
                                navController.navigate(com.example.nexus.navigation.Screen.FriendRequests.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    NexusNavGraph(navController = navController, isLoggedIn = isLoggedIn)
                }

                // Biometric lock overlay
                val appLockEnabled by biometricManager.isAppLockEnabled.collectAsState(initial = false)
                if (!isAuthenticated && appLockEnabled && FirebaseAuth.getInstance().currentUser != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = NexusPrimary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "NEXUS",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Xác thực để tiếp tục",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showBiometricPrompt() },
                                colors = ButtonDefaults.buttonColors(containerColor = NexusPrimary)
                            ) {
                                Text("Xác thực", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkBiometricAndAuthenticate()
    }

    private fun checkBiometricAndAuthenticate() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            isAuthenticated = true
            return
        }

        lifecycleScope.launch {
            val appLockEnabled = biometricManager.isAppLockEnabled.first()
            if (!appLockEnabled) {
                isAuthenticated = true
                return@launch
            }

            val canAuth = biometricManager.canUseBiometric()
            if (!canAuth) {
                isAuthenticated = true
                return@launch
            }

            isAuthenticated = false
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isAuthenticated = true
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    // User cancelled — stay locked, allow retry via button
                }
            }

            override fun onAuthenticationFailed() {
                // Fingerprint not recognized — prompt auto-retries
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực NEXUS")
            .setSubtitle("Sử dụng vân tay hoặc mã PIN để mở khóa")
            .setAllowedAuthenticators(
                AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
                    AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        BiometricPrompt(this, executor, callback).authenticate(promptInfo)
    }

    /**
     * Gọi khi app đang chạy và user bấm vào notification.
     * Cập nhật pendingIntentState để LaunchedEffect trong Compose xử lý navigation.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentState.value = intent
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is already granted
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}