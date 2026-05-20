package com.example.nexus.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class NexusColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val divider: Color,
    val cardBg: Color,
    val inputBg: Color,
    val bottomBarBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val iconTint: Color,
    val iconTintSecondary: Color,
    val avatarBg: Color,
    val sentBubble: Color,
    val receivedBubble: Color,
    val receivedBubbleText: Color,
    val sentBubbleText: Color,
    val unreadBadge: Color,
    val unreadBadgeText: Color,
    val unreadMessageText: Color,
    val unreadTimeText: Color,
    val searchBg: Color,
    val errorText: Color,
    val isLight: Boolean
)

val DarkNexusColors = NexusColors(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceElevated = DarkCardElevated,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    divider = Color.White.copy(alpha = 0.08f),
    cardBg = DarkCard,
    inputBg = DarkCard,
    bottomBarBg = DarkCard.copy(alpha = 0.85f),
    textPrimary = Color.White,
    textSecondary = Color(0xFFB0B8C4),
    textTertiary = Color(0xFF6B7280),
    iconTint = Color.White,
    iconTintSecondary = Color(0xFF9CA3AF),
    avatarBg = Color.DarkGray,
    sentBubble = SentBubbleDark,
    receivedBubble = DarkCardElevated,
    receivedBubbleText = Color.White,
    sentBubbleText = Color.White,
    unreadBadge = NexusPrimary,
    unreadBadgeText = DarkBackground,
    unreadMessageText = Color.White,
    unreadTimeText = NexusPrimary,
    searchBg = Color(0xFF3A3B3C),
    errorText = Color(0xFFEF4444),
    isLight = false
)

val LightNexusColors = NexusColors(
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceElevated = LightCardElevated,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    divider = Color.Black.copy(alpha = 0.08f),
    cardBg = LightCard,
    inputBg = Color(0xFFEFF1F5),
    bottomBarBg = LightSurface.copy(alpha = 0.92f),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textTertiary = Color(0xFF9CA3AF),
    iconTint = Color(0xFF111827),
    iconTintSecondary = Color(0xFF6B7280),
    avatarBg = Color(0xFFE5E7EB),
    sentBubble = SentBubbleLight,
    receivedBubble = Color(0xFFF0F2F5),
    receivedBubbleText = Color(0xFF111827),
    sentBubbleText = Color.White,
    unreadBadge = NexusPrimaryDark,
    unreadBadgeText = Color.White,
    unreadMessageText = Color(0xFF111827),
    unreadTimeText = NexusPrimaryDark,
    searchBg = Color(0xFFEFF1F5),
    errorText = Color(0xFFEF4444),
    isLight = true
)

val LocalNexusColors = staticCompositionLocalOf { DarkNexusColors }
