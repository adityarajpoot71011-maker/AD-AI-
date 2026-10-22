package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AdAiDarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = Color.White,
    primaryContainer = DeepViolet,
    onPrimaryContainer = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = RoyalBlue,
    onSecondaryContainer = Color.White,
    tertiary = VibrantCyan,
    background = VoidDark,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDarkVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Forced modern dark purple-blue theme for AD AI
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AdAiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
