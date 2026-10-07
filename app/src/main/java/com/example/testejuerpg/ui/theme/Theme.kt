package com.example.testejuerpg.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF1E1300),
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldLight,
    secondary = RubyPrimary,
    onSecondary = Color.White,
    secondaryContainer = RubyDark,
    onSecondaryContainer = RubyLight,
    tertiary = ManaBlue,
    onTertiary = Color(0xFF001E2B),
    background = DungeonDark,
    onBackground = TextPrimary,
    surface = DungeonSurface,
    onSurface = TextPrimary,
    surfaceVariant = DungeonSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF4B4663)
)

@Composable
fun TestejueRPGTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
