package com.deliriousvoid.openvkmatcha.ui.screens.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.components.VerifiedBadge
import com.deliriousvoid.openvkmatcha.ui.viewmodel.EventsViewModel

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewModel: EventsViewModel,
    onBack: () -> Unit,
    onOpenEvent: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        AppEvents.setTopBarState(TopBarState(tag = "events", title = "События"))
        onDispose {
            if (AppEvents.topBarState.value?.tag == "events") {
                AppEvents.setTopBarState(null)
            }
        }
    }

    Scaffold(
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(state.error!!, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            } else if (state.events.isEmpty()) {
                Text("У вас пока нет событий", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.events) { event ->
                        EventItem(event, onOpenEvent)
                    }
                }
            }
        }
    }
}

@Composable
fun EventItem(event: UserProfile, onClick: (Int) -> Unit) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.firstName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                if (event.verified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(userId = event.id, isVerified = true)
                }
            }
        },
        supportingContent = {
            Text(event.status, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            AsyncImage(
                model = event.photo200,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        modifier = Modifier.clickable { onClick(event.id) }
    )
}
