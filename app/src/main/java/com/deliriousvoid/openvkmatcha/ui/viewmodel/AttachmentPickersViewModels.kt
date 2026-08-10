package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.Document
import com.deliriousvoid.openvkmatcha.data.model.Video
import com.deliriousvoid.openvkmatcha.data.repository.DocsRepository
import com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository
import com.deliriousvoid.openvkmatcha.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoPickerUiState(
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val canLoadMore: Boolean = true
)

class VideoPickerViewModel(
    private val videoRepository: VideoRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoPickerUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    private var userId: Int? = null

    init {
        loadVideos()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadVideos(isRefresh = true)
    }

    fun loadVideos(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentUserId = userId ?: profileRepository.loadCurrentUser().getOrNull()?.id.also { userId = it }
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = "Ошибка профиля") }
                return@launch
            }

            val query = _uiState.value.searchQuery
            val offset = if (isRefresh) 0 else _uiState.value.videos.size
            
            if (isRefresh) _uiState.update { it.copy(isLoading = true) }
            else _uiState.update { it.copy(isLoadingMore = true) }

            val result = if (query.isNotBlank()) {
                videoRepository.searchVideos(query, offset = offset)
            } else {
                videoRepository.getVideos(currentUserId, offset = offset)
            }

            result.onSuccess { videos ->
                _uiState.update {
                    it.copy(
                        videos = if (isRefresh) videos else (it.videos + videos).distinctBy { v -> v.id },
                        isLoading = false,
                        isLoadingMore = false,
                        canLoadMore = videos.size >= 30
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return
        loadVideos(isRefresh = false)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return VideoPickerViewModel(app.videoRepository, app.profileRepository) as T
            }
        }
    }
}

data class DocsPickerUiState(
    val docs: List<Document> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val canLoadMore: Boolean = true
)

class DocsPickerViewModel(
    private val docsRepository: DocsRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocsPickerUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    private var userId: Int? = null

    init {
        loadDocs()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadDocs(isRefresh = true)
    }

    fun loadDocs(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentUserId = userId ?: profileRepository.loadCurrentUser().getOrNull()?.id.also { userId = it }
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = "Ошибка профиля") }
                return@launch
            }

            val query = _uiState.value.searchQuery
            val offset = if (isRefresh) 0 else _uiState.value.docs.size
            
            if (isRefresh) _uiState.update { it.copy(isLoading = true) }
            else _uiState.update { it.copy(isLoadingMore = true) }

            val result = if (query.isNotBlank()) {
                docsRepository.searchDocs(query, offset = offset)
            } else {
                docsRepository.getDocs(currentUserId, offset = offset)
            }

            result.onSuccess { docs ->
                _uiState.update {
                    it.copy(
                        docs = if (isRefresh) docs else (it.docs + docs).distinctBy { d -> d.id },
                        isLoading = false,
                        isLoadingMore = false,
                        canLoadMore = docs.size >= 30
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return
        loadDocs(isRefresh = false)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return DocsPickerViewModel(app.docsRepository, app.profileRepository) as T
            }
        }
    }
}
