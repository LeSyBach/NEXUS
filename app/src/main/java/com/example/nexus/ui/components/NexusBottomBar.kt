package com.example.nexus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.navigation.BottomNavItem
import com.example.nexus.navigation.bottomNavItems
import com.example.nexus.ui.theme.DarkCard
import com.example.nexus.ui.theme.NexusPrimary

@Composable
fun NexusBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // Glassmorphism background for BottomBar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkCard.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.screen.route
                BottomBarItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            onNavigate(item.screen.route)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (isSelected) NexusPrimary else Color.Gray
    val icon = if (isSelected) item.selectedIcon else item.unselectedIcon

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = item.label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}
