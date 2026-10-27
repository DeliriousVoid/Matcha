package com.deliriousvoid.openvkmatcha.ui.screens.messages

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatInfoViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatMemberItem
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatMaterialItem
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatMaterialSection
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    peerId: Int,
    onBack: () -> Unit,
    onOpenProfile: (Int) -> Unit,
    onSendGift: (Int) -> Unit = {},
    viewModel: ChatInfoViewModel = viewModel(factory = ChatInfoViewModel.factory(peerId))
) {
    val state by viewModel.uiState.collectAsState()
    var showAddMembersDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var memberToKick by remember { mutableStateOf<ChatMemberItem?>(null) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewerInitialIndex by remember { mutableIntStateOf(-1) }
    var selectedVideo by remember { mutableStateOf<com.deliriousvoid.openvkmatcha.data.model.Video?>(null) }
    val context = LocalContext.current

    val topTitle = if (peerId == 0) "Информация" else if (state.isUser) "Информация о пользователе" else "Информация о беседе"

    DisposableEffect(state.isUser, state.isMuted) {
        AppEvents.setTopBarState(TopBarState(
            tag = "chat_info",
            title = topTitle,
            route = com.deliriousvoid.openvkmatcha.ui.navigation.Routes.CHAT_INFO,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Ещё")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (state.isUser) {
                            DropdownMenuItem(
                                text = { Text("Заблокировать", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showBlockConfirmDialog = true
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Выйти из беседы", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showLeaveConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        ))
        onDispose {
            if (AppEvents.topBarState.value?.tag == "chat_info") {
                AppEvents.setTopBarState(null)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = state.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            if (state.avatarUrl.isNotBlank()) {
                                viewerImages = listOf(state.avatarUrl)
                                viewerInitialIndex = 0
                            }
                        }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.isMuted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Muted",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (state.isUser) {
                    val userProf = state.userProfile
                    val statusText = if (userProf?.online == true) {
                        "В сети"
                    } else if (userProf?.lastSeen != null) {
                        com.deliriousvoid.openvkmatcha.ui.util.formatLastSeen(userProf.lastSeen, userProf.sex ?: 0)
                    } else ""

                    if (statusText.isNotBlank()) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (userProf?.online == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "${state.memberCount} участников",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = state.isMuted,
                        onClick = { viewModel.toggleMute() },
                        label = { Text(if (state.isMuted) "Без звука" else "Звук вкл.") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (state.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null
                            )
                        }
                    )

                    if (state.isUser) {
                        FilterChip(
                            selected = false,
                            onClick = { onOpenProfile(peerId) },
                            label = { Text("Профиль") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                    } else if (state.canAddMembers) {
                        FilterChip(
                            selected = false,
                            onClick = { showAddMembersDialog = true },
                            label = { Text("Добавить") },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Section Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChatMaterialSection.entries) { section ->
                    val sectionLabel = if (section == ChatMaterialSection.MEMBERS && state.isUser) "Подарки" else section.title
                    FilterChip(
                        selected = state.selectedSection == section,
                        onClick = { viewModel.selectSection(section) },
                        label = { Text(sectionLabel) }
                    )
                }
            }

            com.deliriousvoid.openvkmatcha.ui.components.ChatCompactMiniPlayer()

            HorizontalDivider()

            // Content
            Box(modifier = Modifier.weight(1f)) {
                if (state.selectedSection == ChatMaterialSection.MEMBERS) {
                    if (state.isUser) {
                        if (state.isLoadingGifts && state.userGifts.isEmpty()) {
                            LoadingBox(modifier = Modifier.fillMaxSize())
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 100.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.userGifts, key = { it.id }) { gift ->
                                    Card(
                                        modifier = Modifier.aspectRatio(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = gift.thumb256,
                                                contentDescription = gift.message ?: "Подарок",
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clickable { onSendGift(peerId) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Подарить",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Подарить",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (state.isLoadingMembers && state.members.isEmpty()) {
                        LoadingBox(modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(state.members, key = { index, member -> "member_${member.id}_$index" }) { _, member ->
                                MemberRow(
                                    member = member,
                                    canModerate = state.canModerate && member.id != state.peerId,
                                    isOwner = state.isOwner,
                                    onOpenProfile = { onOpenProfile(member.id) },
                                    onToggleModerator = { enable -> viewModel.setModerator(member, enable) },
                                    onKick = { memberToKick = member }
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
                            }
                        }
                    }
                } else {
                    val sectionState = state.materials[state.selectedSection]
                    if (sectionState?.isLoading == true && sectionState.items.isEmpty()) {
                        LoadingBox(modifier = Modifier.fillMaxSize())
                    } else if (sectionState == null || sectionState.items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Здесь пока нет материалов", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (state.selectedSection == ChatMaterialSection.PHOTOS || state.selectedSection == ChatMaterialSection.VIDEOS) {
                        val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                        val shouldLoadMoreGrid = remember(gridState, sectionState) {
                            derivedStateOf {
                                val layoutInfo = gridState.layoutInfo
                                val totalItems = layoutInfo.totalItemsCount
                                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                lastVisible >= totalItems - 6 && totalItems > 0 && !sectionState.nextFrom.isNullOrBlank() && !sectionState.isLoading
                            }
                        }

                        LaunchedEffect(shouldLoadMoreGrid.value) {
                            if (shouldLoadMoreGrid.value) {
                                viewModel.loadMaterials(state.selectedSection, refresh = false)
                            }
                        }

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(sectionState.items, key = { index, item -> "${item.id}_$index" }) { _, item ->
                                MaterialTile(item = item, onClick = {
                                    if (state.selectedSection == ChatMaterialSection.PHOTOS) {
                                        val photoItems = sectionState.items.filter { !it.photoUrl.isNullOrBlank() }
                                        val photoUrls = photoItems.mapNotNull { it.photoUrl }
                                        val index = photoItems.indexOf(item).coerceAtLeast(0)
                                        if (photoUrls.isNotEmpty()) {
                                            viewerImages = photoUrls
                                            viewerInitialIndex = index
                                        }
                                    } else if (state.selectedSection == ChatMaterialSection.VIDEOS) {
                                        if (item.video != null) {
                                            selectedVideo = item.video
                                        } else if (!item.videoUrl.isNullOrBlank()) {
                                            selectedVideo = com.deliriousvoid.openvkmatcha.data.model.Video(
                                                id = item.id.hashCode(),
                                                ownerId = 0,
                                                title = item.title,
                                                duration = 0,
                                                thumbnailUrl = item.photoUrl,
                                                videoUrl = item.videoUrl,
                                                playerUrl = item.videoUrl
                                            )
                                        }
                                    }
                                })
                            }
                            if (sectionState.isLoading) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        val listState = rememberLazyListState()
                        val shouldLoadMoreList = remember(listState, sectionState) {
                            derivedStateOf {
                                val layoutInfo = listState.layoutInfo
                                val totalItems = layoutInfo.totalItemsCount
                                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                lastVisible >= totalItems - 4 && totalItems > 0 && !sectionState.nextFrom.isNullOrBlank() && !sectionState.isLoading
                            }
                        }

                        LaunchedEffect(shouldLoadMoreList.value) {
                            if (shouldLoadMoreList.value) {
                                viewModel.loadMaterials(state.selectedSection, refresh = false)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(sectionState.items, key = { index, item -> "${item.id}_$index" }) { _, item ->
                                if (state.selectedSection == ChatMaterialSection.DOCUMENTS && item.document != null) {
                                    val isGif = item.document.ext.lowercase() == "gif" || item.document.type == 3 || !item.document.previewGifUrl.isNullOrBlank()
                                    com.deliriousvoid.openvkmatcha.ui.components.DocumentItem(
                                        document = item.document,
                                        onClick = {
                                            if (isGif) {
                                                val docUrl = item.document.url.orEmpty()
                                                val gifUrl = if (item.document.ext.lowercase() == "gif" || docUrl.lowercase().contains("gif")) {
                                                    docUrl
                                                } else {
                                                    item.document.previewGifUrl ?: docUrl
                                                }
                                                if (gifUrl.isNotBlank()) {
                                                    viewerImages = listOf(gifUrl)
                                                    viewerInitialIndex = 0
                                                }
                                            } else if (!item.document.url.isNullOrBlank()) {
                                                OpenVKMatchaApp.instance.downloadRepository.downloadFile(
                                                    item.document.url,
                                                    item.document.title,
                                                    item.document.ext
                                                )
                                            }
                                        },
                                        onDownload = {
                                            if (!item.document.url.isNullOrBlank()) {
                                                OpenVKMatchaApp.instance.downloadRepository.downloadFile(
                                                    item.document.url,
                                                    item.document.title,
                                                    item.document.ext
                                                )
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                } else {
                                    MaterialRow(item = item, onClick = {
                                        if (state.selectedSection == ChatMaterialSection.AUDIO) {
                                            val tracks = sectionState.items.mapNotNull { it.audioTrack }
                                            val clickedIndex = if (item.audioTrack != null) tracks.indexOf(item.audioTrack).coerceAtLeast(0) else 0
                                            if (tracks.isNotEmpty()) {
                                                OpenVKMatchaApp.instance.playerManager.play(
                                                    tracks = tracks,
                                                    startIndex = clickedIndex,
                                                    source = com.deliriousvoid.openvkmatcha.data.model.PlaylistSource.Unknown
                                                )
                                            }
                                        } else if (!item.docUrl.isNullOrBlank()) {
                                            OpenVKMatchaApp.instance.downloadRepository.downloadFile(
                                                item.docUrl,
                                                item.title,
                                                item.subtitle ?: "bin"
                                            )
                                        } else {
                                            val url = item.linkUrl
                                            if (!url.isNullOrBlank()) {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                            }
                                        }
                                    })
                                }
                                HorizontalDivider()
                            }
                            if (sectionState.isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    if (viewerInitialIndex != -1 && viewerImages.isNotEmpty()) {
        com.deliriousvoid.openvkmatcha.ui.components.ImageViewer(
            imageUrls = viewerImages,
            initialIndex = viewerInitialIndex,
            onDismiss = { viewerInitialIndex = -1 }
        )
    }

    if (selectedVideo != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedVideo = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    com.deliriousvoid.openvkmatcha.ui.components.VideoPlayer(
                        video = selectedVideo!!,
                        modifier = Modifier.fillMaxSize(),
                        initiallyFullscreen = true,
                        onDismiss = { selectedVideo = null }
                    )
                }
            }
        }
    }

    if (memberToKick != null) {
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            title = { Text("Исключить участника?") },
            text = { Text("Вы действительно хотите исключить ${memberToKick?.user?.fullName} из беседы?") },
            confirmButton = {
                TextButton(onClick = {
                    memberToKick?.let { viewModel.kickUser(it) }
                    memberToKick = null
                }) { Text("Исключить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { memberToKick = null }) { Text("Отмена") }
            }
        )
    }

    if (showAddMembersDialog) {
        AddMembersDialog(
            onDismiss = { showAddMembersDialog = false },
            onConfirm = { userIds ->
                viewModel.addUsers(userIds) { showAddMembersDialog = false }
            }
        )
    }

    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text("Выйти из беседы?") },
            text = { Text("Вы действительно хотите покинуть беседу «${state.title}»?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirmDialog = false
                        viewModel.leaveChat { success ->
                            if (success) {
                                onBack()
                            }
                        }
                    }
                ) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = { Text("Заблокировать пользователя?") },
            text = { Text("Вы действительно хотите заблокировать ${state.title}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockConfirmDialog = false
                        viewModel.blockUser { success ->
                            if (success) {
                                onBack()
                            }
                        }
                    }
                ) {
                    Text("Заблокировать", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun MemberRow(
    member: ChatMemberItem,
    canModerate: Boolean,
    isOwner: Boolean,
    onOpenProfile: () -> Unit,
    onToggleModerator: (Boolean) -> Unit,
    onKick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenProfile() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = member.user.photo200.ifBlank { member.user.photo50 },
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            com.deliriousvoid.openvkmatcha.ui.components.OnlineIndicator(
                isOnline = member.user.online,
                isMobile = member.user.mobileOnline,
                modifier = Modifier.align(Alignment.BottomEnd),
                dotSize = 10.dp,
                iconSize = 12.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = member.user.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (member.isOwner) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Владелец",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            val statusText = if (member.user.online) {
                "В сети"
            } else if (member.user.lastSeen != null) {
                com.deliriousvoid.openvkmatcha.ui.util.formatLastSeen(member.user.lastSeen, member.user.sex ?: 0)
            } else null

            if (!statusText.isNullOrBlank()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (member.user.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (canModerate && (!member.isOwner || isOwner)) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Еще")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (isOwner && !member.isOwner) {
                        DropdownMenuItem(
                            text = { Text(if (member.isModerator) "Снять модератора" else "Назначить модератором") },
                            onClick = {
                                showMenu = false
                                onToggleModerator(!member.isModerator)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Исключить из беседы", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onKick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialTile(item: ChatMaterialItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.photoUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        if (!item.videoUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Воспроизвести",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun MaterialRow(item: ChatMaterialItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                !item.audioUrl.isNullOrBlank() -> Icons.Default.MusicNote
                !item.docUrl.isNullOrBlank() -> Icons.Default.InsertDriveFile
                else -> Icons.Default.Link
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.subtitle?.let { sub ->
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AddMembersDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
    profileRepository: com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository = OpenVKMatchaApp.instance.profileRepository
) {
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val myId = profileRepository.loadCurrentUser().getOrNull()?.id ?: 0
        profileRepository.getFriends(myId).onSuccess {
            friends = it
            isLoading = false
        }.onFailure { isLoading = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить участников") },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(friends, key = { it.id }) { user ->
                        val isSelected = selectedIds.contains(user.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isSelected) selectedIds - user.id else selectedIds + user.id
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selectedIds = if (it) selectedIds + user.id else selectedIds - user.id
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(user.fullName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty()
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
