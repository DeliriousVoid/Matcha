package com.deliriousvoid.openvkmatcha.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.deliriousvoid.openvkmatcha.Constants
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.Conversation
import com.deliriousvoid.openvkmatcha.data.model.Document
import com.deliriousvoid.openvkmatcha.data.model.Post
import com.deliriousvoid.openvkmatcha.data.model.Geo
import com.deliriousvoid.openvkmatcha.data.model.PendingAttachment
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import com.deliriousvoid.openvkmatcha.ui.util.LinkHandler
import androidx.compose.ui.res.painterResource
import com.deliriousvoid.openvkmatcha.ui.util.formatDuration
import com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicPickerViewModel
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DoubleTapHeartAnimation(
    offset: androidx.compose.ui.geometry.Offset,
    onAnimationEnd: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val halfSizePx = with(density) { 50.dp.toPx() }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            )
            scale.animateTo(
                targetValue = 1.1f,
                animationSpec = tween(durationMillis = 100)
            )
            delay(200)
            alpha.animateTo(0f, tween(100))
            onAnimationEnd()
        }
    }

    Box(
        modifier = Modifier
            .offset { 
                IntOffset(
                    (offset.x - halfSizePx).toInt(), 
                    (offset.y - halfSizePx).toInt()
                ) 
            }
            .size(100.dp)
            .graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                alpha = alpha.value
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("❤️", fontSize = 60.sp)
    }
}

@Composable
fun MatchaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        )
    )
}

@Composable
fun MatchaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ExpandableParsedText(
    text: String,
    onMentionClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: (String) -> Unit = {},
    onWallClick: (Int, Int) -> Unit = { _, _ -> },
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onDefaultClick: () -> Unit = {},
    onDoubleTap: ((androidx.compose.ui.geometry.Offset) -> Unit)? = null,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLinesBeforeExpansion: Int = 10
) {
    var expanded by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        ParsedText(
            text = text,
            onMentionClick = onMentionClick,
            onProfileClick = onProfileClick,
            onWallClick = onWallClick,
            onMusicClick = onMusicClick,
            onPlaylistClick = onPlaylistClick,
            onDefaultClick = onDefaultClick,
            onDoubleTap = onDoubleTap,
            style = style,
            maxLines = if (expanded) Int.MAX_VALUE else maxLinesBeforeExpansion,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (!expanded) {
                    hasMore = textLayoutResult.hasVisualOverflow
                }
            }
        )
        if (hasMore && !expanded) {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = stringResource(R.string.show_more),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun getPlatformInfo(platform: String?, sourceName: String?): Triple<Int, String?, Boolean> {
    val p = platform?.lowercase()?.trim() ?: ""
    val name = sourceName?.lowercase()?.trim() ?: ""
    
    // Брендинг Matcha и любые упоминания Android/4
    if (name.contains("matcha") || p.contains("matcha") || p == "4" || p == "android" || name.contains("android")) {
        return Triple(R.drawable.ic_android, null, false)
    }

    if (p == "profile_photo" || name == "profile_photo") {
        return Triple(0, "обновил фото профиля", true)
    }

    return when (p) {
        "iphone", "2" -> Triple(R.drawable.ic_ios, "iPhone", false)
        "ipad", "3" -> Triple(R.drawable.ic_ios, "iPad", false)
        "wphone", "5" -> Triple(R.drawable.ic_window, "Windows Phone", false)
        "mobile", "1" -> Triple(R.drawable.ic_mobile_3, "Мобильная версия", false)
        "web", "7" -> Triple(0, "Полная версия", false)
        "api" -> Triple(R.drawable.ic_settings, sourceName ?: "API", false)
        else -> Triple(0, sourceName, true)
    }
}

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCommentClick: (Int, Int) -> Unit = { _, _ -> },
    onRepostClick: () -> Unit = {},
    onAuthorClick: (Any) -> Unit = {},
    onAudioClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onAudioToggleAdded: (AudioTrack) -> Unit = {},
    onAudioDownload: (AudioTrack) -> Unit = {},
    onAudioShare: (AudioTrack) -> Unit = {},
    onAudioAddToQueue: (AudioTrack) -> Unit = {},
    onAudioPlayNext: (AudioTrack) -> Unit = {},
    onDocumentDownload: (Document) -> Unit = {},
    currentTrack: AudioTrack? = null,
    isDownloaded: (Int, Int) -> Boolean = { _, _ -> false },
    getTrackState: (AudioTrack) -> AudioTrack = { it },
    currentUserId: Int? = null,
    onEditClick: (Post) -> Unit = {},
    onDeleteClick: (Post) -> Unit = {},
    onPinClick: (Post) -> Unit = {},
    onUnpinClick: (Post) -> Unit = {},
    onReportClick: (Post) -> Unit = {},
    onPollVote: (Post, List<Int>) -> Unit = { _, _ -> },
    onGeoClick: (Geo) -> Unit = {},
    doubleTapEnabled: Boolean = false,
    doubleTapTimeout: Long = 100L,
) {
    RepostContent(
        post = post,
        onLikeClick = onLikeClick,
        modifier = modifier,
        onCommentClick = onCommentClick,
        onRepostClick = onRepostClick,
        onAuthorClick = onAuthorClick,
        onAudioClick = onAudioClick,
        onMusicClick = onMusicClick,
        onPlaylistClick = onPlaylistClick,
        onAudioToggleAdded = onAudioToggleAdded,
        onAudioDownload = onAudioDownload,
        onAudioShare = onAudioShare,
        onAudioAddToQueue = onAudioAddToQueue,
        onAudioPlayNext = onAudioPlayNext,
        onDocumentDownload = onDocumentDownload,
        currentTrack = currentTrack,
        isDownloaded = isDownloaded,
        getTrackState = getTrackState,
        isRepost = false,
        currentUserId = currentUserId,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick,
        onPinClick = onPinClick,
        onUnpinClick = onUnpinClick,
        onReportClick = onReportClick,
        onPollVote = onPollVote,
        onGeoClick = onGeoClick,
        doubleTapEnabled = doubleTapEnabled,
        doubleTapTimeout = doubleTapTimeout,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RepostContent(
    post: Post,
    modifier: Modifier = Modifier,
    onLikeClick: () -> Unit = {},
    onCommentClick: (Int, Int) -> Unit = { _, _ -> },
    onRepostClick: () -> Unit = {},
    onAuthorClick: (Any) -> Unit = {},
    onAudioClick: (List<AudioTrack>, Int) -> Unit = { _, _ -> },
    onMusicClick: (Int) -> Unit = {},
    onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
    onAudioToggleAdded: (AudioTrack) -> Unit = {},
    onAudioDownload: (AudioTrack) -> Unit = {},
    onAudioShare: (AudioTrack) -> Unit = {},
    onAudioAddToQueue: (AudioTrack) -> Unit = {},
    onAudioPlayNext: (AudioTrack) -> Unit = {},
    onDocumentDownload: (Document) -> Unit = {},
    currentTrack: AudioTrack? = null,
    isDownloaded: (Int, Int) -> Boolean = { _, _ -> false },
    getTrackState: (AudioTrack) -> AudioTrack = { it },
    isRepost: Boolean = false,
    currentUserId: Int? = null,
    onEditClick: (Post) -> Unit = {},
    onDeleteClick: (Post) -> Unit = {},
    onPinClick: (Post) -> Unit = {},
    onUnpinClick: (Post) -> Unit = {},
    onReportClick: (Post) -> Unit = {},
    onPollVote: (Post, List<Int>) -> Unit = { _, _ -> },
    onGeoClick: (Geo) -> Unit = {},
    doubleTapEnabled: Boolean = false,
    doubleTapTimeout: Long = 100L,
) {
    var showMenu by remember { mutableStateOf(false) }
    var isRevealed by remember { mutableStateOf(post.isNsfwRevealed) }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var hearts by remember { mutableStateOf(listOf<Pair<Long, androidx.compose.ui.geometry.Offset>>()) }
    var parentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val viewConfiguration = androidx.compose.ui.platform.LocalViewConfiguration.current
    val customViewConfiguration = remember(viewConfiguration, doubleTapTimeout) {
        object : androidx.compose.ui.platform.ViewConfiguration by viewConfiguration {
            override val doubleTapTimeoutMillis: Long
                get() = doubleTapTimeout
        }
    }

    val triggerDoubleTap: (LayoutCoordinates, androidx.compose.ui.geometry.Offset) -> Unit = remember(doubleTapEnabled, onLikeClick) {
        { childCoords, localOffset ->
            if (doubleTapEnabled) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onLikeClick()
                parentCoords?.let { parent ->
                    val postRelativeOffset = parent.localPositionOf(childCoords, localOffset)
                    hearts = hearts + (System.currentTimeMillis() to postRelativeOffset)
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalViewConfiguration provides if (doubleTapEnabled) customViewConfiguration else viewConfiguration
    ) {
        Card(
            modifier = if (isRepost) modifier.fillMaxWidth() else modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isRepost) Color.Transparent else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { parentCoords = it }
            ) {
                if (isRepost) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(start = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .padding(vertical = 2.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (isRepost) 20.dp else 16.dp,
                            end = if (isRepost) 0.dp else 16.dp,
                            top = if (isRepost) 4.dp else 12.dp,
                            bottom = if (isRepost) 4.dp else 12.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).clickable { onAuthorClick(post.authorId) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                AsyncImage(
                                    model = post.authorAvatar,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(if (isRepost) 32.dp else 40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop,
                                )
                                OnlineIndicator(
                                    isOnline = post.authorOnline,
                                    isMobile = post.authorMobileOnline,
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    dotSize = if (isRepost) 8.dp else 10.dp,
                                    iconSize = if (isRepost) 10.dp else 12.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = post.authorName,
                                        style = if (isRepost) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (post.authorVerified || com.deliriousvoid.openvkmatcha.Constants.CUSTOM_VERIFIED_IDS.contains(post.authorId)) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedBadge(
                                            userId = post.authorId,
                                            isVerified = post.authorVerified,
                                            size = if (isRepost) 14.dp else 16.dp
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatTimeAgo(post.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    val (iconRes, platformName, isCustom) = getPlatformInfo(post.platform, post.sourceName)

                                    if (iconRes != 0 || (platformName != null && isCustom)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            if (iconRes != 0) {
                                                Icon(
                                                    painter = painterResource(id = iconRes),
                                                    contentDescription = platformName,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (platformName != null && isCustom) {
                                                if (iconRes != 0) Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = platformName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (post.isPinned && !isRepost) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Закреплено",
                                modifier = Modifier.size(20.dp).padding(end = 8.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val isOwner = currentUserId != null && post.ownerId == currentUserId
                        val isAuthor = currentUserId != null && post.authorId == currentUserId

                        val canEdit = !isRepost && (post.canEdit || isAuthor)
                        val canDelete = !isRepost && (post.canDelete || isAuthor || isOwner)
                        val canPin = !isRepost && (post.canPin || isOwner)

                        val showTopMenu = !isRepost && (canEdit || canDelete || canPin || (currentUserId != null && post.authorId != currentUserId))
                        val showRepostMenu = isRepost

                        if (showTopMenu || showRepostMenu) {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Меню",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Копировать текст") },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                        onClick = {
                                            showMenu = false
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(post.text))
                                        }
                                    )
                                    if (!isRepost) {
                                        if (canPin) {
                                            DropdownMenuItem(
                                                text = { Text(if (post.isPinned) "Открепить" else "Закрепить") },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = if (post.isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                                                        contentDescription = null
                                                    )
                                                },
                                                onClick = {
                                                    showMenu = false
                                                    if (post.isPinned) onUnpinClick(post) else onPinClick(post)
                                                }
                                            )
                                        }
                                        if (canEdit) {
                                            DropdownMenuItem(
                                                text = { Text("Изменить") },
                                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                                onClick = {
                                                    showMenu = false
                                                    onEditClick(post)
                                                }
                                            )
                                        }
                                        if (canDelete) {
                                            DropdownMenuItem(
                                                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    showMenu = false
                                                    onDeleteClick(post)
                                                }
                                            )
                                        }
                                        if (post.authorId != currentUserId && !canEdit && !canDelete) {
                                            DropdownMenuItem(
                                                text = { Text("Пожаловаться") },
                                                leadingIcon = { Icon(Icons.Default.Campaign, null) },
                                                onClick = {
                                                    showMenu = false
                                                    onReportClick(post)
                                                }
                                            )
                                        }
                                    }
                                    if (showRepostMenu) {
                                        DropdownMenuItem(
                                            text = { Text("Перейти к оригиналу") },
                                            onClick = {
                                                showMenu = false
                                                onCommentClick(post.ownerId, post.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let {
                                    if (post.isNsfw && !isRevealed) it.blur(25.dp) else it
                                }
                        ) {
                            var viewerInitialIndex by remember { mutableIntStateOf(-1) }

                            if (post.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                var textCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                ExpandableParsedText(
                                    text = post.text,
                                    onMentionClick = { onAuthorClick(it) },
                                    onProfileClick = onAuthorClick,
                                    onWallClick = onCommentClick,
                                    onMusicClick = onMusicClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onDefaultClick = { onCommentClick(post.ownerId, post.id) },
                                    onDoubleTap = if (doubleTapEnabled) { offset -> textCoords?.let { triggerDoubleTap(it, offset) } } else null,
                                    modifier = Modifier.onGloballyPositioned { textCoords = it },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            if (post.imageUrls.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                var imageCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                PostImageGrid(
                                    imageUrls = post.imageUrls,
                                    onImageClick = { if (!post.isNsfw || isRevealed) viewerInitialIndex = it },
                                    onDoubleTap = if (doubleTapEnabled) { offset -> imageCoords?.let { triggerDoubleTap(it, offset) } } else null,
                                    modifier = Modifier.onGloballyPositioned { imageCoords = it }
                                )
                            }

                            val gifDocs = post.documents.filter { it.ext.lowercase() == "gif" || it.type == 3 }
                            val otherDocs = post.documents.filterNot { it.ext.lowercase() == "gif" || it.type == 3 }

                            if (gifDocs.isNotEmpty()) {
                                gifDocs.forEach { gif ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    var gifCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                    GifPostItem(
                                        document = gif,
                                        onClick = { 
                                            if (!post.isNsfw || isRevealed) {
                                                viewerInitialIndex = post.imageUrls.size + gifDocs.indexOf(gif)
                                            }
                                        },
                                        onDownload = { onDocumentDownload(gif) },
                                        onDoubleTap = if (doubleTapEnabled) { offset -> gifCoords?.let { triggerDoubleTap(it, offset) } } else null,
                                        modifier = Modifier.onGloballyPositioned { gifCoords = it }
                                    )
                                }
                            }

                            if (otherDocs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    otherDocs.forEach { doc ->
                                        DocumentItem(
                                            document = doc,
                                            onClick = { onDocumentDownload(doc) }
                                        )
                                    }
                                }
                            }

                            if (post.videos.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                post.videos.forEach { video ->
                                    VideoPlayer(
                                        video = video,
                                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            post.copyHistory?.firstOrNull()?.let { originalPost ->
                                Spacer(modifier = Modifier.height(12.dp))
                                RepostContent(
                                    post = originalPost,
                                    onAuthorClick = onAuthorClick,
                                    onCommentClick = onCommentClick,
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
                                    onGeoClick = onGeoClick,
                                    isRepost = true
                                )
                            }

                            if (post.poll != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                PollComponent(
                                    poll = post.poll,
                                    onVote = { onPollVote(post, it) }
                                )
                            }

                            if (post.geo != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                GeoComponent(
                                    geo = post.geo,
                                    onClick = { onGeoClick(post.geo) }
                                )
                            }

                            if (post.audios.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    post.audios.forEachIndexed { index, track ->
                                        val displayTrack = getTrackState(track)
                                        AudioTrackItem(
                                            track = displayTrack,
                                            onClick = { onAudioClick(post.audios, index) },
                                            onToggleAdded = { onAudioToggleAdded(displayTrack) },
                                            onDownload = { onAudioDownload(displayTrack) },
                                            onShare = { onAudioShare(displayTrack) },
                                            onAddToQueue = { onAudioAddToQueue(displayTrack) },
                                            onPlayNext = { onAudioPlayNext(displayTrack) },
                                            isSelected = displayTrack.stableId == currentTrack?.stableId,
                                            isDownloaded = isDownloaded(displayTrack.id, displayTrack.ownerId)
                                        )
                                        if (index < post.audios.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 76.dp),
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            )
                                        }
                                    }
                                }
                            }

                            if (viewerInitialIndex != -1) {
                                val allImageUrls = post.imageUrls + gifDocs.mapNotNull { it.url }
                                ImageViewer(
                                    imageUrls = allImageUrls,
                                    initialIndex = viewerInitialIndex,
                                    onDismiss = { viewerInitialIndex = -1 },
                                    post = post,
                                    onLikeClick = onLikeClick,
                                    onCommentClick = onCommentClick,
                                    onRepostClick = onRepostClick
                                )
                            }
                        }

                        if (post.isNsfw && !isRevealed) {
                            NsfwSpoiler(
                                onClick = {
                                    post.isNsfwRevealed = true
                                    isRevealed = true
                                },
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }

                    if (post.copyrightLink != null) {
                        val context = LocalContext.current
                        val clipboardManager = LocalClipboardManager.current
                        var sourceMenuExpanded by remember { mutableStateOf(false) }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            LinkHandler.handleLink(
                                                context = context,
                                                url = post.copyrightLink,
                                                onProfileClick = onAuthorClick,
                                                onWallClick = onCommentClick,
                                                onMusicClick = onMusicClick,
                                                onPlaylistClick = onPlaylistClick
                                            )
                                        },
                                        onLongClick = {
                                            sourceMenuExpanded = true
                                        }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        R.string.post_source_label,
                                        post.copyrightName ?: post.copyrightLink
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            DropdownMenu(
                                expanded = sourceMenuExpanded,
                                onDismissRequest = { sourceMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.copy_link)) },
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(post.copyrightLink))
                                        sourceMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.open_in_browser)) },
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, post.copyrightLink.toUri())
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                        sourceMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (!isRepost) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(
                                modifier = Modifier.clickable(onClick = onLikeClick),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (post.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = post.likeCount.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                modifier = Modifier.clickable(onClick = { onCommentClick(post.ownerId, post.id) }),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = post.commentCount.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                modifier = Modifier.clickable(onClick = onRepostClick),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(-15f),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = post.repostCount.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.matchParentSize()) {
                    hearts.forEach { (id, offset) ->
                        DoubleTapHeartAnimation(
                            offset = offset,
                            onAnimationEnd = {
                                hearts = hearts.filter { it.first != id }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AsyncImage(
                model = conversation.peerPhoto,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            OnlineIndicator(
                isOnline = conversation.isOnline,
                isMobile = conversation.isMobileOnline,
                modifier = Modifier.align(Alignment.BottomEnd),
                dotSize = 10.dp,
                iconSize = 14.dp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (conversation.peerVerified || com.deliriousvoid.openvkmatcha.Constants.CUSTOM_VERIFIED_IDS.contains(conversation.peerId)) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(
                        userId = conversation.peerId,
                        isVerified = conversation.peerVerified,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimeAgo(conversation.lastMessageDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioTrackItem(
    track: AudioTrack,
    onClick: () -> Unit,
    onToggleAdded: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isDownloaded: Boolean = false,
    isOfflineMode: Boolean = false,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val effectiveIsDownloaded = isDownloaded || track.url?.startsWith("/") == true

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (track.artworkUrl != null) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (effectiveIsDownloaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(14.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        }
        Text(
            text = formatDuration(track.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!isOfflineMode) {
            IconButton(onClick = onToggleAdded) {
                Icon(
                    imageVector = if (track.isAdded) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (track.isAdded) "Убрать" else "Добавить",
                    tint = if (track.isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Опции",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!isOfflineMode) {
                    DropdownMenuItem(
                        text = { Text("Скачать") },
                        onClick = { showMenu = false; onDownload() },
                        leadingIcon = { Icon(Icons.Default.GetApp, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Поделиться") },
                        onClick = { showMenu = false; onShare() },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Добавить в очередь") },
                    onClick = { showMenu = false; onAddToQueue() },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                )
                DropdownMenuItem(
                    text = { Text("Играть следующим") },
                    onClick = { showMenu = false; onPlayNext() },
                    leadingIcon = { Icon(Icons.Default.SkipNext, null) }
                )

                if (onRemoveFromPlaylist != null) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Удалить из плейлиста", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onRemoveFromPlaylist() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistItem(
    playlist: com.deliriousvoid.openvkmatcha.data.model.Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (playlist.photoUrl != null) {
                AsyncImage(
                    model = playlist.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            if (playlist.description.isNotBlank()) {
                Text(
                    text = playlist.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            }
            Text(
                text = "${playlist.trackCount} треков",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun UserListItem(
    user: com.deliriousvoid.openvkmatcha.data.model.UserProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = user.photo200,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            OnlineIndicator(
                isOnline = user.online,
                isMobile = user.mobileOnline,
                modifier = Modifier.align(Alignment.BottomEnd),
                dotSize = 10.dp,
                iconSize = 12.dp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (user.verified || com.deliriousvoid.openvkmatcha.Constants.CUSTOM_VERIFIED_IDS.contains(user.id)) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(userId = user.id, isVerified = user.verified)
                }
            }
            if (user.status.isNotBlank()) {
                Text(
                    text = user.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
fun NsfwSpoiler(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.nsfw_content),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.nsfw_show),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun StateView(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateView(
        icon = icon,
        message = message,
        modifier = modifier,
        actionText = actionText,
        onAction = onAction
    )
}

@Composable
fun ErrorText(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val isNetworkError = com.deliriousvoid.openvkmatcha.util.AppEvents.isNetworkError(message)

    StateView(
        icon = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.SentimentVeryDissatisfied,
        message = if (isNetworkError) "Проверьте соединение" else message,
        modifier = modifier,
        actionText = if (isNetworkError || onRetry != null) "Повторить" else null,
        onAction = onRetry ?: {}
    )
}

@Composable
fun OnlineIndicator(
    isOnline: Boolean,
    isMobile: Boolean,
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    iconSize: Dp = 14.dp,
) {
    if (!isOnline) return

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isMobile) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = "Online from mobile",
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
fun VerifiedBadge(
    userId: Int,
    isVerified: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    val isCustom = Constants.CUSTOM_VERIFIED_IDS.contains(userId)
    if (isCustom || isVerified) {
        val badgeColor = if (isVerified) Color(0xFF82B1FF) else Color(0xFF4BB34B)
        
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = if (isVerified) "Verified" else "Custom Verified",
            tint = badgeColor,
            modifier = modifier.size(size),
        )
    }
}

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Жалоба") },
        text = {
            Column {
                Text(
                    text = "Пожалуйста, опишите причину жалобы:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Комментарий (необязательно)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(comment) }
            ) {
                Text("Отправить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EditPostDialog(
    initialText: String,
    isGroup: Boolean = false,
    initialIsNsfw: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, Boolean) -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var fromGroup by remember(isGroup) { mutableStateOf(isGroup) }
    var isNsfw by remember(initialIsNsfw) { mutableStateOf(initialIsNsfw) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование записи") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Текст записи") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isGroup) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { fromGroup = !fromGroup },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = fromGroup,
                            onCheckedChange = { fromGroup = it }
                        )
                        Text(
                            text = "От имени сообщества",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isNsfw = !isNsfw },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isNsfw,
                        onCheckedChange = { isNsfw = it }
                    )
                    Text(
                        text = "Содержит NSFW",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text, fromGroup, isNsfw) }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EditPlaylistDialog(
    initialTitle: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTitle.isEmpty()) "Создание плейлиста" else "Редактирование плейлиста") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Описание (необязательно)") },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description) },
                enabled = title.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(stringResource(R.string.delete_action_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun PendingAttachmentItem(
    attachment: PendingAttachment,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            when (attachment.type) {
                AttachmentType.PHOTO, AttachmentType.GRAFFITI -> {
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
                                else -> Icons.AutoMirrored.Filled.InsertDriveFile
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
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun FriendshipStatusIcon(
    user: com.deliriousvoid.openvkmatcha.data.model.UserProfile,
    onToggle: (com.deliriousvoid.openvkmatcha.data.model.UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    // friendStatus: 0 - nothing, 1 - outgoing, 2 - incoming, 3 - friends
    when (user.friendStatus) {
        0, null -> {
            IconButton(onClick = { onToggle(user) }, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Добавить в друзья",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        1 -> {
            IconButton(onClick = { onToggle(user) }, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = "Отменить запрос",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        2 -> {
            IconButton(onClick = { onToggle(user) }, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Принять запрос",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        3 -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "В друзьях",
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier.padding(12.dp).size(24.dp)
            )
        }
    }
}

@Composable
fun GroupMembershipIcon(
    group: com.deliriousvoid.openvkmatcha.data.model.UserProfile,
    onToggle: (com.deliriousvoid.openvkmatcha.data.model.UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    if (group.isMember) {
        IconButton(onClick = { onToggle(group) }, modifier = modifier) {
            Icon(
                imageVector = Icons.Default.GroupRemove,
                contentDescription = "Выйти из группы",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        IconButton(onClick = { onToggle(group) }, modifier = modifier) {
            Icon(
                imageVector = Icons.Default.GroupAdd,
                contentDescription = "Вступить в группу",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPickerBottomSheet(
    onDismiss: () -> Unit,
    onTrackSelect: (AudioTrack) -> Unit,
    viewModel: MusicPickerViewModel = viewModel(factory = MusicPickerViewModel.factory())
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex > totalItems - 5 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.8f).padding(horizontal = 16.dp)) {
            Text(
                text = "Выбор музыки",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).focusRequester(focusRequester),
                placeholder = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading && state.tracks.isEmpty()) {
                    LoadingBox(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.tracks) { track ->
                            AudioTrackItem(
                                track = track,
                                onClick = { onTrackSelect(track) },
                                onToggleAdded = {},
                                onDownload = {},
                                onShare = {},
                                onAddToQueue = {},
                                onPlayNext = {}
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        }
                        if (state.isLoadingMore) {
                            item { LoadingBox(modifier = Modifier.padding(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}
