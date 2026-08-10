package com.deliriousvoid.openvkmatcha.ui.screens.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

data class DrawingLine(
    val points: List<Offset>, // Normalized coordinates (0..1)
    val color: Color,
    val strokeWidth: Float
)

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraffitiScreen(
    onBack: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lines = remember { mutableStateListOf<DrawingLine>() }
    val currentLinePoints = remember { mutableStateListOf<Offset>() }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableFloatStateOf(10f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    var showColorPicker by remember { mutableStateOf(false) }
    val recentColors = remember { mutableStateListOf<Color>() }

    // Unlock orientation while on this screen
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val defaultColors = listOf(
        Color.Black, Color.Red, Color.Green, Color.Blue, 
        Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray,
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0)
    )

    val topBar = @Composable {
        TopAppBar(
            title = { if (!isLandscape) Text("Рисование") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, "Отмена")
                }
            },
            actions = {
                IconButton(
                    onClick = { if (lines.isNotEmpty()) lines.removeAt(lines.size - 1) },
                    enabled = lines.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Отмена действия")
                }
                IconButton(onClick = { lines.clear() }) {
                    Icon(Icons.Default.Delete, "Очистить")
                }
                IconButton(
                    onClick = {
                        if (lines.isNotEmpty() && canvasSize.width > 0 && canvasSize.height > 0) {
                            val bitmap = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.STROKE
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                            }

                            lines.forEach { line ->
                                paint.color = line.color.toArgb()
                                paint.strokeWidth = line.strokeWidth
                                val path = android.graphics.Path()
                                line.points.forEachIndexed { index, point ->
                                    val x = point.x * canvasSize.width
                                    val y = point.y * canvasSize.height
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                canvas.drawPath(path, paint)
                            }

                            val file = File(context.cacheDir, "graffiti_${System.currentTimeMillis()}.png")
                            FileOutputStream(file).use {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                            }
                            onConfirm(Uri.fromFile(file))
                        }
                    },
                    enabled = lines.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, "Готово")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        )
    }

    val toolsContent = @Composable { isVertical: Boolean ->
        val padding = if (isVertical) PaddingValues(horizontal = 8.dp, vertical = 16.dp) else PaddingValues(16.dp)
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = if (isVertical) Modifier.fillMaxHeight().width(80.dp) else Modifier.fillMaxWidth()
        ) {
            if (isVertical) {
                Column(
                    modifier = Modifier.padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Thickness slider (Vertical)
                    Box(modifier = Modifier.height(120.dp).padding(vertical = 8.dp)) {
                        Slider(
                            value = currentStrokeWidth,
                            onValueChange = { currentStrokeWidth = it },
                            valueRange = 2f..50f,
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationZ = -90f
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                                .width(100.dp)
                        )
                    }
                    Text("Вес", style = MaterialTheme.typography.labelSmall)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Colors (Vertical list)
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = { showColorPicker = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Palette, null)
                        }

                        recentColors.forEach { color ->
                            ColorSlot(
                                color = color,
                                isSelected = currentColor == color,
                                onClick = { currentColor = color }
                            )
                        }

                        if (recentColors.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp).width(24.dp))
                        }

                        defaultColors.forEach { color ->
                            ColorSlot(
                                color = color,
                                isSelected = currentColor == color,
                                onClick = { currentColor = color }
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.padding(padding)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Толщина:", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = currentStrokeWidth,
                            onValueChange = { currentStrokeWidth = it },
                            valueRange = 2f..50f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            IconButton(
                                onClick = { showColorPicker = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Palette, null)
                            }
                        }

                        items(recentColors) { color ->
                            ColorSlot(
                                color = color,
                                isSelected = currentColor == color,
                                onClick = { currentColor = color }
                            )
                        }

                        if (recentColors.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }

                        items(defaultColors) { color ->
                            ColorSlot(
                                color = color,
                                isSelected = currentColor == color,
                                onClick = { currentColor = color }
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = topBar,
        bottomBar = { if (!isLandscape) toolsContent(false) }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Gray.copy(alpha = 0.1f))
        ) {
            if (isLandscape) {
                toolsContent(true)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .background(Color.White)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                                        currentLinePoints.add(
                                            Offset(offset.x / canvasSize.width, offset.y / canvasSize.height)
                                        )
                                    }
                                },
                                onDrag = { change, _ ->
                                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                                        val offset = change.position
                                        currentLinePoints.add(
                                            Offset(offset.x / canvasSize.width, offset.y / canvasSize.height)
                                        )
                                    }
                                },
                                onDragEnd = {
                                    if (currentLinePoints.isNotEmpty()) {
                                        lines.add(DrawingLine(currentLinePoints.toList(), currentColor, currentStrokeWidth))
                                        currentLinePoints.clear()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val drawSize = size
                        // Draw existing lines
                        lines.forEach { line ->
                            val path = Path()
                            line.points.forEachIndexed { index, point ->
                                val x = point.x * drawSize.width
                                val y = point.y * drawSize.height
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = line.color,
                                style = Stroke(
                                    width = line.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Draw current line
                        if (currentLinePoints.isNotEmpty()) {
                            val path = Path()
                            currentLinePoints.forEachIndexed { index, point ->
                                val x = point.x * drawSize.width
                                val y = point.y * drawSize.height
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = currentColor,
                                style = Stroke(
                                    width = currentStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = currentColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                currentColor = color
                if (!recentColors.contains(color)) {
                    recentColors.add(0, color)
                    if (recentColors.size > 5) recentColors.removeAt(5)
                }
                showColorPicker = false
            }
        )
    }
}

@Composable
fun ColorSlot(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red * 255) }
    var green by remember { mutableFloatStateOf(initialColor.green * 255) }
    var blue by remember { mutableFloatStateOf(initialColor.blue * 255) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбор цвета") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(red / 255f, green / 255f, blue / 255f))
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ColorSlider(label = "Красный", value = red, onValueChange = { red = it }, color = Color.Red)
                ColorSlider(label = "Зеленый", value = green, onValueChange = { green = it }, color = Color.Green)
                ColorSlider(label = "Синий", value = blue, onValueChange = { blue = it }, color = Color.Blue)
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(Color(red / 255f, green / 255f, blue / 255f)) }) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value.toInt().toString(), style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
    }
}
