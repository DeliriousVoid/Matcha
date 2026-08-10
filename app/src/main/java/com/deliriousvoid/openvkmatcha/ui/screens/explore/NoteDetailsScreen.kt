package com.deliriousvoid.openvkmatcha.ui.screens.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deliriousvoid.openvkmatcha.data.model.Note
import com.deliriousvoid.openvkmatcha.ui.screens.comments.CommentItem
import com.deliriousvoid.openvkmatcha.ui.viewmodel.NoteDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailsScreen(
    viewModel: NoteDetailsViewModel,
    onBack: () -> Unit,
    onEditNote: (Note) -> Unit,
    onOpenProfile: (Any) -> Unit,
    currentUserId: Int
) {
    val state by viewModel.uiState.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    DisposableEffect(state.note, showMenu, currentUserId) {
        AppEvents.setTopBarState(TopBarState(
            title = state.note?.title ?: "Заметка",
            actions = {
                if (state.note?.ownerId == currentUserId) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                showMenu = false
                                state.note?.let { onEditNote(it) }
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        ))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить заметку?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(onBack)
                    showDeleteDialog = false
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Написать комментарий...") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    IconButton(
                        onClick = {
                            viewModel.postComment(commentText)
                            commentText = ""
                        },
                        enabled = commentText.isNotBlank() && !state.isSendingComment
                    ) {
                        if (state.isSendingComment) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading && state.note == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null && state.note == null) {
                Text(state.error!!, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            } else if (!state.isLoading && state.note == null) {
                Text("Заметка не найдена", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    state.note?.let { note ->
                        item {
                            SelectionContainer {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatDate(note.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = note.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Комментарии (${state.comments.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    items(state.comments) { comment ->
                        CommentItem(
                            comment = comment,
                            isOwn = comment.fromId == currentUserId,
                            onAuthorClick = { onOpenProfile(comment.fromId) },
                            onMentionClick = { onOpenProfile(it) },
                            onLikeClick = {},
                            onReplyClick = { commentText += "[id${comment.fromId}|${comment.authorName}], " },
                            onEditClick = {},
                            onDeleteClick = {},
                            onReportClick = {},
                            onAudioClick = { _, _ -> },
                            onAudioToggleAdded = {},
                            onAudioDownload = {},
                            onAudioShare = {},
                            onAudioAddToQueue = {},
                            onAudioPlayNext = {},
                            currentTrack = null,
                            isDownloaded = { _, _ -> false },
                            getTrackState = { it },
                            onDocumentDownload = {},
                            onPollVote = {},
                            onImageClick = {},
                            showActions = false
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val sdf = SimpleDateFormat("d MMMM yyyy в HH:mm", Locale.getDefault())
    return sdf.format(date)
}
