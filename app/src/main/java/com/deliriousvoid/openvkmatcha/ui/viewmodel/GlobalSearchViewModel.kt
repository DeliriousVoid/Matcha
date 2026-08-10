package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.data.repository.MusicRepository
import com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.SearchCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GlobalSearchUiState(
    val results: List<Any> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val category: SearchCategory = SearchCategory.PEOPLE
)

class GlobalSearchViewModel(
    private val musicRepository: MusicRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val pageSize = 50

    init {
        viewModelScope.launch {
            AppEvents.searchQuery.collect { query ->
                _uiState.update { it.copy(searchQuery = query, canLoadMore = true) }
                performSearch(isRefresh = true)
            }
        }
        viewModelScope.launch {
            AppEvents.searchCategory.collect { category ->
                _uiState.update { it.copy(category = category, results = emptyList(), canLoadMore = true) }
                performSearch(isRefresh = true)
            }
        }
    }

    private fun performSearch(isRefresh: Boolean = false) {
        searchJob?.cancel()
        val query = _uiState.value.searchQuery
        val category = _uiState.value.category

        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false, canLoadMore = false) }
            return
        }

        searchJob = viewModelScope.launch {
            if (isRefresh) {
                delay(300)
                _uiState.update { it.copy(isLoading = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true, error = null) }
            }
            
            val offset = if (isRefresh) 0 else _uiState.value.results.size

            val result = when (category) {
                SearchCategory.PEOPLE -> profileRepository.searchUsers(query, offset = offset, count = pageSize)
                SearchCategory.GROUPS -> profileRepository.searchGroups(query, offset = offset, count = pageSize)
                SearchCategory.MUSIC -> musicRepository.searchAudio(query, offset = offset, count = pageSize)
                SearchCategory.PLAYLISTS -> musicRepository.searchPlaylists(query, offset = offset, count = pageSize)
            }

            result.onSuccess { data ->
                val enrichedData = if (category == SearchCategory.MUSIC) {
                    val downloadRepository = OpenVKMatchaApp.instance.downloadRepository
                    (data as List<AudioTrack>).map { downloadRepository.enrichTrack(it) }
                } else data

                _uiState.update { state ->
                    val newList = if (isRefresh) enrichedData else (state.results + enrichedData)
                    state.copy(
                        results = newList,
                        isLoading = false,
                        isLoadingMore = false,
                        canLoadMore = data.size >= pageSize
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || !_uiState.value.canLoadMore) return
        performSearch(isRefresh = false)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return GlobalSearchViewModel(app.musicRepository, app.profileRepository) as T
            }
        }
    }
}
