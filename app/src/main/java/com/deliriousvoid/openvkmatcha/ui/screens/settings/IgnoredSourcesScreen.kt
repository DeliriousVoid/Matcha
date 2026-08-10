package com.deliriousvoid.openvkmatcha.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.ui.components.EmptyState
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.components.UserListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IgnoredSourcesUiState(
    val sources: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class IgnoredSourcesViewModel : ViewModel() {
    private val repository = OpenVKMatchaApp.instance.feedRepository
    private val _uiState = MutableStateFlow(IgnoredSourcesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSources()
    }

    fun loadSources() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getIgnoredSources().onSuccess { list ->
                _uiState.update { it.copy(sources = list, isLoading = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false) }
            }
        }
    }

    fun unignore(id: Int) {
        viewModelScope.launch {
            repository.unignoreSource(id).onSuccess {
                _uiState.update { state ->
                    state.copy(sources = state.sources.filter { it.id != id })
                }
            }
        }
    }
}

@Composable
fun IgnoredSourcesScreen(
    onOpenProfile: (Any) -> Unit,
    viewModel: IgnoredSourcesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.sources.isEmpty() -> LoadingBox(modifier = Modifier.fillMaxSize())
            state.error != null && state.sources.isEmpty() -> ErrorText(
                message = state.error!!,
                modifier = Modifier.fillMaxSize(),
                onRetry = { viewModel.loadSources() }
            )
            state.sources.isEmpty() -> EmptyState(
                message = "Список игнорируемых пуст",
                icon = Icons.Default.Block,
                modifier = Modifier.fillMaxSize()
            )
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.sources) { source ->
                        UserListItem(
                            user = source,
                            onClick = { onOpenProfile(source.id) },
                            trailingContent = {
                                TextButton(onClick = { viewModel.unignore(source.id) }) {
                                    Text("Не игнорировать")
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
