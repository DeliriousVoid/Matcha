package com.deliriousvoid.openvkmatcha.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.ui.viewmodel.CreateChatViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatScreen(
    onBack: () -> Unit,
    onChatCreated: (Int, String) -> Unit,
    viewModel: CreateChatViewModel = viewModel(factory = CreateChatViewModel.factory())
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(state.title, state.selectedUserIds, state.isCreating) {
        AppEvents.setTopBarState(TopBarState(
            tag = "create_chat",
            title = "Новая беседа",
            route = Routes.CREATE_CHAT,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                TextButton(
                    onClick = { viewModel.createChat(context, onChatCreated) },
                    enabled = state.title.isNotBlank() && state.selectedUserIds.isNotEmpty() && !state.isCreating
                ) {
                    if (state.isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Создать", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ))
        onDispose {
            if (AppEvents.topBarState.value?.tag == "create_chat") {
                AppEvents.setTopBarState(null)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
            // Group Title Input
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Название беседы") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) }
            )

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Поиск участников...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Text(
                text = "Участники: ${state.selectedUserIds.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider()

            // Friends / Users List
            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoadingFriends && state.friends.isEmpty()) {
                    LoadingBox(modifier = Modifier.fillMaxSize())
                } else {
                    val displayedUsers = if (state.searchQuery.isNotBlank()) {
                        viewModel.filteredFriends(state.searchQuery)
                    } else state.friends

                    if (displayedUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Никого не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                items(displayedUsers, key = { it.id }) { user ->
                                    val isSelected = state.selectedUserIds.contains(user.id)
                                    UserSelectRow(
                                        user = user,
                                        isSelected = isSelected,
                                        onToggle = { viewModel.toggleParticipant(user.id) }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
                                }
                            }

                            // Alphabet Index Sidebar
                            if (state.searchQuery.isBlank() && state.groupedFriends.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 4.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    state.groupedFriends.forEach { (letter, users) ->
                                        Text(
                                            text = letter,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clickable {
                                                    val index = displayedUsers.indexOfFirst {
                                                        it.fullName.startsWith(letter, ignoreCase = true)
                                                    }
                                                    if (index != -1) {
                                                        scope.launch { listState.animateScrollToItem(index) }
                                                    }
                                                }
                                                .padding(vertical = 2.dp, horizontal = 4.dp)
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

@Composable
private fun UserSelectRow(
    user: UserProfile,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        AsyncImage(
            model = user.photo200.ifBlank { user.photo50 },
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.screenName.isNotBlank()) {
                Text(
                    text = "@${user.screenName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
