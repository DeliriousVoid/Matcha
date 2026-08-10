package com.deliriousvoid.openvkmatcha.ui.screens.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.components.UserListItem
import com.deliriousvoid.openvkmatcha.ui.components.FriendshipStatusIcon
import com.deliriousvoid.openvkmatcha.ui.viewmodel.TransferViewModel

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(state.selectedUser) {
        AppEvents.setTopBarState(TopBarState(
            tag = "transfer",
            title = if (state.selectedUser == null) "Выберите получателя" else "Перевод голосов",
            navigationIcon = {
                IconButton(onClick = {
                    if (state.selectedUser != null) viewModel.selectUser(null)
                    else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        ))
        onDispose {
            if (AppEvents.topBarState.value?.tag == "transfer") {
                AppEvents.setTopBarState(null)
            }
        }
    }
    
    LaunchedEffect(state.success) {
        if (state.success) {
            onSuccess()
        }
    }

    Scaffold(
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.selectedUser == null) {
                UserSelectionContent(viewModel, state)
            } else {
                TransferDetailsContent(viewModel, state)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSelectionContent(viewModel: TransferViewModel, state: com.deliriousvoid.openvkmatcha.ui.viewmodel.TransferUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Имя, фамилия или ID") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = if (state.searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            } else null,
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        val list = if (state.searchQuery.isNotEmpty()) state.searchResults else state.friends

        if ((state.isLoading || state.isSearching) && list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(list, key = { it.id }) { user ->
                    UserListItem(
                        user = user,
                        onClick = { viewModel.selectUser(user) },
                        trailingContent = {
                            FriendshipStatusIcon(
                                user = user,
                                onToggle = { viewModel.toggleFriendship(it) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferDetailsContent(viewModel: TransferViewModel, state: com.deliriousvoid.openvkmatcha.ui.viewmodel.TransferUiState) {
    val user = state.selectedUser ?: return
    var amount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user.photo200,
            contentDescription = null,
            modifier = Modifier.size(100.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "${user.firstName} ${user.lastName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = user.screenName.ifBlank { "ID: ${user.id}" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = amount,
            onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
            label = { Text("Количество голосов") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Сообщение (необязательно)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        
        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { viewModel.sendTransfer(amount.toIntOrNull() ?: 0, message) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = amount.isNotEmpty() && (amount.toIntOrNull() ?: 0) > 0 && !state.isSending,
            shape = MaterialTheme.shapes.medium
        ) {
            if (state.isSending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("Перевести")
            }
        }
    }
}
