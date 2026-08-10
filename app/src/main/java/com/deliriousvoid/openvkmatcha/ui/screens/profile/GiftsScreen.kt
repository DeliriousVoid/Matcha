package com.deliriousvoid.openvkmatcha.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.ui.components.EmptyState
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.viewmodel.GiftsViewModel
import com.deliriousvoid.openvkmatcha.ui.util.formatTimeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftsScreen(
    userId: Int,
    onOpenProfile: (Any) -> Unit,
    onSendGift: (Int?) -> Unit = {},
    userName: String? = null,
    modifier: Modifier = Modifier,
    viewModel: GiftsViewModel = viewModel(factory = GiftsViewModel.factory(userId))
) {
    val state by viewModel.uiState.collectAsState()
    val isMyProfile = state.currentUserId == userId

    Column(modifier = modifier.fillMaxSize()) {
        if (isMyProfile) {
            Button(
                onClick = { onSendGift(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Отправить подарок")
            }
        } else {
            Button(
                onClick = { onSendGift(userId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Отправить подарок${if (userName != null) " $userName" else ""}")
            }
        }

        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.loadGifts() },
            modifier = Modifier.weight(1f)
        ) {
            when {
                state.isLoading && state.gifts.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
                state.error != null && state.gifts.isEmpty() -> ErrorText(message = state.error!!)
                state.gifts.isEmpty() -> EmptyState(message = "Подарков пока нет", modifier = Modifier.fillMaxSize())
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.gifts, key = { "${it.id}_${it.date}" }) { gift ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = gift.thumb256,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = gift.senderName ?: "Анонимный отправитель",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { if (gift.fromId != 0) onOpenProfile(gift.fromId) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = formatTimeAgo(gift.date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (gift.message.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = gift.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
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
}
