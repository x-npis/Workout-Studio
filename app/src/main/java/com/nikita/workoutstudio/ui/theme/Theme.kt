package com.nikita.workoutstudio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp

private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val SoftLightSurface = Color(0xFFF2F2F4)
private val SoftLightSurfaceHigh = Color(0xFFE8E8EC)
private val SoftDarkSurface = Color(0xFF161618)
private val SoftDarkSurfaceHigh = Color(0xFF222226)
private val MutedLight = Color(0xFF6B6B70)
private val MutedDark = Color(0xFFA6A6AD)

private val LightColors = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = Black,
    onSecondary = White,
    background = White,
    onBackground = Black,
    surface = SoftLightSurface,
    onSurface = Black,
    surfaceVariant = SoftLightSurfaceHigh,
    onSurfaceVariant = MutedLight,
    outline = Color(0xFFD8D8DD),
    error = Color(0xFFB00020),
    onError = White
)

private val DarkColors = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = White,
    onSecondary = Black,
    background = Black,
    onBackground = White,
    surface = SoftDarkSurface,
    onSurface = White,
    surfaceVariant = SoftDarkSurfaceHigh,
    onSurfaceVariant = MutedDark,
    outline = Color(0xFF3A3A40),
    error = Color(0xFFCF6679),
    onError = Black
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun WorkoutStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        shapes = AppShapes,
        content = content
    )
}
