package com.deliriousvoid.openvkmatcha.ui.screens.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.data.model.Topic
import com.deliriousvoid.openvkmatcha.ui.components.EmptyState
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.viewmodel.BoardViewModel
import com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(
    groupId: Int,
    groupName: String,
    onOpenTopic: (Int, String, Int?) -> Unit,
    viewModel: BoardViewModel = viewModel(factory = BoardViewModel.factory(groupId))
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem > totalItems - 5 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.loadTopics(refresh = true) },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.topics.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
            state.error != null && state.topics.isEmpty() -> ErrorText(
                message = state.error!!,
                modifier = Modifier.fillMaxSize(),
                onRetry = { viewModel.loadTopics() }
            )
            state.topics.isEmpty() -> EmptyState(
                message = "В этом обсуждении пока нет тем",
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                val vidGuesses = remember(state.topics) {
                    state.topics.map { it.id }.sorted().mapIndexed { index, id -> id to index + 1 }.toMap()
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.topics) { topic ->
                        TopicItem(
                            topic = topic,
                            onClick = { 
                                val guess = vidGuesses[topic.id]
                                onOpenTopic(topic.id, topic.title, guess) 
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                    if (state.isLoadingMore) {
                        item {
                            LoadingBox(modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicItem(
    topic: Topic,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = topic.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${topic.commentsCount} сообщений",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Обновлено ${formatTimeAgo(topic.updated)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
