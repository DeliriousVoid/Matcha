package com.deliriousvoid.openvkmatcha.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.PhotoAlbum
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PhotoAlbumsViewModel

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAlbumsScreen(
    userId: Int,
    name: String,
    onBack: () -> Unit,
    onOpenAlbum: (Int?, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoAlbumsViewModel = viewModel(factory = PhotoAlbumsViewModel.factory(userId))
) {
    val state by viewModel.uiState.collectAsState()
    var selectedAlbums by remember { mutableStateOf(setOf<Int>()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var albumToEdit by remember { mutableStateOf<PhotoAlbum?>(null) }
    var albumToDelete by remember { mutableStateOf<List<Int>?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val isSelectionMode = selectedAlbums.isNotEmpty()

    DisposableEffect(isSelectionMode, selectedAlbums, showMenu, name, state.currentUserId, userId) {
        AppEvents.setTopBarState(if (isSelectionMode) {
            TopBarState(
                tag = "photo_albums",
                title = "${selectedAlbums.size} выбрано",
                navigationIcon = {
                    IconButton(onClick = { selectedAlbums = emptySet() }) {
                        Icon(Icons.Default.Close, "Отмена")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Меню")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (selectedAlbums.size == 1) {
                                DropdownMenuItem(
                                    text = { Text("Изменить") },
                                    onClick = {
                                        showMenu = false
                                        albumToEdit = state.albums.find { it.id == selectedAlbums.first() }
                                        selectedAlbums = emptySet()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    albumToDelete = selectedAlbums.toList()
                                }
                            )
                        }
                    }
                }
            )
        } else {
            TopBarState(
                tag = "photo_albums",
                title = if (name.isNotEmpty()) "Альбомы $name" else "Альбомы",
                actions = {
                    if (state.currentUserId == userId) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, "Создать альбом")
                        }
                    }
                }
            )
        })
        onDispose {
            if (AppEvents.topBarState.value?.tag == "photo_albums") {
                AppEvents.setTopBarState(null)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.albums.isNotEmpty(),
        onRefresh = { viewModel.loadAlbums() },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.albums.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
            state.error != null && state.albums.isEmpty() -> ErrorText(
                message = state.error!!,
                onRetry = { viewModel.loadAlbums() }
            )
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    item {
                        AllPhotosCard(
                            onClick = {
                                if (isSelectionMode) {
                                    selectedAlbums = emptySet()
                                } else {
                                    onOpenAlbum(null, "Все фотографии")
                                }
                            }
                        )
                    }
                    items(state.albums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            isSelected = selectedAlbums.contains(album.id),
                            onClick = {
                                if (isSelectionMode) {
                                    selectedAlbums = if (selectedAlbums.contains(album.id)) {
                                        selectedAlbums - album.id
                                    } else {
                                        selectedAlbums + album.id
                                    }
                                } else {
                                    onOpenAlbum(album.id, album.title)
                                }
                            },
                            onLongClick = {
                                if (state.currentUserId == userId) {
                                    selectedAlbums = selectedAlbums + album.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlbumEditDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc ->
                viewModel.createAlbum(title, desc)
                showCreateDialog = false
            }
        )
    }

    albumToEdit?.let { album ->
        AlbumEditDialog(
            initialTitle = album.title,
            initialDescription = album.description,
            onDismiss = { albumToEdit = null },
            onConfirm = { title, desc ->
                viewModel.editAlbum(album.id, title, desc)
                albumToEdit = null
            }
        )
    }

    albumToDelete?.let { ids ->
        val titles = state.albums.filter { ids.contains(it.id) }.joinToString { it.title }
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text(if (ids.size == 1) "Удалить альбом?" else "Удалить альбомы?") },
            text = { Text("Вы действительно хотите удалить ${if (ids.size == 1) "альбом \"$titles\"" else "${ids.size} альбома(ов)"} и все фотографии в них?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ids.forEach { viewModel.deleteAlbum(it) }
                        albumToDelete = null
                        selectedAlbums = emptySet()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumCard(
    album: PhotoAlbum,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column {
            Box(modifier = Modifier.aspectRatio(1f)) {
                AsyncImage(
                    model = album.thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.size} фото",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AllPhotosCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier.aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Все фото",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AlbumEditDialog(
    initialTitle: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTitle.isEmpty()) "Новый альбом" else "Редактировать альбом") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description) },
                enabled = title.isNotBlank()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
