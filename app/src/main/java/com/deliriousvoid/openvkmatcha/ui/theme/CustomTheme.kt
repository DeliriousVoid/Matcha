package com.deliriousvoid.openvkmatcha.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class CustomTheme(
    val id: String,
    val name: String,
    val author: String,
    val colors: CustomColorScheme,
    val internalAccent: CustomAccent? = null,
    val backgroundImagePath: String? = null,
    val backgroundBlur: Float = 0f,
    val backgroundDim: Float = 0f,
    val backgroundAlpha: Float = 1f,
    val isDark: Boolean = true
)

data class CustomAccent(
    val id: String,
    val name: String,
    val author: String,
    val primary: Long,
    val primaryContainer: Long? = null
)

data class CustomColorScheme(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    val outlineVariant: Long,
    val scrim: Long,
    val inverseSurface: Long,
    val inverseOnSurface: Long,
    val inversePrimary: Long,
    val surfaceDim: Long,
    val surfaceBright: Long,
    val surfaceContainerLowest: Long,
    val surfaceContainerLow: Long,
    val surfaceContainer: Long,
    val surfaceContainerHigh: Long,
    val surfaceContainerHighest: Long
)
