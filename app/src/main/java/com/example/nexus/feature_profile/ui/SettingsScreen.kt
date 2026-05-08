package com.example.nexus.feature_profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsSystemDaydream
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.feature_profile.viewmodel.SettingsViewModel
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val nc = MaterialTheme.nexusColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cài đặt",
                        color = nc.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            Text(
                "GIAO DIỆN",
                color = nc.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Theme selection card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ThemeOptionItem(
                    icon = Icons.Outlined.SettingsSystemDaydream,
                    title = "Giao diện hệ thống",
                    isSelected = uiState.useSystemTheme,
                    onClick = { viewModel.updateTheme(useSystemTheme = true) }
                )
                
                Divider(color = nc.textSecondary.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                
                ThemeOptionItem(
                    icon = Icons.Outlined.LightMode,
                    title = "Chế độ sáng",
                    isSelected = !uiState.useSystemTheme && !uiState.isDarkMode,
                    onClick = { viewModel.updateTheme(useSystemTheme = false, isDarkMode = false) }
                )

                Divider(color = nc.textSecondary.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                
                ThemeOptionItem(
                    icon = Icons.Outlined.DarkMode,
                    title = "Chế độ tối",
                    isSelected = !uiState.useSystemTheme && uiState.isDarkMode,
                    onClick = { viewModel.updateTheme(useSystemTheme = false, isDarkMode = true) }
                )
            }
        }
    }
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
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, modifier = Modifier.weight(1f))
        
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = nc.textSecondary
            )
        )
    }
}
