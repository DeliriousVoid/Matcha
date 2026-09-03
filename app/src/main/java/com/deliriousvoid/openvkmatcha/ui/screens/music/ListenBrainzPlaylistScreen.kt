package com.deliriousvoid.openvkmatcha.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.ui.components.AudioTrackItem
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ListenBrainzPlaylistViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PlayerViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicActionsViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.statusBarsPadding
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource

@Composable
fun ListenBrainzPlaylistScreen(
    mbid: String,
    title: String,
    thumbUrl: String? = null,
    viewModel: ListenBrainzPlaylistViewModel = viewModel(factory = ListenBrainzPlaylistViewModel.factory(mbid)),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory()),
    musicActionsViewModel: MusicActionsViewModel = viewModel(factory = MusicActionsViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    val trackStates by musicActionsViewModel.trackStates.collectAsState()
    val downloadedTracks by musicActionsViewModel.downloadedTracks.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val toolbarAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else {
                val threshold = with(density) { 200.dp.toPx() }
                (listState.firstVisibleItemScrollOffset.toFloat() / threshold).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(title, toolbarAlpha) {
        AppEvents.setTopBarState(
            TopBarState(
                title = title,
                isTransparent = true,
                alpha = toolbarAlpha,
                route = com.deliriousvoid.openvkmatcha.ui.navigation.Routes.LISTENBRAINZ_PLAYLIST,
            )
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.tracks.isEmpty() -> {
                LoadingBox(modifier = Modifier.fillMaxSize())
            }
            (state.error != null) && state.tracks.isEmpty() -> {
                ErrorText(
                    message = state.error!!,
                    modifier = Modifier.fillMaxSize(),
                    onRetry = { viewModel.loadTracks() }
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
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
                                .statusBarsPadding()
                                .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumbUrl != null) {
                                    AsyncImage(
                                        model = thumbUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ListenBrainz Recommendations",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (state.isResolving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    IconButton(
                                        onClick = { viewModel.refreshMatches() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = matchaStringResource(R.string.action_refresh),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.isResolving) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(2.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    itemsIndexed(state.tracks) { index, track ->
                        val displayTrack = trackStates[track.stableId] ?: track
                        val isResolved = displayTrack.url != null
                        val isNotFound = displayTrack.accessKey == "not_found"
                        
                        val alpha = when {
                            isResolved -> 1f
                            isNotFound -> 0.2f
                            else -> 0.5f
                        }

                        AudioTrackItem(
                            track = displayTrack,
                            onClick = { viewModel.resolveAndPlay(index, playerViewModel) },
                            onToggleAdded = { musicActionsViewModel.toggleTrackAdded(displayTrack) }, 
                            onDownload = { musicActionsViewModel.downloadTrack(displayTrack) },    
                            onShare = { displayTrack.url?.let { clipboardManager.setText(AnnotatedString(it)) } },
                            onAddToQueue = { musicActionsViewModel.addToQueue(displayTrack) },
                            onPlayNext = { musicActionsViewModel.playNext(displayTrack) },
                            isSelected = displayTrack.artist == currentTrack?.artist && displayTrack.title == currentTrack?.title,
                            isDownloaded = downloadedTracks.contains(displayTrack.stableId),
                            isOfflineMode = false,
                            modifier = Modifier.alpha(alpha)
                        )
                        if (index < state.tracks.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
