package com.example.nexus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.nexus.navigation.NexusNavGraph
import com.example.nexus.ui.theme.NEXUSTheme
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.FirebaseAuth
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import jakarta.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    @Inject
    lateinit var themeManager: com.example.nexus.core.utils.ThemeManager

    // State để trigger recompose khi intent mới đến (app đang mở)
    private val pendingIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        askNotificationPermission()
        pendingIntentState.value = intent

        setContent {
            val isDarkMode by themeManager.isDarkModeFlow.collectAsState(initial = null)
            val useSystemTheme by themeManager.useSystemThemeFlow.collectAsState(initial = true)

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = if (useSystemTheme) isSystemDark else (isDarkMode ?: isSystemDark)

            NEXUSTheme(darkTheme = darkTheme) {
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
            }
        }
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