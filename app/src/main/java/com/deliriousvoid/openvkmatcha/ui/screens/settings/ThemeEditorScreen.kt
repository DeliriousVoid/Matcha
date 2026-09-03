package com.deliriousvoid.openvkmatcha.ui.screens.settings

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.ui.components.ColorPicker
import com.deliriousvoid.openvkmatcha.ui.theme.CustomColorScheme
import com.deliriousvoid.openvkmatcha.ui.theme.CustomTheme
import com.deliriousvoid.openvkmatcha.ui.theme.LocalOpaqueColorScheme
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditorScreen(
    themeId: String? = null,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val opaqueColorScheme = LocalOpaqueColorScheme.current
    val existingTheme = remember { viewModel.customThemes.value.find { it.id == themeId } }
    
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val defaultBackground = MaterialTheme.colorScheme.background
    val defaultSurface = MaterialTheme.colorScheme.surface
    val defaultOnSurface = MaterialTheme.colorScheme.onSurface

    var name by remember { mutableStateOf(existingTheme?.name ?: "") }
    var author by remember { mutableStateOf(existingTheme?.author ?: "") }
    var isDark by remember { mutableStateOf(existingTheme?.isDark ?: true) }
    var blur by remember { mutableStateOf(existingTheme?.backgroundBlur ?: 0f) }
    var dim by remember { mutableStateOf(existingTheme?.backgroundDim ?: 0f) }
    var alpha by remember { mutableStateOf(existingTheme?.backgroundAlpha ?: 1f) }
    
    var primary by remember { mutableStateOf(existingTheme?.colors?.primary?.let { Color(it) } ?: defaultPrimary) }
    var background by remember { mutableStateOf(existingTheme?.colors?.background?.let { Color(it) } ?: defaultBackground) }
    var surface by remember { mutableStateOf(existingTheme?.colors?.surface?.let { Color(it) } ?: defaultSurface) }
    var onSurface by remember { mutableStateOf(existingTheme?.colors?.onSurface?.let { Color(it) } ?: defaultOnSurface) }
    var onSurfaceVariant by remember { mutableStateOf(existingTheme?.colors?.onSurfaceVariant?.let { Color(it) } ?: onSurface.copy(alpha = 0.6f)) }
    var isSecondaryAutomatic by remember { mutableStateOf(existingTheme == null) }
    
    var currentImagePath by remember { mutableStateOf(existingTheme?.backgroundImagePath) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
        uri?.let {
            imageBitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            alpha = 0f
        }
    }

    var pickingColorFor by remember { mutableStateOf<String?>(null) }

    val titleNew = matchaStringResource(com.deliriousvoid.openvkmatcha.R.string.title_new_theme)
    val titleEdit = matchaStringResource(com.deliriousvoid.openvkmatcha.R.string.title_edit_theme)

    DisposableEffect(themeId) {
        com.deliriousvoid.openvkmatcha.util.AppEvents.setTopBarState(com.deliriousvoid.openvkmatcha.util.TopBarState(
            title = if (themeId == null) titleNew else titleEdit,
            route = com.deliriousvoid.openvkmatcha.ui.navigation.Routes.SETTINGS_THEME_EDITOR,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        ))
        onDispose {
            com.deliriousvoid.openvkmatcha.util.AppEvents.setTopBarState(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark Theme")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = isDark, onCheckedChange = { isDark = it })
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Colors", style = MaterialTheme.typography.titleMedium)
            
            ColorSelectionRow("Primary", primary, { pickingColorFor = "primary" }, { primary = defaultPrimary })
            ColorSelectionRow("Background", background, { pickingColorFor = "background" }, { background = defaultBackground })
            ColorSelectionRow("Surface", surface, { pickingColorFor = "surface" }, { surface = defaultSurface })
            ColorSelectionRow("Text", onSurface, { pickingColorFor = "onSurface" }, { onSurface = defaultOnSurface })
            ColorSelectionRow("Secondary Text", onSurfaceVariant, { pickingColorFor = "onSurfaceVariant" }, { onSurfaceVariant = onSurface.copy(alpha = 0.6f) })
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wallpaper", style = MaterialTheme.typography.titleMedium)
                if (imageUri != null || currentImagePath != null) {
                    IconButton(onClick = {
                        imageUri = null
                        imageBitmap = null
                        currentImagePath = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Wallpaper")
                    }
                }
            }
            Button(onClick = { launcher.launch("image/*") }) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose Image")
            }

            if (imageUri != null || currentImagePath != null) {
                Spacer(modifier = Modifier.height(16.dp))
                WallpaperPreview(
                    imageUri = imageUri,
                    imagePath = currentImagePath,
                    blur = blur,
                    dim = dim
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Blur: ${blur.toInt()}")
                Slider(value = blur, onValueChange = { blur = it }, valueRange = 0f..25f)
                Text("Dim: ${(dim * 100).toInt()}%")
                Slider(value = dim, onValueChange = { dim = it }, valueRange = 0f..1f)
                Text("Background Transparency: ${(alpha * 100).toInt()}%")
                Slider(value = alpha, onValueChange = { alpha = it }, valueRange = 0f..1f)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val id = themeId ?: "theme_${System.currentTimeMillis()}"
                    // Simple color scheme derivation for the rest of colors
                    val colors = CustomColorScheme(
                        primary = primary.toArgb().toLong(),
                        onPrimary = Color.White.toArgb().toLong(), // Simple default
                        primaryContainer = primary.copy(alpha = 0.3f).toArgb().toLong(),
                        onPrimaryContainer = primary.toArgb().toLong(),
                        secondary = onSurfaceVariant.toArgb().toLong(),
                        onSecondary = onSurface.toArgb().toLong(),
                        secondaryContainer = onSurfaceVariant.copy(alpha = 0.1f).toArgb().toLong(),
                        onSecondaryContainer = onSurfaceVariant.toArgb().toLong(),
                        tertiary = primary.toArgb().toLong(), // Same as primary for simplicity
                        onTertiary = Color.White.toArgb().toLong(),
                        tertiaryContainer = primary.copy(alpha = 0.3f).toArgb().toLong(),
                        onTertiaryContainer = primary.toArgb().toLong(),
                        error = Color.Red.toArgb().toLong(),
                        onError = Color.White.toArgb().toLong(),
                        errorContainer = Color.Red.copy(alpha = 0.1f).toArgb().toLong(),
                        onErrorContainer = Color.Red.toArgb().toLong(),
                        background = background.toArgb().toLong(),
                        onBackground = onSurface.toArgb().toLong(),
                        surface = surface.toArgb().toLong(),
                        onSurface = onSurface.toArgb().toLong(),
                        surfaceVariant = surface.toArgb().toLong(),
                        onSurfaceVariant = onSurfaceVariant.toArgb().toLong(),
                        outline = onSurfaceVariant.copy(alpha = 0.2f).toArgb().toLong(),
                        outlineVariant = onSurfaceVariant.copy(alpha = 0.1f).toArgb().toLong(),
                        scrim = Color.Black.toArgb().toLong(),
                        inverseSurface = onSurface.toArgb().toLong(),
                        inverseOnSurface = surface.toArgb().toLong(),
                        inversePrimary = primary.toArgb().toLong(),
                        surfaceDim = surface.toArgb().toLong(),
                        surfaceBright = surface.toArgb().toLong(),
                        surfaceContainerLowest = surface.toArgb().toLong(),
                        surfaceContainerLow = surface.toArgb().toLong(),
                        surfaceContainer = surface.toArgb().toLong(),
                        surfaceContainerHigh = surface.toArgb().toLong(),
                        surfaceContainerHighest = surface.toArgb().toLong()
                    )
                    val theme = CustomTheme(
                        id = id,
                        name = name,
                        author = author,
                        colors = colors,
                        backgroundImagePath = currentImagePath, // Updated to use local state
                        backgroundBlur = blur,
                        backgroundDim = dim,
                        backgroundAlpha = alpha,
                        isDark = isDark
                    )
                    viewModel.saveTheme(theme, imageBitmap) {
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        }

    if (pickingColorFor != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickingColorFor = null },
            containerColor = opaqueColorScheme.surface,
            confirmButton = {
                Button(onClick = { pickingColorFor = null }) { Text("OK") }
            },
            text = {
                val initial = when (pickingColorFor) {
                    "primary" -> primary
                    "background" -> background
                    "surface" -> surface
                    "onSurface" -> onSurface
                    "onSurfaceVariant" -> onSurfaceVariant
                    else -> Color.White
                }
                ColorPicker(initialColor = initial) { color ->
                    when (pickingColorFor) {
                        "primary" -> primary = color
                        "background" -> background = color
                        "surface" -> surface = color
                        "onSurface" -> {
                            onSurface = color
                            if (isSecondaryAutomatic) {
                                onSurfaceVariant = color.copy(alpha = 0.6f)
                            }
                        }
                        "onSurfaceVariant" -> {
                            onSurfaceVariant = color
                            isSecondaryAutomatic = false
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun WallpaperPreview(
    imageUri: Uri?,
    imagePath: String?,
    blur: Float,
    dim: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Gray.copy(alpha = 0.2f))
    ) {
        if (imageUri != null || imagePath != null) {
            Image(
                painter = rememberAsyncImagePainter(imageUri ?: imagePath),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blur.dp)
                    .drawWithContent {
                        drawContent()
                        drawRect(Color.Black.copy(alpha = dim))
                    },
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ColorSelectionRow(
    label: String,
    color: Color,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                )
            }
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Reset Color",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

