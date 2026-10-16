package com.deliriousvoid.openvkmatcha.ui.screens.messages

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatInfoViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatMemberItem
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatMaterialItem
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ChatMaterialSection
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    peerId: Int,
    onBack: () -> Unit,
    onOpenProfile: (Int) -> Unit,
    viewModel: ChatInfoViewModel = viewModel(factory = ChatInfoViewModel.factory(peerId))
) {
    val state by viewModel.uiState.collectAsState()
    var showAddMembersDialog by remember { mutableStateOf(false) }
    var memberToKick by remember { mutableStateOf<ChatMemberItem?>(null) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        AppEvents.setTopBarState(TopBarState(
            tag = "chat_info",
            title = "Информация о беседе",
            route = com.deliriousvoid.openvkmatcha.ui.navigation.Routes.CHAT_INFO,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        ))
        onDispose {
            if (AppEvents.topBarState.value?.tag == "chat_info") {
                AppEvents.setTopBarState(null)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = state.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${state.memberCount} участников",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = state.isMuted,
                        onClick = { viewModel.toggleMute() },
                        label = { Text(if (state.isMuted) "Без звука" else "Звук вкл.") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (state.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null
                            )
                        }
                    )

                    if (state.canAddMembers) {
                        FilterChip(
                            selected = false,
                            onClick = { showAddMembersDialog = true },
                            label = { Text("Добавить") },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Section Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChatMaterialSection.entries) { section ->
                    FilterChip(
                        selected = state.selectedSection == section,
                        onClick = { viewModel.selectSection(section) },
                        label = { Text(section.title) }
                    )
                }
            }

            HorizontalDivider()

            // Content
            Box(modifier = Modifier.weight(1f)) {
                if (state.selectedSection == ChatMaterialSection.MEMBERS) {
                    if (state.isLoadingMembers && state.members.isEmpty()) {
                        LoadingBox(modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.members, key = { it.id }) { member ->
                                MemberRow(
                                    member = member,
                                    canModerate = state.canModerate && member.id != state.peerId,
                                    isOwner = state.isOwner,
                                    onOpenProfile = { onOpenProfile(member.id) },
                                    onToggleModerator = { enable -> viewModel.setModerator(member, enable) },
                                    onKick = { memberToKick = member }
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
                            }
                        }
                    }
                } else {
                    val sectionState = state.materials[state.selectedSection]
                    if (sectionState?.isLoading == true && sectionState.items.isEmpty()) {
                        LoadingBox(modifier = Modifier.fillMaxSize())
                    } else if (sectionState == null || sectionState.items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Здесь пока нет материалов", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (state.selectedSection == ChatMaterialSection.PHOTOS || state.selectedSection == ChatMaterialSection.VIDEOS) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sectionState.items, key = { it.id }) { item ->
                                MaterialTile(item = item, onClick = {
                                    val url = item.videoUrl ?: item.photoUrl
                                    if (!url.isNullOrBlank()) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                    }
                                })
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(sectionState.items, key = { it.id }) { item ->
                                MaterialRow(item = item, onClick = {
                                    val url = item.audioUrl ?: item.docUrl ?: item.linkUrl
                                    if (!url.isNullOrBlank()) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                    }
                                })
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

    if (memberToKick != null) {
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            title = { Text("Исключить участника?") },
            text = { Text("Вы действительно хотите исключить ${memberToKick?.user?.fullName} из беседы?") },
            confirmButton = {
                TextButton(onClick = {
                    memberToKick?.let { viewModel.kickUser(it) }
                    memberToKick = null
                }) { Text("Исключить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { memberToKick = null }) { Text("Отмена") }
            }
        )
    }

    if (showAddMembersDialog) {
        AddMembersDialog(
            onDismiss = { showAddMembersDialog = false },
            onConfirm = { userIds ->
                viewModel.addUsers(userIds) { showAddMembersDialog = false }
            }
        )
    }
}

@Composable
private fun MemberRow(
    member: ChatMemberItem,
    canModerate: Boolean,
    isOwner: Boolean,
    onOpenProfile: () -> Unit,
    onToggleModerator: (Boolean) -> Unit,
    onKick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenProfile() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = member.user.photo200.ifBlank { member.user.photo50 },
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.user.fullName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            member.roleTitle?.let { role ->
                Text(
                    text = role,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (canModerate && (!member.isOwner || isOwner)) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Еще")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (isOwner && !member.isOwner) {
                        DropdownMenuItem(
                            text = { Text(if (member.isModerator) "Снять модератора" else "Назначить модератором") },
                            onClick = {
                                showMenu = false
                                onToggleModerator(!member.isModerator)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Исключить из беседы", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onKick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialTile(item: ChatMaterialItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.photoUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        if (!item.videoUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Воспроизвести",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun MaterialRow(item: ChatMaterialItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                !item.audioUrl.isNullOrBlank() -> Icons.Default.MusicNote
                !item.docUrl.isNullOrBlank() -> Icons.Default.InsertDriveFile
                else -> Icons.Default.Link
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.subtitle?.let { sub ->
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AddMembersDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
    profileRepository: com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository = OpenVKMatchaApp.instance.profileRepository
) {
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val myId = profileRepository.loadCurrentUser().getOrNull()?.id ?: 0
        profileRepository.getFriends(myId).onSuccess {
            friends = it
            isLoading = false
        }.onFailure { isLoading = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить участников") },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(friends, key = { it.id }) { user ->
                        val isSelected = selectedIds.contains(user.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isSelected) selectedIds - user.id else selectedIds + user.id
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selectedIds = if (it) selectedIds + user.id else selectedIds - user.id
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(user.fullName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty()
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
