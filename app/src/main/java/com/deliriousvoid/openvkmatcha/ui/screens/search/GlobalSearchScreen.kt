package com.deliriousvoid.openvkmatcha.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.Playlist
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.components.*
import com.deliriousvoid.openvkmatcha.ui.viewmodel.GlobalSearchViewModel
import com.deliriousvoid.openvkmatcha.util.SearchCategory
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun GlobalSearchScreen(
    onOpenProfile: (Int) -> Unit,
    onOpenPlaylist: (Int, Int, String) -> Unit,
    onPlayTrack: (List<AudioTrack>, Int) -> Unit,
    onToggleTrackAdded: (AudioTrack) -> Unit,
    onDownloadTrack: (AudioTrack) -> Unit,
    onShareTrack: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onPlayNext: (AudioTrack) -> Unit,
    currentTrackId: Int?,
    downloadedTracks: Set<String>,
    viewModel: GlobalSearchViewModel = viewModel(factory = GlobalSearchViewModel.factory())
) {
    val state by viewModel.uiState.collectAsState()
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

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.results.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
            state.error != null && state.results.isEmpty() -> ErrorText(message = state.error!!, modifier = Modifier.fillMaxSize())
            state.results.isEmpty() && state.searchQuery.isNotBlank() -> EmptyState(message = "Ничего не найдено", modifier = Modifier.fillMaxSize())
            state.results.isEmpty() -> EmptyState(message = "Введите запрос для поиска", modifier = Modifier.fillMaxSize())
            else -> {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(state.results) { item ->
                        when (item) {
                            is UserProfile -> {
                                UserListItem(
                                    user = item,
                                    onClick = { onOpenProfile(item.id) }
                                )
                            }
                            is AudioTrack -> {
                                AudioTrackItem(
                                    track = item,
                                    onClick = { 
                                        val tracks = state.results.filterIsInstance<AudioTrack>()
                                        val index = tracks.indexOf(item)
                                        onPlayTrack(tracks, index)
                                    },
                                    onToggleAdded = { onToggleTrackAdded(item) },
                                    onDownload = { onDownloadTrack(item) },
                                    onShare = { onShareTrack(item) },
                                    onAddToQueue = { onAddToQueue(item) },
                                    onPlayNext = { onPlayNext(item) },
                                    isSelected = item.id == currentTrackId,
                                    isDownloaded = downloadedTracks.contains(item.stableId)
                                )
                            }
                            is Playlist -> {
                                PlaylistItem(
                                    playlist = item,
                                    onClick = { onOpenPlaylist(item.ownerId, item.id, item.title) }
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = if (item is UserProfile || item is AudioTrack) 76.dp else 92.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                    
                    if (state.isLoadingMore) {
                        item {
                            LoadingBox(modifier = Modifier.fillMaxWidth().padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
