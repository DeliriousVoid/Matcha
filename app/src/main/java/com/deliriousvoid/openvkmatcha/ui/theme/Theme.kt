package com.deliriousvoid.openvkmatcha.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

enum class AppTheme {
    AMOLED, LIGHT, CUSTOM
}

enum class AccentColor {
    BLUE, GREEN, MONET, CUSTOM
}

val LocalOpaqueColorScheme = staticCompositionLocalOf<ColorScheme> {
    error("No ColorScheme provided")
}

val LocalCustomTheme = staticCompositionLocalOf<CustomTheme?> {
    null
}

val LocalAppTheme = staticCompositionLocalOf<AppTheme> {
    AppTheme.AMOLED
}

data class ExtraColors(
    val online: Color,
    val like: Color,
    val wallet: Color,
    val greenTick: Color,
    val blueTick: Color
)

val LocalExtraColors = staticCompositionLocalOf<ExtraColors> {
    error("No ExtraColors provided")
}

private fun getCustomColorScheme(theme: CustomTheme): ColorScheme {
    val c = theme.colors
    return if (theme.isDark) {
        darkColorScheme(
            primary = Color(c.primary),
            onPrimary = Color(c.onPrimary),
            primaryContainer = Color(c.primaryContainer),
            onPrimaryContainer = Color(c.onPrimaryContainer),
            secondary = Color(c.secondary),
            onSecondary = Color(c.onSecondary),
            secondaryContainer = Color(c.secondaryContainer),
            onSecondaryContainer = Color(c.onSecondaryContainer),
            tertiary = Color(c.tertiary),
            onTertiary = Color(c.onTertiary),
            tertiaryContainer = Color(c.tertiaryContainer),
            onTertiaryContainer = Color(c.onTertiaryContainer),
            error = Color(c.error),
            onError = Color(c.onError),
            errorContainer = Color(c.errorContainer),
            onErrorContainer = Color(c.onErrorContainer),
            background = Color(c.background),
            onBackground = Color(c.onBackground),
            surface = Color(c.surface),
            onSurface = Color(c.onSurface),
            surfaceVariant = Color(c.surfaceVariant),
            onSurfaceVariant = Color(c.onSurfaceVariant),
            outline = Color(c.outline),
            outlineVariant = Color(c.outlineVariant),
            scrim = Color(c.scrim),
            inverseSurface = Color(c.inverseSurface),
            inverseOnSurface = Color(c.inverseOnSurface),
            inversePrimary = Color(c.inversePrimary),
            surfaceDim = Color(c.surfaceDim),
            surfaceBright = Color(c.surfaceBright),
            surfaceContainerLowest = Color(c.surfaceContainerLowest),
            surfaceContainerLow = Color(c.surfaceContainerLow),
            surfaceContainer = Color(c.surfaceContainer),
            surfaceContainerHigh = Color(c.surfaceContainerHigh),
            surfaceContainerHighest = Color(c.surfaceContainerHighest),
        )
    } else {
        lightColorScheme(
            primary = Color(c.primary),
            onPrimary = Color(c.onPrimary),
            primaryContainer = Color(c.primaryContainer),
            onPrimaryContainer = Color(c.onPrimaryContainer),
            secondary = Color(c.secondary),
            onSecondary = Color(c.onSecondary),
            secondaryContainer = Color(c.secondaryContainer),
            onSecondaryContainer = Color(c.onSecondaryContainer),
            tertiary = Color(c.tertiary),
            onTertiary = Color(c.onTertiary),
            tertiaryContainer = Color(c.tertiaryContainer),
            onTertiaryContainer = Color(c.onTertiaryContainer),
            error = Color(c.error),
            onError = Color(c.onError),
            errorContainer = Color(c.errorContainer),
            onErrorContainer = Color(c.onErrorContainer),
            background = Color(c.background),
            onBackground = Color(c.onBackground),
            surface = Color(c.surface),
            onSurface = Color(c.onSurface),
            surfaceVariant = Color(c.surfaceVariant),
            onSurfaceVariant = Color(c.onSurfaceVariant),
            outline = Color(c.outline),
            outlineVariant = Color(c.outlineVariant),
            scrim = Color(c.scrim),
            inverseSurface = Color(c.inverseSurface),
            inverseOnSurface = Color(c.inverseOnSurface),
            inversePrimary = Color(c.inversePrimary),
            surfaceDim = Color(c.surfaceDim),
            surfaceBright = Color(c.surfaceBright),
            surfaceContainerLowest = Color(c.surfaceContainerLowest),
            surfaceContainerLow = Color(c.surfaceContainerLow),
            surfaceContainer = Color(c.surfaceContainer),
            surfaceContainerHigh = Color(c.surfaceContainerHigh),
            surfaceContainerHighest = Color(c.surfaceContainerHighest),
        )
    }
}

private fun getLightColorScheme(accent: AccentColor, customAccent: CustomAccent? = null): ColorScheme {
    val primaryColor = when (accent) {
        AccentColor.BLUE -> VkBlue
        AccentColor.GREEN -> VkGreen
        AccentColor.MONET -> VkGreen
        AccentColor.CUSTOM -> customAccent?.let { Color(it.primary) } ?: VkGreen
    }
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

private fun getAmoledColorScheme(accent: AccentColor, customAccent: CustomAccent? = null): ColorScheme {
    val primaryColor = when (accent) {
        AccentColor.BLUE -> VkBlue80
        AccentColor.GREEN -> VkGreen80
        AccentColor.MONET -> VkGreen80
        AccentColor.CUSTOM -> customAccent?.let { Color(it.primary) } ?: VkGreen80
    }
    val containerColor = when (accent) {
        AccentColor.BLUE -> VkBlueDark
        AccentColor.GREEN -> VkGreenDark
        AccentColor.MONET -> VkGreenDark
        AccentColor.CUSTOM -> customAccent?.let { it.primaryContainer?.let { Color(it) } ?: Color(it.primary).copy(alpha = 0.3f) } ?: VkGreenDark
    }
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
    customTheme: CustomTheme? = null,
    customAccent: CustomAccent? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val baseColorScheme = remember(theme, accent, customTheme, customAccent) {
        val isDynamic = accent == AccentColor.MONET && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        
        when (theme) {
            AppTheme.LIGHT -> {
                if (isDynamic) dynamicLightColorScheme(context).copy(
                    error = VkError,
                    onError = Color.White
                )
                else getLightColorScheme(accent, customAccent)
            }
            AppTheme.AMOLED -> {
                if (isDynamic) dynamicDarkColorScheme(context).copy(
                    background = Color.Black,
                    surface = Color.Black,
                    error = VkError,
                    onError = Color.White
                )
                else getAmoledColorScheme(accent, customAccent)
            }
            AppTheme.CUSTOM -> customTheme?.let { getCustomColorScheme(it) } ?: getAmoledColorScheme(accent, customAccent)
        }
    }

    val hasWallpaper = remember(theme, customTheme) {
        theme == AppTheme.CUSTOM && customTheme?.backgroundImagePath != null
    }
    
    val colorScheme = remember(baseColorScheme, hasWallpaper, customTheme) {
        if (hasWallpaper && customTheme != null) {
            val alpha = customTheme.backgroundAlpha
            baseColorScheme.copy(
                background = Color.Transparent,
                surface = baseColorScheme.surface.copy(alpha = alpha),
                surfaceVariant = baseColorScheme.surfaceVariant.copy(alpha = (alpha + 0.05f).coerceAtMost(1f)),
                surfaceContainer = baseColorScheme.surfaceContainer.copy(alpha = (alpha + 0.05f).coerceAtMost(1f)),
                surfaceContainerLow = baseColorScheme.surfaceContainerLow.copy(alpha = (alpha + 0.05f).coerceAtMost(1f)),
                surfaceContainerHigh = baseColorScheme.surfaceContainerHigh.copy(alpha = (alpha + 0.05f).coerceAtMost(1f)),
                surfaceContainerLowest = baseColorScheme.surfaceContainerLowest.copy(alpha = (alpha + 0.05f).coerceAtMost(1f)),
                surfaceContainerHighest = baseColorScheme.surfaceContainerHighest.copy(alpha = (alpha + 0.05f).coerceAtMost(1f)),
            )
        } else {
            baseColorScheme
        }
    }

    val extraColors = remember(colorScheme, customTheme) {
        val c = customTheme?.colors
        ExtraColors(
            online = c?.online?.let { Color(it) } ?: colorScheme.primary,
            like = c?.like?.let { Color(it) } ?: colorScheme.error,
            wallet = c?.wallet?.let { Color(it) } ?: colorScheme.primary,
            greenTick = c?.greenTick?.let { Color(it) } ?: Color(0xFF4BB34B),
            blueTick = c?.blueTick?.let { Color(it) } ?: Color(0xFF82B1FF)
        )
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (hasWallpaper) Color.Transparent.toArgb() else colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = (theme == AppTheme.LIGHT || (theme == AppTheme.CUSTOM && customTheme?.isDark == false))
        }
    }

    CompositionLocalProvider(
        LocalOpaqueColorScheme provides baseColorScheme,
        LocalCustomTheme provides customTheme,
        LocalAppTheme provides theme,
        LocalExtraColors provides extraColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            if (hasWallpaper && customTheme != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = rememberAsyncImagePainter(customTheme.backgroundImagePath),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(customTheme.backgroundBlur.dp)
                            .drawWithContent {
                                drawContent()
                                drawRect(Color.Black.copy(alpha = customTheme.backgroundDim))
                            },
                        contentScale = ContentScale.Crop
                    )
                    content()
                }
            } else {
                content()
            }
        }
    }
}
