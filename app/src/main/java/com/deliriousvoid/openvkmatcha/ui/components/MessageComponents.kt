package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.model.*
import com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    authors: Map<Int, AuthorInfo> = emptyMap(),
    showAvatar: Boolean = true,
    showName: Boolean = true,
    isHighlighted: Boolean = false,
    onReplyClick: (ChatMessage) -> Unit = {},
    onReplyPreviewClick: (Int) -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: (ChatMessage) -> Unit = {},
    onAuthorClick: (Int) -> Unit = {},
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
) {
    val isOut = message.isOutgoing
    val isGroup = message.peerId >= 2000000000
    val author = authors[message.fromId]
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
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

        Surface(
            color = if (isOut) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = if (!isOut && isGroup && !showName) 4.dp else 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOut) 16.dp else (if (isGroup && !showAvatar) 16.dp else 4.dp),
                bottomEnd = if (isOut) 4.dp else 16.dp
            ),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { onLongClick(message) }
                )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (!isOut && isGroup && author != null && showName) {
                    Text(
                        text = author.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp).clickable { onAuthorClick(message.fromId) }
                    )
                }

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
                        onImageClick = onImageClick
                    )
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = if (isOut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isEdited) {
                        Text(
                            text = matchaStringResource(R.string.edited_marker),
                            style = MaterialTheme.typography.labelSmall,
                            color = (if (isOut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = formatTimeAgo(message.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (isOut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                    )
                    if (isOut) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            tint = if (isOut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
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
    Surface(
        color = if (isOutgoingBubble) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(bottom = 6.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .background(if (isOutgoingBubble) Color.White else MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = authorName ?: "ID ${message.fromId}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutgoingBubble) Color.White else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (message.text.isNotBlank()) message.text else matchaStringResource(R.string.attachment),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isOutgoingBubble) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MessageAttachments(
    attachments: List<MessageAttachment>,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> }
) {
    val photos = attachments.filterIsInstance<MessageAttachment.Photo>()
    val gifDocs = attachments.filterIsInstance<MessageAttachment.Doc>().filter { 
        it.ext.lowercase() == "gif" || it.previewGifUrl != null || it.previewUrl != null 
    }
    
    val allImageUrls = photos.map { it.url } + gifDocs.mapNotNull { it.previewUrl ?: it.url }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (allImageUrls.isNotEmpty()) {
            PostImageGrid(
                imageUrls = allImageUrls,
                onImageClick = { index -> onImageClick(allImageUrls, index) }
            )
        }

        attachments.forEach { attachment ->
            when (attachment) {
                is MessageAttachment.Photo -> { /* Handled in grid */ }
                is MessageAttachment.Doc -> {
                    if (gifDocs.contains(attachment)) return@forEach
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(Icons.Default.InsertDriveFile, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(attachment.title, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Text("${attachment.size / 1024} KB", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                is MessageAttachment.Sticker -> {
                    AsyncImage(
                        model = attachment.url,
                        contentDescription = null,
                        modifier = Modifier.size(160.dp)
                    )
                }
                is MessageAttachment.Audio -> {
                   Row(verticalAlignment = Alignment.CenterVertically) {
                       Icon(Icons.Default.PlayArrow, null)
                       Text("${attachment.artist} - ${attachment.title}", style = MaterialTheme.typography.bodySmall)
                   }
                }
                else -> { /* Other attachments */ }
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
                Text(
                    text = message.text.takeIf { it.isNotBlank() } ?: matchaStringResource(R.string.attachment),
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
