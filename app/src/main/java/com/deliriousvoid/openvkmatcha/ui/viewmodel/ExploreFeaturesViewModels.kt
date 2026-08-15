package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.*
import com.deliriousvoid.openvkmatcha.data.repository.*
import com.deliriousvoid.openvkmatcha.util.AppEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- VIDEOS ---

data class VideosUiState(
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class VideosViewModel(
    private val repository: VideoRepository,
    private val userId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideosUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init { 
        loadVideos()
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { VideosUiState(isLoading = true) }
                loadVideos()
            }
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getVideos(userId).onSuccess { list ->
                _uiState.update { it.copy(videos = list, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    companion object {
        fun factory(userId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VideosViewModel(OpenVKMatchaApp.instance.videoRepository, userId) as T
            }
        }
    }
}

// --- DOCUMENTS ---

data class DocsUiState(
    val docs: List<Document> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DocsViewModel(
    private val repository: DocsRepository,
    private val userId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init { 
        loadDocs()
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { DocsUiState(isLoading = true) }
                loadDocs()
            }
        }
    }

    fun loadDocs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getDocs(userId).onSuccess { list ->
                _uiState.update { it.copy(docs = list, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    companion object {
        fun factory(userId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DocsViewModel(OpenVKMatchaApp.instance.docsRepository, userId) as T
            }
        }
    }
}

// --- NOTES ---

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotesViewModel(
    private val repository: NotesRepository,
    private val userId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotesUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init { 
        loadNotes()
        viewModelScope.launch {
            AppEvents.refreshNotes.collect {
                loadNotes()
            }
        }
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { NotesUiState(isLoading = true) }
                loadNotes()
            }
        }
    }

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getNotes(userId).onSuccess { response ->
                _uiState.update { it.copy(notes = response.items, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    companion object {
        fun factory(userId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotesViewModel(OpenVKMatchaApp.instance.notesRepository, userId) as T
            }
        }
    }
}

data class NoteDetailsUiState(
    val note: Note? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isSendingComment: Boolean = false,
    val error: String? = null
)

class NoteDetailsViewModel(
    private val repository: NotesRepository,
    private val profileRepository: ProfileRepository,
    private val ownerId: Int,
    private val noteId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteDetailsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init { 
        loadData()
        viewModelScope.launch {
            AppEvents.refreshNotes.collect {
                loadData()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val noteResult = repository.getNoteById(ownerId, noteId)
            val commentsResult = repository.getComments(ownerId, noteId)

            noteResult.onSuccess { note ->
                _uiState.update { it.copy(note = note) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }

            commentsResult.onSuccess { response ->
                _uiState.update { it.copy(comments = response.items) }
                fetchMissingProfiles(response.items)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun fetchMissingProfiles(comments: List<Comment>) {
        val missingUserIds = comments.filter { it.fromId > 0 && it.authorName.startsWith("id") }.map { it.fromId }.distinct()
        val missingGroupIds = comments.filter { it.fromId < 0 && (it.authorName.startsWith("club") || it.authorName.startsWith("id-")) }.map { kotlin.math.abs(it.fromId) }.distinct()

        if (missingUserIds.isEmpty() && missingGroupIds.isEmpty()) return

        if (missingUserIds.isNotEmpty()) {
            profileRepository.loadUsers(missingUserIds).onSuccess { users ->
                updateCommentsWithUsers(users)
            }
        }
        if (missingGroupIds.isNotEmpty()) {
            profileRepository.loadGroupsByIds(missingGroupIds).onSuccess { groups ->
                updateCommentsWithUsers(groups)
            }
        }
    }

    private fun updateCommentsWithUsers(profiles: List<UserProfile>) {
        _uiState.update { state ->
            state.copy(comments = state.comments.map { comment ->
                val profile = profiles.find { it.id == comment.fromId }
                if (profile != null) {
                    comment.copy(
                        authorName = profile.fullName,
                        authorAvatar = profile.photo50,
                        authorVerified = profile.verified
                    )
                } else comment
            })
        }
    }

    fun postComment(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true) }
            repository.createComment(ownerId, noteId, message).onSuccess {
                AppEvents.emitRefreshNotes()
                loadData()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isSendingComment = false) }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.deleteNote(noteId).onSuccess {
                AppEvents.emitRefreshNotes()
                onDeleted()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    companion object {
        fun factory(ownerId: Int, noteId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return NoteDetailsViewModel(app.notesRepository, app.profileRepository, ownerId, noteId) as T
            }
        }
    }
}

// --- CREATE / EDIT NOTE ---

data class CreateEditNoteUiState(
    val title: String = "",
    val text: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class CreateEditNoteViewModel(
    private val repository: NotesRepository,
    private val ownerId: Int? = null,
    private val noteId: Int? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateEditNoteUiState(isLoading = noteId != null))
    val uiState = _uiState.asStateFlow()

    init {
        if (ownerId != null && noteId != null) {
            loadNote(ownerId, noteId)
        }
    }

    private fun loadNote(ownerId: Int, noteId: Int) {
        viewModelScope.launch {
            repository.getNoteById(ownerId, noteId).onSuccess { note ->
                if (note != null) {
                    _uiState.update { it.copy(title = note.title, text = note.text, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Заметка не найдена") }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun save() {
        val title = _uiState.value.title
        val text = _uiState.value.text
        if (title.isBlank() || text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = if (noteId == null) {
                repository.addNote(title, text)
            } else {
                repository.editNote(noteId, title, text)
            }

            result.onSuccess {
                AppEvents.emitRefreshNotes()
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    companion object {
        fun factory(ownerId: Int? = null, noteId: Int? = null): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CreateEditNoteViewModel(OpenVKMatchaApp.instance.notesRepository, ownerId, noteId) as T
            }
        }
    }
}

// --- EVENTS ---

data class EventsUiState(
    val events: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class EventsViewModel(
    private val repository: ProfileRepository,
    private val userId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init { 
        loadEvents()
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { EventsUiState(isLoading = true) }
                loadEvents()
            }
        }
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getUserEvents(userId).onSuccess { list ->
                _uiState.update { it.copy(events = list, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    companion object {
        fun factory(userId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EventsViewModel(OpenVKMatchaApp.instance.profileRepository, userId) as T
            }
        }
    }
}

// --- TRANSFER ---

data class TransferUiState(
    val friends: List<UserProfile> = emptyList(),
    val searchResults: List<UserProfile> = emptyList(),
    val searchQuery: String = "",
    val selectedUser: UserProfile? = null,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class TransferViewModel(
    private val profileRepository: ProfileRepository,
    private val currentUserId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState = _uiState.asStateFlow()

    init { 
        loadFriends()
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { TransferUiState(isLoading = true) }
                loadFriends()
            }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            profileRepository.getFriends(currentUserId).onSuccess { list ->
                _uiState.update { it.copy(friends = list, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300) // Debounce
            _uiState.update { it.copy(isSearching = true) }
            profileRepository.searchUsers(query).onSuccess { list ->
                _uiState.update { it.copy(searchResults = list, isSearching = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSearching = false, error = e.message) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        search(query)
    }

    fun toggleFriendship(user: UserProfile) {
        viewModelScope.launch {
            val result = when (user.friendStatus) {
                0, null -> profileRepository.addFriend(user.id)
                1 -> profileRepository.deleteFriend(user.id)
                2 -> profileRepository.addFriend(user.id)
                3 -> profileRepository.deleteFriend(user.id)
                else -> return@launch
            }

            result.onSuccess {
                val newStatus = when (user.friendStatus) {
                    0, null -> 1
                    1 -> 0
                    2 -> 3
                    3 -> 0
                    else -> user.friendStatus
                }
                
                _uiState.update { state ->
                    state.copy(
                        searchResults = state.searchResults.map {
                            if (it.id == user.id) it.copy(friendStatus = newStatus) else it
                        },
                        friends = if (newStatus == 3) (state.friends + user.copy(friendStatus = 3)).distinctBy { it.id } 
                                 else state.friends.filter { it.id != user.id }
                    )
                }
            }.onFailure { error ->
                if (error is com.deliriousvoid.openvkmatcha.data.api.ApiException && error.errorCode == 15) {
                    loadFriends()
                } else {
                    com.deliriousvoid.openvkmatcha.util.AppEvents.showSnackbar(error.message ?: "Ошибка при выполнении действия")
                }
            }
        }
    }

    fun selectUser(user: UserProfile?) {
        _uiState.update { it.copy(selectedUser = user, error = null) }
    }

    fun sendTransfer(amount: Int, message: String?) {
        val user = _uiState.value.selectedUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            profileRepository.sendVotes(user.id, amount, message).onSuccess {
                _uiState.update { it.copy(isSending = false, success = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSending = false, error = e.message) }
            }
        }
    }

    companion object {
        fun factory(userId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TransferViewModel(OpenVKMatchaApp.instance.profileRepository, userId) as T
            }
        }
    }
}
