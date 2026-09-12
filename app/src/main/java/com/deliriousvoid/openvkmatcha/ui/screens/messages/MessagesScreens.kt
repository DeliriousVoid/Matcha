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
    viewModel: MessagesViewModel = viewModel(factory = MessagesViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()

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
                    ConversationItem(
                        conversation = conversation,
                        typingStatus = state.typingPeers[conversation.peer.id],
                        onClick = { onConversationClick(conversation.peer.id, conversation.peer.title) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    )
                }
            }
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val baseUrl = OpenVKMatchaApp.instance.api.baseUrl

    androidx.activity.compose.BackHandler(enabled = selectedMessages.isNotEmpty()) {
        selectedMessages = emptySet()
    }

    DisposableEffect(state.isPeerTyping, selectedMessages, title) {
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
                        .clickable { onOpenProfile(peerId) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val peer = state.peer
                    if (peer != null) {
                        if (peer.photo.isNotBlank()) {
                            AsyncImage(
                                model = peer.photo,
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
                                    imageVector = when (peer.type) {
                                        PeerType.CHAT -> Icons.Default.Group
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        if (state.isPeerTyping) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TypingIndicator(modifier = Modifier.padding(end = 6.dp))
                                Text(
                                    text = "печатает...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            actions = {
                if (selectedMessages.isNotEmpty()) {
                    IconButton(onClick = { 
                        viewModel.deleteMessages(selectedMessages.toList())
                        selectedMessages = emptySet()
                    }) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, matchaStringResource(R.string.desc_more))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                    IconButton(onClick = { /* TODO: Attachments */ }) {
                        Icon(Icons.Default.Add, null)
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
                        onClick = viewModel::sendMessage,
                        enabled = state.inputText.isNotBlank() || state.editingMessage != null,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.isSending) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }
            }
        }
    }
}
