package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.model.*
import com.deliriousvoid.openvkmatcha.ui.util.formatMessageTime
import com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun handleProfileTarget(target: String, onAuthorClick: (Int) -> Unit, onProfileClick: (String) -> Unit) {
    val id = target.toIntOrNull()
    if (id != null) {
        onAuthorClick(id)
    } else {
        val parsedId = when {
            target.startsWith("id", ignoreCase = true) && target.substring(2).all { it.isDigit() } -> target.substring(2).toIntOrNull()
            target.startsWith("club", ignoreCase = true) && target.substring(4).all { it.isDigit() } -> target.substring(4).toIntOrNull()?.let { -it }
            target.startsWith("public", ignoreCase = true) && target.substring(6).all { it.isDigit() } -> target.substring(6).toIntOrNull()?.let { -it }
            target.startsWith("event", ignoreCase = true) && target.substring(5).all { it.isDigit() } -> target.substring(5).toIntOrNull()?.let { -it }
            else -> null
        }
        if (parsedId != null) {
            onAuthorClick(parsedId)
        } else {
            onProfileClick(target)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    authors: Map<Int, AuthorInfo> = emptyMap(),
    showAvatar: Boolean = true,
    showName: Boolean = true,
    isFirstInBlock: Boolean = true,
    isLastInBlock: Boolean = true,
    isHighlighted: Boolean = false,
    isSelectionMode: Boolean = false,
    onReplyClick: (ChatMessage) -> Unit = {},
    onEditClick: (ChatMessage) -> Unit = {},
    onCopyClick: (ChatMessage) -> Unit = {},
    onForwardClick: (ChatMessage) -> Unit = {},
    onPinClick: (ChatMessage) -> Unit = {},
    onDeleteClick: (ChatMessage) -> Unit = {},
    onReplyPreviewClick: (Int) -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: (ChatMessage) -> Unit = {},
    onAuthorClick: (Int) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    onAudioClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onVideoClick: (Video) -> Unit = {},
    onDocumentClick: (Document) -> Unit = {},
    onWallPostClick: (Int, Int) -> Unit = { _, _ -> },
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onChatClick: (Int) -> Unit = {},
    onUrlClick: (String) -> Unit = {},
    onSendGift: (Int) -> Unit = {}
) {
    val isOut = message.isOutgoing
    val isGroup = message.peerId >= 2000000000
    val author = authors[message.fromId]
    val targetUserId = if (!isOut && message.fromId > 0) message.fromId else if (message.peerId in 1..1999999999) message.peerId else message.fromId
    val isGiftOnly = message.text.isBlank() && message.replyMessage == null && message.fwdMessages.isEmpty() && message.attachments.singleOrNull() is MessageAttachment.Gift
    val isStickerOnly = message.text.isBlank() && message.replyMessage == null && message.fwdMessages.isEmpty() && (message.attachments.singleOrNull() is MessageAttachment.Sticker || (message.attachments.isEmpty() && message.stickerId != null))
    val isTransparentBubble = isGiftOnly || isStickerOnly

    var showMenu by remember { mutableStateOf(false) }
    LaunchedEffect(isSelectionMode) {
        if (isSelectionMode) showMenu = false
    }

    val dragOffsetState = remember(message.id) { mutableFloatStateOf(0f) }
    val animatableOffset = remember(message.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var hasTriggeredHaptic by remember(message.id) { mutableStateOf(false) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val thresholdPx = with(density) { 44.dp.toPx() }
    val maxSwipePx = with(density) { 96.dp.toPx() }
    val totalOffset = animatableOffset.value + dragOffsetState.floatValue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        hasTriggeredHaptic = false
                        dragOffsetState.floatValue = 0f
                        scope.launch { animatableOffset.snapTo(0f) }
                    },
                    onDragEnd = {
                        val finalOffset = dragOffsetState.floatValue
                        if (kotlin.math.abs(finalOffset) >= thresholdPx) {
                            onReplyClick(message)
                        }
                        scope.launch {
                            animatableOffset.snapTo(dragOffsetState.floatValue)
                            dragOffsetState.floatValue = 0f
                            animatableOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            animatableOffset.snapTo(dragOffsetState.floatValue)
                            dragOffsetState.floatValue = 0f
                            animatableOffset.animateTo(0f)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (message.id > 0 && !message.isDeleted) {
                            val newOffset = (dragOffsetState.floatValue + dragAmount).coerceIn(-maxSwipePx, 0f)
                            if (newOffset != dragOffsetState.floatValue) {
                                change.consume()
                                dragOffsetState.floatValue = newOffset
                                if (kotlin.math.abs(newOffset) >= thresholdPx && !hasTriggeredHaptic) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    hasTriggeredHaptic = true
                                }
                            }
                        }
                    }
                )
            }
    ) {
        if (kotlin.math.abs(totalOffset) > 8f) {
            val replyIconAlpha = (kotlin.math.abs(totalOffset) / thresholdPx).coerceIn(0f, 1f)
            val replyIconScale = (0.5f + (kotlin.math.abs(totalOffset) / thresholdPx) * 0.5f).coerceIn(0.5f, 1f)

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = replyIconAlpha
                        scaleX = replyIconScale
                        scaleY = replyIconScale
                    }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Ответить",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { androidx.compose.ui.unit.IntOffset(totalOffset.toInt(), 0) }
                .padding(vertical = 1.dp, horizontal = 8.dp),
            horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isOut && isGroup) {
                if (showAvatar) {
                    AsyncImage(
                        model = author?.avatar,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onAuthorClick(message.fromId) },
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Spacer(modifier = Modifier.width(44.dp)) // 36dp (avatar) + 8dp (padding)
                }
            }

        Box {
            Surface(
                color = if (isTransparentBubble) Color.Transparent else if (isOut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = if (isTransparentBubble) 0.dp else 0.dp,
                shape = RoundedCornerShape(
                    topStart = if (!isOut && isGroup && !showName) 4.dp else (if (!isOut && !isFirstInBlock) 4.dp else 16.dp),
                    topEnd = if (isOut && !isFirstInBlock) 4.dp else 16.dp,
                    bottomStart = if (isOut) 16.dp else (if (isGroup && !showAvatar) 16.dp else (if (!isLastInBlock) 4.dp else 4.dp)),
                    bottomEnd = if (isOut) (if (!isLastInBlock) 4.dp else 4.dp) else 16.dp
                ),
                modifier = Modifier
                    .widthIn(max = if (isTransparentBubble) 300.dp else 280.dp)
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onClick()
                            } else if (!message.isDeleted) {
                                showMenu = true
                            }
                        },
                        onLongClick = { if (!message.isDeleted) onLongClick(message) }
                    )
            ) {
                Column(modifier = Modifier.padding(if (isTransparentBubble) 0.dp else 9.dp)) {
                    if (!isOut && isGroup && author != null && showName) {
                        Text(
                            text = author.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp).clickable { onAuthorClick(message.fromId) }
                        )
                    }

                    if (message.isDeleted) {
                        Text(
                            text = "Сообщение удалено",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = (if (isOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                        )
                    } else {
                        val hasContent = message.text.isNotBlank() || message.attachments.isNotEmpty() || message.fwdMessages.isNotEmpty() || message.replyMessage != null || message.stickerId != null

                        if (hasContent) {
                            if (message.replyMessage != null) {
                                ReplyPreview(
                                    message = message.replyMessage,
                                    isOutgoingBubble = isOut,
                                    authorName = authors[message.replyMessage.fromId]?.name,
                                    onClick = { onReplyPreviewClick(message.replyMessage.id) }
                                )
                            }

                            if (message.attachments.isNotEmpty()) {
                                MessageAttachments(
                                    attachments = message.attachments,
                                    isOutgoingBubble = isOut,
                                    onImageClick = onImageClick,
                                    onAudioClick = onAudioClick,
                                    onVideoClick = onVideoClick,
                                    onDocumentClick = onDocumentClick,
                                    onWallPostClick = onWallPostClick,
                                    onMusicClick = onMusicClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onChatClick = onChatClick,
                                    onUrlClick = onUrlClick,
                                    onAuthorClick = onAuthorClick,
                                    onProfileClick = onProfileClick,
                                    onSendGiftClick = { if (targetUserId > 0) onSendGift(targetUserId) }
                                )
                            }

                            if (message.text.isNotBlank()) {
                                val bubbleTextColor = if (isOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                val bubbleLinkColor = MaterialTheme.colorScheme.primary

                                ParsedText(
                                    text = message.text,
                                    onMentionClick = { onAuthorClick(it) },
                                    onProfileClick = { target -> handleProfileTarget(target, onAuthorClick, onProfileClick) },
                                    onUrlClick = onUrlClick,
                                    onWallClick = onWallPostClick,
                                    onMusicClick = onMusicClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onChatClick = onChatClick,
                                    onDefaultClick = {
                                        if (isSelectionMode) {
                                            onClick()
                                        } else {
                                            showMenu = true
                                        }
                                    },
                                    onDefaultLongClick = { onLongClick(message) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = bubbleTextColor,
                                    linkColor = bubbleLinkColor
                                )
                            }

                            if (message.fwdMessages.isNotEmpty()) {
                                ForwardedMessages(
                                    fwdMessages = message.fwdMessages,
                                    isOutgoingBubble = isOut,
                                    authors = authors,
                                    onImageClick = onImageClick,
                                    onAudioClick = onAudioClick,
                                    onVideoClick = onVideoClick,
                                    onDocumentClick = onDocumentClick,
                                    onWallPostClick = onWallPostClick,
                                    onAuthorClick = onAuthorClick,
                                    onProfileClick = onProfileClick,
                                    onMusicClick = onMusicClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onChatClick = onChatClick,
                                    onUrlClick = onUrlClick
                                )
                            }
                        } else {
                            val bubbleTextColor = if (isOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            val bubbleLinkColor = MaterialTheme.colorScheme.primary

                            ParsedText(
                                text = com.deliriousvoid.openvkmatcha.Constants.UNSUPPORTED_MESSAGE_TEXT,
                                onMentionClick = { onAuthorClick(it) },
                                onProfileClick = { target -> handleProfileTarget(target, onAuthorClick, onProfileClick) },
                                onUrlClick = onUrlClick,
                                onWallClick = onWallPostClick,
                                onMusicClick = onMusicClick,
                                onPlaylistClick = onPlaylistClick,
                                onChatClick = onChatClick,
                                onDefaultClick = {
                                    if (isSelectionMode) {
                                        onClick()
                                    } else {
                                        showMenu = true
                                    }
                                },
                                onDefaultLongClick = { onLongClick(message) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = bubbleTextColor,
                                linkColor = bubbleLinkColor
                            )
                        }
                    }

                    if (!isGiftOnly) {
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!message.isDeleted && message.isEdited) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = matchaStringResource(R.string.edited_marker),
                                    tint = (if (isOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .padding(end = 2.dp)
                                        .size(12.dp)
                                )
                            }
                            Text(
                                text = formatMessageTime(message.date),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = (if (isOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.75f)
                            )
                            if (isOut && !message.isDeleted) {
                                Spacer(modifier = Modifier.width(4.dp))
                                if (message.id < 0) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(matchaStringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDeleteClick(message)
                    }
                )
                if (message.isOutgoing) {
                    DropdownMenuItem(
                        text = { Text(matchaStringResource(R.string.action_edit)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showMenu = false
                            onEditClick(message)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(matchaStringResource(R.string.action_reply)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showMenu = false
                        onReplyClick(message)
                    }
                )
                DropdownMenuItem(
                    text = { Text(matchaStringResource(R.string.action_copy)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showMenu = false
                        onCopyClick(message)
                    }
                )
                DropdownMenuItem(
                    text = { Text(matchaStringResource(R.string.action_forward)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Forward,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showMenu = false
                        onForwardClick(message)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (message.isPinned) matchaStringResource(R.string.action_unpin)
                            else matchaStringResource(R.string.action_pin)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (message.isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showMenu = false
                        onPinClick(message)
                    }
                )
            }
        }
    }
}
}

@Composable
fun ReplyPreview(
    message: ChatMessage,
    isOutgoingBubble: Boolean,
    authorName: String? = null,
    onClick: () -> Unit = {}
) {
    val containerColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val accentColor = MaterialTheme.colorScheme.primary
    val titleColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
    val textColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(bottom = 6.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(32.dp)
                    .background(accentColor, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = authorName ?: "ID ${message.fromId}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                val previewText = when {
                    message.text.isNotBlank() -> message.text
                    message.attachments.isNotEmpty() -> {
                        val audio = message.attachments.filterIsInstance<MessageAttachment.Audio>().firstOrNull()
                        if (audio != null) {
                            val artist = audio.artist.trim()
                            val title = audio.title.trim()
                            when {
                                artist.isNotBlank() && title.isNotBlank() -> "[♫ $artist - $title]"
                                title.isNotBlank() -> "[♫ $title]"
                                artist.isNotBlank() -> "[♫ $artist]"
                                else -> "[♫ Аудиозапись]"
                            }
                        } else {
                            matchaStringResource(R.string.attachment)
                        }
                    }
                    message.fwdMessages.isNotEmpty() -> "[Пересланное сообщение]"
                    else -> com.deliriousvoid.openvkmatcha.Constants.UNSUPPORTED_MESSAGE_TEXT
                }
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun ForwardedMessages(
    fwdMessages: List<ChatMessage>,
    isOutgoingBubble: Boolean,
    authors: Map<Int, AuthorInfo>,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    onAudioClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onVideoClick: (Video) -> Unit = {},
    onDocumentClick: (Document) -> Unit = {},
    onWallPostClick: (Int, Int) -> Unit = { _, _ -> },
    onAuthorClick: (Int) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onChatClick: (Int) -> Unit = {},
    onUrlClick: (String) -> Unit = {}
) {
    val containerColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val accentColor = MaterialTheme.colorScheme.primary
    val titleColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
    val fwdTextColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val timeColor = (if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.65f)

    Column(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .fillMaxWidth()
    ) {
        fwdMessages.forEach { fwd ->
            Surface(
                color = containerColor,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(6.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .fillMaxHeight()
                            .background(
                                accentColor,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val authorName = authors[fwd.fromId]?.name ?: "ID ${fwd.fromId}"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = authorName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = titleColor,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clickable { onAuthorClick(fwd.fromId) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatTimeAgo(fwd.date),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = timeColor
                            )
                        }

                        val hasFwdContent = fwd.text.isNotBlank() || fwd.attachments.isNotEmpty() || fwd.fwdMessages.isNotEmpty() || fwd.replyMessage != null || fwd.stickerId != null

                        if (fwd.text.isNotBlank()) {
                            ParsedText(
                                text = fwd.text,
                                onMentionClick = { onAuthorClick(it) },
                                onProfileClick = { target -> handleProfileTarget(target, onAuthorClick, onProfileClick) },
                                onUrlClick = onUrlClick,
                                onWallClick = onWallPostClick,
                                onMusicClick = onMusicClick,
                                onPlaylistClick = onPlaylistClick,
                                onChatClick = onChatClick,
                                style = MaterialTheme.typography.bodySmall,
                                color = fwdTextColor,
                                linkColor = accentColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if (fwd.attachments.isNotEmpty()) {
                            MessageAttachments(
                                attachments = fwd.attachments,
                                isOutgoingBubble = isOutgoingBubble,
                                onImageClick = onImageClick,
                                onAudioClick = onAudioClick,
                                onVideoClick = onVideoClick,
                                onDocumentClick = onDocumentClick,
                                onWallPostClick = onWallPostClick,
                                onMusicClick = onMusicClick,
                                onPlaylistClick = onPlaylistClick,
                                onChatClick = onChatClick,
                                onUrlClick = onUrlClick,
                                onAuthorClick = onAuthorClick,
                                onProfileClick = onProfileClick
                            )
                        }

                        if (fwd.fwdMessages.isNotEmpty()) {
                            ForwardedMessages(
                                fwdMessages = fwd.fwdMessages,
                                isOutgoingBubble = isOutgoingBubble,
                                authors = authors,
                                onImageClick = onImageClick,
                                onAudioClick = onAudioClick,
                                onVideoClick = onVideoClick,
                                onDocumentClick = onDocumentClick,
                                onWallPostClick = onWallPostClick,
                                onAuthorClick = onAuthorClick,
                                onProfileClick = onProfileClick,
                                onMusicClick = onMusicClick,
                                onPlaylistClick = onPlaylistClick,
                                onChatClick = onChatClick,
                                onUrlClick = onUrlClick
                            )
                        }

                        if (!hasFwdContent) {
                            ParsedText(
                                text = com.deliriousvoid.openvkmatcha.Constants.UNSUPPORTED_MESSAGE_TEXT,
                                onMentionClick = { onAuthorClick(it) },
                                onProfileClick = { target -> handleProfileTarget(target, onAuthorClick, onProfileClick) },
                                onUrlClick = onUrlClick,
                                onWallClick = onWallPostClick,
                                onMusicClick = onMusicClick,
                                onPlaylistClick = onPlaylistClick,
                                onChatClick = onChatClick,
                                style = MaterialTheme.typography.bodySmall,
                                color = fwdTextColor,
                                linkColor = accentColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageWallPostCard(
    wall: MessageAttachment.Wall,
    isOutgoingBubble: Boolean,
    onWallPostClick: (Int, Int) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onAudioClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onVideoClick: (Video) -> Unit = {},
    onDocumentClick: (Document) -> Unit = {},
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onChatClick: (Int) -> Unit = {},
    onUrlClick: (String) -> Unit = {},
    onAuthorClick: (Int) -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
    val containerColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val accentColor = MaterialTheme.colorScheme.primary
    val titleColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
    val wallTextColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWallPostClick(wall.ownerId, wall.id) }
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = accentColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = wall.authorName ?: "Запись на стене",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }

            if (wall.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                ParsedText(
                    text = wall.text,
                    onMentionClick = { onAuthorClick(it) },
                    onProfileClick = { target -> handleProfileTarget(target, onAuthorClick, onProfileClick) },
                    onUrlClick = onUrlClick,
                    onWallClick = onWallPostClick,
                    onMusicClick = onMusicClick,
                    onPlaylistClick = onPlaylistClick,
                    onChatClick = onChatClick,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = wallTextColor,
                    linkColor = accentColor
                )
            }

            if (wall.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                MessageAttachments(
                    attachments = wall.attachments,
                    isOutgoingBubble = isOutgoingBubble,
                    onImageClick = onImageClick,
                    onAudioClick = onAudioClick,
                    onVideoClick = onVideoClick,
                    onDocumentClick = onDocumentClick,
                    onWallPostClick = onWallPostClick,
                    onMusicClick = onMusicClick,
                    onPlaylistClick = onPlaylistClick,
                    onChatClick = onChatClick,
                    onUrlClick = onUrlClick,
                    onAuthorClick = onAuthorClick,
                    onProfileClick = onProfileClick
                )
            }
        }
    }
}

@Composable
fun MessageAttachments(
    attachments: List<MessageAttachment>,
    isOutgoingBubble: Boolean = false,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    onAudioClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onVideoClick: (Video) -> Unit = {},
    onDocumentClick: (Document) -> Unit = {},
    onWallPostClick: (Int, Int) -> Unit = { _, _ -> },
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onChatClick: (Int) -> Unit = {},
    onUrlClick: (String) -> Unit = {},
    onAuthorClick: (Int) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onSendGiftClick: () -> Unit = {}
) {
    val photos = attachments.filterIsInstance<MessageAttachment.Photo>()
    val gifDocs = attachments.filterIsInstance<MessageAttachment.Doc>().filter { 
        it.ext.lowercase() == "gif" || !it.previewGifUrl.isNullOrBlank() || it.url?.lowercase()?.contains("gif") == true
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (photos.isNotEmpty()) {
            val photoUrls = photos.map { it.url }
            PostImageGrid(
                imageUrls = photoUrls,
                onImageClick = { index -> onImageClick(photoUrls, index) },
                isDialog = true
            )
        }

        gifDocs.forEach { attachment ->
            val doc = Document(
                id = attachment.id,
                ownerId = attachment.ownerId,
                title = attachment.title,
                size = attachment.size,
                ext = attachment.ext,
                url = attachment.url,
                date = 0,
                type = 3,
                previewUrl = attachment.previewUrl,
                previewGifUrl = attachment.previewGifUrl
            )
            val gifUrl = if (doc.ext.lowercase() == "gif" || doc.url?.lowercase()?.contains("gif") == true) {
                doc.url ?: doc.previewGifUrl ?: ""
            } else {
                doc.previewGifUrl ?: doc.url ?: ""
            }
            GifPostItem(
                document = doc,
                onClick = {
                    if (gifUrl.isNotBlank()) {
                        onImageClick(listOf(gifUrl), 0)
                    }
                },
                onDownload = { onDocumentClick(doc) }
            )
        }

        attachments.forEach { attachment ->
            when (attachment) {
                is MessageAttachment.Photo -> { /* Handled in photo grid */ }
                is MessageAttachment.Doc -> {
                    if (gifDocs.contains(attachment)) return@forEach
                    val doc = Document(
                        id = attachment.id,
                        ownerId = attachment.ownerId,
                        title = attachment.title,
                        size = attachment.size,
                        ext = attachment.ext,
                        url = attachment.url,
                        date = 0,
                        type = 0,
                        previewUrl = attachment.previewUrl,
                        previewGifUrl = attachment.previewGifUrl
                    )
                    DocumentItem(
                        document = doc,
                        onClick = { onDocumentClick(doc) }
                    )
                }
                is MessageAttachment.Sticker -> {
                    AsyncImage(
                        model = attachment.url,
                        contentDescription = null,
                        modifier = Modifier.size(160.dp)
                    )
                }
                is MessageAttachment.Audio -> {
                    val track = AudioTrack(
                        id = attachment.id,
                        ownerId = attachment.ownerId,
                        artist = attachment.artist,
                        title = attachment.title,
                        duration = attachment.duration,
                        url = attachment.url
                    )
                    ChatAudioAttachmentCard(
                        attachment = attachment,
                        isOutgoing = isOutgoingBubble,
                        onClick = { onAudioClick(listOf(track), 0) }
                    )
                }
                is MessageAttachment.Video -> {
                    val video = Video(
                        id = attachment.id,
                        ownerId = attachment.ownerId,
                        title = attachment.title,
                        duration = attachment.duration,
                        thumbnailUrl = attachment.thumbnailUrl,
                        videoUrl = null,
                        playerUrl = null,
                        accessKey = attachment.accessKey
                    )
                    ChatVideoAttachmentCard(
                        video = video,
                        onClick = { onVideoClick(video) }
                    )
                }
                is MessageAttachment.Gift -> {
                    ChatGiftAttachmentCard(
                        gift = attachment,
                        onImageClick = { onImageClick(listOf(attachment.url), 0) },
                        onSendGiftClick = onSendGiftClick
                    )
                }
                is MessageAttachment.Wall -> {
                    MessageWallPostCard(
                        wall = attachment,
                        isOutgoingBubble = isOutgoingBubble,
                        onWallPostClick = onWallPostClick,
                        onImageClick = onImageClick,
                        onAudioClick = onAudioClick,
                        onVideoClick = onVideoClick,
                        onDocumentClick = onDocumentClick,
                        onMusicClick = onMusicClick,
                        onPlaylistClick = onPlaylistClick,
                        onChatClick = onChatClick,
                        onUrlClick = onUrlClick,
                        onAuthorClick = onAuthorClick,
                        onProfileClick = onProfileClick
                    )
                }
                else -> {
                    val attTextColor = if (isOutgoingBubble) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    val attLinkColor = MaterialTheme.colorScheme.primary

                    ParsedText(
                        text = com.deliriousvoid.openvkmatcha.Constants.UNSUPPORTED_MESSAGE_TEXT,
                        onMentionClick = { onAuthorClick(it) },
                        onProfileClick = { target -> handleProfileTarget(target, onAuthorClick, onProfileClick) },
                        onUrlClick = onUrlClick,
                        onWallClick = onWallPostClick,
                        onMusicClick = onMusicClick,
                        onPlaylistClick = onPlaylistClick,
                        onChatClick = onChatClick,
                        style = MaterialTheme.typography.bodySmall,
                        color = attTextColor,
                        linkColor = attLinkColor
                    )
                }
            }
        }
    }
}

@Composable
fun PinnedMessageBar(message: ChatMessage, onUnpin: () -> Unit, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PushPin, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = matchaStringResource(R.string.pinned_message),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val pinnedText = when {
                    message.text.isNotBlank() -> message.text
                    message.attachments.isNotEmpty() -> {
                        val audio = message.attachments.filterIsInstance<MessageAttachment.Audio>().firstOrNull()
                        if (audio != null) {
                            val artist = audio.artist.trim()
                            val title = audio.title.trim()
                            when {
                                artist.isNotBlank() && title.isNotBlank() -> "[♫ $artist - $title]"
                                title.isNotBlank() -> "[♫ $title]"
                                artist.isNotBlank() -> "[♫ $artist]"
                                else -> "[♫ Аудиозапись]"
                            }
                        } else {
                            matchaStringResource(R.string.attachment)
                        }
                    }
                    message.fwdMessages.isNotEmpty() -> "[Пересланное сообщение]"
                    else -> com.deliriousvoid.openvkmatcha.Constants.UNSUPPORTED_MESSAGE_TEXT
                }
                Text(
                    text = pinnedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onUnpin) {
                Icon(Icons.Default.Close, null)
            }
        }
    }
}

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TypingIndicator")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.4f at 0
                1.0f at 200
                0.4f at 400
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                delayMillis = 200
                0.4f at 0
                1.0f at 200
                0.4f at 400
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                delayMillis = 400
                0.4f at 0
                1.0f at 200
                0.4f at 400
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DotIndicator(alpha = dot1Alpha, color = color)
        DotIndicator(alpha = dot2Alpha, color = color)
        DotIndicator(alpha = dot3Alpha, color = color)
    }
}

@Composable
private fun DotIndicator(alpha: Float, color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .background(color.copy(alpha = alpha), shape = CircleShape)
    )
}

@Composable
fun ChatAudioAttachmentCard(
    attachment: MessageAttachment.Audio,
    isOutgoing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerManager = com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance.playerManager
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()

    val isCurrentTrack = (currentTrack?.id == attachment.id && currentTrack?.ownerId == attachment.ownerId) || (currentTrack?.url == attachment.url)
    val isThisPlaying = isCurrentTrack && isPlaying

    val durationText = remember(attachment.duration) {
        val minutes = attachment.duration / 60
        val seconds = attachment.duration % 60
        "%d:%02d".format(minutes, seconds)
    }

    val handlePlayPause = {
        if (isCurrentTrack) {
            playerManager.playPause()
        } else {
            onClick()
        }
    }

    Surface(
        color = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { handlePlayPause() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable { handlePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isThisPlaying) "Пауза" else "Воспроизвести",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.title.ifBlank { "Аудиозапись" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = attachment.artist.ifBlank { "Неизвестный исполнитель" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = durationText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChatVideoAttachmentCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationText = remember(video.duration) {
        val minutes = video.duration / 60
        val seconds = video.duration % 60
        "%d:%02d".format(minutes, seconds)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        if (!video.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Воспроизвести видео",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Text(
                text = durationText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ChatGiftAttachmentCard(
    gift: MessageAttachment.Gift,
    onImageClick: () -> Unit,
    onSendGiftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (gift.url.isNotBlank()) {
            AsyncImage(
                model = gift.url,
                contentDescription = gift.message ?: matchaStringResource(R.string.title_gift),
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onImageClick() },
                contentScale = ContentScale.Fit
            )
        }

        if (!gift.message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = gift.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Button(
            onClick = onSendGiftClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Отправить подарок в ответ",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun getSystemEventText(
    action: MessageAction,
    actorId: Int,
    authors: Map<Int, AuthorInfo>,
    currentUserId: Int? = null
): String {
    val actorName = authors[actorId]?.name?.trim()?.ifBlank { null } ?: "Пользователь"
    val memberId = action.memberId
    val memberName = memberId?.let { id ->
        authors[id]?.name?.trim()?.ifBlank { null } ?: "id${kotlin.math.abs(id)}"
    } ?: "пользователя"

    return when (action.type.lowercase()) {
        "chat_invite_user", "chat_invite_user_by_link" -> "$actorName пригласил(а) $memberName"
        "chat_kick_user" -> {
            if (currentUserId != null && memberId == currentUserId) {
                if (actorId == currentUserId) {
                    "Вы покинули беседу"
                } else {
                    "Вас исключил из беседы $actorName"
                }
            } else if (memberId == actorId) {
                "$actorName покинул(а) беседу"
            } else {
                "$actorName исключил(а) $memberName"
            }
        }
        "chat_promote_user", "chat_user_promote", "chat_admin_add", "chat_set_admin", "chat_moderator_add" ->
            "$actorName назначил(а) $memberName администратором"
        "chat_demote_user", "chat_user_demote", "chat_admin_remove", "chat_remove_admin", "chat_moderator_remove" ->
            "$actorName снял(а) $memberName с должности администратора"
        "chat_title_update" -> "$actorName изменил(а) название беседы"
        "chat_photo_update" -> "$actorName обновил(а) фото беседы"
        "chat_photo_remove" -> "$actorName удалил(а) фото беседы"
        "chat_create" -> "$actorName создал(а) беседу"
        "chat_pin_message" -> "$actorName закрепил(а) сообщение"
        "chat_unpin_message" -> "$actorName открепил(а) сообщение"
        else -> action.text?.trim()?.ifBlank { null } ?: "Системное сообщение"
    }
}

@Composable
fun SystemMessagePlaque(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = CircleShape
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DatePill(
    dateText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
        shadowElevation = 1.dp
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardRecipientBottomSheet(
    messagesToForward: List<ChatMessage>,
    conversations: List<Conversation>,
    isLoading: Boolean,
    currentPeerId: Int,
    onDismiss: () -> Unit,
    onSelectRecipient: (targetPeerId: Int, targetTitle: String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { conv ->
                conv.peer.title.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.Transparent,
    ) {
        ThemedPopupBackground {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Forward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = matchaStringResource(R.string.select_recipient),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val count = messagesToForward.size
                            val countText = when {
                                count % 10 == 1 && count % 100 != 11 -> "$count сообщение"
                                count % 10 in 2..4 && count % 100 !in 12..14 -> "$count сообщения"
                                else -> "$count сообщений"
                            }
                            Text(
                                text = "Переслать $countText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Поиск получателя...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                val contentHeight = 420.dp

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(contentHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filteredConversations.isEmpty()) {
                    EmptyState(
                        message = if (searchQuery.isNotBlank()) "Диалоги не найдены" else matchaStringResource(R.string.messages_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(contentHeight)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(contentHeight)
                    ) {
                        items(filteredConversations, key = { it.peer.id }) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = {
                                    onSelectRecipient(conversation.peer.id, conversation.peer.title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

