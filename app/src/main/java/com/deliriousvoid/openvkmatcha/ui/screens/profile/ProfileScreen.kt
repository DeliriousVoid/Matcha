package com.deliriousvoid.openvkmatcha.ui.screens.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource
import kotlin.math.absoluteValue
import com.deliriousvoid.openvkmatcha.ui.components.DeleteConfirmationDialog
import com.deliriousvoid.openvkmatcha.ui.components.EditPostDialog
import com.deliriousvoid.openvkmatcha.ui.components.ReportDialog
import com.deliriousvoid.openvkmatcha.ui.components.ImageViewer
import com.deliriousvoid.openvkmatcha.ui.components.OnlineIndicator
import com.deliriousvoid.openvkmatcha.ui.util.formatLastSeen
import com.deliriousvoid.openvkmatcha.ui.components.VerifiedBadge
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.PostCard
import com.deliriousvoid.openvkmatcha.ui.components.RepostBottomSheet
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ProfileViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicActionsViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.PlayerViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.RepostViewModel
import com.deliriousvoid.openvkmatcha.util.downloadDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenProfile: (Any) -> Unit,
    onOpenComments: (Int, Int) -> Unit,
    onOpenPlaylist: (Int, Int, String) -> Unit = { _, _, _ -> },
    onOpenFriends: (Int, String) -> Unit = { _, _ -> },
    onOpenGroups: (Int, String) -> Unit = { _, _ -> },
    onOpenMusic: (Int, String) -> Unit = { _, _ -> },
    onOpenGifts: (Int, String) -> Unit = { _, _ -> },
    onOpenTopics: (Int, String) -> Unit = { _, _ -> },
    onOpenFollowers: (Int, Boolean, String) -> Unit = { _, _, _ -> },
    onOpenPhotos: (Int, String) -> Unit = { _, _ -> },
    onOpenChat: (Int, String) -> Unit = { _, _ -> },
    onOpenEditProfile: () -> Unit = {},
    onOpenEditGroup: (Int) -> Unit = {},
    onOpenCreatePost: (Int) -> Unit = {},
    userIdOrName: String? = null,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(userIdOrName)),
    musicActionsViewModel: MusicActionsViewModel = viewModel(factory = MusicActionsViewModel.factory()),
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory()),
    repostViewModel: RepostViewModel = viewModel(factory = RepostViewModel.factory()),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    val doubleTapToLike by settingsViewModel.doubleTapToLike.collectAsState()
    val doubleTapTimeout by settingsViewModel.doubleTapTimeout.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val musicActionsState by musicActionsViewModel.trackStates.collectAsState()
    val downloadedTracks by musicActionsViewModel.downloadedTracks.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var sharingPost by remember { mutableStateOf<com.deliriousvoid.openvkmatcha.data.model.Post?>(null) }
    var editingPost by remember { mutableStateOf<com.deliriousvoid.openvkmatcha.data.model.Post?>(null) }
    var postToDelete by remember { mutableStateOf<com.deliriousvoid.openvkmatcha.data.model.Post?>(null) }
    var reportingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var reportingPost by remember { mutableStateOf<com.deliriousvoid.openvkmatcha.data.model.Post?>(null) }

    LaunchedEffect(state.profile) {
        state.profile?.let { profile ->
            val baseUrl = com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance.api.baseUrl
            val url = if (profile.id > 0) "$baseUrl/id${profile.id}" else "$baseUrl/club${-profile.id}"
            com.deliriousvoid.openvkmatcha.util.AppEvents.setCurrentQrData(
                com.deliriousvoid.openvkmatcha.util.QrData(
                    url = url,
                    title = profile.fullName,
                    avatarUrl = profile.photo200
                )
            )
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            com.deliriousvoid.openvkmatcha.util.AppEvents.setCurrentQrData(null)
        }
    }

    LaunchedEffect(state.wallPosts) {
        val allAudios = state.wallPosts.flatMap { it.audios }
        if (allAudios.isNotEmpty()) {
            musicActionsViewModel.loadArtworks(allAudios)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.loadProfile(refresh = true, isManual = true) },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.profile == null -> LoadingBox(modifier = Modifier.fillMaxSize())
            state.error != null && state.profile == null -> ErrorText(
                message = state.error!!,
                modifier = Modifier.fillMaxSize(),
                onRetry = { viewModel.loadProfile(refresh = true, isManual = true) }
            )
            state.profile != null -> {
                val listState = rememberLazyListState()

                val shouldLoadMore = remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        lastVisibleItem > totalItems - 5 && totalItems > 0
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.scrollToTop.collect {
                        listState.animateScrollToItem(0)
                    }
                }

                LaunchedEffect(shouldLoadMore.value) {
                    if (shouldLoadMore.value) {
                        viewModel.loadMore()
                    }
                }

                ProfileContent(
                    profile = state.profile!!,
                    wallPosts = state.wallPosts,
                    isLoadingMore = state.isLoadingMore,
                    listState = listState,
                    onOpenProfile = { clickedId ->
                        if (clickedId.toString() == state.profile?.id.toString()) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        } else {
                            onOpenProfile(clickedId)
                        }
                    },
                    onOpenComments = onOpenComments,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenFriends = onOpenFriends,
                    onOpenGroups = onOpenGroups,
                    onOpenMusic = onOpenMusic,
                    onOpenGifts = onOpenGifts,
                    onOpenTopics = onOpenTopics,
                    onOpenFollowers = onOpenFollowers,
                    onOpenPhotos = onOpenPhotos,
                    onOpenChat = onOpenChat,
                    onOpenEditProfile = onOpenEditProfile,
                    onOpenEditGroup = onOpenEditGroup,
                    onOpenCreatePost = onOpenCreatePost,
                    onLikeClick = viewModel::toggleLike,
                    onRepostClick = { sharingPost = it },
                    currentUserId = state.currentUserId,
                    onEditClick = { editingPost = it },
                    onDeleteClick = { postToDelete = it },
                    onPinClick = viewModel::pinPost,
                    onUnpinClick = viewModel::unpinPost,
                    onReportClick = { reportingProfile = it },
                    onReportPost = { reportingPost = it },
                    onIgnoreClick = { viewModel.toggleIgnore(it) },
                    onBlacklistClick = { viewModel.toggleBlacklist(it) },
                    onPollVote = { post, answerIds -> viewModel.votePoll(post, answerIds) },
                    onGeoClick = { com.deliriousvoid.openvkmatcha.ui.util.LinkHandler.handleGeo(context, it) },
                    doubleTapToLike = doubleTapToLike,
                    doubleTapTimeout = doubleTapTimeout,
                    musicActionsViewModel = musicActionsViewModel,
                    musicActionsState = musicActionsState,
                    playerViewModel = playerViewModel,
                    currentTrack = currentTrack,
                    downloadedTracks = downloadedTracks,
                    clipboardManager = clipboardManager,
                    onFriendAction = { viewModel.toggleFriendship() },
                    onGroupAction = { viewModel.toggleGroupMembership() },
                    modifier = Modifier.fillMaxSize()
                )
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
            title = androidx.compose.ui.res.stringResource(com.deliriousvoid.openvkmatcha.R.string.post_delete_confirm),
            onDismiss = { postToDelete = null },
            onConfirm = { viewModel.deletePost(post) }
        )
    }

    reportingProfile?.let { profile ->
        ReportDialog(
            onDismiss = { reportingProfile = null },
            onConfirm = { comment ->
                viewModel.report(if (profile.isGroup) "group" else "user", profile.id, comment = comment)
                reportingProfile = null
            }
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

@Composable
private fun ProfileContent(
    profile: UserProfile,
    wallPosts: List<com.deliriousvoid.openvkmatcha.data.model.Post>,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpenProfile: (Any) -> Unit,
    onOpenComments: (Int, Int) -> Unit,
    onOpenPlaylist: (Int, Int, String) -> Unit = { _, _, _ -> },
    onOpenFriends: (Int, String) -> Unit = { _, _ -> },
    onOpenGroups: (Int, String) -> Unit = { _, _ -> },
    onOpenMusic: (Int, String) -> Unit = { _, _ -> },
    onOpenGifts: (Int, String) -> Unit = { _, _ -> },
    onOpenTopics: (Int, String) -> Unit = { _, _ -> },
    onOpenFollowers: (Int, Boolean, String) -> Unit = { _, _, _ -> },
    onOpenPhotos: (Int, String) -> Unit = { _, _ -> },
    onOpenChat: (Int, String) -> Unit = { _, _ -> },
    onOpenEditProfile: () -> Unit = {},
    onOpenEditGroup: (Int) -> Unit = {},
    onOpenCreatePost: (Int) -> Unit = {},
    onLikeClick: (com.deliriousvoid.openvkmatcha.data.model.Post) -> Unit,
    onRepostClick: (com.deliriousvoid.openvkmatcha.data.model.Post) -> Unit,
    currentUserId: Int? = null,
    onEditClick: (com.deliriousvoid.openvkmatcha.data.model.Post) -> Unit = {},
    onDeleteClick: (com.deliriousvoid.openvkmatcha.data.model.Post) -> Unit = {},
    onPinClick: (com.deliriousvoid.openvkmatcha.data.model.Post) -> Unit = {},
    onUnpinClick: (com.deliriousvoid.openvkmatcha.data.model.Post) -> Unit = {},
    onReportPost: (com.deliriousvoid.openvkmatcha.data.model.Post) -> Unit = {},
    onPollVote: (com.deliriousvoid.openvkmatcha.data.model.Post, List<Int>) -> Unit = { _, _ -> },
    onGeoClick: (com.deliriousvoid.openvkmatcha.data.model.Geo) -> Unit = {},
    doubleTapToLike: Boolean = false,
    doubleTapTimeout: Long = 100L,
    onReportClick: (UserProfile) -> Unit = {},
    onIgnoreClick: (UserProfile) -> Unit = {},
    onBlacklistClick: (UserProfile) -> Unit = {},
    musicActionsViewModel: MusicActionsViewModel,
    musicActionsState: Map<String, com.deliriousvoid.openvkmatcha.data.model.AudioTrack>,
    playerViewModel: PlayerViewModel,
    currentTrack: com.deliriousvoid.openvkmatcha.data.model.AudioTrack?,
    downloadedTracks: Set<String>,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    onFriendAction: () -> Unit = {},
    onGroupAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAvatarViewer by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val baseUrl = com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance.api.baseUrl

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                AsyncImage(
                                    model = profile.photo200,
                                    contentDescription = "Аватар",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clickable { showAvatarViewer = true }
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop,
                                )
                                OnlineIndicator(
                                    isOnline = profile.online,
                                    isMobile = profile.mobileOnline,
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    dotSize = 14.dp,
                                    iconSize = 20.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.fullName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (profile.verified || com.deliriousvoid.openvkmatcha.Constants.CUSTOM_VERIFIED_IDS.contains(profile.id)) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        VerifiedBadge(
                                            userId = profile.id,
                                            isVerified = profile.verified,
                                            size = 20.dp
                                        )
                                    }
                                }
                                if (profile.status.isNotBlank()) {
                                    Text(
                                        text = profile.status,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (profile.online) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "онлайн",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (profile.mobileOnline) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val platform = profile.lastSeen?.platform
                                            when (platform) {
                                                2, 3 -> Icon(
                                                    imageVector = Icons.Default.PhoneIphone,
                                                    contentDescription = "iPhone",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                4 -> Icon(
                                                    imageVector = Icons.Default.Android,
                                                    contentDescription = "Android",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                5, 6 -> Icon(
                                                    painter = painterResource(com.deliriousvoid.openvkmatcha.R.drawable.ic_window),
                                                    contentDescription = "Windows",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                else -> Icon(
                                                    imageVector = Icons.Default.Smartphone,
                                                    contentDescription = "Mobile",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = profile.lastSeen?.let { formatLastSeen(it, profile.sex ?: 0) } ?: "@${profile.screenName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Box {
                        androidx.compose.material3.IconButton(onClick = { showProfileMenu = true }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                                contentDescription = "Меню профиля"
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            val isMe = currentUserId != null && profile.id == currentUserId
                            val profileUrl = if (profile.id > 0) "$baseUrl/id${profile.id}" else "$baseUrl/club${-profile.id}"

                            if (isMe) {
                                if (!profile.isGroup) {
                                    DropdownMenuItem(
                                        text = { Text("Редактировать профиль") },
                                        onClick = {
                                            showProfileMenu = false
                                            onOpenEditProfile()
                                        }
                                    )
                                }
                            }

                            if (profile.isAdmin && profile.isGroup) {
                                DropdownMenuItem(
                                    text = { Text("Редактировать группу") },
                                    onClick = {
                                        showProfileMenu = false
                                        onOpenEditGroup(Math.abs(profile.id))
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Копировать ссылку") },
                                onClick = {
                                    showProfileMenu = false
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(profileUrl))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Открыть в браузере") },
                                onClick = {
                                    showProfileMenu = false
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(profileUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            )

                            if (!isMe) {
                                DropdownMenuItem(
                                    text = { Text(if (profile.isIgnored) "Перестать игнорировать" else "Игнорировать") },
                                    onClick = {
                                        showProfileMenu = false
                                        onIgnoreClick(profile)
                                    }
                                )
                                if (!profile.isGroup) {
                                    DropdownMenuItem(
                                        text = { Text(if (profile.blacklistedByMe) "Убрать из чёрного списка" else "Добавить в чёрный список") },
                                        onClick = {
                                            showProfileMenu = false
                                            onBlacklistClick(profile)
                                        }
                                    )
                                }
                                if (!profile.isAdmin) {
                                    DropdownMenuItem(
                                        text = { Text("Пожаловаться") },
                                        onClick = {
                                            showProfileMenu = false
                                            onReportClick(profile)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    if (!profile.isGroup && profile.friendsCount > 0) {
                        CounterItem(
                            count = profile.friendsCount,
                            label = "друзья",
                            onClick = { onOpenFriends(profile.id, profile.fullName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (profile.followersCount > 0) {
                        CounterItem(
                            count = profile.followersCount,
                            label = if (profile.isGroup) "участники" else "подписчики",
                            onClick = { onOpenFollowers(profile.id, profile.isGroup, profile.fullName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (profile.isGroup) {
                        CounterItem(
                            count = profile.topicsCount,
                            label = "обсуждения",
                            onClick = { onOpenTopics(profile.id, profile.fullName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (profile.isGroup || profile.photosCount > 0) {
                        CounterItem(
                            count = profile.photosCount,
                            label = "фото",
                            onClick = { onOpenPhotos(profile.id, profile.fullName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (profile.isGroup || profile.audiosCount > 0) {
                        CounterItem(
                            count = profile.audiosCount,
                            label = "аудио",
                            onClick = { onOpenMusic(profile.id, profile.fullName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        if (profile.groupsCount > 0) {
                            CounterItem(
                                count = profile.groupsCount,
                                label = "группы",
                                onClick = { onOpenGroups(profile.id, profile.fullName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (profile.giftsCount > 0) {
                            CounterItem(
                                count = profile.giftsCount,
                                label = "подарки",
                                onClick = { onOpenGifts(profile.id, profile.fullName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }

                if (profile.about.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile.about,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                ExtendedProfileInfo(
                    profile = profile,
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMe = currentUserId != null && profile.id == currentUserId
                    val canPostWall = profile.canPost || (profile.isGroup && profile.isAdmin) || isMe
                    if (canPostWall) {
                        OutlinedButton(
                            onClick = { onOpenCreatePost(profile.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Что у вас нового?")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (currentUserId != null && currentUserId != profile.id) {
                        if (profile.isGroup) {
                            val (icon, tint) = if (profile.isMember) {
                                Icons.Default.GroupRemove to MaterialTheme.colorScheme.error
                            } else {
                                Icons.Default.GroupAdd to MaterialTheme.colorScheme.primary
                            }
                            IconButton(onClick = onGroupAction) {
                                Icon(icon, contentDescription = null, tint = tint)
                            }
                        } else {
                            val (icon, tint) = when (profile.friendStatus) {
                                0, null -> Icons.Default.PersonAdd to MaterialTheme.colorScheme.primary
                                1 -> Icons.Default.PersonRemove to MaterialTheme.colorScheme.onSurfaceVariant
                                2 -> Icons.Default.PersonAdd to MaterialTheme.colorScheme.primary
                                3 -> Icons.Default.PersonRemove to MaterialTheme.colorScheme.error
                                else -> Icons.Default.PersonAdd to MaterialTheme.colorScheme.primary
                            }
                            IconButton(onClick = onFriendAction) {
                                Icon(icon, contentDescription = null, tint = tint)
                            }
                        }

                        if (!profile.isGroup) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { onOpenChat(profile.id, profile.fullName) }) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Сообщение",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Text(
                text = "Записи на стене",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp),
            )
        }

        items(wallPosts, key = { "${it.ownerId}_${it.id}" }) { post ->
            PostCard(
                post = post,
                onLikeClick = { onLikeClick(post) },
                onCommentClick = onOpenComments,
                onRepostClick = { onRepostClick(post) },
                onAuthorClick = onOpenProfile,
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
                onPlaylistClick = { ownerId, playlistId -> onOpenPlaylist(ownerId, playlistId, "") },
                currentTrack = currentTrack,
                isDownloaded = { id, ownerId -> downloadedTracks.contains("${ownerId}_$id") },
                getTrackState = { track -> musicActionsState[track.stableId] ?: track },
                currentUserId = currentUserId,
                onEditClick = { onEditClick(it) },
                onDeleteClick = { onDeleteClick(it) },
                onPinClick = { onPinClick(it) },
                onUnpinClick = { onUnpinClick(it) },
                onReportClick = { onReportPost(it) },
                onPollVote = onPollVote,
                onGeoClick = onGeoClick,
                doubleTapEnabled = doubleTapToLike,
                doubleTapTimeout = doubleTapTimeout
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
        }

        if (isLoadingMore) {
            item {
                LoadingBox(modifier = Modifier.padding(16.dp))
            }
        }
    }

    if (showAvatarViewer) {
        val viewerUrl = profile.photoMaxOrig?.takeIf { it.isNotBlank() } ?: profile.photo200
        ImageViewer(
            imageUrls = listOf(viewerUrl),
            onDismiss = { showAvatarViewer = false }
        )
    }
}

@Composable
private fun CounterItem(
    count: Int, 
    label: String, 
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExtendedProfileInfo(
    profile: UserProfile,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sections = mutableListOf<InfoSection>()

    val openUrl = { url: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    // Main Info
    val mainInfo = mutableListOf<InfoItem>()
    val bdate = profile.birthdayDisplay ?: profile.bdate
    bdate?.let { mainInfo.add(InfoItem(Icons.Default.Cake, "День рождения: $it")) }
    profile.city?.let { mainInfo.add(InfoItem(Icons.Default.LocationOn, "Город: $it")) }
    profile.homeTown?.let { mainInfo.add(InfoItem(Icons.Default.Home, "Родной город: $it")) }
    profile.sex?.let { mainInfo.add(InfoItem(Icons.Default.Wc, "Пол: ${getSexString(it)}")) }
    profile.relation?.let { mainInfo.add(InfoItem(Icons.Default.Favorite, "Положение: ${getRelationString(it, profile.sex ?: 0)}")) }

    profile.telegram?.let { tg ->
        val username = tg.removePrefix("@")
        mainInfo.add(InfoItem(Icons.Default.Language, "Telegram: $tg", onClick = { openUrl("https://t.me/$username") }))
    }

    if (mainInfo.isNotEmpty()) sections.add(InfoSection("Основная информация", mainInfo))

    // Contacts
    val contacts = mutableListOf<InfoItem>()
    profile.mobilePhone?.let { contacts.add(InfoItem(Icons.Default.Phone, "Моб. телефон: $it")) }
    profile.homePhone?.let { contacts.add(InfoItem(Icons.Default.Phone, "Доп. телефон: $it")) }
    profile.site?.let { site ->
        val url = if (site.startsWith("http://") || site.startsWith("https://")) site else "http://$site"
        contacts.add(InfoItem(Icons.Default.Public, "Сайт: $site", onClick = { openUrl(url) }))
    }
    profile.skype?.let { contacts.add(InfoItem(Icons.Default.Language, "Skype: $it")) }
    profile.facebook?.let { contacts.add(InfoItem(Icons.Default.Language, "Facebook: $it")) }
    profile.twitter?.let { contacts.add(InfoItem(Icons.Default.Language, "Twitter: $it")) }
    profile.instagram?.let { contacts.add(InfoItem(Icons.Default.Language, "Instagram: $it")) }
    if (contacts.isNotEmpty()) sections.add(InfoSection("Контактная информация", contacts))

    // Education
    val education = mutableListOf<InfoItem>()
    profile.universityName?.let { education.add(InfoItem(Icons.Default.School, "ВУЗ: $it")) }
    profile.facultyName?.let { education.add(InfoItem(Icons.Default.School, "Факультет: $it")) }
    profile.graduation?.let { education.add(InfoItem(Icons.Default.School, "Год выпуска: $it")) }
    if (education.isNotEmpty()) sections.add(InfoSection("Образование", education))

    // Personal
    val personal = mutableListOf<InfoItem>()
    profile.activities?.let { personal.add(InfoItem(Icons.Default.Info, "Деятельность: $it")) }
    profile.interests?.let { personal.add(InfoItem(Icons.Default.Info, "Интересы: $it")) }
    profile.music?.let { personal.add(InfoItem(Icons.Default.Info, "Любимая музыка: $it")) } 
    profile.movies?.let { personal.add(InfoItem(Icons.Default.Info, "Любимые фильмы: $it")) }
    profile.tv?.let { personal.add(InfoItem(Icons.Default.Info, "Любимые шоу: $it")) }
    profile.books?.let { personal.add(InfoItem(Icons.Default.Info, "Любимые книги: $it")) }
    profile.games?.let { personal.add(InfoItem(Icons.Default.Info, "Любимые игры: $it")) }
    profile.quotes?.let { personal.add(InfoItem(Icons.Default.Info, "Любимые цитаты: $it")) }
    if (personal.isNotEmpty()) sections.add(InfoSection("Личная информация", personal))

    if (sections.isEmpty()) return

    Column(modifier = Modifier.padding(top = 16.dp)) {
        if (!expanded) {
            androidx.compose.material3.TextButton(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Показать подробную информацию")
            }
        } else {
            sections.forEach { section ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                section.items.forEach { item ->
                    InfoRow(item)
                }
            }
            androidx.compose.material3.TextButton(
                onClick = { onExpandedChange(false) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Скрыть подробную информацию")
            }
        }
    }
}

@Composable
private fun InfoRow(item: InfoItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .let { if (item.onClick != null) it.clickable { item.onClick.invoke() } else it }
            .padding(vertical = 4.dp, horizontal = 0.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (item.onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (item.onClick != null) FontWeight.Medium else FontWeight.Normal
        )
    }
}

private data class InfoSection(val title: String, val items: List<InfoItem>)
private data class InfoItem(val icon: ImageVector, val text: String, val onClick: (() -> Unit)? = null)

private fun getSexString(sex: Int): String = when (sex) {
    1 -> "Женский"
    2 -> "Мужской"
    else -> "Не указан"
}

private fun getRelationString(relation: Int, sex: Int): String {
    val isFemale = sex == 1
    return when (relation) {
        1 -> if (isFemale) "не замужем" else "не женат"
        2 -> "есть друг/подруга"
        3 -> if (isFemale) "помолвлена" else "помолвлен"
        4 -> if (isFemale) "замужем" else "женат"
        5 -> "всё сложно"
        6 -> "в активном поиске"
        7 -> "влюблён/влюблена"
        else -> "не указано"
    }
}
