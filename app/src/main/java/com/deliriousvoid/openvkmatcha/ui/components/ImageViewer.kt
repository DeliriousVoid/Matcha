package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.Post
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewer(
    imageUrls: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    post: Post? = null,
    onLikeClick: () -> Unit = {},
    onCommentClick: (Int, Int) -> Unit = { _, _ -> },
    onRepostClick: () -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { imageUrls.size })
    var showMenu by remember { mutableStateOf(false) }
    
    // Track if any page is currently zoomed to disable pager scrolling
    var isAnyPageZoomed by remember { mutableStateOf(false) }
    
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 150.dp.toPx() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            containerColor = Color.Black.copy(alpha = (1f - (abs(offsetY.value) / (dismissThreshold * 2f))).coerceIn(0f, 1f)),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isAnyPageZoomed) {
                    if (!isAnyPageZoomed) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                }
                            },
                            onDragEnd = {
                                if (abs(offsetY.value) > dismissThreshold) {
                                    onDismiss()
                                } else {
                                    scope.launch {
                                        offsetY.animateTo(0f)
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    offsetY.animateTo(0f)
                                }
                            }
                        )
                    }
                },
            topBar = {
                if (abs(offsetY.value) < 10f) {
                    TopAppBar(
                        title = {
                            if (imageUrls.size > 1) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        },
                        actions = {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Скачать") },
                                        onClick = {
                                            showMenu = false
                                            val currentUrl = imageUrls[pagerState.currentPage]
                                            val extension = if (currentUrl.contains(".gif", ignoreCase = true)) "gif" else "jpg"
                                            OpenVKMatchaApp.instance.downloadRepository.downloadFile(
                                                currentUrl, 
                                                "file_${System.currentTimeMillis()}.$extension"
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.Download, null) }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                }
            },
            bottomBar = {
                if (post != null && abs(offsetY.value) < 10f && !isAnyPageZoomed) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Like
                            Row(
                                modifier = Modifier.clickable(onClick = onLikeClick),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (post.isLiked) MaterialTheme.colorScheme.error else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = post.likeCount.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                            
                            // Comment
                            Row(
                                modifier = Modifier.clickable(onClick = { onCommentClick(post.ownerId, post.id) }),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = post.commentCount.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                            
                            // Repost
                            Row(
                                modifier = Modifier.clickable(onClick = onRepostClick),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(-15f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = post.repostCount.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .graphicsLayer {
                        translationY = offsetY.value
                        alpha = (1f - (abs(offsetY.value) / (dismissThreshold * 3f))).coerceIn(0f, 1f)
                    },
                userScrollEnabled = !isAnyPageZoomed,
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { pageIndex ->
                ZoomableImage(
                    url = imageUrls[pageIndex],
                    onZoomChange = { zoomed -> 
                        // Only update global zoom state if it's the current page
                        if (pageIndex == pagerState.currentPage) {
                            isAnyPageZoomed = zoomed
                        }
                    }
                )
            }
        }
    }
    
    // Reset zoom state when changing pages
    LaunchedEffect(pagerState.currentPage) {
        isAnyPageZoomed = false
    }
}

@Composable
private fun ZoomableImage(
    url: String,
    onZoomChange: (Boolean) -> Unit
) {
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var imageSize by remember { mutableStateOf<androidx.compose.ui.unit.IntSize?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(url) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val tapPosition = down.position
                    val currentTime = System.currentTimeMillis()
                    var isAnimating = false
                    
                    if (currentTime - lastTapTime < 300) {
                        // Double tap!
                        isAnimating = true
                        scope.launch {
                            if (scale.value > 1.05f) {
                                launch { scale.animateTo(1f) }
                                launch { offsetX.animateTo(0f) }
                                launch { offsetY.animateTo(0f) }
                                onZoomChange(false)
                            } else {
                                val targetScale = 3f
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                
                                val targetOffsetX = (centerX - tapPosition.x) * (targetScale - 1f)
                                val targetOffsetY = (centerY - tapPosition.y) * (targetScale - 1f)

                                val bounds = calculateBounds(targetScale, size, imageSize)
                                val clampedX = targetOffsetX.coerceIn(-bounds.x, bounds.x)
                                val clampedY = targetOffsetY.coerceIn(-bounds.y, bounds.y)

                                launch { scale.animateTo(targetScale) }
                                launch { offsetX.animateTo(clampedX) }
                                launch { offsetY.animateTo(clampedY) }
                                onZoomChange(true)
                            }
                        }
                    }
                    lastTapTime = currentTime

                    do {
                        val event = awaitPointerEvent()
                        if (isAnimating) {
                            event.changes.forEach { it.consume() }
                            continue
                        }
                        
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid()
                        
                        val oldScale = scale.value
                        val newScale = (oldScale * zoom).coerceIn(1f, 5f)
                        val isNowZoomed = newScale > 1.05f
                        
                        val shouldConsume = event.changes.size > 1 || scale.value > 1.05f
                        
                        if (shouldConsume) {
                            event.changes.forEach { it.consume() }
                            
                            if (zoom != 1f && oldScale != newScale) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val factor = 1f - (newScale / oldScale)
                                val addX = (centroid.x - centerX - offsetX.value) * factor
                                val addY = (centroid.y - centerY - offsetY.value) * factor
                                
                                val targetOffsetX = offsetX.value + addX
                                val targetOffsetY = offsetY.value + addY
                                
                                scope.launch {
                                    scale.snapTo(newScale)
                                    offsetX.snapTo(targetOffsetX)
                                    offsetY.snapTo(targetOffsetY)
                                }
                                onZoomChange(isNowZoomed)
                            } else if (isNowZoomed) {
                                val targetOffsetX = offsetX.value + pan.x
                                val targetOffsetY = offsetY.value + pan.y
                                
                                scope.launch {
                                    offsetX.snapTo(targetOffsetX)
                                    offsetY.snapTo(targetOffsetY)
                                }
                            }

                            if (!isNowZoomed && zoom != 1f) {
                                scope.launch {
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    
                    if (!isAnimating) {
                        scope.launch {
                            if (scale.value <= 1.05f) {
                                launch { scale.animateTo(1f) }
                                launch { offsetX.animateTo(0f) }
                                launch { offsetY.animateTo(0f) }
                                onZoomChange(false)
                            } else {
                                // Snap back to boundaries
                                val bounds = calculateBounds(scale.value, size, imageSize)
                                val clampedX = offsetX.value.coerceIn(-bounds.x, bounds.x)
                                val clampedY = offsetY.value.coerceIn(-bounds.y, bounds.y)
                                launch { offsetX.animateTo(clampedX) }
                                launch { offsetY.animateTo(clampedY) }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    translationX = offsetX.value,
                    translationY = offsetY.value
                ),
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                imageSize = state.painter.intrinsicSize.let { 
                    androidx.compose.ui.unit.IntSize(it.width.toInt(), it.height.toInt())
                }
            }
        )
    }
    
    // Reset local state when this specific component instance changes or is reused
    LaunchedEffect(url) {
        scale.snapTo(1f)
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        onZoomChange(false)
    }
}

private fun calculateBounds(
    scale: Float,
    containerSize: androidx.compose.ui.unit.IntSize,
    contentSize: androidx.compose.ui.unit.IntSize?
): androidx.compose.ui.geometry.Offset {
    if (contentSize == null || scale <= 1f) return androidx.compose.ui.geometry.Offset.Zero
    
    val containerWidth = containerSize.width.toFloat()
    val containerHeight = containerSize.height.toFloat()
    val contentWidth = contentSize.width.toFloat()
    val contentHeight = contentSize.height.toFloat()
    
    val contentAspectRatio = contentWidth / contentHeight
    val containerAspectRatio = containerWidth / containerHeight
    
    val (fittedWidth, fittedHeight) = if (contentAspectRatio > containerAspectRatio) {
        containerWidth to (containerWidth / contentAspectRatio)
    } else {
        (containerHeight * contentAspectRatio) to containerHeight
    }
    
    val boundX = ((fittedWidth * scale) - containerWidth).coerceAtLeast(0f) / 2f
    val boundY = ((fittedHeight * scale) - containerHeight).coerceAtLeast(0f) / 2f
    
    return androidx.compose.ui.geometry.Offset(boundX, boundY)
}
