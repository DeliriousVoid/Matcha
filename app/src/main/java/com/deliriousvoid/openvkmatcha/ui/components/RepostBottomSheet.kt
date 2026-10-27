package com.deliriousvoid.openvkmatcha.ui.components

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import com.deliriousvoid.openvkmatcha.data.model.Post
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.RepostViewModel

enum class RepostMode {
    CHAT,
    MY_WALL,
    GROUP
}

private fun triggerSuccessHaptic(haptic: HapticFeedback) {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}

private fun triggerErrorHaptic(context: Context, haptic: HapticFeedback) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    if (vibrator != null && vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 60, 150),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 60, 150), -1)
        }
    } else {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepostBottomSheet(
    post: Post,
    onDismiss: () -> Unit,
    viewModel: RepostViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var mode by remember { mutableStateOf(RepostMode.CHAT) }
    var repostText by remember { mutableStateOf("") }
    var asGroup by remember { mutableStateOf(false) }
    var signed by remember { mutableStateOf(false) }
    var showGroupOptionsDialog by remember { mutableStateOf(false) }
    var showMusicPicker by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it, AttachmentType.PHOTO) }
    }

    LaunchedEffect(post) {
        viewModel.loadConversations()
        viewModel.loadAdminGroups()
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetState()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.Transparent,
    ) {
        ThemedPopupBackground {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = matchaStringResource(R.string.action_share),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        viewModel.resetState()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                ) {
                    if (state.isReposting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                // Middle Content Area according to Mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .weight(1f, fill = false),
                    contentAlignment = Alignment.Center
                ) {
                    when (mode) {
                        RepostMode.CHAT -> {
                            if (state.isLoadingConversations) {
                                CircularProgressIndicator()
                            } else if (state.conversations.isEmpty()) {
                                Text(
                                    text = "Нет диалогов для отправки",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(state.conversations, key = { it.peer.id }) { conversation ->
                                        val isSending = state.sendingPeerId == conversation.peer.id
                                        val isSent = state.sentPeerIds.contains(conversation.peer.id)
                                        val isFailed = state.failedPeerIds.contains(conversation.peer.id)

                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    text = conversation.peer.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            leadingContent = {
                                                AsyncImage(
                                                    model = conversation.peer.photo,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            },
                                            trailingContent = {
                                                when {
                                                    isSending -> {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(24.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }
                                                    isSent -> {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Отправлено",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    isFailed -> {
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.sendToChat(
                                                                    post = post,
                                                                    peerId = conversation.peer.id,
                                                                    message = repostText,
                                                                    onSuccess = { triggerSuccessHaptic(haptic) },
                                                                    onError = { triggerErrorHaptic(context, haptic) }
                                                                )
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Ошибка, повторить",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        OutlinedButton(
                                                            onClick = {
                                                                viewModel.sendToChat(
                                                                    post = post,
                                                                    peerId = conversation.peer.id,
                                                                    message = repostText,
                                                                    onSuccess = { triggerSuccessHaptic(haptic) },
                                                                    onError = { triggerErrorHaptic(context, haptic) }
                                                                )
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(32.dp)
                                                        ) {
                                                            Text(
                                                                text = "Отправить",
                                                                style = MaterialTheme.typography.labelMedium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        RepostMode.GROUP -> {
                            if (state.isLoadingGroups) {
                                CircularProgressIndicator()
                            } else if (state.adminGroups.isEmpty()) {
                                Text(
                                    text = "Нет сообществ, где вы администратор",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(state.adminGroups, key = { it.id }) { group ->
                                        val isSending = state.sendingGroupId == group.id
                                        val isSent = state.sentGroupIds.contains(group.id)
                                        val isFailed = state.failedGroupIds.contains(group.id)

                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    text = group.fullName,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            supportingContent = if (group.status.isNotBlank()) {
                                                {
                                                    Text(
                                                        text = group.status,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else null,
                                            leadingContent = {
                                                AsyncImage(
                                                    model = group.photo50,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            },
                                            trailingContent = {
                                                when {
                                                    isSending -> {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(24.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }
                                                    isSent -> {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Отправлено",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    isFailed -> {
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.repost(
                                                                    post = post,
                                                                    message = repostText,
                                                                    groupId = group.id,
                                                                    asGroup = asGroup,
                                                                    signed = signed,
                                                                    onSuccess = { triggerSuccessHaptic(haptic) },
                                                                    onError = { triggerErrorHaptic(context, haptic) }
                                                                )
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Ошибка, повторить",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        OutlinedButton(
                                                            onClick = {
                                                                viewModel.repost(
                                                                    post = post,
                                                                    message = repostText,
                                                                    groupId = group.id,
                                                                    asGroup = asGroup,
                                                                    signed = signed,
                                                                    onSuccess = { triggerSuccessHaptic(haptic) },
                                                                    onError = { triggerErrorHaptic(context, haptic) }
                                                                )
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(32.dp)
                                                        ) {
                                                            Text(
                                                                text = "Отправить",
                                                                style = MaterialTheme.typography.labelMedium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        RepostMode.MY_WALL -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Публикация записи на вашей странице",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Pending Attachments
                if (state.pendingAttachments.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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

                // Comment Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = repostText,
                        onValueChange = { repostText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(matchaStringResource(R.string.comment_placeholder)) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        maxLines = 3,
                        enabled = !state.isReposting,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                        trailingIcon = when (mode) {
                            RepostMode.MY_WALL -> {
                                @Composable {
                                    IconButton(
                                        onClick = {
                                            viewModel.repost(
                                                post = post,
                                                message = repostText,
                                                onSuccess = {
                                                    triggerSuccessHaptic(haptic)
                                                    viewModel.resetState()
                                                    onDismiss()
                                                },
                                                onError = {
                                                    triggerErrorHaptic(context, haptic)
                                                }
                                            )
                                        },
                                        enabled = !state.isReposting
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.Send,
                                            contentDescription = "Отправить",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            RepostMode.GROUP -> {
                                @Composable {
                                    IconButton(onClick = { showGroupOptionsDialog = true }) {
                                        Icon(
                                            Icons.Outlined.Tune,
                                            contentDescription = "Опции",
                                            tint = if (asGroup || signed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            RepostMode.CHAT -> null
                        }
                    )
                }

                // Attachments bar
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = { photoPicker.launch("image/*") }) {
                        Icon(Icons.Default.PhotoCamera, matchaStringResource(R.string.attachment_photo), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showMusicPicker = true }) {
                        Icon(Icons.Default.MusicNote, matchaStringResource(R.string.attachment_audio), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                HorizontalDivider()

                // Bottom Action Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

                    // On my page / wall
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                mode = if (mode == RepostMode.MY_WALL) RepostMode.CHAT else RepostMode.MY_WALL
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = if (mode == RepostMode.MY_WALL) activeColor else inactiveColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "На своей странице",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mode == RepostMode.MY_WALL) activeColor else inactiveColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // On group wall
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                mode = if (mode == RepostMode.GROUP) RepostMode.CHAT else RepostMode.GROUP
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = if (mode == RepostMode.GROUP) activeColor else inactiveColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "На стене группы",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mode == RepostMode.GROUP) activeColor else inactiveColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Copy Link
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val webInstance = OpenVKMatchaApp.instance.tokenManager.getWebInstance()
                                val link = "$webInstance/wall${post.ownerId}_${post.id}"
                                clipboardManager.setText(AnnotatedString(link))
                                Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                                onDismiss()
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = inactiveColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Скопировать ссылку",
                            style = MaterialTheme.typography.labelSmall,
                            color = inactiveColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Other (Share intent)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val webInstance = OpenVKMatchaApp.instance.tokenManager.getWebInstance()
                                val link = "$webInstance/wall${post.ownerId}_${post.id}"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, link)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            tint = inactiveColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Другое",
                            style = MaterialTheme.typography.labelSmall,
                            color = inactiveColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showGroupOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showGroupOptionsDialog = false },
            title = { Text("Опции публикации") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("От имени группы", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = asGroup,
                            onCheckedChange = { asGroup = it }
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Подпись автора", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = signed,
                            onCheckedChange = { signed = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "При публикации на стене группы запись появится от ее имени.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupOptionsDialog = false }) {
                    Text("Готово")
                }
            }
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
}
