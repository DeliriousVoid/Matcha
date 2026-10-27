package com.deliriousvoid.openvkmatcha.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.theme.LocalOpaqueColorScheme
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.data.model.SelectableGift
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.ThemedPopupBackground
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SendGiftViewModel

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.StringUtils
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendGiftScreen(
    userId: Int? = null,
    onBack: () -> Unit,
    viewModel: SendGiftViewModel = viewModel(factory = SendGiftViewModel.factory(userId))
) {
    val state by viewModel.uiState.collectAsState()
    val opaqueColorScheme = LocalOpaqueColorScheme.current
    var showFriendPicker by remember { mutableStateOf(false) }
    var showGiftPicker by remember { mutableStateOf(false) }
    val title = matchaStringResource(R.string.action_send_gift)

    DisposableEffect(Unit) {
        AppEvents.setTopBarState(TopBarState(title = title, route = Routes.SEND_GIFT))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    LaunchedEffect(state.isSent) {
        if (state.isSent) {
            onBack()
        }
    }

    Scaffold(
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val selectedFriend = state.friends.find { it.id == state.selectedUserId }
                
                // Recipient Section
                Surface(
                    onClick = { showFriendPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedFriend != null) {
                            AsyncImage(
                                model = selectedFriend.photo200,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(matchaStringResource(R.string.gift_recipient), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(selectedFriend?.fullName ?: matchaStringResource(R.string.gift_choose_hint), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Large Gift Preview (Center)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                if (state.selectedGift != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = state.selectedGift!!.thumb256,
                            contentDescription = null,
                            modifier = Modifier
                                .size(200.dp)
                                .clickable { showGiftPicker = true },
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getPriceText(state.selectedGift!!),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.selectedGift!!.left != null && state.selectedGift!!.left!! > 0) {
                            Text(
                                text = matchaStringResource(R.string.gift_left_format, state.selectedGift!!.left!!),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = { showGiftPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(200.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close, // Placeholder
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                matchaStringResource(R.string.gift_choose_picker_hint),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Section (Message & Anonymity & Button)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.message,
                    onValueChange = { viewModel.setMessage(it) },
                    label = { Text(matchaStringResource(R.string.chat_input_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(matchaStringResource(R.string.gift_anonymous_label), style = MaterialTheme.typography.bodyLarge)
                        Text(matchaStringResource(R.string.gift_anonymous_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.isAnonymous,
                        onCheckedChange = { viewModel.setAnonymous(it) }
                    )
                }

                if (state.error != null) {
                    Text(
                        state.error!!, 
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Button(
                    onClick = { viewModel.send() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = state.selectedUserId != null && state.selectedGift != null && !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(matchaStringResource(R.string.action_send), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

    if (showFriendPicker) {
        ModalBottomSheet(
            onDismissRequest = { showFriendPicker = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = Color.Transparent
        ) {
            ThemedPopupBackground {
                Column(modifier = Modifier.fillMaxHeight(0.7f).padding(horizontal = 16.dp)) {
                    Text(
                        matchaStringResource(R.string.gift_who_to_send),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(state.friends) { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectUser(friend.id)
                                        showFriendPicker = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = friend.photo200,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(friend.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGiftPicker) {
        ModalBottomSheet(
            onDismissRequest = { showGiftPicker = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = Color.Transparent
        ) {
            ThemedPopupBackground {
                Column(modifier = Modifier.fillMaxHeight(0.8f)) {
                    Text(
                        matchaStringResource(R.string.gift_select_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingBox()
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(state.categoriesWithGifts) { item ->
                                Column {
                                    Text(
                                        text = item.category.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(item.gifts) { gift ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .width(100.dp)
                                                    .clickable {
                                                        viewModel.selectGift(gift)
                                                        showGiftPicker = false
                                                    }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(100.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = gift.thumb256,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val priceText = getPriceText(gift)
                                                val outOfStock = matchaStringResource(R.string.gift_out_of_stock)
                                                val leftPrefix = matchaStringResource(R.string.gift_left_format, 0).split(":")[0]
                                                val isFree = matchaStringResource(R.string.gift_free)
                                                
                                                Text(
                                                    text = priceText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (priceText.startsWith(leftPrefix) || priceText == outOfStock) Color.Red.copy(alpha = 0.7f) 
                                                            else if (priceText == isFree) MaterialTheme.colorScheme.primary 
                                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
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
    }
}

private fun getPriceText(gift: SelectableGift): String {
    val hasLimit = gift.left != null
    val left = gift.left ?: 0
    val price = gift.price ?: 0

    return when {
        hasLimit && left > 0 -> OpenVKMatchaApp.instance.languageManager.getStringOrDefault(R.string.gift_left_format, left)
        hasLimit && left == 0 && price == 0 -> OpenVKMatchaApp.instance.languageManager.getStringOrDefault(R.string.gift_out_of_stock)
        gift.priceStr != null -> gift.priceStr!!
        price == 0 -> OpenVKMatchaApp.instance.languageManager.getStringOrDefault(R.string.gift_free)
        else -> "$price ${StringUtils.getGolosString(price)}"
    }
}
