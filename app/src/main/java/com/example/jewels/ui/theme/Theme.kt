package com.example.jewels.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = NaoluxGold,
    onPrimary = NaoluxOnGold,

    secondary = NaoluxGoldDark,
    onSecondary = NaoluxOnGold,

    background = SurfaceWarm,
    onBackground = Color(0xFF1A1A1A),

    surface = SurfaceWarm,
    onSurface = Color(0xFF1A1A1A),

    surfaceVariant = SurfaceCard,
    onSurfaceVariant = Color(0xFF2A2A2A),

    outline = OutlineSoft
)

private val DarkColors = darkColorScheme(
    primary = NaoluxGold,
    onPrimary = NaoluxOnGold,

    secondary = NaoluxGoldDark,
    onSecondary = NaoluxOnGold,

    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFEDEDED),

    surface = Color(0xFF121217),
    onSurface = Color(0xFFCCCCCC),

    surfaceVariant = Color(0xFF1A1A20),
    onSurfaceVariant = Color(0xFFE0E0E0),

    outline = Color(0xFF3A3A3A)
)

@Composable
fun JewelsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
