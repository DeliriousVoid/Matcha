package com.deliriousvoid.openvkmatcha.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource
import com.deliriousvoid.openvkmatcha.data.model.Post
import com.deliriousvoid.openvkmatcha.ui.components.DeleteConfirmationDialog
import com.deliriousvoid.openvkmatcha.ui.components.EditPostDialog
import com.deliriousvoid.openvkmatcha.ui.components.EmptyState
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.PostCard
import com.deliriousvoid.openvkmatcha.ui.components.ReportDialog
import com.deliriousvoid.openvkmatcha.ui.components.RepostBottomSheet
import com.deliriousvoid.openvkmatcha.ui.util.LinkHandler
import com.deliriousvoid.openvkmatcha.ui.viewmodel.FeedViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicActionsViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PlayerViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.RepostViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel
import com.deliriousvoid.openvkmatcha.util.downloadDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FeedViewModel,
    onOpenProfile: (Any) -> Unit,
    onOpenComments: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenPlaylist: (Int, Int, String) -> Unit = { _, _, _ -> },
    onOpenMusic: (Int, String) -> Unit = { _, _ -> },
    musicActionsViewModel: MusicActionsViewModel = viewModel(factory = MusicActionsViewModel.factory()),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory()),
    repostViewModel: RepostViewModel = viewModel(factory = RepostViewModel.factory()),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    val musicActionsState by musicActionsViewModel.trackStates.collectAsState()
    val downloadedTracks by musicActionsViewModel.downloadedTracks.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val doubleTapToLike by settingsViewModel.doubleTapToLike.collectAsState()
    val doubleTapTimeout by settingsViewModel.doubleTapTimeout.collectAsState()
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var sharingPost by remember { mutableStateOf<Post?>(null) }
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    var reportingPost by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(state.posts) {
        val allAudios = state.posts.flatMap { it.audios }
        if (allAudios.isNotEmpty()) {
            musicActionsViewModel.loadArtworks(allAudios)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.loadFeed(refresh = true, isManual = true) },
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading -> LoadingBox(modifier = Modifier.fillMaxSize())
            (state.error != null && state.posts.isEmpty()) -> ErrorText(
                message = state.error!!,
                modifier = Modifier.fillMaxSize(),
            ) {
                viewModel.loadFeed(refresh = true, isManual = true)
            }
            state.posts.isEmpty() -> EmptyState(
                message = "Ваша лента пока пуста",
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                val listState = rememberLazyListState()
                
                LaunchedEffect(Unit) {
                    viewModel.scrollToTop.collect {
                        listState.animateScrollToItem(0)
                    }
                }

                val shouldLoadMore = remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        lastVisibleItem > totalItems - 5 && totalItems > 0
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
                    items(state.posts, key = { "${it.ownerId}_${it.id}" }) { post ->
                        PostCard(
                            post = post,
                            onLikeClick = { viewModel.toggleLike(post) },
                            onCommentClick = onOpenComments,
                            onRepostClick = { sharingPost = post },
                            onAuthorClick = onOpenProfile,
                            onAudioClick = { tracks, index -> playerViewModel.play(tracks, index, PlaylistSource.LocalAudio(tracks)) },
                            onAudioToggleAdded = { musicActionsViewModel.toggleTrackAdded(it) },
                            onAudioDownload = { musicActionsViewModel.downloadTrack(it) },
                            onAudioShare = {
                                val url = it.remoteUrl ?: it.url ?: ""
                                if (url.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(url))
                                }
                            },
                            onAudioAddToQueue = { musicActionsViewModel.addToQueue(it) },
                            onAudioPlayNext = { musicActionsViewModel.playNext(it) },
                            onDocumentDownload = { downloadDocument(it) },
                            onMusicClick = { onOpenMusic(it, "") },
                            onPlaylistClick = { ownerId, playlistId -> onOpenPlaylist(ownerId, playlistId, "") },
                            currentTrack = currentTrack,
                            isDownloaded = { id, ownerId -> downloadedTracks.contains("${ownerId}_$id") },
                            getTrackState = { track -> musicActionsState[track.stableId] ?: track },
                            currentUserId = state.currentUserId,
                            onEditClick = { editingPost = it },
                            onDeleteClick = { postToDelete = it },
                            onPinClick = { viewModel.pinPost(it) },
                            onUnpinClick = { viewModel.unpinPost(it) },
                            onReportClick = { reportingPost = it },
                            onPollVote = { post, answerIds -> viewModel.votePoll(post, answerIds) },
                            onGeoClick = { LinkHandler.handleGeo(context, it) },
                            doubleTapEnabled = doubleTapToLike,
                            doubleTapTimeout = doubleTapTimeout
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        )
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

    sharingPost?.let { post ->
        RepostBottomSheet(
            post = post,
            onDismiss = { sharingPost = null },
            viewModel = repostViewModel
        )
    }

    editingPost?.let { post ->
        EditPostDialog(
            initialText = post.text,
            isGroup = post.ownerId < 0,
            initialIsNsfw = post.isNsfw,
            onDismiss = { editingPost = null },
            onConfirm = { newText, fromGroup, isNsfw ->
                viewModel.editPost(post, newText, fromGroup, isNsfw)
                editingPost = null
            }
        )
    }

    postToDelete?.let { post ->
        DeleteConfirmationDialog(
            title = stringResource(R.string.post_delete_confirm),
            onDismiss = { postToDelete = null },
            onConfirm = { viewModel.deletePost(post) }
        )
    }

    reportingPost?.let { post ->
        ReportDialog(
            onDismiss = { reportingPost = null },
            onConfirm = { comment ->
                viewModel.report("post", post.ownerId, post.id, comment)
                reportingPost = null
            }
        )
    }
}
