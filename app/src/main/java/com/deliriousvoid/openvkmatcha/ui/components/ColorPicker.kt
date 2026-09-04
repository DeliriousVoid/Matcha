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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun ColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    // Use a stable state object so closures (gestures) always see the latest value
    val colorState = remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        mutableStateOf(ColorState(hsv[0], hsv[1], hsv[2], initialColor.alpha))
    }

    var hexTextFieldValue by remember {
        val hex = String.format("%08X", initialColor.toArgb().toLong() and 0xFFFFFFFFL)
        mutableStateOf(TextFieldValue(hex, selection = TextRange(hex.length)))
    }
    var isHexFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(colorState.value) {
        // Only update the text field from the sliders if the user is NOT actively typing in it
        if (!isHexFieldFocused) {
            val currentState = colorState.value
            val currentColor = Color.hsv(currentState.h, currentState.s, currentState.v, currentState.a)
            val currentHex = String.format("%08X", currentColor.toArgb().toLong() and 0xFFFFFFFFL)
            hexTextFieldValue = TextFieldValue(
                text = currentHex,
                selection = TextRange(currentHex.length)
            )
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val current = colorState.value
                        val newS = (current.s + dragAmount.x / size.width).coerceIn(0f, 1f)
                        val newV = (current.v - dragAmount.y / size.height).coerceIn(0f, 1f)
                        val next = current.copy(s = newS, v = newV)
                        colorState.value = next
                        onColorChanged(Color.hsv(next.h, next.s, next.v, next.a))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val current = colorState.value
                        val newS = (offset.x / size.width).coerceIn(0f, 1f)
                        val newV = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        val next = current.copy(s = newS, v = newV)
                        colorState.value = next
                        onColorChanged(Color.hsv(next.h, next.s, next.v, next.a))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val current = colorState.value
                // Draw Saturation-Value box using gradients for efficiency
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, Color.hsv(current.h, 1f, 1f))
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
                
                // Draw cursor
                drawCircle(
                    color = if (current.v > 0.5f) Color.Black else Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(current.s * size.width, (1f - current.v) * size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hue Slider
        val current = colorState.value
        Text("Hue", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = current.h,
            onValueChange = {
                val next = colorState.value.copy(h = it)
                colorState.value = next
                onColorChanged(Color.hsv(next.h, next.s, next.v, next.a))
            },
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth()
        )

        // Alpha Slider
        Text("Alpha", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = current.a,
            onValueChange = {
                val next = colorState.value.copy(a = it)
                colorState.value = next
                onColorChanged(Color.hsv(next.h, next.s, next.v, next.a))
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val displayState = colorState.value
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray) // Simple background to show transparency
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.hsv(displayState.h, displayState.s, displayState.v, displayState.a))
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = hexTextFieldValue,
                onValueChange = { newValue ->
                    hexTextFieldValue = newValue
                    val parsedColor = parseHexColor(newValue.text)
                    if (parsedColor != null) {
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(parsedColor.toArgb(), hsv)
                        colorState.value = ColorState(hsv[0], hsv[1], hsv[2], parsedColor.alpha)
                        onColorChanged(parsedColor)
                    }
                },
                label = { Text("HEX") },
                modifier = Modifier
                    .width(150.dp)
                    .onFocusChanged { isHexFieldFocused = it.isFocused },
                singleLine = true
            )
        }
    }
}

private data class ColorState(val h: Float, val s: Float, val v: Float, val a: Float)

private fun parseHexColor(hex: String): Color? {
    val cleanHex = hex.removePrefix("#").trim()
    if (!cleanHex.all { it in "0123456789ABCDEFabcdef" }) return null

    return try {
        when (cleanHex.length) {
            3 -> {
                val r = cleanHex[0].toString().repeat(2).toInt(16)
                val g = cleanHex[1].toString().repeat(2).toInt(16)
                val b = cleanHex[2].toString().repeat(2).toInt(16)
                Color(r, g, b)
            }
            4 -> {
                val a = cleanHex[0].toString().repeat(2).toInt(16)
                val r = cleanHex[1].toString().repeat(2).toInt(16)
                val g = cleanHex[2].toString().repeat(2).toInt(16)
                val b = cleanHex[3].toString().repeat(2).toInt(16)
                Color(r, g, b, a)
            }
            6 -> {
                val r = cleanHex.substring(0, 2).toInt(16)
                val g = cleanHex.substring(2, 4).toInt(16)
                val b = cleanHex.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val a8 = cleanHex.substring(0, 2).toInt(16)
                val r8 = cleanHex.substring(2, 4).toInt(16)
                val g8 = cleanHex.substring(4, 6).toInt(16)
                val b8 = cleanHex.substring(6, 8).toInt(16)
                Color(r8, g8, b8, a8)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
