package com.aihubmax.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7CC4FF),
    onPrimary = Color(0xFF003355),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFC8E6FF),
    secondary = Color(0xFFA7F3D0),
    onSecondary = Color(0xFF003828),
    background = Color(0xFF0B101B),
    onBackground = Color(0xFFE6ECF5),
    surface = Color(0xFF121927),
    onSurface = Color(0xFFE6ECF5),
    surfaceVariant = Color(0xFF1C2638),
    onSurfaceVariant = Color(0xFFB4C0D3)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00639B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDE5FF),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF006B54),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF121826),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121826),
    surfaceVariant = Color(0xFFE9EFF7),
    onSurfaceVariant = Color(0xFF445066)
)

@Composable
fun AiHubMaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
