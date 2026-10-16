package com.deliriousvoid.openvkmatcha.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.data.repository.MessagesRepository
import com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateChatUiState(
    val friends: List<UserProfile> = emptyList(),
    val groupedFriends: List<Pair<String, List<UserProfile>>> = emptyList(),
    val globalUsers: List<UserProfile> = emptyList(),
    val selectedUserIds: Set<Int> = emptySet(),
    val searchQuery: String = "",
    val title: String = "",
    val avatarUri: Uri? = null,
    val isLoadingFriends: Boolean = false,
    val isLoadingGlobal: Boolean = false,
    val isCreating: Boolean = false,
    val createdPeerId: Int? = null,
    val error: String? = null
)

class CreateChatViewModel(
    private val profileRepository: ProfileRepository,
    private val messagesRepository: MessagesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateChatUiState())
    val uiState: StateFlow<CreateChatUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadFriends()
    }

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFriends = true, error = null) }
            val currentUser = profileRepository.loadCurrentUser().getOrNull()
            val userId = currentUser?.id ?: 0
            profileRepository.getFriends(userId = userId, offset = 0)
                .onSuccess { list ->
                    val sorted = list.sortedBy { it.fullName }
                    val grouped = groupFriends(sorted)
                    _uiState.update {
                        it.copy(
                            friends = sorted,
                            groupedFriends = grouped,
                            isLoadingFriends = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingFriends = false, error = error.message) }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchGlobal(query)
    }

    private fun searchGlobal(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(globalUsers = emptyList(), isLoadingGlobal = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isLoadingGlobal = true) }
            profileRepository.searchUsers(trimmed)
                .onSuccess { list ->
                    _uiState.update { it.copy(globalUsers = list, isLoadingGlobal = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingGlobal = false) }
                }
        }
    }

    fun toggleParticipant(userId: Int) {
        _uiState.update { state ->
            val set = state.selectedUserIds.toMutableSet()
            if (set.contains(userId)) set.remove(userId) else set.add(userId)
            state.copy(selectedUserIds = set)
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateAvatarUri(uri: Uri?) {
        _uiState.update { it.copy(avatarUri = uri) }
    }

    fun createChat(context: Context, onSuccess: (Int, String) -> Unit) {
        val title = _uiState.value.title.trim()
        val userIds = _uiState.value.selectedUserIds.toList()
        if (title.isBlank() || userIds.isEmpty() || _uiState.value.isCreating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            messagesRepository.createChat(title, userIds)
                .onSuccess { json ->
                    val responseObj = json.optJSONObject("response") ?: json
                    val chatId = if (json.has("response") && json.optInt("response") != 0) {
                        json.optInt("response")
                    } else {
                        responseObj.optInt("chat_id", responseObj.optInt("id"))
                    }

                    if (chatId != 0) {
                        val peerId = 2000000000 + chatId
                        val avatarUri = _uiState.value.avatarUri
                        if (avatarUri != null) {
                            try {
                                val bytes = context.contentResolver.openInputStream(avatarUri)?.use { it.readBytes() }
                                if (bytes != null) {
                                    messagesRepository.setChatPhoto(chatId, bytes, "avatar.jpg")
                                }
                            } catch (_: Exception) {}
                        }
                        _uiState.update { it.copy(isCreating = false, createdPeerId = peerId) }
                        onSuccess(peerId, title)
                    } else {
                        _uiState.update { it.copy(isCreating = false, error = "Failed to obtain chat ID") }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isCreating = false, error = error.message) }
                }
        }
    }

    fun filteredFriends(query: String): List<UserProfile> {
        if (query.isBlank()) return _uiState.value.friends
        return _uiState.value.friends.filter {
            it.fullName.contains(query, ignoreCase = true) || it.screenName.contains(query, ignoreCase = true)
        }
    }

    private fun groupFriends(list: List<UserProfile>): List<Pair<String, List<UserProfile>>> {
        val groups = list.groupBy { user ->
            val firstChar = user.fullName.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar in 'A'..'Z' || firstChar in 'А'..'Я') firstChar.toString() else "#"
        }
        return groups.keys.sortedWith { a, b ->
            if (a == "#") 1 else if (b == "#") -1 else a.compareTo(b)
        }.map { key -> key to (groups[key] ?: emptyList()) }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return CreateChatViewModel(app.profileRepository, app.messagesRepository) as T
            }
        }
    }
}
