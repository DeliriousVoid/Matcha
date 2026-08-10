package com.deliriousvoid.openvkmatcha.ui.screens.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.TopicComment
import com.deliriousvoid.openvkmatcha.ui.components.EmptyState
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.CommentAttachmentsList
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource
import com.deliriousvoid.openvkmatcha.data.model.Document
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicActionsViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PlayerViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.BoardViewModel
import com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import com.deliriousvoid.openvkmatcha.data.model.PendingAttachment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Description
import com.deliriousvoid.openvkmatcha.ui.screens.comments.PendingAttachmentItem
import com.deliriousvoid.openvkmatcha.ui.components.MusicPickerBottomSheet
import com.deliriousvoid.openvkmatcha.ui.components.VideoPickerBottomSheet
import com.deliriousvoid.openvkmatcha.ui.components.DocsPickerBottomSheet
import com.deliriousvoid.openvkmatcha.ui.components.ReportDialog
import com.deliriousvoid.openvkmatcha.ui.components.ParsedText
import com.deliriousvoid.openvkmatcha.ui.components.ImageViewer
import com.deliriousvoid.openvkmatcha.util.downloadDocument
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCommentsScreen(
    groupId: Int,
    topicId: Int,
    title: String,
    vidGuess: Int? = null,
    onOpenProfile: (Any) -> Unit,
    onOpenPlaylist: (Int, Int, String) -> Unit = { _, _, _ -> },
    onOpenComments: (Int, Int) -> Unit = { _, _ -> },
    onOpenMusic: (Int, String) -> Unit = { _, _ -> },
    viewModel: BoardViewModel = viewModel(factory = BoardViewModel.factory(groupId, topicId, vidGuess)),
    musicActionsViewModel: MusicActionsViewModel = viewModel(factory = MusicActionsViewModel.factory()),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val musicActionsState by musicActionsViewModel.trackStates.collectAsState()
    val downloadedTracks by musicActionsViewModel.downloadedTracks.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    var showMusicPicker by remember { mutableStateOf(false) }
    var showVideoPicker by remember { mutableStateOf(false) }
    var showDocsPicker by remember { mutableStateOf(false) }
    var reportingComment by remember { mutableStateOf<TopicComment?>(null) }
    var viewerInitialIndex by remember { mutableIntStateOf(-1) }
    var activeCommentForViewer by remember { mutableStateOf<TopicComment?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it, AttachmentType.PHOTO) }
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

    Scaffold(
        bottomBar = {
            CommentInput(
                text = state.inputText,
                isSending = state.isSending,
                isAdmin = state.isAdmin,
                fromGroup = state.fromGroup,
                isDeveloperMode = state.isDeveloperMode,
                pendingAttachments = state.pendingAttachments,
                onTextChange = viewModel::updateInput,
                onFromGroupChange = viewModel::setFromGroup,
                onRemoveAttachment = viewModel::removeAttachment,
                onAddPhoto = { photoPicker.launch("image/*") },
                onAddVideo = { showVideoPicker = true },
                onAddAudio = { showMusicPicker = true },
                onAddDoc = { showDocsPicker = true },
                onSend = { viewModel.postComment(state.inputText) }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.loadComments(refresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isResolvingVid -> LoadingBox(modifier = Modifier.fillMaxSize())
                state.isLoading && state.comments.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
                state.error != null && state.comments.isEmpty() -> ErrorText(
                    message = state.error!!,
                    modifier = Modifier.fillMaxSize(),
                    onRetry = { viewModel.loadComments() }
                )
                state.comments.isEmpty() -> EmptyState(
                    message = "В этой теме пока нет комментариев",
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.comments) { comment ->
                            CommentItem(
                                comment = comment,
                                onAuthorClick = { onOpenProfile(comment.fromId) },
                                onMentionClick = { onOpenProfile(it) },
                                onProfileClick = { onOpenProfile(it) },
                                onWallClick = onOpenComments,
                                onMusicClick = { onOpenMusic(it, "") },
                                onPlaylistClick = { oId, pId -> onOpenPlaylist(oId, pId, "") },
                                onReplyClick = { viewModel.replyTo(comment) },
                                onReportClick = { reportingComment = comment },
                                onLikeClick = { viewModel.toggleLike(comment) },
                                onAudioClick = { tracks, index -> playerViewModel.play(tracks, index, PlaylistSource.LocalAudio(tracks)) },
                                onAudioToggleAdded = { musicActionsViewModel.toggleTrackAdded(it) },
                                onDownload = { musicActionsViewModel.downloadTrack(it) },
                                onShare = { /* Handle share */ },
                                onAddToQueue = { musicActionsViewModel.addToQueue(it) },
                                onPlayNext = { musicActionsViewModel.playNext(it) },
                                currentTrack = currentTrack,
                                isDownloaded = { id, ownerId -> musicActionsViewModel.isDownloaded(id, ownerId) },
                                getTrackState = { musicActionsViewModel.getTrack(it) },
                                onDocumentDownload = { downloadDocument(it) },
                                onPollVote = { answers -> viewModel.votePoll(comment, answers) },
                                onImageClick = {
                                    activeCommentForViewer = comment
                                    viewerInitialIndex = it
                                }
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
    }

    if (showMusicPicker) {
        MusicPickerBottomSheet(
            onDismiss = { showMusicPicker = false },
            onTrackSelect = { track ->
                viewModel.addExistingAttachment(
                    attachmentString = "audio${track.ownerId}_${track.id}",
                    type = AttachmentType.AUDIO,
                    name = "${track.artist} - ${track.title}"
                )
                showMusicPicker = false
            }
        )
    }

    if (showVideoPicker) {
        VideoPickerBottomSheet(
            onDismiss = { showVideoPicker = false },
            onVideoSelect = { video ->
                viewModel.addExistingAttachment(
                    attachmentString = "video${video.ownerId}_${video.id}",
                    type = AttachmentType.VIDEO,
                    name = video.title
                )
                showVideoPicker = false
            }
        )
    }

    if (showDocsPicker) {
        DocsPickerBottomSheet(
            onDismiss = { showDocsPicker = false },
            onDocSelect = { doc ->
                viewModel.addExistingAttachment(
                    attachmentString = "document${doc.ownerId}_${doc.id}",
                    type = AttachmentType.DOCUMENT,
                    name = doc.title
                )
                showDocsPicker = false
            }
        )
    }

    reportingComment?.let { comment ->
        ReportDialog(
            onDismiss = { reportingComment = null },
            onConfirm = { text ->
                viewModel.report("comment", comment.ownerId, comment.id, text)
                reportingComment = null
            }
        )
    }

    if (viewerInitialIndex != -1 && activeCommentForViewer != null) {
        val comment = activeCommentForViewer!!
        val gifDocs = comment.documents.filter { it.ext.lowercase() == "gif" || it.type == 3 }
        val allImageUrls = comment.imageUrls + gifDocs.mapNotNull { it.url }

        ImageViewer(
            imageUrls = allImageUrls,
            initialIndex = viewerInitialIndex,
            onDismiss = {
                viewerInitialIndex = -1
                activeCommentForViewer = null
            }
        )
    }
}

@Composable
private fun CommentItem(
    comment: TopicComment,
    onAuthorClick: () -> Unit,
    onMentionClick: (Int) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onWallClick: (Int, Int) -> Unit = { _, _ -> },
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onReplyClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onAudioClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onAudioToggleAdded: (AudioTrack) -> Unit = {},
    onDownload: (AudioTrack) -> Unit = {},
    onShare: (AudioTrack) -> Unit = {},
    onAddToQueue: (AudioTrack) -> Unit = {},
    onPlayNext: (AudioTrack) -> Unit = {},
    currentTrack: AudioTrack? = null,
    isDownloaded: (Int, Int) -> Boolean = { _, _ -> false },
    getTrackState: (AudioTrack) -> AudioTrack = { it },
    onDocumentDownload: (Document) -> Unit = {},
    onPollVote: (List<Int>) -> Unit = {},
    onImageClick: (Int) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        AsyncImage(
            model = comment.authorAvatar,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onAuthorClick() },
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onAuthorClick)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimeAgo(comment.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Копировать текст") },
                            onClick = {
                                showMenu = false
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(comment.text))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Пожаловаться") },
                            onClick = { showMenu = false; onReportClick() }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            SelectionContainer {
                ParsedText(
                    text = comment.text,
                    onMentionClick = onMentionClick,
                    onProfileClick = onProfileClick,
                    onWallClick = onWallClick,
                    onMusicClick = onMusicClick,
                    onPlaylistClick = onPlaylistClick,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            CommentAttachmentsList(
                imageUrls = comment.imageUrls,
                videos = comment.videos,
                audios = comment.audios,
                documents = comment.documents,
                poll = comment.poll,
                onAudioClick = onAudioClick,
                onAudioToggleAdded = onAudioToggleAdded,
                onAudioDownload = onDownload,
                onAudioShare = onShare,
                onAudioAddToQueue = onAddToQueue,
                onAudioPlayNext = onPlayNext,
                currentTrack = currentTrack,
                isDownloaded = isDownloaded,
                getTrackState = getTrackState,
                onDocumentDownload = onDocumentDownload,
                onPollVote = onPollVote,
                onImageClick = onImageClick
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.clickable { onLikeClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    if (comment.likeCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comment.likeCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Ответить",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onReplyClick() }
                )
            }
        }
    }
}

@Composable
private fun CommentInput(
    text: String,
    isSending: Boolean,
    isAdmin: Boolean = false,
    fromGroup: Boolean = false,
    isDeveloperMode: Boolean = false,
    pendingAttachments: List<PendingAttachment> = emptyList(),
    onTextChange: (String) -> Unit,
    onFromGroupChange: (Boolean) -> Unit = {},
    onRemoveAttachment: (PendingAttachment) -> Unit,
    onAddPhoto: () -> Unit,
    onAddVideo: () -> Unit,
    onAddAudio: () -> Unit,
    onAddDoc: () -> Unit,
    onSend: () -> Unit
) {
    var showAttachMenu by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFromGroupChange(!fromGroup) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = fromGroup,
                        onCheckedChange = { onFromGroupChange(it) },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "От имени сообщества",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (pendingAttachments.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(pendingAttachments) { attachment ->
                        PendingAttachmentItem(
                            attachment = attachment,
                            onRemove = { onRemoveAttachment(attachment) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAttachMenu = true }) {
                    Icon(Icons.Default.Add, "Прикрепить", tint = MaterialTheme.colorScheme.primary)
                    
                    DropdownMenu(
                        expanded = showAttachMenu,
                        onDismissRequest = { showAttachMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Фото") },
                            onClick = { showAttachMenu = false; onAddPhoto() }
                        )
                        if (isDeveloperMode) {
                            DropdownMenuItem(
                                text = { Text("Видео") },
                                onClick = { showAttachMenu = false; onAddVideo() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Аудио") },
                            onClick = { showAttachMenu = false; onAddAudio() }
                        )
                        if (isDeveloperMode) {
                            DropdownMenuItem(
                                text = { Text("Документ") },
                                onClick = { showAttachMenu = false; onAddDoc() }
                            )
                        }
                    }
                }

                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Введите сообщение...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    maxLines = 5
                )
                
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(8.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = text.isNotBlank() || pendingAttachments.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = if (text.isNotBlank() || pendingAttachments.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
