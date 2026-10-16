package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.StickerItem
import com.deliriousvoid.openvkmatcha.data.model.StickerPack

@Composable
fun StickerPickerPanel(
    stickerPacks: List<StickerPack>,
    recentStickers: List<StickerItem>,
    isLoading: Boolean,
    onStickerSelected: (StickerItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPackId by remember(recentStickers, stickerPacks) {
        mutableIntStateOf(if (recentStickers.isNotEmpty()) -1 else (stickerPacks.firstOrNull()?.id ?: -1))
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tabs Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recentStickers.isNotEmpty()) {
                    item {
                        IconButton(
                            onClick = { selectedPackId = -1 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selectedPackId == -1) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Недавние",
                                tint = if (selectedPackId == -1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(stickerPacks, key = { it.id }) { pack ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (selectedPackId == pack.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { selectedPackId = pack.id },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = pack.iconUrl ?: pack.stickers.firstOrNull()?.url,
                            contentDescription = pack.title,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Sticker Grid
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && stickerPacks.isEmpty() && recentStickers.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val currentStickers = when (selectedPackId) {
                        -1 -> recentStickers
                        else -> stickerPacks.find { it.id == selectedPackId }?.stickers ?: emptyList()
                    }

                    if (currentStickers.isEmpty()) {
                        Text(
                            text = "Нет доступных стикеров",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 64.dp),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentStickers, key = { it.id }) { sticker ->
                                AsyncImage(
                                    model = sticker.url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onStickerSelected(sticker) },
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
