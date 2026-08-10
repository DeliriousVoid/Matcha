package com.deliriousvoid.openvkmatcha.ui.screens.explore

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deliriousvoid.openvkmatcha.ui.viewmodel.CreateEditNoteViewModel

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditNoteScreen(
    viewModel: CreateEditNoteViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(state.isLoading, state.title, state.text, state.isSaving) {
        AppEvents.setTopBarState(TopBarState(
            title = if (state.isLoading) "Заметка" else if (state.title.isEmpty()) "Новая заметка" else "Редактирование",
            actions = {
                IconButton(
                    onClick = { viewModel.save() },
                    enabled = !state.isSaving && state.title.isNotBlank() && state.text.isNotBlank()
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
                    }
                }
            }
        ))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaved()
        }
    }

    Scaffold(
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Заголовок") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.text,
                onValueChange = { viewModel.updateText(it) },
                label = { Text("Текст заметки") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
