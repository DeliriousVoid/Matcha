package com.deliriousvoid.openvkmatcha.ui.screens.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Description
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.Comment
import com.deliriousvoid.openvkmatcha.data.model.Post
import com.deliriousvoid.openvkmatcha.ui.components.DeleteConfirmationDialog
import com.deliriousvoid.openvkmatcha.ui.components.EditPostDialog
import com.deliriousvoid.openvkmatcha.ui.components.ReportDialog
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.PostCard
import com.deliriousvoid.openvkmatcha.ui.components.ParsedText
import com.deliriousvoid.openvkmatcha.ui.components.RepostBottomSheet
import com.deliriousvoid.openvkmatcha.ui.components.CommentAttachmentsList
import com.deliriousvoid.openvkmatcha.ui.components.VerifiedBadge
import com.deliriousvoid.openvkmatcha.ui.components.MusicPickerBottomSheet
import com.deliriousvoid.openvkmatcha.ui.components.VideoPickerBottomSheet
import com.deliriousvoid.openvkmatcha.ui.components.DocsPickerBottomSheet
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource
import com.deliriousvoid.openvkmatcha.data.model.Document
import com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo
import com.deliriousvoid.openvkmatcha.ui.viewmodel.CommentsViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicActionsViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PlayerViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.RepostViewModel
import com.deliriousvoid.openvkmatcha.util.downloadDocument
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import com.deliriousvoid.openvkmatcha.data.model.PendingAttachment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import android.provider.OpenableColumns

@Composable
fun CommentsScreen(
    ownerId: Int,
    postId: Int,
    onOpenProfile: (Any) -> Unit,
    onOpenPlaylist: (Int, Int, String) -> Unit = { _, _, _ -> },
    onOpenComments: (Int, Int) -> Unit = { _, _ -> },
    onOpenMusic: (Int, String) -> Unit = { _, _ -> },
    viewModel: CommentsViewModel = viewModel(factory = CommentsViewModel.factory(ownerId, postId)),
    musicActionsViewModel: MusicActionsViewModel = viewModel(factory = MusicActionsViewModel.factory()),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory()),
    repostViewModel: RepostViewModel = viewModel(factory = RepostViewModel.factory()),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val musicActionsState by musicActionsViewModel.trackStates.collectAsState()
    val downloadedTracks by musicActionsViewModel.downloadedTracks.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val doubleTapToLike by settingsViewModel.doubleTapToLike.collectAsState()
    val doubleTapTimeout by settingsViewModel.doubleTapTimeout.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var sharingPost by remember { mutableStateOf<Post?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }
    var reportingPost by remember { mutableStateOf<Post?>(null) }
    var reportingComment by remember { mutableStateOf<Comment?>(null) }
    var showMusicPicker by remember { mutableStateOf(false) }
    var showVideoPicker by remember { mutableStateOf(false) }
    var showDocsPicker by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it, AttachmentType.PHOTO) }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(state.post) {
        state.post?.audios?.let { audios ->
            if (audios.isNotEmpty()) {
                musicActionsViewModel.loadArtworks(audios)
            }
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

    LaunchedEffect(state.comments) {
        if (state.comments.isNotEmpty()) {
            listState.animateScrollToItem(state.comments.size)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (state.isLoading && state.comments.isEmpty()) {
                LoadingBox(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    state.post?.let { post ->
                        item {
                            PostCard(
                                post = post,
                                onLikeClick = { viewModel.toggleLikePost(post) },
                                onCommentClick = { oId, pId ->
                                    if (oId != post.ownerId || pId != post.id) {
                                        onOpenComments(oId, pId)
                                    }
                                },
                                onRepostClick = { sharingPost = post },
                                onAuthorClick = { onOpenProfile(it) },
                                onAudioClick = { tracks, index -> playerViewModel.play(tracks, index, PlaylistSource.LocalAudio(tracks)) },
                                onAudioToggleAdded = { musicActionsViewModel.toggleTrackAdded(it) },
                                onAudioDownload = { musicActionsViewModel.downloadTrack(it) },
                                onAudioShare = {
                                    val url = it.remoteUrl ?: it.url ?: ""
                                    if (url.isNotBlank()) {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(url))
                                    }
                                },
                                onAudioAddToQueue = { musicActionsViewModel.addToQueue(it) },
                                onAudioPlayNext = { musicActionsViewModel.playNext(it) },
                                onDocumentDownload = { downloadDocument(it) },
                                onMusicClick = { onOpenMusic(it, "") },
                                onPlaylistClick = { oId, pId -> onOpenPlaylist(oId, pId, "") },
                                currentTrack = currentTrack,
                                isDownloaded = { id, ownerId -> downloadedTracks.contains("${ownerId}_$id") },
                                getTrackState = { track -> musicActionsState[track.stableId] ?: track },
                                currentUserId = state.currentUserId,
                                onEditClick = { editingPost = it },
                                onDeleteClick = { postToDelete = it },
                                onReportClick = { reportingPost = it },
                                onGeoClick = { com.deliriousvoid.openvkmatcha.ui.util.LinkHandler.handleGeo(context, it) },
                                doubleTapEnabled = doubleTapToLike,
                                doubleTapTimeout = doubleTapTimeout
                            )
                            HorizontalDivider(
                                thickness = 4.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    items(state.threadedComments, key = { it.item.id }) { threadedItem ->
                        val comment = threadedItem.item
                        CommentItem(
                            comment = comment,
                            isOwn = comment.fromId == state.currentUserId,
                            canDelete = comment.canDelete || comment.fromId == state.currentUserId || (state.post?.ownerId == state.currentUserId),
                            level = threadedItem.level,
                            isLastInThread = threadedItem.isLastInThread,
                            hasNextInThread = threadedItem.hasNextInThread,
                            onAuthorClick = { onOpenProfile(comment.fromId) },
                            onMentionClick = { onOpenProfile(it) },
                            onProfileClick = { onOpenProfile(it) },
                            onWallClick = onOpenComments,
                            onMusicClick = { onOpenMusic(it, "") },
                            onPlaylistClick = { oId, pId -> onOpenPlaylist(oId, pId, "") },
                            onLikeClick = { viewModel.toggleLike(comment) },
                            onReplyClick = { viewModel.replyTo(comment) },
                            onEditClick = { viewModel.startEditing(comment) },
                            onDeleteClick = { commentToDelete = comment },
                            onReportClick = { reportingComment = comment },
                            onAudioClick = { tracks, index -> playerViewModel.play(tracks, index, PlaylistSource.LocalAudio(tracks)) },
                            onAudioToggleAdded = { musicActionsViewModel.toggleTrackAdded(it) },
                            onAudioDownload = { musicActionsViewModel.downloadTrack(it) },
                            onAudioShare = { /* Handle share */ },
                            onAudioAddToQueue = { musicActionsViewModel.addToQueue(it) },
                            onAudioPlayNext = { musicActionsViewModel.playNext(it) },
                            currentTrack = currentTrack,
                            isDownloaded = { id, ownerId -> musicActionsViewModel.isDownloaded(id, ownerId) },
                            getTrackState = { musicActionsViewModel.getTrack(it) },
                            onDocumentDownload = { /* Handle doc download */ },
                            onPollVote = { answers -> viewModel.votePoll(comment, answers) }
                        )
                        if ((threadedItem.level == 0 && !threadedItem.hasNextInThread) || (threadedItem.level == 1 && threadedItem.isLastInThread)) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
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

        CommentInput(
            text = state.inputText,
            isSending = state.isSending,
            isEditing = state.editingComment != null,
            replyingTo = state.replyingTo,
            isAdmin = state.isAdmin,
            fromGroup = state.fromGroup,
            isDeveloperMode = state.isDeveloperMode,
            pendingAttachments = state.pendingAttachments,
            onCancelEdit = viewModel::cancelEditing,
            onCancelReply = viewModel::cancelReply,
            onTextChange = viewModel::updateInput,
            onFromGroupChange = viewModel::setFromGroup,
            onRemoveAttachment = viewModel::removeAttachment,
            onAddPhoto = { photoPicker.launch("image/*") },
            onAddVideo = { showVideoPicker = true },
            onAddAudio = { showMusicPicker = true },
            onAddDoc = { showDocsPicker = true },
            onSend = viewModel::postComment
        )
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

    sharingPost?.let { post ->
        RepostBottomSheet(
            post = post,
            onDismiss = { sharingPost = null },
            viewModel = repostViewModel
        )
    }

    postToDelete?.let { post ->
        DeleteConfirmationDialog(
            title = androidx.compose.ui.res.stringResource(com.deliriousvoid.openvkmatcha.R.string.post_delete_confirm),
            onDismiss = { postToDelete = null },
            onConfirm = { viewModel.deletePost(post) }
        )
    }

    commentToDelete?.let { comment ->
        DeleteConfirmationDialog(
            title = androidx.compose.ui.res.stringResource(com.deliriousvoid.openvkmatcha.R.string.comment_delete_confirm),
            onDismiss = { commentToDelete = null },
            onConfirm = { viewModel.deleteComment(comment) }
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

    reportingComment?.let { comment ->
        ReportDialog(
            onDismiss = { reportingComment = null },
            onConfirm = { text ->
                viewModel.report("comment", comment.ownerId, comment.id, text)
                reportingComment = null
            }
        )
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isOwn: Boolean,
    canDelete: Boolean = false,
    level: Int = 0,
    isLastInThread: Boolean = false,
    hasNextInThread: Boolean = false,
    onAuthorClick: () -> Unit,
    onMentionClick: (Int) -> Unit,
    onProfileClick: (String) -> Unit = {},
    onWallClick: (Int, Int) -> Unit = { _, _ -> },
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit = {},
    onAudioClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onAudioToggleAdded: (AudioTrack) -> Unit = {},
    onAudioDownload: (AudioTrack) -> Unit = {},
    onAudioShare: (AudioTrack) -> Unit = {},
    onAudioAddToQueue: (AudioTrack) -> Unit = {},
    onAudioPlayNext: (AudioTrack) -> Unit = {},
    currentTrack: AudioTrack? = null,
    isDownloaded: (Int, Int) -> Boolean = { _, _ -> false },
    getTrackState: (AudioTrack) -> AudioTrack = { it },
    onDocumentDownload: (Document) -> Unit = {},
    onPollVote: (List<Int>) -> Unit = {},
    onImageClick: (Int) -> Unit = {},
    showActions: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (level > 0) 24.dp else 0.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 2.dp.toPx()
            val avatarSizeRoot = 36.dp.toPx()
            val avatarSizeReply = 32.dp.toPx()
            val padding = 16.dp.toPx()
            val indent = 24.dp.toPx()
            
            // Absolute center of root avatar relative to the root container
            // Root avatar is at padding,padding.
            val rootCenterX = padding + avatarSizeRoot / 2
            
            if (level == 0) {
                if (hasNextInThread) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.6f),
                        start = androidx.compose.ui.geometry.Offset(rootCenterX, padding + avatarSizeRoot),
                        end = androidx.compose.ui.geometry.Offset(rootCenterX, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            } else {
                // Coordinate of the thread line relative to this indented container
                val threadLineX = rootCenterX - indent
                
                // Vertical line from top
                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = androidx.compose.ui.geometry.Offset(threadLineX, 0f),
                    end = androidx.compose.ui.geometry.Offset(threadLineX, if (hasNextInThread) size.height else padding + avatarSizeReply / 2),
                    strokeWidth = strokeWidth
                )
                // Horizontal stub to avatar
                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = androidx.compose.ui.geometry.Offset(threadLineX, padding + avatarSizeReply / 2),
                    end = androidx.compose.ui.geometry.Offset(padding, padding + avatarSizeReply / 2),
                    strokeWidth = strokeWidth
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = comment.authorAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(if (level > 0) 32.dp else 36.dp)
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onAuthorClick() }
                )
                if (comment.authorVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(userId = comment.fromId, isVerified = true)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimeAgo(comment.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                if (showActions) {
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
                            if (isOwn) {
                                DropdownMenuItem(
                                    text = { Text("Изменить") },
                                    onClick = { showMenu = false; onEditClick() }
                                )
                            }
                            if (isOwn || canDelete) {
                                DropdownMenuItem(
                                    text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; onDeleteClick() }
                                )
                            }
                            if (!isOwn && !canDelete) {
                                DropdownMenuItem(
                                    text = { Text("Пожаловаться") },
                                    onClick = { showMenu = false; onReportClick() }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))

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
                onAudioDownload = onAudioDownload,
                onAudioShare = onAudioShare,
                onAudioAddToQueue = onAudioAddToQueue,
                onAudioPlayNext = onAudioPlayNext,
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
}

@Composable
fun CommentInput(
    text: String,
    isSending: Boolean,
    isEditing: Boolean,
    replyingTo: Comment? = null,
    isAdmin: Boolean = false,
    fromGroup: Boolean = false,
    isDeveloperMode: Boolean = false,
    pendingAttachments: List<PendingAttachment> = emptyList(),
    onCancelEdit: () -> Unit,
    onCancelReply: () -> Unit = {},
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
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }

    LaunchedEffect(text) {
        if (text != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = text,
                selection = TextRange(text.length)
            )
        }
    }

    Surface(
        tonalElevation = 2.dp,
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

            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Редактирование комментария",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onCancelEdit, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "В ответ пользователю ${replyingTo.authorName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.secondary)
                    }
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
                    .fillMaxWidth()
                    .padding(8.dp),
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
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        if (text != it.text) {
                            onTextChange(it.text)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ваш комментарий...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
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

@Composable
fun PendingAttachmentItem(
    attachment: PendingAttachment,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.BottomStart)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            when (attachment.type) {
                AttachmentType.PHOTO -> {
                    AsyncImage(
                        model = attachment.uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (attachment.type) {
                                AttachmentType.VIDEO -> Icons.Default.PlayArrow
                                AttachmentType.AUDIO -> Icons.Default.MusicNote
                                else -> Icons.Default.Description
                            },
                            contentDescription = null
                        )
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
