package com.deliriousvoid.openvkmatcha.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Default.Add, contentDescription = "Добавить аккаунт")
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
            items(accounts) { account ->
                val isCurrent = account.id == currentId
                
                AccountItem(
                    name = account.fullName,
                    instance = account.instance,
                    photoUrl = account.photoUrl,
                    isCurrent = isCurrent,
                    onClick = {
                        if (!isCurrent) {
                            viewModel.switchAccount(account.id, onAccountSwitched)
                        }
                    },
                    onRemove = {
                        accountToRemove = account.id
                    }
                )
            }
        }
    }

    if (accountToRemove != null) {
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = { Text("Удалить аккаунт") },
            text = { Text("Вы уверены, что хотите выйти из этого аккаунта?") },
            confirmButton = {
                TextButton(onClick = {
                    accountToRemove?.let { viewModel.removeAccount(it) }
                    accountToRemove = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRemove = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun AccountItem(
    name: String,
    instance: String,
    photoUrl: String,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = instance,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
