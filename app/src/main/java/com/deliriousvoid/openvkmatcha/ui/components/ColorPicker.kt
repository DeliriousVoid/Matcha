package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    var hsv by remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        mutableStateOf(hsv)
    }
    var alpha by remember { mutableStateOf(initialColor.alpha) }

    Column(modifier = Modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val newS = (hsv[1] + dragAmount.x / size.width).coerceIn(0f, 1f)
                        val newV = (hsv[2] - dragAmount.y / size.height).coerceIn(0f, 1f)
                        hsv = floatArrayOf(hsv[0], newS, newV)
                        onColorChanged(Color.hsv(hsv[0], hsv[1], hsv[2], alpha))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newS = (offset.x / size.width).coerceIn(0f, 1f)
                        val newV = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        hsv = floatArrayOf(hsv[0], newS, newV)
                        onColorChanged(Color.hsv(hsv[0], hsv[1], hsv[2], alpha))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw Saturation-Value box using gradients for efficiency
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, Color.hsv(hsv[0], 1f, 1f))
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
                
                // Draw cursor
                drawCircle(
                    color = if (hsv[2] > 0.5f) Color.Black else Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(hsv[1] * size.width, (1f - hsv[2]) * size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hue Slider
        Text("Hue", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = hsv[0],
            onValueChange = {
                hsv = floatArrayOf(it, hsv[1], hsv[2])
                onColorChanged(Color.hsv(hsv[0], hsv[1], hsv[2], alpha))
            },
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth()
        )

        // Alpha Slider
        Text("Alpha", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = alpha,
            onValueChange = {
                alpha = it
                onColorChanged(Color.hsv(hsv[0], hsv[1], hsv[2], alpha))
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray) // Simple background to show transparency
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.hsv(hsv[0], hsv[1], hsv[2], alpha))
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            val currentColor = Color.hsv(hsv[0], hsv[1], hsv[2], alpha)
            Text(
                text = String.format("#%08X", currentColor.toArgb().toLong() and 0xFFFFFFFFL),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

fun Color.toArgb(): Int {
    return (alpha * 255.0f + 0.5f).toInt() shl 24 or
            (red * 255.0f + 0.5f).toInt() shl 16 or
            (green * 255.0f + 0.5f).toInt() shl 8 or
            (blue * 255.0f + 0.5f).toInt()
}
