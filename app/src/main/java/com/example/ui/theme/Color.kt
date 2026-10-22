package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val VoidDark = Color(0xFF080612)
val VoidNavy = Color(0xFF0D0B1D)
val CardDark = Color(0xFF140F2C)
val CardDarkVariant = Color(0xFF1C153B)
val CardBorder = Color(0xFF2E2356)
val CardBorderHighlight = Color(0xFF4C388E)

val ElectricPurple = Color(0xFFA855F7)
val DeepViolet = Color(0xFF7C3AED)
val ElectricBlue = Color(0xFF38BDF8)
val RoyalBlue = Color(0xFF2563EB)
val VibrantCyan = Color(0xFF06B6D4)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val UserBubbleStart = Color(0xFF7C3AED)
val UserBubbleEnd = Color(0xFF2563EB)
val AiBubbleBg = Color(0xFF130E29)
val AiBubbleBorder = Color(0xFF2A1F52)

val AccentGold = Color(0xFFFBBF24)
val SuccessGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFF87171)

// Gradients
val PurpleBlueGradient = Brush.linearGradient(
    listOf(DeepViolet, RoyalBlue)
)

val NeonBadgeGradient = Brush.horizontalGradient(
    listOf(ElectricPurple, ElectricBlue)
)

val BackgroundGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF0C071C),
        Color(0xFF090614),
        Color(0xFF05030A)
    )
)

val GlassInputGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF1B143B).copy(alpha = 0.95f),
        Color(0xFF130D2C).copy(alpha = 0.98f)
    )
)
