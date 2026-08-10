package com.deliriousvoid.openvkmatcha.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.Notification
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.VerifiedBadge
import com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo
import com.deliriousvoid.openvkmatcha.ui.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onOpenProfile: (Any) -> Unit,
    onOpenPost: (Int, Int) -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.factory())
) {
    val state by viewModel.uiState.collectAsState()

    // Mark as read when the list of notifications changes and we have unread ones
    LaunchedEffect(state.notifications, state.isArchive) {
        if (!state.isArchive && state.unreadCount > 0) {
            viewModel.markAsRead()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = if (state.isArchive) 1 else 0,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            Tab(
                selected = !state.isArchive,
                onClick = { viewModel.setArchive(false) },
                text = { Text("Новые") }
            )
            Tab(
                selected = state.isArchive,
                onClick = { viewModel.setArchive(true) },
                text = { Text("Архив") }
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.loadNotifications(refresh = true, isManual = true) },
            modifier = Modifier.weight(1f)
        ) {
            if (state.isLoading) {
                LoadingBox(modifier = Modifier.fillMaxSize())
            } else if (state.notifications.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.isArchive) "Архив пуст" else "Уведомлений пока нет",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onAuthorClick = { onOpenProfile(notification.authorId) },
                            onItemClick = {
                                if (notification.ownerId != 0 && notification.itemId != 0) {
                                    onOpenPost(notification.ownerId, notification.itemId)
                                }
                            },
                            onLoadDetails = { viewModel.loadCommentDetails(notification) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onAuthorClick: () -> Unit,
    onItemClick: () -> Unit,
    onLoadDetails: () -> Unit = {}
) {
    LaunchedEffect(notification.id) {
        onLoadDetails()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = notification.authorAvatar,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onAuthorClick() },
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.authorName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (notification.authorVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(userId = notification.authorId, isVerified = true)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimeAgo(notification.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = notification.action,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            notification.text?.let { text ->
                if (text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            notification.parentText?.let { parent ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = parent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
