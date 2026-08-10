package com.deliriousvoid.openvkmatcha.ui.screens.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Add
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.ui.components.AudioTrackItem
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.StateView
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.MusicNote
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.PlaylistItem
import com.deliriousvoid.openvkmatcha.ui.components.EditPlaylistDialog
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicMode
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PlayerViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onOpenPlaylist: (Int, Int, String) -> Unit,
    userId: Int? = null,
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = viewModel(factory = MusicViewModel.factory(userId)),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory()),
    isOfflineMode: Boolean = false,
) {
    val state by viewModel.uiState.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            OpenVKMatchaApp.instance.downloadRepository.refresh()
            viewModel.loadMusic(isRefresh = true)
        }
    }

    val hasPermission = remember(state.mode) {
        if (state.mode == MusicMode.Downloaded) {
            OpenVKMatchaApp.instance.downloadRepository.hasStoragePermission()
        } else true
    }

    LaunchedEffect(state.mode) {
        if (state.mode == MusicMode.Downloaded && !OpenVKMatchaApp.instance.downloadRepository.hasStoragePermission()) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(state.userProfile) {
        state.userProfile?.let {
            AppEvents.setCustomTitle("Музыка ${it.fullName}")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AppEvents.setCustomTitle(null)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { if (!isOfflineMode) viewModel.loadMusic(isRefresh = true, isManual = true) },
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isOfflineMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        FilterChip(
                            selected = state.mode == MusicMode.Tracks,
                            onClick = { viewModel.setMode(MusicMode.Tracks) },
                            label = { Text("Треки") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = state.mode == MusicMode.Playlists,
                            onClick = { viewModel.setMode(MusicMode.Playlists) },
                            label = { Text("Плейлисты") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = state.mode == MusicMode.Downloaded,
                            onClick = { viewModel.setMode(MusicMode.Downloaded) },
                            label = { Text("Скачанное") }
                        )
                    }
                }

                when {
                    state.isLoading && state.tracks.isEmpty() && state.playlists.isEmpty() && state.downloadedTracks.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
                    state.error != null && state.tracks.isEmpty() && state.playlists.isEmpty() && state.downloadedTracks.isEmpty() -> ErrorText(
                        message = state.error!!,
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { viewModel.loadMusic(isRefresh = true) }
                    )
                    state.mode == MusicMode.Tracks && state.tracks.isEmpty() && !state.isLoading -> {
                        if (state.searchQuery.isNotBlank()) {
                            StateView(icon = Icons.Default.Search, message = "Ничего не найдено", modifier = Modifier.fillMaxSize())
                        } else {
                            StateView(icon = Icons.Default.MusicNote, message = "Нет аудиозаписей", modifier = Modifier.fillMaxSize())
                        }
                    }
                    state.mode == MusicMode.Playlists && state.playlists.isEmpty() -> StateView(icon = Icons.Default.MusicNote, message = "Плейлисты не найдены", modifier = Modifier.fillMaxSize())
                    state.mode == MusicMode.Downloaded && state.downloadedTracks.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StateView(
                                icon = Icons.Default.GetApp,
                                message = if (hasPermission) "У вас пока нет скачанных треков" else "Нет доступа к памяти",
                                modifier = Modifier.wrapContentSize()
                            )
                            if (!hasPermission) {
                                Button(
                                    onClick = {
                                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            Manifest.permission.READ_MEDIA_AUDIO
                                        } else {
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        }
                                        permissionLauncher.launch(permission)
                                    },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Предоставить доступ")
                                }
                            }
                        }
                    }
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
                            if (state.mode == MusicMode.Tracks || state.mode == MusicMode.Downloaded) {
                                val tracksToDisplay = if (state.mode == MusicMode.Downloaded) state.downloadedTracks else state.tracks
                                itemsIndexed(tracksToDisplay, key = { _, track -> "${track.ownerId}_${track.id}" }) { index, track ->
                                    AudioTrackItem(
                                        track = track,
                                        onClick = { 
                                            val source = when (state.mode) {
                                                MusicMode.Tracks -> {
                                                    if (state.searchQuery.isNotBlank()) {
                                                        PlaylistSource.SearchAudio(state.searchQuery)
                                                    } else {
                                                        PlaylistSource.UserAudio(state.currentUserId ?: 0)
                                                    }
                                                }
                                                MusicMode.Downloaded -> PlaylistSource.LocalAudio(tracksToDisplay)
                                                else -> PlaylistSource.Unknown
                                            }
                                            playerViewModel.play(tracksToDisplay, index, source) 
                                        },
                                        onToggleAdded = { viewModel.toggleTrackAdded(track) },
                                        onDownload = { viewModel.downloadTrack(track) },
                                        onShare = {
                                            val shareUrl = track.remoteUrl ?: track.url ?: ""
                                            clipboardManager.setText(AnnotatedString(shareUrl))
                                        },
                                        onAddToQueue = { viewModel.addToQueue(track) },
                                        onPlayNext = { viewModel.playNext(track) },
                                        isSelected = track.stableId == currentTrack?.stableId,
                                        isDownloaded = track.url?.startsWith("/") == true || downloadedTracks.contains(track.stableId),
                                        isOfflineMode = state.mode == MusicMode.Downloaded
                                    )
                                    if (index < tracksToDisplay.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 76.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        )
                                    }
                                }
                            } else {
                                itemsIndexed(state.playlists, key = { _, playlist -> "${playlist.ownerId}_${playlist.id}" }) { index, playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        onClick = { onOpenPlaylist(playlist.ownerId, playlist.id, playlist.title) }
                                    )
                                    if (index < state.playlists.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 96.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        )
                                    }
                                }
                            }

                            if (state.isLoadingMore) {
                                item {
                                    LoadingBox(modifier = Modifier.padding(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
