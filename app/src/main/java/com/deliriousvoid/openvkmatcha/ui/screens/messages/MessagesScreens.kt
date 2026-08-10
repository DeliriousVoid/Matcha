package com.deliriousvoid.openvkmatcha.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.ui.components.ConversationItem
import com.deliriousvoid.openvkmatcha.ui.components.EmptyState
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MessagesViewModel

@Composable
fun MessagesScreen(
    onConversationClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessagesViewModel = viewModel(factory = MessagesViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.isLoading -> LoadingBox(modifier = modifier.fillMaxSize())
        state.error != null && state.conversations.isEmpty() -> ErrorText(
            message = state.error!!,
            modifier = modifier.fillMaxSize(),
            onRetry = { viewModel.loadConversations() }
        )
        state.conversations.isEmpty() -> EmptyState(
            message = "У вас пока нет сообщений",
            modifier = modifier.fillMaxSize()
        )
        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
            items(state.conversations, key = { it.peerId }) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    onClick = { onConversationClick(conversation.peerId, conversation.title) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                )
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
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(peerId)),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> LoadingBox(modifier = Modifier.weight(1f))
                state.error != null && state.messages.isEmpty() -> ErrorText(
                    message = state.error!!,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onRetry = { viewModel.loadMessages() }
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = viewModel::updateInput,
                    placeholder = { Text("Сообщение") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 4,
                )
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = state.inputText.isNotBlank() && !state.isSending,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: com.deliriousvoid.openvkmatcha.data.model.ChatMessage,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (message.isOutgoing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.text,
                color = if (message.isOutgoing) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Text(
            text = formatTimeAgo(message.date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
        )
    }
}
