package com.deliriousvoid.openvkmatcha.ui.screens.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.Photo
import com.deliriousvoid.openvkmatcha.ui.components.EmptyState
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.ImageViewer
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PhotosViewModel
import kotlin.math.abs

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    userId: Int,
    title: String,
    albumId: Int? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotosViewModel = viewModel(factory = PhotosViewModel.factory(userId, albumId))
) {
    val state by viewModel.uiState.collectAsState()
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val placeholder = remember { ColorPainter(Color.Gray.copy(alpha = 0.2f)) }

    var selectedPhotos by remember { mutableStateOf(setOf<Int>()) }
    var photoToEdit by remember { mutableStateOf<Photo?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val isSelectionMode = selectedPhotos.isNotEmpty()

    DisposableEffect(isSelectionMode, selectedPhotos, title, state.reversed) {
        AppEvents.setTopBarState(if (isSelectionMode) {
            TopBarState(
                tag = "photos",
                title = "${selectedPhotos.size} выбрано",
                navigationIcon = {
                    IconButton(onClick = { selectedPhotos = emptySet() }) {
                        Icon(Icons.Default.Close, "Отмена")
                    }
                },
                actions = {
                    if (selectedPhotos.size == 1) {
                        IconButton(onClick = {
                            photoToEdit = state.photos.find { it.id == selectedPhotos.first() }
                        }) {
                            Icon(Icons.Default.Edit, "Изменить")
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Удалить")
                    }
                }
            )
        } else {
            TopBarState(
                tag = "photos",
                title = title,
                actions = {
                    IconButton(onClick = { viewModel.toggleSort() }) {
                        Icon(
                            imageVector = if (state.reversed) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = "Сортировка",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        })
        onDispose {
            if (AppEvents.topBarState.value?.tag == "photos") {
                AppEvents.setTopBarState(null)
            }
        }
    }

    var columnsCount by remember { mutableIntStateOf(3) }
    var pinchScale by remember { mutableFloatStateOf(1f) }
    var isPinching by remember { mutableStateOf(false) }
    var pinchStartColumns by remember { mutableIntStateOf(3) }
    var pinchCentroid by remember { mutableStateOf(Offset.Zero) }
    
    val haptic = LocalHapticFeedback.current
    
    val mainGridState = rememberLazyGridState()
    val previewGridState = rememberLazyGridState()

    // Pagination
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = mainGridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem > totalItems - 10 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    // Calculated "live" column count (fractional)
    val effectiveColumns = remember(pinchScale, pinchStartColumns, isPinching) {
        if (!isPinching) columnsCount.toFloat()
        else (pinchStartColumns / pinchScale).coerceIn(2f, 9f)
    }

    // Determine the two layers to interpolate between
    val colsLow = kotlin.math.floor(effectiveColumns).toInt()
    val colsHigh = kotlin.math.ceil(effectiveColumns).toInt()
    val fraction = effectiveColumns - colsLow

    // Sync preview grid with main grid during pinch
    LaunchedEffect(isPinching) {
        if (isPinching) {
            snapshotFlow { mainGridState.firstVisibleItemIndex to mainGridState.firstVisibleItemScrollOffset }
                .collect { (index, offset) ->
                    previewGridState.scrollToItem(index, offset)
                }
        }
    }

    // Snap target for the end of the gesture
    val snapTargetColumns = remember(effectiveColumns) {
        kotlin.math.round(effectiveColumns).toInt()
    }

    // Haptic feedback when crossing integer boundaries
    var lastRoundedCols by remember { mutableIntStateOf(columnsCount) }
    LaunchedEffect(snapTargetColumns) {
        if (isPinching && snapTargetColumns != lastRoundedCols) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            lastRoundedCols = snapTargetColumns
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.loadPhotos(isRefresh = true, isManual = true) },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.photos.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
            state.error != null && state.photos.isEmpty() -> ErrorText(
                message = state.error!!,
                onRetry = { viewModel.loadPhotos() }
            )
            state.photos.isEmpty() -> EmptyState(
                message = "Нет фотографий",
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.size > 1) {
                                        if (!isPinching) {
                                            isPinching = true
                                            pinchStartColumns = columnsCount
                                            lastRoundedCols = columnsCount
                                        }

                                        // Update centroid
                                        pinchCentroid = event.calculateCentroid()

                                        val zoom = event.calculateZoom()
                                        if (zoom != 1f) {
                                            pinchScale *= zoom
                                            event.changes.forEach { it.consume() }
                                        }
                                    } else if (event.changes.all { !it.pressed }) {
                                        if (isPinching) {
                                            // Calculate final snap target locally to avoid stale closure issues
                                            val finalEffective = (pinchStartColumns / pinchScale).coerceIn(2f, 9f)
                                            columnsCount = kotlin.math.round(finalEffective).toInt()

                                            isPinching = false
                                            pinchScale = 1f
                                        }
                                        break
                                    }
                                }
                            }
                        }
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()

                    if (!isPinching) {
                        // Normal state: Single Grid
                        PhotoGrid(
                            photos = state.photos,
                            columns = columnsCount,
                            state = mainGridState,
                            placeholder = placeholder,
                            selectedPhotos = selectedPhotos,
                            onPhotoClick = { index ->
                                val photo = state.photos[index]
                                if (isSelectionMode) {
                                    selectedPhotos = if (selectedPhotos.contains(photo.id)) {
                                        selectedPhotos - photo.id
                                    } else {
                                        selectedPhotos + photo.id
                                    }
                                } else {
                                    selectedPhotoIndex = index
                                }
                            },
                            onPhotoLongClick = { index ->
                                if (state.currentUserId == userId) {
                                    selectedPhotos = selectedPhotos + state.photos[index].id
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Pinching state: Dual Layer Interpolation

                        // Transform origin relative to the grid size
                        val origin = TransformOrigin(
                            pivotFractionX = if (widthPx > 0) pinchCentroid.x / widthPx else 0.5f,
                            pivotFractionY = if (heightPx > 0) pinchCentroid.y / heightPx else 0.5f
                        )

                        // Layer A (Lower count)
                        PhotoGrid(
                            photos = state.photos,
                            columns = colsLow,
                            state = if (colsLow == pinchStartColumns) mainGridState else previewGridState,
                            placeholder = placeholder,
                            selectedPhotos = selectedPhotos,
                            onPhotoClick = { },
                            onPhotoLongClick = { },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    transformOrigin = origin
                                    scaleX = colsLow / effectiveColumns
                                    scaleY = colsLow / effectiveColumns
                                    alpha = 1f - fraction
                                }
                        )

                        // Layer B (Higher count)
                        if (colsLow != colsHigh) {
                            PhotoGrid(
                                photos = state.photos,
                                columns = colsHigh,
                                state = if (colsHigh == pinchStartColumns) mainGridState else previewGridState,
                                placeholder = placeholder,
                                selectedPhotos = selectedPhotos,
                                onPhotoClick = { },
                                onPhotoLongClick = { },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        transformOrigin = origin
                                        scaleX = colsHigh / effectiveColumns
                                        scaleY = colsHigh / effectiveColumns
                                        alpha = fraction
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPhotoIndex?.let { index ->
        ImageViewer(
            imageUrls = state.photos.map { it.url },
            initialIndex = index,
            onDismiss = { selectedPhotoIndex = null }
        )
    }

    photoToEdit?.let { photo ->
        var caption by remember { mutableStateOf(photo.text) }
        AlertDialog(
            onDismissRequest = { photoToEdit = null },
            title = { Text("Редактировать описание") },
            text = {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Описание") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.editPhoto(photo.id, caption)
                    photoToEdit = null
                    selectedPhotos = emptySet()
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToEdit = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить фото?") },
            text = { Text("Вы действительно хотите удалить ${selectedPhotos.size} фото?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePhotos(selectedPhotos.toList())
                        selectedPhotos = emptySet()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    columns: Int,
    state: LazyGridState,
    placeholder: ColorPainter,
    selectedPhotos: Set<Int>,
    onPhotoClick: (Int) -> Unit,
    onPhotoLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        modifier = modifier
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
            val isSelected = selectedPhotos.contains(photo.id)
            val model = photo.thumbUrl.takeIf { it.isNotBlank() } ?: photo.url
            
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(1.dp)
                    .combinedClickable(
                        onClick = { onPhotoClick(index) },
                        onLongClick = { onPhotoLongClick(index) }
                    )
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = placeholder,
                    error = placeholder
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }
    }
}
