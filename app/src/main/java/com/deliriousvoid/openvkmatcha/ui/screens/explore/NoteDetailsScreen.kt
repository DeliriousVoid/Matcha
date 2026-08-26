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
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.theme.LocalOpaqueColorScheme
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.data.model.Note
import com.deliriousvoid.openvkmatcha.ui.screens.comments.CommentItem
import com.deliriousvoid.openvkmatcha.ui.components.ImageViewer
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
    val opaqueColorScheme = LocalOpaqueColorScheme.current
    var commentText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var viewerInitialIndex by remember { mutableIntStateOf(-1) }
    var activeCommentForViewer by remember { mutableStateOf<com.deliriousvoid.openvkmatcha.data.model.Comment?>(null) }

    val noteTitle = state.note?.title ?: matchaStringResource(R.string.title_note)
    val menuLabel = matchaStringResource(R.string.desc_menu)

    DisposableEffect(state.note, showMenu, currentUserId) {
        AppEvents.setTopBarState(TopBarState(
            title = noteTitle,
            route = Routes.NOTE_DETAILS,
            actions = {
                if (state.note?.ownerId == currentUserId) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = menuLabel)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(matchaStringResource(R.string.action_edit)) },
                            onClick = {
                                showMenu = false
                                state.note?.let { onEditNote(it) }
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(matchaStringResource(R.string.action_delete)) },
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
            containerColor = opaqueColorScheme.surface,
            title = { Text(matchaStringResource(R.string.delete_note_q)) },
            text = { Text(matchaStringResource(R.string.delete_action_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(onBack)
                    showDeleteDialog = false
                }) {
                    Text(matchaStringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(matchaStringResource(R.string.cancel))
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
                        placeholder = { Text(matchaStringResource(R.string.write_comment)) },
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
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = matchaStringResource(R.string.action_send))
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
                Text(matchaStringResource(R.string.note_not_found), modifier = Modifier.align(Alignment.Center))
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
                                        text = formatDate(note.date, matchaStringResource(R.string.date_format_full)),
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
                                        text = matchaStringResource(R.string.comments_count_label, state.comments.size),
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
                            onImageClick = {
                                activeCommentForViewer = comment
                                viewerInitialIndex = it
                            },
                            showActions = false
                        )
                    }
                }
            }
        }
    }

    if (viewerInitialIndex != -1 && activeCommentForViewer != null) {
        val comment = activeCommentForViewer!!
        val gifDocs = comment.documents.filter { it.ext.lowercase() == "gif" || it.type == 3 }
        val allImageUrls = comment.imageUrls + gifDocs.mapNotNull { it.url }

        ImageViewer(
            imageUrls = allImageUrls,
            initialIndex = viewerInitialIndex,
            onDismiss = {
                viewerInitialIndex = -1
                activeCommentForViewer = null
            }
        )
    }
}

private fun formatDate(timestamp: Long, format: String): String {
    val date = Date(timestamp * 1000)
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    return sdf.format(date)
}
