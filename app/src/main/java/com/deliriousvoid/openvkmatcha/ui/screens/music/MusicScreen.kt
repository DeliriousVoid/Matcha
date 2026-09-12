package com.deliriousvoid.openvkmatcha.ui.screens.music

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.components.*
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicMode
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PlayerViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.SearchCategory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.media3.common.Player
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.Manifest
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onOpenPlaylist: (Int, Int, String) -> Unit,
    onOpenListenBrainzPlaylist: (String, String, String?) -> Unit = { _, _, _ -> },
    userId: Int? = null,
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = viewModel(factory = MusicViewModel.factory(userId)),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory()),
    isOfflineMode: Boolean = false,
) {
    val state by viewModel.uiState.collectAsState()
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (granted) {
            viewModel.updateStoragePermission()
        }
    }
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val listenBrainzEnabled by viewModel.listenBrainzEnabled.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val playbackSource by playerViewModel.playlistSource.collectAsState()
    val shuffleMode by playerViewModel.shuffleMode.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()

    val isMainMusicPlaying by remember(isPlaying, playbackSource, state.currentUserId) {
        derivedStateOf {
            isPlaying && when (val source = playbackSource) {
                is PlaylistSource.UserAudio -> source.userId == (state.currentUserId ?: 0)
                else -> false
            }
        }
    }

    val mainListState = rememberLazyListState()
    val otherListState = rememberLazyListState()
    val density = androidx.compose.ui.platform.LocalDensity.current

    BackHandler(state.mode != MusicMode.Main) {
        viewModel.setMode(MusicMode.Main)
    }
    
    val toolbarAlpha by remember(state.mode) {
        derivedStateOf {
            val listState = if (state.mode == MusicMode.Main) mainListState else otherListState
            if (listState.firstVisibleItemIndex > 0) 1f
            else {
                val threshold = with(density) { 200.dp.toPx() }
                (listState.firstVisibleItemScrollOffset.toFloat() / threshold).coerceIn(0f, 1f)
            }
        }
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = otherListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex > totalItems - 5 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && state.mode != MusicMode.Main) {
            viewModel.loadMore()
        }
    }

    LaunchedEffect(state.mode, toolbarAlpha, userId) {
        if (state.mode == MusicMode.Main && userId == null) {
            AppEvents.setTopBarTransparent(true)
            AppEvents.setTopBarAlpha(toolbarAlpha)
        } else {
            AppEvents.setTopBarTransparent(false)
            AppEvents.setTopBarAlpha(1f)
        }
    }

    val tracksTitle = matchaStringResource(R.string.music_tracks)
    val playlistsTitle = matchaStringResource(R.string.music_playlists)
    val downloadedTitle = matchaStringResource(R.string.music_downloaded)
    val recommendationsTitle = matchaStringResource(R.string.music_recommendations)

    LaunchedEffect(state.mode) {
        val title = when (state.mode) {
            MusicMode.Tracks -> tracksTitle
            MusicMode.Playlists -> playlistsTitle
            MusicMode.Downloaded -> downloadedTitle
            MusicMode.Recommendations -> recommendationsTitle
            else -> null
        }
        AppEvents.setCustomTitle(title)
        
        // Update search category based on mode
        val newCategory = when (state.mode) {
            MusicMode.Main -> SearchCategory.MUSIC
            MusicMode.Tracks -> SearchCategory.MY_MUSIC
            MusicMode.Playlists -> SearchCategory.PLAYLISTS
            MusicMode.Downloaded -> SearchCategory.DOWNLOADED
            else -> null
        }
        if (newCategory != null) {
            AppEvents.setSearchCategory(newCategory)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AppEvents.setTopBarTransparent(false)
            AppEvents.setTopBarAlpha(1f)
            AppEvents.setCustomTitle(null)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { if (!isOfflineMode) viewModel.loadMusic(isRefresh = true, isManual = true) },
        modifier = modifier.fillMaxSize()
    ) {
        key(state.mode) {
            if (state.mode == MusicMode.Main) {
                MusicMainContent(
                    tracks = state.tracks,
                    playlists = state.playlists,
                    downloadedTracks = state.downloadedTracks,
                    recommendations = state.recommendations,
                    hasStoragePermission = state.hasStoragePermission,
                    isPlaying = isMainMusicPlaying,
                    shuffleMode = shuffleMode,
                    repeatMode = repeatMode,
                    onPlayTrack = { track ->
                        val currentSource = PlaylistSource.UserAudio(state.currentUserId ?: 0)
                        val isCurrentSourceActive = when (val source = playbackSource) {
                            is PlaylistSource.UserAudio -> source.userId == (state.currentUserId ?: 0)
                            else -> false
                        }
                        if (isCurrentSourceActive && currentTrack?.stableId == track.stableId) {
                            playerViewModel.playPause()
                        } else {
                            playerViewModel.play(state.tracks, state.tracks.indexOf(track), currentSource)
                        }
                    },
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenListenBrainzPlaylist = onOpenListenBrainzPlaylist,
                    onOpenFullMusic = { viewModel.setMode(MusicMode.Tracks) },
                    onOpenFullPlaylists = { viewModel.setMode(MusicMode.Playlists) },
                    onOpenFullDownloaded = { viewModel.setMode(MusicMode.Downloaded) },
                    onOpenFullRecommendations = { viewModel.setMode(MusicMode.Recommendations) },
                    onRequestStoragePermission = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        storagePermissionLauncher.launch(permissions)
                    },
                    listenBrainzEnabled = listenBrainzEnabled,
                    onPlayAll = {
                        val currentSource = PlaylistSource.UserAudio(state.currentUserId ?: 0)
                        val isCurrentSourceActive = when (val source = playbackSource) {
                            is PlaylistSource.UserAudio -> source.userId == (state.currentUserId ?: 0)
                            else -> false
                        }
                        if (isCurrentSourceActive && currentTrack != null) {
                            playerViewModel.playPause()
                        } else if (state.tracks.isNotEmpty()) {
                            playerViewModel.play(state.tracks, 0, currentSource)
                        }
                    },
                    onToggleTrackAdded = { viewModel.toggleTrackAdded(it) },
                    onDownloadTrack = { viewModel.downloadTrack(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onPlayNext = { viewModel.playNext(it) },
                    onToggleShuffle = { playerViewModel.toggleShuffle() },
                    onToggleRepeat = { playerViewModel.toggleRepeat() },
                    isLoading = state.isLoading,
                    listState = mainListState,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        when {
                            state.isLoading && state.tracks.isEmpty() && state.playlists.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
                            state.error != null && state.tracks.isEmpty() -> ErrorText(message = state.error!!, modifier = Modifier.fillMaxSize(), onRetry = { viewModel.loadMusic(isRefresh = true) })
                            else -> {
                                val clipboardManager = LocalClipboardManager.current
                                LazyColumn(state = otherListState, modifier = Modifier.fillMaxSize()) {
                                    if (state.mode == MusicMode.Tracks || state.mode == MusicMode.Downloaded) {
                                        val tracks = if (state.mode == MusicMode.Downloaded) state.downloadedTracks else state.tracks
                                        if (tracks.isEmpty() && !state.isLoading) {
                                            item { StateView(icon = Icons.Default.MusicNote, message = matchaStringResource(if (state.searchQuery.isBlank()) R.string.music_empty else R.string.music_not_found)) }
                                        } else {
                                            itemsIndexed(tracks, key = { _, track -> "${track.ownerId}_${track.id}" }) { index, track ->
                                                AudioTrackItem(
                                                    track = track,
                                                    onClick = {
                                                        val source = if (state.mode == MusicMode.Downloaded) PlaylistSource.LocalAudio(tracks) else PlaylistSource.UserAudio(state.currentUserId ?: 0)
                                                        playerViewModel.play(tracks, index, source)
                                                    },
                                                    onToggleAdded = { viewModel.toggleTrackAdded(track) },
                                                    onDownload = { viewModel.downloadTrack(track) },
                                                    onShare = { track.url?.let { clipboardManager.setText(AnnotatedString(it)) } },
                                                    onAddToQueue = { viewModel.addToQueue(track) },
                                                    onPlayNext = { viewModel.playNext(track) },
                                                    isSelected = track.stableId == currentTrack?.stableId,
                                                    isDownloaded = downloadedTracks.contains(track.stableId),
                                                    isOfflineMode = state.mode == MusicMode.Downloaded
                                                )
                                                if (index < tracks.size - 1) HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                            }
                                        }
                                    } else {
                                        val playlists = if (state.mode == MusicMode.Recommendations) state.recommendations else state.playlists
                                        if (playlists.isEmpty() && !state.isLoading) {
                                            item { StateView(icon = Icons.Default.MusicNote, message = matchaStringResource(R.string.music_playlists_not_found)) }
                                        } else {
                                            itemsIndexed(playlists, key = { _, p -> p.id }) { index, p ->
                                                PlaylistItem(playlist = p, onClick = { if (state.mode == MusicMode.Recommendations) onOpenListenBrainzPlaylist(p.mbid ?: "", p.title, p.photoUrl) else onOpenPlaylist(p.ownerId, p.id, p.title) })
                                                if (index < playlists.size - 1) HorizontalDivider(modifier = Modifier.padding(start = 96.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                    item { Spacer(modifier = Modifier.height(80.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicMainContent(
    tracks: List<AudioTrack>,
    playlists: List<com.deliriousvoid.openvkmatcha.data.model.Playlist>,
    downloadedTracks: List<AudioTrack>,
    recommendations: List<com.deliriousvoid.openvkmatcha.data.model.Playlist>,
    hasStoragePermission: Boolean,
    isPlaying: Boolean,
    shuffleMode: Boolean,
    repeatMode: Int,
    onPlayTrack: (AudioTrack) -> Unit,
    onOpenPlaylist: (Int, Int, String) -> Unit,
    onOpenListenBrainzPlaylist: (String, String, String?) -> Unit,
    onOpenFullMusic: () -> Unit,
    onOpenFullPlaylists: () -> Unit,
    onOpenFullDownloaded: () -> Unit,
    onOpenFullRecommendations: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onPlayAll: () -> Unit,
    onToggleTrackAdded: (AudioTrack) -> Unit,
    onDownloadTrack: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onPlayNext: (AudioTrack) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    isLoading: Boolean,
    listState: LazyListState,
    listenBrainzEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(450.dp).background(Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), Color.Transparent))).blur(80.dp))
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Spacer(modifier = Modifier.height(statusBarHeight + 64.dp))
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onPlayAll, modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
                        Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onToggleShuffle)) {
                        Icon(Icons.Default.Shuffle, null, tint = if (shuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(matchaStringResource(R.string.shuffle), style = MaterialTheme.typography.labelSmall, color = if (shuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onToggleRepeat)) {
                        Icon(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(matchaStringResource(R.string.desc_repeat), style = MaterialTheme.typography.labelSmall, color = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            item {
                val clipboardManager = LocalClipboardManager.current
                MusicMainHeader(matchaStringResource(R.string.music_my_music), onOpenFullMusic)
                if (tracks.isNotEmpty()) {
                    MusicHorizontalSection(tracks.size.coerceAtMost(15), 3) { index ->
                        AudioTrackItem(
                            track = tracks[index],
                            onClick = { onPlayTrack(tracks[index]) },
                            onToggleAdded = { onToggleTrackAdded(tracks[index]) },
                            onDownload = { onDownloadTrack(tracks[index]) },
                            onShare = { tracks[index].url?.let { clipboardManager.setText(AnnotatedString(it)) } },
                            onAddToQueue = { onAddToQueue(tracks[index]) },
                            onPlayNext = { onPlayNext(tracks[index]) },
                            isOfflineMode = false
                        )
                    }
                } else if (isLoading) LoadingBox(Modifier.height(100.dp))
                else Text(matchaStringResource(R.string.music_empty), modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                MusicMainHeader(matchaStringResource(R.string.music_playlists), onOpenFullPlaylists)
                if (playlists.isNotEmpty()) {
                    MusicHorizontalSection(playlists.size.coerceAtMost(15), 3) { index ->
                        PlaylistItem(playlist = playlists[index], onClick = { onOpenPlaylist(playlists[index].ownerId, playlists[index].id, playlists[index].title) })
                    }
                } else if (isLoading) LoadingBox(Modifier.height(100.dp))
                else Text(matchaStringResource(R.string.music_playlists_not_found), modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                MusicMainHeader(matchaStringResource(R.string.music_downloaded), onOpenFullDownloaded)
                if (!hasStoragePermission) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Button(onClick = onRequestStoragePermission) {
                            Text(matchaStringResource(R.string.music_grant_permission))
                        }
                    }
                } else {
                    if (downloadedTracks.isNotEmpty()) {
                        val clipboardManager = LocalClipboardManager.current
                        MusicHorizontalSection(downloadedTracks.size.coerceAtMost(15), 3) { index ->
                            AudioTrackItem(
                                track = downloadedTracks[index],
                                onClick = { onPlayTrack(downloadedTracks[index]) },
                                onToggleAdded = { onToggleTrackAdded(downloadedTracks[index]) },
                                onDownload = { onDownloadTrack(downloadedTracks[index]) },
                                onShare = { downloadedTracks[index].url?.let { clipboardManager.setText(AnnotatedString(it)) } },
                                onAddToQueue = { onAddToQueue(downloadedTracks[index]) },
                                onPlayNext = { onPlayNext(downloadedTracks[index]) },
                                isOfflineMode = true
                            )
                        }
                    } else if (isLoading) LoadingBox(Modifier.height(100.dp))
                    else Text(matchaStringResource(R.string.music_no_downloaded), modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (listenBrainzEnabled) {
                item {
                    MusicMainHeader(matchaStringResource(R.string.music_recommendations), onOpenFullRecommendations)
                    if (recommendations.isNotEmpty()) {
                        MusicHorizontalSection(recommendations.size.coerceAtMost(15), 3) { index ->
                            PlaylistItem(playlist = recommendations[index], onClick = { onOpenListenBrainzPlaylist(recommendations[index].mbid ?: "", recommendations[index].title, recommendations[index].photoUrl) })
                        }
                    } else if (isLoading) LoadingBox(Modifier.height(100.dp))
                    else Text(matchaStringResource(R.string.music_playlists_not_found), modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun MusicMainHeader(title: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onClick, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicHorizontalSection(total: Int, itemsPerPage: Int, content: @Composable (Int) -> Unit) {
    val pageCount = (total + itemsPerPage - 1) / itemsPerPage
    val pagerState = rememberPagerState(pageCount = { pageCount })
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 24.dp), pageSpacing = 16.dp) { page ->
        Column {
            for (i in 0 until itemsPerPage) {
                val index = page * itemsPerPage + i
                if (index < total) content(index)
            }
        }
    }
}
