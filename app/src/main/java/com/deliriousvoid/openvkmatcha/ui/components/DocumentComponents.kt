package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.Document
import com.deliriousvoid.openvkmatcha.ui.util.formatFileSize

@Composable
fun DocumentItem(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { 
                onClick() 
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            val isGif = document.ext.lowercase() == "gif" || document.type == 3
            val gifModel = document.previewGifUrl ?: if (isGif) document.url else null
            
            if (gifModel != null) {
                AsyncImage(
                    model = gifModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GIF",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (document.previewUrl != null && isImageExt(document.ext)) {
                AsyncImage(
                    model = document.previewUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = getIconForExt(document.ext),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatFileSize(document.size)} • ${document.ext.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.FileDownload,
            contentDescription = "Download",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun GifPostItem(
    document: Document,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onDoubleTap: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.05f))
            .indication(interactionSource, ripple())
            .pointerInput(onDoubleTap == null) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        try {
                            awaitRelease()
                        } finally {
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    },
                    onTap = { currentOnClick() },
                    onDoubleTap = if (onDoubleTap != null) { offset -> currentOnDoubleTap?.invoke(offset) } else null
                )
            }
    ) {
        val gifModel = document.previewGifUrl ?: document.url
        AsyncImage(
            model = gifModel,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )

        // GIF Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "GIF",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun isImageExt(ext: String): Boolean {
    return when (ext.lowercase()) {
        "jpg", "jpeg", "png", "webp", "gif" -> true
        else -> false
    }
}

private fun getIconForExt(ext: String): ImageVector {
    return when (ext.lowercase()) {
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx", "txt", "md" -> Icons.Default.Description
        "jpg", "jpeg", "png", "webp", "gif", "psd", "ai" -> Icons.AutoMirrored.Filled.InsertDriveFile // Or specific image icon if available
        "zip", "rar", "7z", "tar", "gz" -> Icons.AutoMirrored.Filled.InsertDriveFile // Should have archive icon
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
