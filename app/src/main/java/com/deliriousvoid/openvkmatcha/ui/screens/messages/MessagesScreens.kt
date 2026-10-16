package com.deliriousvoid.openvkmatcha.ui.screens.messages

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.core.net.toUri
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.model.ChatMessage
import com.deliriousvoid.openvkmatcha.data.model.PeerType
import com.deliriousvoid.openvkmatcha.ui.components.*
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatScrollEvent
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MessagesViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onConversationClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    onCreateChat: () -> Unit = {},
    viewModel: MessagesViewModel = viewModel(factory = MessagesViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedConversationForMenu by remember { mutableStateOf<com.deliriousvoid.openvkmatcha.data.model.Conversation?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && state.conversations.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
            state.error != null && state.conversations.isEmpty() -> ErrorText(
                message = state.error!!,
                modifier = Modifier.fillMaxSize(),
                onRetry = { viewModel.loadConversations() }
            )
            state.conversations.isEmpty() -> EmptyState(
                message = matchaStringResource(R.string.messages_empty),
                modifier = Modifier.fillMaxSize()
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.conversations, key = { it.peer.id }) { conversation ->
                    Box {
                        ConversationItem(
                            conversation = conversation,
                            typingStatus = state.typingPeers[conversation.peer.id],
                            onClick = { onConversationClick(conversation.peer.id, conversation.peer.title) },
                            onLongClick = { selectedConversationForMenu = conversation }
                        )
                        DropdownMenu(
                            expanded = selectedConversationForMenu == conversation,
                            onDismissRequest = { selectedConversationForMenu = null }
                        ) {
                            if (conversation.unreadCount > 0) {
                                DropdownMenuItem(
                                    text = { Text("Пометить как прочитанное") },
                                    onClick = {
                                        selectedConversationForMenu = null
                                        viewModel.markConversationAsRead(conversation.peer.id)
                                    }
                                )
                            }
                            if (conversation.peer.type == PeerType.CHAT) {
                                DropdownMenuItem(
                                    text = { Text("Выйти из беседы", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        selectedConversationForMenu = null
                                        viewModel.leaveChat(conversation.peer.id)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Удалить чат", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    selectedConversationForMenu = null
                                    viewModel.deleteConversation(conversation.peer.id)
                                }
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateChat,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Новая беседа")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerId: Int,
    title: String,
    onBack: () -> Unit,
    onOpenProfile: (Int) -> Unit = {},
    onOpenChatInfo: (Int) -> Unit = {},
    onOpenPost: (Int, Int) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(peerId)),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val isScrolledUp by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }
    val scope = rememberCoroutineScope()
    var selectedMessages by remember { mutableStateOf(setOf<Int>()) }
    var highlightedMessageId by remember { mutableStateOf<Int?>(null) }
    var viewerInitialIndex by remember { mutableIntStateOf(-1) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val baseUrl = OpenVKMatchaApp.instance.api.baseUrl

    androidx.activity.compose.BackHandler(enabled = selectedMessages.isNotEmpty() || showStickerPicker) {
        if (showStickerPicker) {
            showStickerPicker = false
        } else {
            selectedMessages = emptySet()
        }
    }

    DisposableEffect(state.isPeerTyping, selectedMessages, title, state.peer) {
        AppEvents.setTopBarState(TopBarState(
            route = Routes.CHAT,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            },
            customContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (peerId >= 2000000000 || state.peer?.type == PeerType.CHAT) {
                                onOpenChatInfo(peerId)
                            } else {
                                onOpenProfile(peerId)
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    val peer = state.peer
                    val photoUrl = peer?.photo?.takeIf { it.isNotBlank() }

                    Box(contentAlignment = Alignment.Center) {
                        if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        peerId >= 2000000000 || peer?.type == PeerType.CHAT -> Icons.Default.Group
                                        peerId < 0 || peer?.type == PeerType.GROUP -> Icons.Default.Groups
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (peer?.type == PeerType.USER) {
                            OnlineIndicator(
                                isOnline = peer.online,
                                isMobile = peer.mobileOnline,
                                modifier = Modifier.align(Alignment.BottomEnd),
                                dotSize = 8.dp,
                                iconSize = 10.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = peer?.title?.takeIf { it.isNotBlank() } ?: title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        if (state.isPeerTyping || state.typingText != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TypingIndicator(modifier = Modifier.padding(end = 6.dp))
                                Text(
                                    text = state.typingText ?: "печатает...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            val statusText = formatPeerStatus(peer)
                            if (!statusText.isNullOrBlank()) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (peer?.online == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            actions = {
                if (selectedMessages.isNotEmpty()) {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, matchaStringResource(R.string.desc_more))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (peerId >= 2000000000 || state.peer?.type == PeerType.CHAT) {
                            DropdownMenuItem(
                                text = { Text("Информация о беседе") },
                                onClick = {
                                    showMenu = false
                                    onOpenChatInfo(peerId)
                                }
                            )
                        }
                        val currentUrl = "$baseUrl/im?sel=$peerId"
                        DropdownMenuItem(
                            text = { Text(matchaStringResource(R.string.copy_link)) },
                            onClick = { 
                                showMenu = false
                                clipboardManager.setText(AnnotatedString(currentUrl))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(matchaStringResource(R.string.open_in_browser)) },
                            onClick = { 
                                showMenu = false
                                context.startActivity(Intent(Intent.ACTION_VIEW, currentUrl.toUri()))
                            }
                        )
                    }
                }
            }
        ))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scrollEvents.collect { event ->
            when (event) {
                is ChatScrollEvent.ScrollToBottom -> {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(0)
                    }
                }
                is ChatScrollEvent.JumpToId -> {
                    val index = state.messages.indexOfFirst { it.id == event.id }
                    if (index != -1) {
                        val viewportHeight = listState.layoutInfo.viewportSize.height
                        // Используем 2/3 высоты для позиционирования в верхней трети экрана (в reverseLayout).
                        val offset = viewportHeight * 2 / 3
                        if (event.isSmooth) {
                            listState.animateScrollToItem(index, offset)
                        } else {
                            listState.scrollToItem(index, offset)
                        }
                        highlightedMessageId = event.id
                        scope.launch {
                            delay(2000)
                            if (highlightedMessageId == event.id) {
                                highlightedMessageId = null
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Pinned message
        state.messages.find { it.isPinned }?.let { pinned ->
            PinnedMessageBar(
                message = pinned,
                onUnpin = { /* TODO */ },
                onClick = { 
                    val index = state.messages.indexOf(pinned)
                    if (index != -1) scope.launch { listState.animateScrollToItem(index) }
                }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (state.isLoading && state.messages.isEmpty()) {
                LoadingBox(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(state.messages, key = { _, message -> message.id }) { index, message ->
                        val nextMessage = state.messages.getOrNull(index - 1) // next is newer, so index - 1
                        val prevMessage = state.messages.getOrNull(index + 1) // prev is older, so index + 1
                        
                        val isFirstInBlock = prevMessage == null || prevMessage.fromId != message.fromId
                        val isLastInBlock = nextMessage == null || nextMessage.fromId != message.fromId

                        val isSelected = selectedMessages.contains(message.id)
                        val isHighlighted = message.id == highlightedMessageId
                        val highlightColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else -> Color.Transparent
                            },
                            animationSpec = if (isHighlighted || isSelected) tween(300) else tween(1000),
                            label = "highlightColor"
                        )

                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .background(highlightColor)
                            .clickable(
                                enabled = selectedMessages.isNotEmpty(),
                                onClick = {
                                    if (selectedMessages.contains(message.id)) selectedMessages -= message.id
                                    else selectedMessages += message.id
                                }
                            )
                        ) {
                            MessageBubble(
                                message = message,
                                authors = state.authors,
                                showAvatar = isLastInBlock,
                                showName = isFirstInBlock,
                                isHighlighted = isHighlighted || isSelected,
                                onClick = {
                                    if (selectedMessages.isNotEmpty()) {
                                        if (selectedMessages.contains(message.id)) selectedMessages -= message.id
                                        else selectedMessages += message.id
                                    }
                                },
                                onReplyPreviewClick = { replyId ->
                                    viewModel.jumpToMessage(replyId)
                                },
                                onLongClick = { 
                                    if (selectedMessages.contains(it.id)) selectedMessages -= it.id
                                    else selectedMessages += it.id
                                },
                                onAuthorClick = onOpenProfile,
                                onImageClick = { images, index ->
                                    viewerImages = images
                                    viewerInitialIndex = index
                                },
                                onAudioClick = { tracks, index ->
                                    OpenVKMatchaApp.instance.playerManager.play(tracks, index, com.deliriousvoid.openvkmatcha.data.model.PlaylistSource.Unknown)
                                },
                                onVideoClick = { video ->
                                    val urlStr = video.videoUrl ?: video.playerUrl ?: "$baseUrl/video${video.ownerId}_${video.id}"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, urlStr.toUri()))
                                },
                                onDocumentClick = { doc ->
                                    val docUrl = doc.url
                                    if (!docUrl.isNullOrBlank()) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, docUrl.toUri()))
                                    }
                                },
                                onWallPostClick = { ownerId, postId ->
                                    onOpenPost(ownerId, postId)
                                }
                            )
                        }
                    }

                    if (state.hasMore && !state.isJumped) {
                        item(key = "load_more") {
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                if (state.isLoadingMore) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                else TextButton(onClick = { viewModel.loadMessages(false) }) { Text("Загрузить еще") }
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = state.isJumped || state.jumpSourceId != null || isScrolledUp,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (state.isJumped || state.jumpSourceId != null) {
                            viewModel.returnFromJump()
                        } else {
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (state.isJumped || state.jumpSourceId != null) 
                            Icons.Default.KeyboardDoubleArrowDown else Icons.Default.ArrowDownward,
                        contentDescription = null
                    )
                }
            }
            
            if (state.isLoading) {
                LoadingBox(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f)))
            }
        }

        if (viewerInitialIndex != -1) {
            ImageViewer(
                imageUrls = viewerImages,
                initialIndex = viewerInitialIndex,
                onDismiss = { viewerInitialIndex = -1 }
            )
        }

        // Input Bar
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Reply/Edit preview
                AnimatedVisibility(visible = state.replyingTo != null) {
                    state.replyingTo?.let {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val authorName = state.authors[it.fromId]?.name ?: "ID ${it.fromId}"
                                Text("Ответ $authorName", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(it.text, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = viewModel::cancelReply) { Icon(Icons.Default.Close, null) }
                        }
                    }
                }

                AnimatedVisibility(visible = state.editingMessage != null) {
                    state.editingMessage?.let {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Редактирование", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(it.text, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = viewModel::cancelEdit) { Icon(Icons.Default.Close, null) }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        showStickerPicker = !showStickerPicker
                        if (showStickerPicker) {
                            viewModel.loadStickerPacks()
                        }
                    }) {
                        Icon(
                            imageVector = if (showStickerPicker) Icons.Default.Keyboard else Icons.Default.Mood,
                            contentDescription = "Стикеры"
                        )
                    }

                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = viewModel::updateInput,
                        placeholder = { Text(matchaStringResource(R.string.chat_input_placeholder)) },
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        )
                    )

                    IconButton(
                        onClick = {
                            showStickerPicker = false
                            viewModel.sendMessage()
                        },
                        enabled = state.inputText.isNotBlank() || state.editingMessage != null,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }

                AnimatedVisibility(visible = showStickerPicker) {
                    StickerPickerPanel(
                        stickerPacks = state.stickerPacks,
                        recentStickers = state.recentStickers,
                        isLoading = state.isLoadingStickers,
                        onStickerSelected = { sticker ->
                            viewModel.sendSticker(sticker)
                            showStickerPicker = false
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удалить сообщения?") },
            text = { Text("Выберите параметр удаления для ${selectedMessages.size} сообщений") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMessages(selectedMessages.toList(), forAll = true)
                    selectedMessages = emptySet()
                    showDeleteConfirmDialog = false
                }) {
                    Text("Удалить у всех", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.deleteMessages(selectedMessages.toList(), forAll = false)
                        selectedMessages = emptySet()
                        showDeleteConfirmDialog = false
                    }) {
                        Text("Удалить у себя")
                    }
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Отмена")
                    }
                }
            }
        )
    }
}

private fun getPlatformName(platform: Int): String? {
    return when (platform) {
        1 -> "мобильная версия"
        2 -> "iPhone"
        3 -> "iPad"
        4 -> "Android"
        5 -> "Windows Phone"
        6 -> "Windows 10"
        7 -> "Web"
        else -> null
    }
}

private fun formatPeerStatus(peer: com.deliriousvoid.openvkmatcha.data.model.Peer?): String? {
    if (peer == null) return null
    return when (peer.type) {
        com.deliriousvoid.openvkmatcha.data.model.PeerType.USER -> {
            if (peer.online) {
                val platform = peer.lastSeen?.platform?.let { getPlatformName(it) }
                if (!platform.isNullOrBlank()) "В сети · $platform"
                else if (peer.mobileOnline) "В сети · Мобильная версия"
                else "В сети"
            } else if (peer.lastSeen != null && peer.lastSeen.time > 0) {
                val timeStr = com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo(peer.lastSeen.time)
                if (timeStr.isNotBlank()) "был(а) в сети $timeStr" else "не в сети"
            } else {
                "не в сети"
            }
        }
        com.deliriousvoid.openvkmatcha.data.model.PeerType.GROUP -> "сообщество"
        com.deliriousvoid.openvkmatcha.data.model.PeerType.CHAT -> {
            val count = peer.membersCount
            if (count != null && count > 0) "$count участников" else "беседа"
        }
    }
}
