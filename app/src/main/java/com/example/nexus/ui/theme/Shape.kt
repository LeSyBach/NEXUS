package com.example.nexus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ══════════════════════════════════════════════════════════════
// NEXUS Shapes - Rounded, modern feel
// ══════════════════════════════════════════════════════════════

val NexusShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// Custom shapes for specific components
val MessageBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 4.dp,
    bottomEnd = 16.dp
)

val MessageBubbleShapeMine = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 16.dp,
    bottomEnd = 4.dp
)

val BottomSheetShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp
)

val AvatarShape = RoundedCornerShape(50)

val ChatInputShape = RoundedCornerShape(24.dp)

val CardShape = RoundedCornerShape(16.dp)

val ButtonShape = RoundedCornerShape(12.dp)

val ChipShape = RoundedCornerShape(20.dp)
