package com.deliriousvoid.openvkmatcha.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme {
    AMOLED, LIGHT
}

enum class AccentColor {
    BLUE, GREEN
}

private fun getLightColorScheme(accent: AccentColor): ColorScheme {
    val primaryColor = if (accent == AccentColor.BLUE) VkBlue else VkGreen
    return lightColorScheme(
        primary = primaryColor,
        onPrimary = VkSurface,
        primaryContainer = primaryColor.copy(alpha = 0.25f).compositeOver(VkSurface),
        onPrimaryContainer = primaryColor,
        secondary = VkSecondaryText,
        background = VkBackground,
        surface = VkSurface,
        onBackground = VkOnSurface,
        onSurface = VkOnSurface,
        onSurfaceVariant = VkSecondaryText,
        outline = VkDivider,
        error = VkError,
    )
}

private fun getAmoledColorScheme(accent: AccentColor): ColorScheme {
    val primaryColor = if (accent == AccentColor.BLUE) VkBlue80 else VkGreen80
    val containerColor = if (accent == AccentColor.BLUE) VkBlueDark else VkGreenDark
    return darkColorScheme(
        primary = primaryColor,
        onPrimary = VkOnSurfaceDark,
        primaryContainer = containerColor.copy(alpha = 0.3f),
        onPrimaryContainer = primaryColor,
        secondary = VkSecondaryTextDark,
        background = Color.Black,
        surface = Color.Black,
        onBackground = VkOnSurfaceDark,
        onSurface = VkOnSurfaceDark,
        onSurfaceVariant = VkSecondaryTextDark,
        outline = VkSecondaryTextDark.copy(alpha = 0.3f),
        error = VkError,
    )
}

@Composable
fun OpenVKMatchaTheme(
    theme: AppTheme = AppTheme.AMOLED,
    accent: AccentColor = AccentColor.GREEN,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (theme) {
        AppTheme.LIGHT -> getLightColorScheme(accent)
        AppTheme.AMOLED -> getAmoledColorScheme(accent)
    }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = (theme == AppTheme.LIGHT)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
