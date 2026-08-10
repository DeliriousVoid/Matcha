package com.deliriousvoid.openvkmatcha.ui.screens.music

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.ui.components.AudioTrackItem
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.PlaylistItem
import com.deliriousvoid.openvkmatcha.ui.components.EditPlaylistDialog
import com.deliriousvoid.openvkmatcha.ui.components.DeleteConfirmationDialog
import com.deliriousvoid.openvkmatcha.ui.components.MusicPickerBottomSheet
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PlayerViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PlaylistDetailsViewModel

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@Composable
fun PlaylistDetailsScreen(
    ownerId: Int,
    playlistId: Int,
    title: String,
    onBack: () -> Unit,
    viewModel: PlaylistDetailsViewModel = viewModel(factory = PlaylistDetailsViewModel.factory(ownerId, playlistId)),
    musicViewModel: MusicViewModel = viewModel(factory = MusicViewModel.factory()),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory()),
    settingsViewModel: com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel = viewModel(factory = com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    val downloadedTracks by musicViewModel.downloadedTracks.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val shuffleMode by playerViewModel.shuffleMode.collectAsState()
    val experimentalFeatures by settingsViewModel.experimentalFeatures.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val isCurrentPlaylistActive = state.tracks.isNotEmpty() && 
                                  state.tracks.any { it.id == currentTrack?.id }
    val isPlaylistPlaying = isPlaying && isCurrentPlaylistActive

    var localShuffle by remember { mutableStateOf(shuffleMode) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMusicPicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    DisposableEffect(state.playlist, state.isBookmarked, state.isOwner, showMenu) {
        AppEvents.setTopBarState(TopBarState(
            title = state.playlist?.title ?: title,
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Поделиться") },
                            onClick = { 
                                showMenu = false
                                val baseUrl = com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance.api.baseUrl
                                val url = "$baseUrl/playlist${ownerId}_$playlistId"
                                clipboardManager.setText(AnnotatedString(url))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Открыть в браузере") },
                            onClick = { 
                                showMenu = false
                                val baseUrl = com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance.api.baseUrl
                                val url = "$baseUrl/playlist${ownerId}_$playlistId"
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                        
                        if (state.isOwner) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Редактировать") },
                                onClick = { showMenu = false; showEditDialog = true },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; showDeleteConfirm = true },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        ))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    LaunchedEffect(state.playlistDeleted) {
        if (state.playlistDeleted) {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.tracks.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
            state.error != null && state.tracks.isEmpty() -> ErrorText(
                message = state.error!!,
                modifier = Modifier.fillMaxSize(),
                onRetry = { viewModel.loadTracks(isRefresh = true) }
            )
            else -> {
                val listState = rememberLazyListState()
                val shouldLoadMore = remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        lastVisibleItemIndex > totalItems - 5 && totalItems > 0
                    }
                }

                LaunchedEffect(shouldLoadMore.value) {
                    if (shouldLoadMore.value) {
                        viewModel.loadMore()
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Section
                    item {
                        PlaylistHeader(
                            title = state.playlist?.title ?: title,
                            description = state.playlist?.description ?: "",
                            photoUrl = state.playlist?.photoUrl,
                            trackCount = state.playlist?.trackCount ?: state.tracks.size,
                            isPlaying = isPlaylistPlaying,
                            isBookmarked = state.isBookmarked,
                            localShuffle = localShuffle,
                            onPlayPause = {
                                if (isCurrentPlaylistActive) {
                                    playerViewModel.playPause()
                                } else if (state.tracks.isNotEmpty()) {
                                    val playTracks = if (localShuffle) state.tracks.shuffled() else state.tracks
                                    playerViewModel.play(playTracks, 0, PlaylistSource.PlaylistAudio(ownerId, playlistId))
                                }
                            },
                            onToggleShuffle = { localShuffle = !localShuffle },
                            onToggleBookmark = { viewModel.toggleBookmark() }
                        )
                    }

                    if (state.tracks.isEmpty() && !state.isLoading) {
                        item {
                            Text(
                                text = "В этом плейлисте пока нет треков",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    itemsIndexed(state.tracks, key = { _, track -> "${track.ownerId}_${track.id}" }) { index, track ->
                        AudioTrackItem(
                            track = track,
                            onClick = { playerViewModel.play(state.tracks, index, PlaylistSource.PlaylistAudio(ownerId, playlistId)) },
                            onToggleAdded = { viewModel.toggleTrackAdded(track) },
                            onDownload = { musicViewModel.downloadTrack(track) },
                            onShare = {
                                val shareUrl = track.remoteUrl ?: track.url ?: ""
                                clipboardManager.setText(AnnotatedString(shareUrl))
                            },
                            onAddToQueue = { playerViewModel.playerManager.addToQueue(track) },
                            onPlayNext = { playerViewModel.playerManager.playNext(track) },
                            isSelected = track.stableId == currentTrack?.stableId,
                            isDownloaded = downloadedTracks.contains(track.stableId),
                            isOfflineMode = false,
                            onRemoveFromPlaylist = if (state.isOwner && experimentalFeatures) { { viewModel.removeTrack(track) } } else null
                        )
                        if (index < state.tracks.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            )
                        }
                    }

                    if (state.isOwner) {
                        item {
                            AddMoreButton(onClick = { showMusicPicker = true })
                        }
                    }

                    if (state.isLoadingMore) {
                        item {
                            LoadingBox(modifier = Modifier.padding(16.dp))
                        }
                    }
                    
                    // Extra padding for mini-player
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
    if (showEditDialog) {
        EditPlaylistDialog(
            initialTitle = state.playlist?.title ?: "",
            initialDescription = state.playlist?.description ?: "",
            onDismiss = { showEditDialog = false },
            onConfirm = { title, desc ->
                viewModel.editPlaylist(title, desc)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = "Удаление плейлиста",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                viewModel.deletePlaylist()
                showDeleteConfirm = false
            }
        )
    }

    if (showMusicPicker) {
        MusicPickerBottomSheet(
            onDismiss = { showMusicPicker = false },
            onTrackSelect = { track ->
                viewModel.addTrack(track)
                showMusicPicker = false
            }
        )
    }
}

@Composable
private fun AddMoreButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Добавить ещё")
    }
}

@Composable
private fun PlaylistHeader(
    title: String,
    description: String,
    photoUrl: String?,
    trackCount: Int,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    localShuffle: Boolean,
    onPlayPause: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleBookmark: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$trackCount треков",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPlayPause,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(28.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) "Пауза" else "Слушать")
                }
            }

            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (localShuffle) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Вперемешку",
                    tint = if (localShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isBookmarked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Сохранить",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
