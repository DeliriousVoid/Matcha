package com.deliriousvoid.openvkmatcha.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.components.EmptyState
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.UserListItem
import com.deliriousvoid.openvkmatcha.ui.components.FriendshipStatusIcon
import com.deliriousvoid.openvkmatcha.ui.viewmodel.FriendsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    userId: Int,
    currentUserId: Int?,
    onOpenProfile: (Any) -> Unit,
    initialTab: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = viewModel(factory = FriendsViewModel.factory(userId, currentUserId))
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val tabs = if (state.isMe) {
        listOf("Друзья", "Онлайн", "Заявки")
    } else {
        listOf("Друзья", "Онлайн", "Подписчики")
    }

    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState, selectedTab, state.searchQuery) {
        androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                if (lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 5) {
                    viewModel.loadMore(selectedTab)
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.searchQuery.isEmpty()) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { 
                            if (selectedTab == index) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            } else {
                                selectedTab = index 
                            }
                        },
                        text = {
                            val count = when (index) {
                                0 -> state.friends.size
                                1 -> state.onlineFriends.size
                                2 -> if (state.isMe) state.requests.size else state.followers.size
                                else -> 0
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = false, // Handle refresh if needed
            onRefresh = { viewModel.loadFriends(); if (state.isMe) viewModel.loadRequests() },
            modifier = Modifier.fillMaxSize()
        ) {
            val listToDisplay = if (state.searchQuery.isNotEmpty()) {
                state.searchResults
            } else {
                when (selectedTab) {
                    0 -> state.friends
                    1 -> state.onlineFriends
                    2 -> if (state.isMe) state.requests else state.followers
                    else -> emptyList()
                }
            }

            when {
                (state.isLoading || state.isSearching) && listToDisplay.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
                state.error != null && listToDisplay.isEmpty() -> ErrorText(message = state.error!!)
                else -> {
                    if (listToDisplay.isEmpty()) {
                        EmptyState(
                            message = if (state.searchQuery.isNotEmpty()) {
                                "Ничего не найдено"
                            } else {
                                when (selectedTab) {
                                    0 -> "Список друзей пуст"
                                    1 -> "Никого нет в сети"
                                    2 -> if (state.isMe) "У вас нет новых заявок" else "Список подписчиков пуст"
                                    else -> ""
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(listToDisplay, key = { it.id }) { user ->
                                UserListItem(
                                    user = user,
                                    onClick = { onOpenProfile(user.id) },
                                    trailingContent = {
                                        if (state.searchQuery.isNotEmpty()) {
                                            FriendshipStatusIcon(
                                                user = user,
                                                onToggle = { viewModel.toggleFriendship(it) }
                                            )
                                        } else if (selectedTab == 2 && state.isMe) {
                                            Row {
                                                IconButton(onClick = { viewModel.acceptRequest(user) }) {
                                                    Icon(Icons.Default.Check, "Принять", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { viewModel.deleteFriend(user) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close, 
                                                        contentDescription = "Отклонить", 
                                                        tint = androidx.compose.ui.graphics.Color.Red
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            if (state.isLoadingMore) {
                                item {
                                    LoadingBox(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
