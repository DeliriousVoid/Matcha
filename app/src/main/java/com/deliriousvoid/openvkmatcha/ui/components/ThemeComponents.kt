package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.deliriousvoid.openvkmatcha.ui.theme.AppTheme
import com.deliriousvoid.openvkmatcha.ui.theme.LocalAppTheme
import com.deliriousvoid.openvkmatcha.ui.theme.LocalCustomTheme
import com.deliriousvoid.openvkmatcha.ui.theme.LocalOpaqueColorScheme

@Composable
fun ThemedPopupBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val appTheme = LocalAppTheme.current
    val customTheme = LocalCustomTheme.current
    val opaqueColorScheme = LocalOpaqueColorScheme.current
    val hasWallpaper = appTheme == AppTheme.CUSTOM && customTheme?.backgroundImagePath != null

    Box(modifier = modifier) {
        if (hasWallpaper && customTheme != null) {
            Image(
                painter = rememberAsyncImagePainter(customTheme.backgroundImagePath),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .blur(customTheme.backgroundBlur.dp)
                    .drawWithContent {
                        drawContent()
                        drawRect(Color.Black.copy(alpha = customTheme.backgroundDim))
                    },
                contentScale = ContentScale.Crop
            )
            // Add a slight overlay to make it look like a surface
            Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.2f)))
        } else {
            Box(modifier = Modifier.matchParentSize().background(opaqueColorScheme.surface))
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}
