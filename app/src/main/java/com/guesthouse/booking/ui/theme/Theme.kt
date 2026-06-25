package com.guesthouse.booking.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = Sage,
    onPrimaryContainer = ForestGreenDark,
    secondary = ForestGreenDark,
    background = WarmCream,
    surface = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = BookedRed
)

private val DarkColors = darkColorScheme(
    primary = AvailableGreen,
    onPrimary = Color.Black,
    primaryContainer = ForestGreenDark,
    secondary = Sage,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun GuesthouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
