package com.padil.stickly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B4EFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF1C0C66),
    secondary = Color(0xFFFF6B9D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E6),
    onSecondaryContainer = Color(0xFF5E1130),
    tertiary = Color(0xFF12B5A5),
    onTertiary = Color.White,
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE7E1EC),
    onSurfaceVariant = Color(0xFF49454E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7A8FF),
    onPrimary = Color(0xFF2A1B7C),
    primaryContainer = Color(0xFF4A3BBC),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFFFB3CB),
    onSecondary = Color(0xFF7E2143),
    secondaryContainer = Color(0xFFA43A5E),
    onSecondaryContainer = Color(0xFFFFD9E6),
    tertiary = Color(0xFF5FE3D3),
    onTertiary = Color(0xFF003731),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4CF),
)

@Composable
fun StickLyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}