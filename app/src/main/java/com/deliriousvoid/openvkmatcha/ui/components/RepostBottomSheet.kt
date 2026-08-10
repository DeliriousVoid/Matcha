package com.deliriousvoid.openvkmatcha.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import com.deliriousvoid.openvkmatcha.data.model.Post
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.viewmodel.RepostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepostBottomSheet(
    post: Post,
    onDismiss: () -> Unit,
    viewModel: RepostViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var repostText by remember { mutableStateOf("") }
    var showGroupSelector by remember { mutableStateOf(false) }
    var showMusicPicker by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addAttachment(it, AttachmentType.PHOTO) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            if (state.isReposting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!showGroupSelector) {
                Text(
                    text = "Поделиться",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                OutlinedTextField(
                    value = repostText,
                    onValueChange = { repostText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Ваш комментарий...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 5,
                    enabled = !state.isReposting
                )

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

                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = { photoPicker.launch("image/*") }) {
                        Icon(Icons.Default.PhotoCamera, "Фото", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showMusicPicker = true }) {
                        Icon(Icons.Default.MusicNote, "Аудио", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                ListItem(
                    headlineContent = { Text("На свою стену") },
                    leadingContent = { Icon(Icons.Filled.Campaign, null) },
                    modifier = Modifier.clickable(enabled = !state.isReposting) {
                        viewModel.repost(post, repostText) {
                            onDismiss()
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("В сообщество") },
                    leadingContent = { Icon(Icons.Outlined.Group, null) },
                    modifier = Modifier.clickable(enabled = !state.isReposting) {
                        viewModel.loadAdminGroups()
                        showGroupSelector = true
                    }
                )

                ListItem(
                    headlineContent = { Text("Скопировать ссылку") },
                    leadingContent = { Icon(Icons.Outlined.ContentCopy, null) },
                    modifier = Modifier.clickable(enabled = !state.isReposting) {
                        val link = "https://openvk.org/wall${post.ownerId}_${post.id}"
                        clipboardManager.setText(AnnotatedString(link))
                        onDismiss()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showGroupSelector = false }) {
                        Icon(Icons.Default.Close, null)
                    }
                    Text(
                        text = "Выберите сообщество",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (state.isLoadingGroups) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(state.adminGroups) { group ->
                            ListItem(
                                headlineContent = { Text(group.fullName) },
                                leadingContent = {
                                    AsyncImage(
                                        model = group.photo50,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.repost(post, repostText, group.id) {
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            if (state.error != null) {
                ErrorText(
                    message = state.error!!,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
