package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun PostImageGrid(
    imageUrls: List<String>,
    onImageClick: (Int) -> Unit,
    onDoubleTap: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (imageUrls.isEmpty()) return

    val count = imageUrls.size
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (count) {
            1 -> {
                GridImage(
                    url = imageUrls[0], 
                    onClick = { onImageClick(0) }, 
                    onDoubleTap = onDoubleTap,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
            2 -> {
                Row(modifier = Modifier.fillMaxWidth().aspectRatio(2f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GridImage(url = imageUrls[0], onClick = { onImageClick(0) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxHeight())
                    GridImage(url = imageUrls[1], onClick = { onImageClick(1) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
            3 -> {
                Row(modifier = Modifier.fillMaxWidth().aspectRatio(1.5f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GridImage(url = imageUrls[0], onClick = { onImageClick(0) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1.5f).fillMaxHeight())
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GridImage(url = imageUrls[1], onClick = { onImageClick(1) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxWidth())
                        GridImage(url = imageUrls[2], onClick = { onImageClick(2) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            4 -> {
                Row(modifier = Modifier.fillMaxWidth().aspectRatio(1.5f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GridImage(url = imageUrls[0], onClick = { onImageClick(0) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxWidth())
                        GridImage(url = imageUrls[1], onClick = { onImageClick(1) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GridImage(url = imageUrls[2], onClick = { onImageClick(2) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxWidth())
                        GridImage(url = imageUrls[3], onClick = { onImageClick(3) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            else -> {
                // Mosaic for 5+ images
                Row(modifier = Modifier.fillMaxWidth().aspectRatio(1.5f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(modifier = Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GridImage(url = imageUrls[0], onClick = { onImageClick(0) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GridImage(url = imageUrls[1], onClick = { onImageClick(1) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxWidth())
                        GridImage(url = imageUrls[2], onClick = { onImageClick(2) }, onDoubleTap = onDoubleTap, modifier = Modifier.weight(1f).fillMaxWidth())
                        GridImage(
                            url = imageUrls[3], 
                            onClick = { onImageClick(3) }, 
                            onDoubleTap = onDoubleTap,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            overlayText = "+${count - 4}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridImage(
    url: String,
    onClick: () -> Unit,
    onDoubleTap: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    overlayText: String? = null
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
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
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
        if (overlayText != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = overlayText,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
