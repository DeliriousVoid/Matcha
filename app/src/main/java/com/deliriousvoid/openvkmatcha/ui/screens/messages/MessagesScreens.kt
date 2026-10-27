package com.deliriousvoid.openvkmatcha.ui.screens.messages

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import com.deliriousvoid.openvkmatcha.ui.components.DragToSelectContainer
import com.deliriousvoid.openvkmatcha.ui.components.rememberDragToSelectState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.core.net.toUri
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.model.ChatMessage
import com.deliriousvoid.openvkmatcha.data.model.PeerType
import com.deliriousvoid.openvkmatcha.ui.components.*
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.ui.util.formatDatePill
import com.deliriousvoid.openvkmatcha.ui.util.isSameDay
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatScrollEvent
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MessagesViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    LaunchedEffect(Unit) {
        viewModel.loadConversations(isRefresh = true)
    }

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

private data class FloatingDateInfo(
    val dateText: String,
    val pushOffsetPx: Float,
    val isVisible: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerId: Int,
    title: String,
    onBack: () -> Unit,
    onOpenProfile: (Any) -> Unit = {},
    onOpenChatInfo: (Int) -> Unit = {},
    onOpenPost: (Int, Int) -> Unit = { _, _ -> },
    onOpenMusic: (Int) -> Unit = {},
    onOpenPlaylist: (Int, Int) -> Unit = { _, _ -> },
    onOpenChat: (Int, String) -> Unit = { _, _ -> },
    onSendGift: (Int) -> Unit = {},
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(peerId)),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val isScrolledUp by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }
    val scope = rememberCoroutineScope()
    var selectedMessages by remember { mutableStateOf(setOf<Int>()) }
    val dragToSelectState = rememberDragToSelectState(
        lazyListState = listState,
        items = state.messages,
        getItemKey = { it.id },
        selectedIds = selectedMessages,
        onSelectionChanged = { selectedMessages = it },
        isReverseLayout = true
    )
    var highlightedMessageId by remember { mutableStateOf<Int?>(null) }
    var viewerInitialIndex by remember { mutableIntStateOf(-1) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var messagesToDelete by remember { mutableStateOf<List<ChatMessage>?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showMusicPicker by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addAttachment(uri, AttachmentType.PHOTO)
        }
    }
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

    val tag = "chat_$peerId"
    DisposableEffect(peerId, state.isPeerTyping, selectedMessages, title, state.peer, state.isMuted) {
        AppEvents.setTopBarState(TopBarState(
            tag = tag,
            route = Routes.CHAT,
            navigationIcon = {
                if (selectedMessages.isNotEmpty()) {
                    IconButton(onClick = { selectedMessages = emptySet() }) {
                        Icon(Icons.Default.Close, contentDescription = "Отменить выделение")
                    }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            },
            customContent = {
                if (selectedMessages.isNotEmpty()) {
                    Text(
                        text = "${selectedMessages.size} выбрано",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onOpenChatInfo(peerId)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = peer?.title?.takeIf { it.isNotBlank() } ?: title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )
                                if (state.isMuted) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                        contentDescription = "Muted",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
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
                }
            },
            actions = {
                if (selectedMessages.isNotEmpty()) {
                    IconButton(onClick = {
                        val selectedMsgs = state.messages
                            .filter { it.id in selectedMessages }
                            .sortedBy { it.date }
                        val textToCopy = selectedMsgs
                            .mapNotNull { it.text.takeIf { t -> t.isNotBlank() } }
                            .joinToString("\n\n")
                        if (textToCopy.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
                        }
                        selectedMessages = emptySet()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Копировать"
                        )
                    }

                    if (selectedMessages.size == 1) {
                        IconButton(onClick = {
                            val msgToReply = state.messages.find { it.id in selectedMessages }
                            if (msgToReply != null) {
                                viewModel.startReply(msgToReply)
                            }
                            selectedMessages = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Ответить"
                            )
                        }
                    }

                    IconButton(onClick = {
                        val selectedMsgs = state.messages
                            .filter { it.id in selectedMessages }
                            .sortedBy { it.date }
                        if (selectedMsgs.isNotEmpty()) {
                            viewModel.openForwardPicker(selectedMsgs)
                        }
                        selectedMessages = emptySet()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Forward,
                            contentDescription = "Переслать"
                        )
                    }

                    IconButton(onClick = {
                        messagesToDelete = state.messages.filter { it.id in selectedMessages }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        ))
        onDispose {
            if (AppEvents.topBarState.value?.tag == tag) {
                AppEvents.setTopBarState(null)
            }
        }
    }

    LaunchedEffect(peerId) {
        viewModel.checkPendingForward()
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
                    scope.launch {
                        val currentMsgs = if (state.messages.any { it.id == event.id }) state.messages
                                          else snapshotFlow { state.messages }.first { list -> list.any { it.id == event.id } }
                        val index = currentMsgs.indexOfFirst { it.id == event.id }
                        if (index != -1) {
                            val isAlreadyVisible = listState.layoutInfo.visibleItemsInfo.any { item ->
                                currentMsgs.getOrNull(item.index)?.id == event.id
                            }

                            if (isAlreadyVisible) {
                                highlightedMessageId = event.id
                                delay(2500)
                                if (highlightedMessageId == event.id) {
                                    highlightedMessageId = null
                                }
                            } else {
                                if (event.isSmooth) {
                                    listState.animateScrollToItem(index)
                                } else {
                                    listState.scrollToItem(index)
                                }
                                highlightedMessageId = event.id
                                delay(2500)
                                if (highlightedMessageId == event.id) {
                                    highlightedMessageId = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                val visibleMsg = state.messages.getOrNull(index)
                if (visibleMsg != null && visibleMsg.id > 0) {
                    viewModel.onMessageVisible(visibleMsg.id)
                }
            }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    ) {
        com.deliriousvoid.openvkmatcha.ui.components.ChatCompactMiniPlayer()

        // Pinned message
        state.messages.find { it.isPinned }?.let { pinned ->
            PinnedMessageBar(
                message = pinned,
                onUnpin = { viewModel.unpinMessage() },
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
                DragToSelectContainer(
                    state = dragToSelectState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        contentPadding = PaddingValues(bottom = 8.dp, top = 8.dp)
                    ) {
                        itemsIndexed(state.messages, key = { _, message -> message.id }) { index, message ->
                            if (index >= state.messages.size - 5 && state.hasMore && !state.isLoadingMore && !state.isJumped && !state.isLoading) {
                                LaunchedEffect(state.messages.size) {
                                    viewModel.loadMessages(false)
                                }
                            }
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

                            val isNewDay = prevMessage == null || !isSameDay(message.date, prevMessage.date)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (isNewDay) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DatePill(
                                            dateText = formatDatePill(message.date)
                                        )
                                    }
                                }

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
                                    if (message.action != null) {
                                        val currentUserId = remember {
                                            com.deliriousvoid.openvkmatcha.data.security.AccountManager(context).getCurrentAccount()?.userId?.toIntOrNull()
                                        }
                                        SystemMessagePlaque(
                                            text = getSystemEventText(
                                                action = message.action,
                                                actorId = message.fromId,
                                                authors = state.authors,
                                                currentUserId = currentUserId
                                            )
                                        )
                                    } else {
                                        MessageBubble(
                                        message = message,
                                        authors = state.authors,
                                        showAvatar = isLastInBlock,
                                        showName = isFirstInBlock,
                                        isFirstInBlock = isFirstInBlock,
                                        isLastInBlock = isLastInBlock,
                                        isHighlighted = isHighlighted || isSelected,
                                        isSelectionMode = selectedMessages.isNotEmpty(),
                                        onSendGift = onSendGift,
                                        onReplyClick = { viewModel.startReply(it) },
                                        onEditClick = { viewModel.startEdit(it) },
                                        onCopyClick = { msg ->
                                            val textToCopy = msg.text.ifBlank {
                                                msg.attachments.mapNotNull { att ->
                                                    when (att) {
                                                        is com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Photo -> "[Фотография]"
                                                        is com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Video -> "[Видеозапись]"
                                                        is com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Audio -> "[Аудиозапись: ${att.artist} - ${att.title}]"
                                                        is com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Doc -> "[Документ: ${att.title}]"
                                                        is com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Sticker -> "[Стикер]"
                                                        is com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Gift -> if (!att.message.isNullOrBlank()) "[Подарок: ${att.message}]" else "[Подарок]"
                                                        is com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Wall -> "[Запись на стене]"
                                                        else -> null
                                                    }
                                                }.joinToString("\n")
                                            }.ifBlank {
                                                com.deliriousvoid.openvkmatcha.Constants.UNSUPPORTED_MESSAGE_TEXT
                                            }
                                            if (textToCopy.isNotBlank()) {
                                                clipboardManager.setText(AnnotatedString(textToCopy))
                                                Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onForwardClick = {
                                            viewModel.openForwardPicker(listOf(it))
                                            selectedMessages = emptySet()
                                        },
                                        onPinClick = { msg ->
                                            if (msg.isPinned) {
                                                viewModel.unpinMessage()
                                            } else {
                                                viewModel.pinMessage(msg.id)
                                            }
                                        },
                                        onDeleteClick = { msg -> messagesToDelete = listOf(msg) },
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
                                            if (!dragToSelectState.isDragging) {
                                                if (selectedMessages.contains(it.id)) selectedMessages -= it.id
                                                else selectedMessages += it.id
                                            }
                                        },
                                        onAuthorClick = { id -> onOpenProfile(id) },
                                        onProfileClick = { target -> onOpenProfile(target) },
                                        onMusicClick = onOpenMusic,
                                        onPlaylistClick = onOpenPlaylist,
                                        onChatClick = { targetPeerId -> onOpenChat(targetPeerId, "") },
                                        onUrlClick = { url ->
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                            } catch (_: Exception) {}
                                        },
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
                            }
                        }

                        if (state.isLoadingMore) {
                            item(key = "loading_more_indicator") {
                                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
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

            val density = LocalDensity.current
            val pillApproxHeightPx = remember(density) { with(density) { 36.dp.toPx() } }

            val floatingDateInfo by remember(listState, state.messages) {
                derivedStateOf {
                    @Suppress("UNUSED_VARIABLE")
                    val scrollIndex = listState.firstVisibleItemIndex
                    @Suppress("UNUSED_VARIABLE")
                    val scrollOffset = listState.firstVisibleItemScrollOffset

                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty() || state.messages.isEmpty()) {
                        return@derivedStateOf FloatingDateInfo(dateText = "", pushOffsetPx = 0f, isVisible = false)
                    }

                    // In reverseLayout = true, the item with the highest index in visibleItemsInfo is the top-most visible message on screen.
                    val topItem = visibleItems.maxByOrNull { it.index }
                        ?: return@derivedStateOf FloatingDateInfo(dateText = "", pushOffsetPx = 0f, isVisible = false)

                    val topMsg = state.messages.getOrNull(topItem.index)
                        ?: return@derivedStateOf FloatingDateInfo(dateText = "", pushOffsetPx = 0f, isVisible = false)

                    val topDateText = formatDatePill(topMsg.date)

                    var pushOffset = 0f
                    for (item in visibleItems) {
                        if (item.index >= topItem.index) continue

                        val msg = state.messages.getOrNull(item.index) ?: continue
                        val prevMsg = state.messages.getOrNull(item.index + 1)
                        val isNewDay = prevMsg == null || !isSameDay(msg.date, prevMsg.date)

                        if (isNewDay) {
                            val distancePx = (item.offset - topItem.offset).toFloat()
                            if (distancePx in 0f..pillApproxHeightPx) {
                                pushOffset = distancePx - pillApproxHeightPx
                                break
                            }
                        }
                    }

                    FloatingDateInfo(
                        dateText = topDateText,
                        pushOffsetPx = pushOffset,
                        isVisible = topDateText.isNotEmpty()
                    )
                }
            }

            val isScrollInProgress = listState.isScrollInProgress
            val datePillAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

            LaunchedEffect(isScrollInProgress) {
                if (isScrollInProgress) {
                    datePillAlpha.animateTo(1f, animationSpec = tween(150))
                } else {
                    kotlinx.coroutines.delay(1500)
                    datePillAlpha.animateTo(0f, animationSpec = tween(350))
                }
            }

            if (floatingDateInfo.isVisible && datePillAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .graphicsLayer {
                            translationY = floatingDateInfo.pushOffsetPx
                            alpha = datePillAlpha.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    DatePill(
                        dateText = floatingDateInfo.dateText
                    )
                }
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
            if (!state.canSendMessages) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Вы больше не можете отправлять сообщения",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column {
                    // Forward preview
                    AnimatedVisibility(visible = state.forwardedMessages.isNotEmpty()) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Forward, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val count = state.forwardedMessages.size
                                val subtitle = state.forwardedMessages.take(2).joinToString(", ") { msg ->
                                    val authorName = state.authors[msg.fromId]?.name ?: "ID ${msg.fromId}"
                                    when {
                                        msg.text.isNotBlank() -> "$authorName: ${msg.text}"
                                        msg.attachments.isNotEmpty() -> {
                                            val gift = msg.attachments.filterIsInstance<com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Gift>().firstOrNull()
                                            if (gift != null) "$authorName: [Подарок]" else "$authorName: [Вложение]"
                                        }
                                        msg.fwdMessages.isNotEmpty() -> "$authorName: [Пересланное сообщение]"
                                        else -> "$authorName: ${com.deliriousvoid.openvkmatcha.Constants.UNSUPPORTED_MESSAGE_TEXT}"
                                    }
                                }
                                Text("Пересылаемые сообщения ($count)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                if (subtitle.isNotBlank()) {
                                    Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = viewModel::cancelForward) { Icon(Icons.Default.Close, null) }
                        }
                    }
                    AnimatedVisibility(visible = state.replyingTo != null) {
                        state.replyingTo?.let {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val authorName = state.authors[it.fromId]?.name ?: "ID ${it.fromId}"
                                    val replyText = when {
                                        it.text.isNotBlank() -> it.text
                                        it.attachments.isNotEmpty() -> {
                                            val audio = it.attachments.filterIsInstance<com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Audio>().firstOrNull()
                                            val gift = it.attachments.filterIsInstance<com.deliriousvoid.openvkmatcha.data.model.MessageAttachment.Gift>().firstOrNull()
                                            when {
                                                audio != null -> {
                                                    val artist = audio.artist.trim()
                                                    val title = audio.title.trim()
                                                    when {
                                                        artist.isNotBlank() && title.isNotBlank() -> "[♫ $artist - $title]"
                                                        title.isNotBlank() -> "[♫ $title]"
                                                        artist.isNotBlank() -> "[♫ $artist]"
                                                        else -> "[♫ Аудиозапись]"
                                                    }
                                                }
                                                gift != null -> {
                                                    if (!gift.message.isNullOrBlank()) "[Подарок: ${gift.message}]" else "[Подарок]"
                                                }
                                                else -> matchaStringResource(R.string.attachment)
                                            }
                                        }
                                        it.fwdMessages.isNotEmpty() -> "[Пересланное сообщение]"
                                        else -> com.deliriousvoid.openvkmatcha.Constants.UNSUPPORTED_MESSAGE_TEXT
                                    }
                                    Text("Ответ $authorName", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(replyText, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

                    AnimatedVisibility(visible = state.pendingAttachments.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.pendingAttachments) { attachment ->
                                PendingAttachmentItem(
                                    attachment = attachment,
                                    onRemove = { viewModel.removeAttachment(attachment) }
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

                        Box {
                            IconButton(
                                onClick = { showAttachmentMenu = true },
                                enabled = state.pendingAttachments.size < 10 && !state.isSending
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Прикрепить вложение"
                                )
                            }

                            DropdownMenu(
                                expanded = showAttachmentMenu,
                                onDismissRequest = { showAttachmentMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Фотография") },
                                    leadingIcon = { Icon(Icons.Default.Photo, null) },
                                    onClick = {
                                        showAttachmentMenu = false
                                        photoPickerLauncher.launch("image/*")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Аудиозапись") },
                                    leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                                    onClick = {
                                        showAttachmentMenu = false
                                        showMusicPicker = true
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                showStickerPicker = false
                                viewModel.sendMessage()
                            },
                            enabled = (state.inputText.isNotBlank() || state.pendingAttachments.isNotEmpty() || state.forwardedMessages.isNotEmpty() || state.editingMessage != null) && !state.isSending,
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
    }

    messagesToDelete?.let { msgs ->
        val allOutgoing = msgs.all { it.isOutgoing }
        AlertDialog(
            onDismissRequest = { messagesToDelete = null },
            title = { Text(if (msgs.size > 1) "Удалить сообщения?" else "Удалить сообщение?") },
            text = {
                Text(
                    if (msgs.size > 1)
                        "Выберите параметр удаления для ${msgs.size} сообщений"
                    else
                        "Вы уверены, что хотите удалить это сообщение?"
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (allOutgoing) {
                        TextButton(onClick = {
                            viewModel.deleteMessages(msgs.map { it.id }, forAll = true)
                            selectedMessages = emptySet()
                            messagesToDelete = null
                        }) {
                            Text("Удалить у всех", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = {
                        viewModel.deleteMessages(msgs.map { it.id }, forAll = false)
                        selectedMessages = emptySet()
                        messagesToDelete = null
                    }) {
                        Text("Удалить у себя", color = if (!allOutgoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { messagesToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showMusicPicker) {
        MusicPickerBottomSheet(
            onDismiss = { showMusicPicker = false },
            onTrackSelect = { track ->
                showMusicPicker = false
                viewModel.addExistingAttachment(
                    "audio${track.ownerId}_${track.id}",
                    AttachmentType.AUDIO,
                    "${track.artist} - ${track.title}"
                )
            }
        )
    }

    if (state.messagesToForward != null) {
        ForwardRecipientBottomSheet(
            messagesToForward = state.messagesToForward!!,
            conversations = state.forwardDialogs,
            isLoading = state.isLoadingForwardDialogs,
            currentPeerId = peerId,
            onDismiss = viewModel::closeForwardPicker,
            onSelectRecipient = { targetPeerId, targetTitle ->
                val msgsToForward = state.messagesToForward!!
                if (targetPeerId == peerId) {
                    viewModel.confirmForwardToCurrentChat()
                } else {
                    viewModel.closeForwardPicker()
                    selectedMessages = emptySet()
                    AppEvents.setPendingForward(targetPeerId, msgsToForward)
                    onOpenChat(targetPeerId, targetTitle)
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
    if (peer == null || peer.id == 0) return null
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



