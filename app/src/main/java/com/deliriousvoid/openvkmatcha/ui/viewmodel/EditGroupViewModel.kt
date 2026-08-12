package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.GroupSettings
import com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository
import com.deliriousvoid.openvkmatcha.util.AppEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditGroupTab {
    MAIN, ADVANCED
}

data class EditGroupUiState(
    val groupId: Int,
    val settings: GroupSettings = GroupSettings(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val currentTab: EditGroupTab = EditGroupTab.MAIN
)

class EditGroupViewModel(
    private val groupId: Int,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditGroupUiState(groupId = groupId))
    val uiState: StateFlow<EditGroupUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            profileRepository.getGroupSettings(groupId)
                .onSuccess { settings ->
                    _uiState.update { it.copy(settings = settings, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun updateSettings(update: (GroupSettings) -> GroupSettings) {
        _uiState.update { it.copy(settings = update(it.settings)) }
    }

    fun setTab(tab: EditGroupTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            profileRepository.editGroup(groupId, _uiState.value.settings)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                    AppEvents.emitRefreshProfile(-groupId)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
        }
    }

    companion object {
        fun factory(groupId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditGroupViewModel(groupId, OpenVKMatchaApp.instance.profileRepository) as T
            }
        }
    }
}
