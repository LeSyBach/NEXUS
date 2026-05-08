package com.example.nexus

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        askNotificationPermission()

        setContent {
            val isDarkMode by themeManager.isDarkModeFlow.collectAsState(initial = null)
            val useSystemTheme by themeManager.useSystemThemeFlow.collectAsState(initial = true)
            
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = if (useSystemTheme) isSystemDark else (isDarkMode ?: isSystemDark)

            NEXUSTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    val navController = rememberNavController()
                    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
                    NexusNavGraph(navController = navController, isLoggedIn = isLoggedIn)
                }
            }
        }
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