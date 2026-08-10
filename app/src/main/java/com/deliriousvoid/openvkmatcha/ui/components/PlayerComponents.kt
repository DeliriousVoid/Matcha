package com.deliriousvoid.openvkmatcha.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.LrcLine
import kotlinx.coroutines.launch

@Composable
fun MiniPlayer(
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    onToggleAdded: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currentTrack == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (currentTrack.artworkUrl != null) {
                    AsyncImage(
                        model = currentTrack.artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = currentTrack.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
                Text(
                    text = currentTrack.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            }

            IconButton(onClick = onToggleAdded) {
                Icon(
                    imageVector = if (currentTrack.isAdded) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (currentTrack.isAdded) "Убрать" else "Добавить",
                    tint = if (currentTrack.isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onPlayPause) {
                if (isBuffering) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayer(
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: Int,
    shuffleMode: Boolean,
    currentPosition: Long,
    duration: Long,
    syncedLyrics: List<LrcLine>,
    currentLineIndex: Int,
    queue: List<AudioTrack>,
    hidePlayedTracks: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleAdded: () -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveInQueue: (Int, Int) -> Unit,
    onShare: () -> Unit,
    onSkipToQueueItem: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (currentTrack == null) return

    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val queueSwipeOffset = remember { Animatable(0f) }

    LaunchedEffect(showQueue) {
        if (showQueue) {
            queueSwipeOffset.snapTo(0f)
        }
    }

    BackHandler(enabled = showQueue || showLyrics) {
        if (showQueue) {
            scope.launch {
                queueSwipeOffset.animateTo(1000f) // Approximate width or we can get real width
                showQueue = false
            }
        }
        else if (showLyrics) showLyrics = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BottomSheetDefaults.DragHandle()

                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showLyrics && syncedLyrics.isNotEmpty()) {
                        LyricsView(
                            lyrics = syncedLyrics,
                            currentLineIndex = currentLineIndex,
                            onLineClick = onSeek,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (currentTrack.artworkUrl != null) {
                        AsyncImage(
                            model = currentTrack.artworkUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                        )
                        Text(
                            text = currentTrack.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                        )
                    }
                    
                    Row {
                        IconButton(onClick = onToggleAdded) {
                            Icon(
                                imageVector = if (currentTrack.isAdded) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = if (currentTrack.isAdded) "Убрать" else "Добавить",
                                tint = if (currentTrack.isAdded) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                        IconButton(
                            onClick = { showLyrics = !showLyrics },
                            enabled = syncedLyrics.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = "Lyrics",
                                tint = if (showLyrics) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { showQueue = !showQueue }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (showQueue) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                var sliderPosition by remember { mutableStateOf<Float?>(null) }

                Slider(
                    value = sliderPosition ?: currentPosition.toFloat(),
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        sliderPosition?.let {
                            onSeek(it.toLong())
                            sliderPosition = null
                        }
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleMode) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }

                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    ) {
                        if (isBuffering) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }

                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showQueue,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = if (queueSwipeOffset.value > 0) fadeOut() else slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = queueSwipeOffset.value.coerceAtLeast(0f)
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (queueSwipeOffset.value > size.width / 4) {
                                            queueSwipeOffset.animateTo(size.width.toFloat())
                                            showQueue = false
                                        } else {
                                            queueSwipeOffset.animateTo(0f)
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { queueSwipeOffset.animateTo(0f) }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        queueSwipeOffset.snapTo(queueSwipeOffset.value + dragAmount)
                                    }
                                }
                            )
                        },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    QueueView(
                        queue = queue,
                        currentTrackId = currentTrack.id,
                        hidePlayedTracks = hidePlayedTracks,
                        onTrackClick = onSkipToQueueItem,
                        onRemove = onRemoveFromQueue,
                        onMove = onMoveInQueue
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun LyricsView(
    lyrics: List<LrcLine>,
    currentLineIndex: Int,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -150
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isCurrent = index == currentLineIndex
            
            val color by animateColorAsState(
                targetValue = if (isCurrent) Color.White else Color.White.copy(alpha = 0.4f),
                label = "lyricColor"
            )
            
            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.1f else 1.0f,
                animationSpec = spring(),
                label = "lyricScale"
            )

            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                ),
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable { onLineClick(line.timestampMs) }
                    .padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun QueueView(
    queue: List<AudioTrack>,
    currentTrackId: Int,
    hidePlayedTracks: Boolean,
    onTrackClick: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }
    
    val currentIndex = queue.indexOfFirst { it.id == currentTrackId }
    val displayQueue = if (hidePlayedTracks && currentIndex != -1) {
        queue.subList(currentIndex, queue.size)
    } else {
        queue
    }
    val offset = if (hidePlayedTracks && currentIndex != -1) currentIndex else 0

    val currentOnMove by rememberUpdatedState(onMove)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Очередь воспроизведения",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            // Visual indicator for swipe-to-dismiss (handle on the left)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .width(4.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .zIndex(2f)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(displayQueue, key = { _, track -> track.stableId }) { index, track ->
                    val originalIndex = index + offset
                    val isPlaying = track.id == currentTrackId
                    val isDragging = draggedItemIndex == index
                    
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 8.dp else 0.dp,
                        label = "dragElevation"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = if (isDragging) draggingOffset else 0f
                            }
                            .zIndex(if (isDragging) 1f else 0f)
                            .shadow(elevation)
                            .background(
                                if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onTrackClick(originalIndex) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Перетащить",
                            modifier = Modifier
                                .size(24.dp)
                                .pointerInput(track.stableId) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { 
                                            draggedItemIndex = index
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggingOffset += dragAmount.y
                                            
                                            val currentIdxInDisplay = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                                            val currentIdxInOriginal = currentIdxInDisplay + offset
                                            
                                            // Simple swap logic
                                            val itemHeight = 64.dp.toPx() 
                                            val threshold = itemHeight * 0.5f
                                            
                                            if (draggingOffset > threshold && currentIdxInDisplay < displayQueue.size - 1) {
                                                currentOnMove(currentIdxInOriginal, currentIdxInOriginal + 1)
                                                draggedItemIndex = currentIdxInDisplay + 1
                                                draggingOffset -= itemHeight
                                            } else if (draggingOffset < -threshold && currentIdxInDisplay > 0) {
                                                currentOnMove(currentIdxInOriginal, currentIdxInOriginal - 1)
                                                draggedItemIndex = currentIdxInDisplay - 1
                                                draggingOffset += itemHeight
                                            }
                                        },
                                        onDragEnd = {
                                            draggedItemIndex = null
                                            draggingOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedItemIndex = null
                                            draggingOffset = 0f
                                        }
                                    )
                                },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (track.artworkUrl != null) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onRemove(originalIndex) }) {
                            Icon(Icons.Default.Close, contentDescription = "Удалить", modifier = Modifier.size(20.dp))
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp), 
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}
