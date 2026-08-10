package com.deliriousvoid.openvkmatcha.ui.screens.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.ui.components.EmptyState
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.UserListItem
import com.deliriousvoid.openvkmatcha.ui.viewmodel.FollowersViewModel

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersScreen(
    id: Int,
    isGroup: Boolean,
    onOpenProfile: (Any) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowersViewModel = viewModel(factory = FollowersViewModel.factory(id, isGroup))
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

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

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.users.isNotEmpty(),
        onRefresh = { viewModel.loadFollowers(isRefresh = true) },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.users.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
            state.error != null && state.users.isEmpty() -> ErrorText(
                message = state.error!!,
                onRetry = { viewModel.loadFollowers(isRefresh = true) }
            )
            state.users.isEmpty() -> EmptyState(
                message = "Список пуст",
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(state.users, key = { it.id }) { user ->
                        UserListItem(
                            user = user,
                            onClick = { onOpenProfile(user.id) }
                        )
                    }

                    if (state.isLoadingMore) {
                        item {
                            LoadingBox(modifier = Modifier.fillMaxWidth().padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
