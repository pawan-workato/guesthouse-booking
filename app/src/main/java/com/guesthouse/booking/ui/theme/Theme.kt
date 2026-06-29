package com.guesthouse.booking.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = ForestGreenDark,
    secondary = Color(0xFF546E7A),
    onSecondary = Color.White,
    tertiary = Color(0xFF0288D1),
    background = GlassLightBase,
    onBackground = TextPrimary,
    surface = Color(0xE6FFFFFF),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xCCFFFFFF),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFB0BEC5),
    error = BookedRed
)

private val DarkColors = darkColorScheme(
    primary = AvailableGreen,
    onPrimary = Color(0xFF0D1117),
    primaryContainer = ForestGreenDark,
    onPrimaryContainer = Sage,
    secondary = Color(0xFF90A4AE),
    tertiary = Color(0xFF4FC3F7),
    background = GlassDarkBase,
    onBackground = Color(0xFFECEFF1),
    surface = Color(0x14FFFFFF),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0x1FFFFFFF),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF455A64),
    error = Color(0xFFEF5350)
)

private val GlassShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GuesthouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = GlassShapes,
        content = content
    )
}
