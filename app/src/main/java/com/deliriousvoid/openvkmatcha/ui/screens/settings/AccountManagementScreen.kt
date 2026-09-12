package com.deliriousvoid.openvkmatcha.ui.screens.settings

import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.ui.viewmodel.AccountsViewModel

@Composable
fun AccountManagementScreen(
    onAddAccount: () -> Unit,
    onAccountSwitched: () -> Unit,
    viewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.factory())
) {
    val accounts by viewModel.accounts.collectAsState()
    val currentId by viewModel.currentAccountId.collectAsState()
    
    var accountToRemove by remember { mutableStateOf<String?>(null) }

    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Default.Add, contentDescription = matchaStringResource(R.string.add_account))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                val isCurrent = account.id == currentId
                val isDragging = draggedItemIndex == index

                DraggableAccountItem(
                    name = account.fullName,
                    instance = account.instance,
                    photoUrl = account.photoUrl,
                    isCurrent = isCurrent,
                    isDragging = isDragging,
                    draggingOffset = if (isDragging) draggingOffset else 0f,
                    onClick = {
                        if (!isCurrent) {
                            viewModel.switchAccount(account.id, onAccountSwitched)
                        }
                    },
                    onRemove = {
                        accountToRemove = account.id
                    },
                    onDragStart = {
                        draggedItemIndex = index
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { delta ->
                        val currentIdx = draggedItemIndex ?: return@DraggableAccountItem
                        val itemHeight = with(density) { (72.dp + 12.dp).toPx() } // Approx height of AccountItem + spacing
                        val threshold = itemHeight * 0.5f

                        var newOffset = draggingOffset + delta
                        
                        // Boundary clamping
                        if (currentIdx == 0) {
                            newOffset = newOffset.coerceAtLeast(0f)
                        }
                        if (currentIdx == accounts.size - 1) {
                            newOffset = newOffset.coerceAtMost(0f)
                        }
                        
                        draggingOffset = newOffset

                        if (draggingOffset > threshold && (currentIdx < accounts.size - 1)) {
                            viewModel.moveAccount(currentIdx, currentIdx + 1)
                            draggedItemIndex = currentIdx + 1
                            draggingOffset -= itemHeight
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else if (draggingOffset < -threshold && (currentIdx > 0)) {
                            viewModel.moveAccount(currentIdx, currentIdx - 1)
                            draggedItemIndex = currentIdx - 1
                            draggingOffset += itemHeight
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragEnd = {
                        draggedItemIndex = null
                        draggingOffset = 0f
                    }
                )
            }
        }
    }

    if (accountToRemove != null) {
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = { Text(matchaStringResource(R.string.delete_account_q)) },
            text = { Text(matchaStringResource(R.string.logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    accountToRemove?.let { viewModel.removeAccount(it) }
                    accountToRemove = null
                }) {
                    Text(matchaStringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRemove = null }) {
                    Text(matchaStringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun DraggableAccountItem(
    name: String,
    instance: String,
    photoUrl: String,
    isCurrent: Boolean,
    isDragging: Boolean,
    draggingOffset: Float,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 2.dp,
        label = "dragElevation"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = draggingOffset
            }
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(elevation, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .pointerInput(name) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { currentOnDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnDrag(dragAmount.y)
                            },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = matchaStringResource(R.string.desc_drag),
                    tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val formattedInstance = remember(instance) {
                    instance.removePrefix("https://")
                        .removePrefix("http://")
                        .removeSuffix("/")
                        .substringBeforeLast(".")
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formattedInstance,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            if (isCurrent) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = matchaStringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
