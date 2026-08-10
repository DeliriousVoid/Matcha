package com.deliriousvoid.openvkmatcha.ui.screens.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.deliriousvoid.openvkmatcha.ui.navigation.MainTab
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val activeTabs by viewModel.navigationTabs.collectAsState()
    val allTabs = MainTab.entries
    val availableTabs = allTabs.filter { it !in activeTabs }

    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    DisposableEffect(Unit) {
        AppEvents.setTopBarState(TopBarState(
            title = "Настройка навигации",
            actions = {
                TextButton(onClick = {
                    viewModel.setNavigationTabs(listOf(
                        MainTab.Home, MainTab.Explore, MainTab.Messages, MainTab.Music, MainTab.Profile
                    ))
                }) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сброс")
                }
            }
        ))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                text = "Активные вкладки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Text(
                text = "Перетаскивайте для изменения порядка. Минимум 3, максимум 5.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemsIndexed(activeTabs, key = { _, tab -> tab.name }) { index, tab ->
            val isDragging = draggedItemIndex == index
            DraggableTabItem(
                tab = tab,
                isDragging = isDragging,
                draggingOffset = if (isDragging) draggingOffset else 0f,
                canRemove = activeTabs.size > 3,
                onRemove = {
                    val newList = activeTabs.toMutableList()
                    newList.removeAt(index)
                    viewModel.setNavigationTabs(newList)
                },
                onDragStart = { 
                    draggedItemIndex = index 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { delta ->
                    val currentIdx = draggedItemIndex ?: return@DraggableTabItem
                    val itemHeight = with(density) { 64.dp.toPx() } // 56dp height + 8dp total vertical padding
                    val threshold = itemHeight * 0.5f

                    var newOffset = draggingOffset + delta
                    
                    // Boundary clamping: don't allow dragging past the first or last item
                    if (currentIdx == 0) {
                        newOffset = newOffset.coerceAtLeast(0f)
                    }
                    if (currentIdx == activeTabs.size - 1) {
                        newOffset = newOffset.coerceAtMost(0f)
                    }
                    
                    draggingOffset = newOffset

                    if (draggingOffset > threshold && (currentIdx < activeTabs.size - 1)) {
                        viewModel.moveNavigationTab(currentIdx, currentIdx + 1)
                        draggedItemIndex = currentIdx + 1
                        draggingOffset -= itemHeight
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else if (draggingOffset < -threshold && (currentIdx > 0)) {
                        viewModel.moveNavigationTab(currentIdx, currentIdx - 1)
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

        if (availableTabs.isNotEmpty()) {
            item {
                Text(
                    text = "Доступные вкладки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
            }

            itemsIndexed(availableTabs, key = { _, tab -> "avail_${tab.name}" }) { _, tab ->
                AvailableTabItem(
                    tab = tab,
                    canAdd = activeTabs.size < 5,
                    onAdd = {
                        val newList = activeTabs.toMutableList()
                        newList.add(tab)
                        viewModel.setNavigationTabs(newList)
                    }
                )
            }
        }
    }
}

@Composable
fun DraggableTabItem(
    tab: MainTab,
    isDragging: Boolean,
    draggingOffset: Float,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "dragElevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                translationY = draggingOffset
            }
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(elevation, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .height(56.dp) // Fixed height for consistent dragging
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(tab.name) {
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
                    contentDescription = "Перетащить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = tab.icon(true),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = tab.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Удалить")
                }
            }
        }
    }
}

@Composable
fun AvailableTabItem(
    tab: MainTab,
    canAdd: Boolean,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tab.icon(false),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = tab.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            IconButton(
                onClick = onAdd,
                enabled = canAdd
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Добавить",
                    tint = if (canAdd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}
