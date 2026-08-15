package com.deliriousvoid.openvkmatcha.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.ChatMessage
import com.deliriousvoid.openvkmatcha.data.model.Comment
import com.deliriousvoid.openvkmatcha.data.model.CommentThreadItem
import com.deliriousvoid.openvkmatcha.data.model.Conversation
import com.deliriousvoid.openvkmatcha.data.model.Gift
import com.deliriousvoid.openvkmatcha.data.model.GiftCategory
import com.deliriousvoid.openvkmatcha.data.model.SelectableGift
import com.deliriousvoid.openvkmatcha.data.model.Notification
import com.deliriousvoid.openvkmatcha.data.model.Photo
import com.deliriousvoid.openvkmatcha.data.model.PhotosResponse
import com.deliriousvoid.openvkmatcha.data.model.Playlist
import com.deliriousvoid.openvkmatcha.data.model.Post
import com.deliriousvoid.openvkmatcha.data.model.PendingAttachment
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.data.repository.ArtworkRepository
import com.deliriousvoid.openvkmatcha.data.repository.FeedRepository
import com.deliriousvoid.openvkmatcha.data.repository.MessagesRepository
import com.deliriousvoid.openvkmatcha.data.repository.MusicRepository
import com.deliriousvoid.openvkmatcha.data.repository.ProfileRepository
import com.deliriousvoid.openvkmatcha.util.AppEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

enum class FeedType { GLOBAL, SUBSCRIPTIONS }

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val nextFrom: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val currentUserId: Int? = null,
    val feedType: FeedType = FeedType.GLOBAL,
)

class FeedViewModel(
    private val feedRepository: FeedRepository,
    private val profileRepository: ProfileRepository,
    private val settingsViewModel: SettingsViewModel,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState(
        isLoading = true,
        feedType = settingsViewModel.feedType.value
    ))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _scrollToTop = MutableSharedFlow<Unit>()
    val scrollToTop = _scrollToTop.asSharedFlow()

    init {
        loadCurrentUserId()
        loadFeed()
        viewModelScope.launch {
            AppEvents.refreshFeed.collect {
                loadFeed(refresh = true, shouldScroll = true)
            }
        }
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { it.copy(posts = emptyList(), isLoading = true, nextFrom = null) }
                loadCurrentUserId()
                loadFeed(refresh = true)
            }
        }
    }

    fun setFeedType(type: FeedType) {
        if (_uiState.value.feedType == type) return
        _uiState.update { it.copy(feedType = type, posts = emptyList(), isLoading = true) }
        settingsViewModel.setFeedType(type)
        loadFeed(refresh = true)
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            profileRepository.loadCurrentUser().onSuccess { user ->
                _uiState.update { it.copy(currentUserId = user.id) }
            }
        }
    }

    fun loadFeed(refresh: Boolean = false, shouldScroll: Boolean = true, isManual: Boolean = false) {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = if (isManual) false else (refresh || it.posts.isEmpty()),
                    isRefreshing = isManual,
                    error = null,
                    canLoadMore = if (refresh) true else it.canLoadMore
                )
            }
            val responseTask = if (_uiState.value.feedType == FeedType.GLOBAL) {
                feedRepository.loadGlobalFeed(
                    startFrom = if (refresh) null else _uiState.value.nextFrom,
                    count = 20
                )
            } else {
                feedRepository.loadSubscriptionsFeed(
                    startFrom = if (refresh) null else _uiState.value.nextFrom,
                    count = 20
                )
            }

            responseTask.onSuccess { response ->
                _uiState.update {
                    val newList = if (refresh) {
                        response.posts
                    } else {
                        (it.posts + response.posts).distinctBy { post -> "${post.ownerId}_${post.id}" }
                    }
                    it.copy(
                        posts = newList,
                        nextFrom = response.nextFrom,
                        isLoading = false,
                        isRefreshing = false,
                        canLoadMore = !response.nextFrom.isNullOrBlank()
                    )
                }
                if (refresh && shouldScroll) {
                    _scrollToTop.emit(Unit)
                }
            }.onFailure { error ->
                if (AppEvents.isNetworkError(error.message)) {
                    AppEvents.emitNetworkError()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message,
                    )
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        if (settingsViewModel.offlineMode.value) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val responseTask = if (_uiState.value.feedType == FeedType.GLOBAL) {
                feedRepository.loadGlobalFeed(startFrom = state.nextFrom, count = 20)
            } else {
                feedRepository.loadSubscriptionsFeed(startFrom = state.nextFrom, count = 20)
            }

            responseTask.onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            posts = (it.posts + response.posts).distinctBy { post -> "${post.ownerId}_${post.id}" },
                            nextFrom = response.nextFrom,
                            isLoadingMore = false,
                            canLoadMore = !response.nextFrom.isNullOrBlank()
                        )
                    }
                }
                .onFailure { error ->
                    if (AppEvents.isNetworkError(error.message)) {
                        AppEvents.emitNetworkError()
                    }
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            feedRepository.toggleLike(post)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { item ->
                                if ((item.id == post.id) && (item.ownerId == post.ownerId)) {
                                    item.copy(
                                        isLiked = !item.isLiked,
                                        likeCount = if (item.isLiked) item.likeCount - 1 else item.likeCount + 1,
                                    )
                                } else {
                                    item
                                }
                            },
                        )
                    }
                }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            feedRepository.deletePost(post.ownerId, post.id).onSuccess {
                _uiState.update { state ->
                    state.copy(posts = state.posts.filter { it.id != post.id || it.ownerId != post.ownerId })
                }
            }
        }
    }

    fun editPost(post: Post, text: String, fromGroup: Boolean = true, isNsfw: Boolean = false) {
        viewModelScope.launch {
            feedRepository.editPost(post.ownerId, post.id, text, fromGroup, isNsfw).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.map {
                            if (it.id == post.id && it.ownerId == post.ownerId) {
                                it.copy(text = text, isNsfw = isNsfw)
                            } else it
                        }
                    )
                }
            }
        }
    }

    fun pinPost(post: Post) {
        viewModelScope.launch {
            feedRepository.pinPost(post.ownerId, post.id).onSuccess {
                loadFeed(refresh = true)
            }
        }
    }

    fun unpinPost(post: Post) {
        viewModelScope.launch {
            feedRepository.unpinPost(post.ownerId, post.id).onSuccess {
                loadFeed(refresh = true)
            }
        }
    }

    fun votePoll(post: Post, answerIds: List<Int>) {
        val poll = post.poll ?: return
        viewModelScope.launch {
            val result = if (answerIds.isEmpty()) {
                feedRepository.deletePollVote(poll.ownerId, poll.id)
            } else {
                feedRepository.addPollVote(poll.ownerId, poll.id, answerIds)
            }

            result.onSuccess {
                feedRepository.getPostById(post.ownerId, post.id).onSuccess { updatedPost ->
                    if (updatedPost != null) {
                        _uiState.update { state ->
                            state.copy(
                                posts = state.posts.map { if (it.id == post.id && it.ownerId == post.ownerId) updatedPost else it }
                            )
                        }
                    }
                }
            }
        }
    }

    fun report(type: String, ownerId: Int, itemId: Int? = null, comment: String? = null) {
        viewModelScope.launch {
            OpenVKMatchaApp.instance.reportsRepository.report(type, ownerId, itemId, comment)
                .onSuccess {
                    AppEvents.showSnackbar("Жалоба успешно отправлена")
                }
        }
    }


    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                val settingsViewModel = SettingsViewModel(app)
                return FeedViewModel(app.feedRepository, app.profileRepository, settingsViewModel) as T
            }
        }
    }
}

data class MessagesUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class MessagesViewModel(
    private val messagesRepository: MessagesRepository,
    private val settingsViewModel: SettingsViewModel,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState(isLoading = true))
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { it.copy(conversations = emptyList(), isLoading = true) }
                loadConversations()
            }
        }
    }

    fun loadConversations() {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.conversations.isEmpty(), error = null) }
            messagesRepository.loadConversations()
                .onSuccess { conversations ->
                    _uiState.update { it.copy(conversations = conversations, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }


    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return MessagesViewModel(app.messagesRepository, SettingsViewModel(app)) as T
            }
        }
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val messagesRepository: MessagesRepository,
    private val peerId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(isLoading = true))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.messages.isEmpty(), error = null) }
            messagesRepository.loadHistory(peerId)
                .onSuccess { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            messagesRepository.sendMessage(peerId, text)
                .onSuccess {
                    _uiState.update { it.copy(inputText = "", isSending = false) }
                    loadMessages()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSending = false, error = error.message) }
                }
        }
    }


    companion object {
        fun factory(peerId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(OpenVKMatchaApp.instance.messagesRepository, peerId) as T
            }
        }
    }
}

enum class MusicMode {
    Tracks, Playlists, Downloaded
}

data class MusicUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloadedTracks: List<AudioTrack> = emptyList(),
    val userProfile: UserProfile? = null,
    val mode: MusicMode = MusicMode.Tracks,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val currentUserId: Int? = null,
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
class MusicViewModel(
    private val musicRepository: MusicRepository,
    private val profileRepository: ProfileRepository,
    private val artworkRepository: ArtworkRepository,
    private val downloadRepository: com.deliriousvoid.openvkmatcha.data.repository.DownloadRepository,
    private val settingsViewModel: SettingsViewModel,
    private val initialUserId: Int? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState(isLoading = true))
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()
    val downloadedTracks = downloadRepository.downloadedTracksFlow
    private var userId: Int? = initialUserId
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            uiState.map { it.mode }
                .distinctUntilChanged()
                .collect { mode ->
                    val state = _uiState.value
                    val needsLoad = when (mode) {
                        MusicMode.Tracks -> state.tracks.isEmpty()
                        MusicMode.Playlists -> state.playlists.isEmpty()
                        MusicMode.Downloaded -> state.downloadedTracks.isEmpty()
                    }
                    if (needsLoad) {
                        loadMusic(isRefresh = true, isManual = false)
                    }
                }
        }
        viewModelScope.launch {
            AppEvents.refreshMusic.collect {
                loadMusic(isRefresh = true, isManual = false)
            }
        }
        viewModelScope.launch {
            AppEvents.searchQuery.collect { query ->
                _uiState.update { it.copy(searchQuery = query) }
                loadMusic(isRefresh = true, isManual = false)
            }
        }
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                userId = null
                _uiState.update { MusicUiState(isLoading = true) }
                loadMusic(isRefresh = true, isManual = false)
            }
        }
    }

    fun setMode(mode: MusicMode) {
        if (_uiState.value.mode != mode) {
            _uiState.update { it.copy(mode = mode, searchQuery = "") }
        }
    }

    fun loadMusic(isRefresh: Boolean = false, isManual: Boolean = false) {
        if (settingsViewModel.offlineMode.value && _uiState.value.mode != MusicMode.Downloaded) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }
        val pageSize = settingsViewModel.tracksPerPage.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (_uiState.value.searchQuery.isNotBlank() && isRefresh) {
                delay(300)
            }
            _uiState.update {
                it.copy(
                    isLoading = if (isManual) false else (isRefresh || (it.tracks.isEmpty() && it.playlists.isEmpty() && it.downloadedTracks.isEmpty())),
                    isRefreshing = isManual,
                    canLoadMore = if (isRefresh) true else it.canLoadMore,
                    error = null,
                    tracks = if (isRefresh && !isManual) emptyList() else it.tracks,
                    playlists = if (isRefresh && !isManual) emptyList() else it.playlists,
                    downloadedTracks = if (isRefresh && !isManual) emptyList() else it.downloadedTracks
                )
            }
            val mode = _uiState.value.mode
            val id = if (mode == MusicMode.Downloaded) {
                null
            } else {
                val currentId = userId ?: profileRepository.loadCurrentUser().getOrNull()?.id.also { 
                    userId = it
                    _uiState.update { state -> state.copy(currentUserId = it) }
                }
                if (currentId == null) {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "Не удалось загрузить профиль") }
                    return@launch
                }
                if (initialUserId != null && _uiState.value.userProfile == null) {
                    profileRepository.loadProfile(initialUserId.toString()).onSuccess { profile ->
                        _uiState.update { it.copy(userProfile = profile) }
                    }
                }
                currentId
            }

            val result = when (mode) {
                MusicMode.Tracks -> {
                    val query = _uiState.value.searchQuery
                    if (query.isNotBlank()) {
                        musicRepository.searchAudio(
                            query = query,
                            offset = if (isRefresh) 0 else _uiState.value.tracks.size,
                            count = pageSize.coerceAtMost(200)
                        )
                    } else {
                        musicRepository.loadMyAudio(
                            userId = id!!,
                            offset = if (isRefresh) 0 else _uiState.value.tracks.size,
                            count = pageSize
                        )
                    }
                }
                MusicMode.Playlists -> {
                    val query = _uiState.value.searchQuery
                    if (query.isNotBlank()) {
                        musicRepository.searchPlaylists(
                            query = query,
                            offset = if (isRefresh) 0 else _uiState.value.playlists.size,
                            count = pageSize
                        )
                    } else {
                        musicRepository.loadPlaylists(
                            ownerId = id!!,
                            offset = if (isRefresh) 0 else _uiState.value.playlists.size,
                            count = pageSize
                        )
                    }
                }
                MusicMode.Downloaded -> {
                    Result.success(downloadRepository.getDownloadedTracksFromFiles())
                }
            }

            result
                .onSuccess { data ->
                    _uiState.update { state ->
                        when (state.mode) {
                            MusicMode.Tracks -> {
                                val downloadRepository = OpenVKMatchaApp.instance.downloadRepository
                                val tracks = (data as List<AudioTrack>).map { downloadRepository.enrichTrack(it) }
                                val isSearch = state.searchQuery.isNotBlank()
                                val pagedTracks = if (isRefresh) {
                                    tracks
                                } else {
                                    (state.tracks + tracks).distinctBy { it.stableId }
                                }
                                val effectivePageSize = if (isSearch) pageSize.coerceAtMost(200) else pageSize
                                state.copy(
                                    tracks = if (isSearch) {
                                        pagedTracks
                                    } else {
                                        pagedTracks.map { it.copy(isAdded = true) }
                                    },
                                    isLoading = false,
                                    isRefreshing = false,
                                    canLoadMore = tracks.size >= effectivePageSize
                                )
                            }
                            MusicMode.Playlists -> {
                                val playlists = data as List<Playlist>
                                val newList = if (isRefresh) {
                                    playlists
                                } else {
                                    (state.playlists + playlists).distinctBy { "${it.ownerId}_${it.id}" }
                                }
                                state.copy(
                                    playlists = newList,
                                    isLoading = false,
                                    isRefreshing = false,
                                    canLoadMore = playlists.size >= pageSize
                                )
                            }
                            MusicMode.Downloaded -> {
                                state.copy(downloadedTracks = data as List<AudioTrack>, isLoading = false, isRefreshing = false, canLoadMore = false)
                            }
                        }
                    }
                    if (data is List<*> && data.isNotEmpty() && data[0] is AudioTrack) {
                        @Suppress("UNCHECKED_CAST")
                        loadArtworks(data as List<AudioTrack>)
                    }
                }
                .onFailure { error ->
                    if (AppEvents.isNetworkError(error.message)) {
                        AppEvents.emitNetworkError()
                    }
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = error.message) }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        if (settingsViewModel.offlineMode.value) return
        val pageSize = settingsViewModel.tracksPerPage.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val id = userId ?: profileRepository.loadCurrentUser().getOrNull()?.id.also { userId = it }
            if (id == null) {
                _uiState.update { it.copy(isLoadingMore = false) }
                return@launch
            }

            val result = when (state.mode) {
                MusicMode.Tracks -> {
                    if (state.searchQuery.isNotBlank()) {
                        musicRepository.searchAudio(
                            query = state.searchQuery,
                            offset = state.tracks.size,
                            count = pageSize.coerceAtMost(200)
                        )
                    } else {
                        musicRepository.loadMyAudio(id, offset = state.tracks.size, count = pageSize)
                    }
                }
                MusicMode.Playlists -> {
                    if (state.searchQuery.isNotBlank()) {
                        musicRepository.searchPlaylists(
                            query = state.searchQuery,
                            offset = state.playlists.size,
                            count = pageSize
                        )
                    } else {
                        musicRepository.loadPlaylists(id, offset = state.playlists.size, count = pageSize)
                    }
                }
                MusicMode.Downloaded -> {
                    Result.success(emptyList<AudioTrack>())
                }
            }

            result.onSuccess { data ->
                _uiState.update { s ->
                    when (s.mode) {
                        MusicMode.Tracks -> {
                            val tracks = data as List<AudioTrack>
                            val isSearch = s.searchQuery.isNotBlank()
                            val effectivePageSize = if (isSearch) pageSize.coerceAtMost(200) else pageSize
                            val newTracks = if (isSearch) {
                                tracks
                            } else {
                                tracks.map { it.copy(isAdded = true) }
                            }
                            s.copy(
                                tracks = (s.tracks + newTracks).distinctBy { it.stableId },
                                isLoadingMore = false,
                                canLoadMore = tracks.size >= effectivePageSize
                            )
                        }
                        MusicMode.Playlists -> {
                            val playlists = data as List<Playlist>
                            s.copy(
                                playlists = (s.playlists + playlists).distinctBy { "${it.ownerId}_${it.id}" },
                                isLoadingMore = false,
                                canLoadMore = playlists.size >= pageSize
                            )
                        }
                        MusicMode.Downloaded -> {
                            s.copy(isLoadingMore = false, canLoadMore = false)
                        }
                    }
                }
                if (data is List<*> && data.isNotEmpty() && data[0] is AudioTrack) {
                    @Suppress("UNCHECKED_CAST")
                    loadArtworks(data as List<AudioTrack>)
                }
            }.onFailure { error ->
                if (AppEvents.isNetworkError(error.message)) {
                    AppEvents.emitNetworkError()
                }
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private fun loadArtworks(tracks: List<AudioTrack>) {
        viewModelScope.launch {
            tracks.forEach { track ->
                if (track.artworkUrl == null) {
                    artworkRepository.getArtworkUrl(track.artist, track.title)
                        .onSuccess { url ->
                            if (url != null) {
                                _uiState.update { state ->
                                    state.copy(
                                        tracks = state.tracks.map {
                                            if (it.id == track.id && it.ownerId == track.ownerId) {
                                                it.copy(artworkUrl = url)
                                            } else it
                                        }
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    fun toggleTrackAdded(track: AudioTrack) {
        viewModelScope.launch {
            val result = if (track.isAdded) {
                musicRepository.deleteAudio(track.id, track.ownerId)
            } else {
                musicRepository.addAudio(track.id, track.ownerId)
            }

            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        tracks = state.tracks.map {
                            if (it.id == track.id && it.ownerId == track.ownerId) {
                                it.copy(isAdded = !track.isAdded)
                            } else it
                        }
                    )
                }
            }
        }
    }

    fun downloadTrack(track: AudioTrack) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
            loadMusic()
        }
    }

    fun addToQueue(track: AudioTrack) {
        OpenVKMatchaApp.instance.playerManager.addToQueue(track)
    }

    fun playNext(track: AudioTrack) {
        OpenVKMatchaApp.instance.playerManager.playNext(track)
    }

    fun createPlaylist(title: String, description: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(title, description).onSuccess {
                loadMusic(isRefresh = true)
            }
        }
    }

    fun isDownloaded(trackId: Int, ownerId: Int) = downloadRepository.isDownloaded(trackId, ownerId)

    companion object {
        fun factory(userId: Int? = null): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                val settingsViewModel = SettingsViewModel(app)
                return MusicViewModel(app.musicRepository, app.profileRepository, app.artworkRepository, app.downloadRepository, settingsViewModel, userId) as T
            }
        }
    }
}

data class ProfileUiState(
    val profile: UserProfile? = null,
    val wallPosts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val currentUserId: Int? = null,
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val feedRepository: FeedRepository,
    private val settingsViewModel: SettingsViewModel,
    private val userIdOrName: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _scrollToTop = MutableSharedFlow<Unit>()
    val scrollToTop = _scrollToTop.asSharedFlow()

    init {
        loadCurrentUserId()
        loadProfile()
        viewModelScope.launch {
            AppEvents.refreshProfile.collect { targetId ->
                val currentProfileId = _uiState.value.profile?.id
                if (targetId == null || targetId == currentProfileId) {
                    loadProfile(refresh = true, isManual = true)
                }
                _scrollToTop.emit(Unit)
            }
        }
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { ProfileUiState(isLoading = true) }
                loadCurrentUserId()
                loadProfile(refresh = true)
            }
        }
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            profileRepository.loadCurrentUser().onSuccess { user ->
                _uiState.update { it.copy(currentUserId = user.id) }
            }
        }
    }

    fun loadProfile(refresh: Boolean = false, isManual: Boolean = false) {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = (!refresh && it.profile == null),
                    isRefreshing = isManual,
                    error = null,
                    canLoadMore = if (refresh) true else it.canLoadMore
                )
            }
            val profileResult = if (userIdOrName == null) {
                profileRepository.loadCurrentUser()
            } else {
                profileRepository.loadProfile(userIdOrName)
            }

            profileResult
                .onSuccess { profile ->
                    _uiState.update { it.copy(profile = profile) }
                    
                    if (userIdOrName == null && profile.bdate == null) {
                        viewModelScope.launch {
                            profileRepository.getProfileInfo().onSuccess { info ->
                                if (info.bdate.isNotBlank() && !isEmptyBirthday(info.bdate)) {
                                    _uiState.update { state ->
                                        if (state.profile?.id == profile.id) {
                                            state.copy(profile = state.profile.copy(bdate = info.bdate))
                                        } else state
                                    }
                                }
                            }
                        }
                    }

                    profileRepository.loadUserWall(
                        userId = profile.id,
                        offset = if (refresh) 0 else _uiState.value.wallPosts.size,
                        count = 20
                    ).onSuccess { posts ->
                        _uiState.update {
                            val newList = if (refresh) posts else it.wallPosts + posts
                            it.copy(
                                wallPosts = newList,
                                isLoading = false,
                                isRefreshing = false,
                                canLoadMore = posts.size >= 20
                            )
                        }
                    }.onFailure { error ->
                        if (AppEvents.isNetworkError(error.message)) {
                            AppEvents.emitNetworkError()
                        }
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = error.message) }
                    }
                }
                .onFailure { error ->
                    if (AppEvents.isNetworkError(error.message)) {
                        AppEvents.emitNetworkError()
                    }
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = error.message) }
                }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            feedRepository.toggleLike(post)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            wallPosts = state.wallPosts.map { item ->
                                if ((item.id == post.id) && (item.ownerId == post.ownerId)) {
                                    item.copy(
                                        isLiked = !item.isLiked,
                                        likeCount = if (item.isLiked) item.likeCount - 1 else item.likeCount + 1,
                                    )
                                } else {
                                    item
                                }
                            },
                        )
                    }
                }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            feedRepository.deletePost(post.ownerId, post.id).onSuccess {
                _uiState.update { state ->
                    state.copy(wallPosts = state.wallPosts.filter { it.id != post.id || it.ownerId != post.ownerId })
                }
            }
        }
    }

    fun editPost(post: Post, text: String, fromGroup: Boolean = true, isNsfw: Boolean = false) {
        viewModelScope.launch {
            feedRepository.editPost(post.ownerId, post.id, text, fromGroup, isNsfw).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        wallPosts = state.wallPosts.map {
                            if (it.id == post.id && it.ownerId == post.ownerId) {
                                it.copy(text = text, isNsfw = isNsfw)
                            } else it
                        }
                    )
                }
            }
        }
    }

    fun pinPost(post: Post) {
        viewModelScope.launch {
            feedRepository.pinPost(post.ownerId, post.id).onSuccess {
                loadProfile(refresh = true)
            }
        }
    }

    fun unpinPost(post: Post) {
        viewModelScope.launch {
            feedRepository.unpinPost(post.ownerId, post.id).onSuccess {
                loadProfile(refresh = true)
            }
        }
    }

    fun votePoll(post: Post, answerIds: List<Int>) {
        val poll = post.poll ?: return
        viewModelScope.launch {
            val result = if (answerIds.isEmpty()) {
                feedRepository.deletePollVote(poll.ownerId, poll.id)
            } else {
                feedRepository.addPollVote(poll.ownerId, poll.id, answerIds)
            }

            result.onSuccess {
                feedRepository.getPostById(post.ownerId, post.id).onSuccess { updatedPost ->
                    if (updatedPost != null) {
                        _uiState.update { state ->
                            state.copy(
                                wallPosts = state.wallPosts.map { if (it.id == post.id && it.ownerId == post.ownerId) updatedPost else it }
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore || state.profile == null) return
        if (settingsViewModel.offlineMode.value) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val currentOffset = state.wallPosts.size
            profileRepository.loadUserWall(userId = state.profile.id, offset = currentOffset, count = 20)
                .onSuccess { posts ->
                    _uiState.update {
                        it.copy(
                            wallPosts = it.wallPosts + posts,
                            isLoadingMore = false,
                            canLoadMore = posts.size >= 20
                        )
                    }
                }
                .onFailure { error ->
                    if (AppEvents.isNetworkError(error.message)) {
                        AppEvents.emitNetworkError()
                    }
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun toggleFriendship() {
        val currentProfile = _uiState.value.profile ?: return
        if (currentProfile.isGroup) return

        viewModelScope.launch {
            val result = when (currentProfile.friendStatus) {
                0, null -> profileRepository.addFriend(currentProfile.id)
                1 -> profileRepository.deleteFriend(currentProfile.id)
                2 -> profileRepository.addFriend(currentProfile.id)
                3 -> profileRepository.deleteFriend(currentProfile.id)
                else -> return@launch
            }

            result.onSuccess {
                loadProfile(refresh = true)
            }.onFailure { error ->
                if (error is com.deliriousvoid.openvkmatcha.data.api.ApiException && error.errorCode == 15) {
                    loadProfile(refresh = true)
                } else {
                    AppEvents.showSnackbar(error.message ?: "Ошибка при выполнении действия")
                    loadProfile(refresh = true)
                }
            }
        }
    }

    fun toggleGroupMembership() {
        val currentProfile = _uiState.value.profile ?: return
        if (!currentProfile.isGroup) return

        viewModelScope.launch {
            val result = if (currentProfile.isMember) {
                profileRepository.leaveGroup(currentProfile.id)
            } else {
                profileRepository.joinGroup(currentProfile.id)
            }

            result.onSuccess {
                loadProfile(refresh = true)
            }.onFailure { error ->
                AppEvents.showSnackbar(error.message ?: "Ошибка при выполнении действия")
                loadProfile(refresh = true)
            }
        }
    }

    fun report(type: String, ownerId: Int, itemId: Int? = null, comment: String? = null) {
        viewModelScope.launch {
            OpenVKMatchaApp.instance.reportsRepository.report(type, ownerId, itemId, comment)
                .onSuccess {
                    AppEvents.showSnackbar("Жалоба успешно отправлена")
                }
        }
    }

    fun toggleIgnore(profile: UserProfile) {
        viewModelScope.launch {
            val result = if (profile.isIgnored) {
                feedRepository.unignoreSource(profile.id)
            } else {
                feedRepository.ignoreSource(profile.id)
            }
            result.onSuccess {
                _uiState.update { state ->
                    if (state.profile?.id == profile.id) {
                        state.copy(profile = state.profile.copy(isIgnored = !profile.isIgnored))
                    } else state
                }
            }
        }
    }

    fun toggleBlacklist(profile: UserProfile) {
        viewModelScope.launch {
            val result = if (profile.blacklistedByMe) {
                profileRepository.unbanUser(profile.id)
            } else {
                profileRepository.banUser(profile.id)
            }
            result.onSuccess {
                _uiState.update { state ->
                    if (state.profile?.id == profile.id) {
                        state.copy(profile = state.profile?.copy(blacklistedByMe = !profile.blacklistedByMe))
                    } else state
                }
            }
        }
    }

    private fun isEmptyBirthday(bdate: String): Boolean {
        val parts = bdate.split(".").mapNotNull { it.trim().toIntOrNull() }
        return parts.size == 3 && parts[0] == 1 && parts[1] == 1 && parts[2] == 1970
    }

    companion object {
        fun factory(userIdOrName: String? = null): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return ProfileViewModel(app.profileRepository, app.feedRepository, SettingsViewModel(app), userIdOrName) as T
            }
        }
    }
}

data class PlaylistDetailsUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val playlist: Playlist? = null,
    val isBookmarked: Boolean = false,
    val isOwner: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val playlistDeleted: Boolean = false,
)

class PlaylistDetailsViewModel(
    private val musicRepository: MusicRepository,
    private val profileRepository: ProfileRepository,
    private val artworkRepository: ArtworkRepository,
    private val settingsViewModel: SettingsViewModel,
    private val ownerId: Int,
    private val playlistId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailsUiState(isLoading = true))
    val uiState: StateFlow<PlaylistDetailsUiState> = _uiState.asStateFlow()

    init {
        loadTracks()
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { it.copy(tracks = emptyList(), isLoading = true, playlist = null) }
                loadTracks(isRefresh = true)
            }
        }
    }

    fun loadTracks(isRefresh: Boolean = false) {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        val pageSize = settingsViewModel.tracksPerPage.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh && it.tracks.isEmpty(),
                    canLoadMore = if (isRefresh) true else it.canLoadMore,
                    error = null
                )
            }
            musicRepository.loadPlaylistTracks(
                ownerId = ownerId,
                playlistId = playlistId,
                offset = if (isRefresh) 0 else _uiState.value.tracks.size,
                count = pageSize
            ).onSuccess { tracks ->
                val enrichedTracks = tracks.map { OpenVKMatchaApp.instance.downloadRepository.enrichTrack(it) }
                if (isRefresh || _uiState.value.playlist == null) {
                    musicRepository.loadPlaylists(ownerId).onSuccess { playlists ->
                        val currentPlaylist = playlists.find { it.id == playlistId }
                        _uiState.update { it.copy(playlist = currentPlaylist) }
                    }
                    
                    // Check if bookmarked in my collection
                    viewModelScope.launch {
                        val currentUser = profileRepository.loadCurrentUser().getOrNull()
                        if (currentUser != null) {
                            _uiState.update { it.copy(isOwner = ownerId == currentUser.id) }
                            musicRepository.loadPlaylists(currentUser.id).onSuccess { myPlaylists ->
                                 val found = myPlaylists.any { it.id == playlistId }
                                 _uiState.update { it.copy(isBookmarked = found) }
                            }
                        }
                    }
                }
                _uiState.update {
                    val newList = if (isRefresh) {
                        enrichedTracks
                    } else {
                        (it.tracks + enrichedTracks).distinctBy { it.stableId }
                    }
                    it.copy(
                        tracks = newList,
                        isLoading = false,
                        canLoadMore = tracks.size >= pageSize
                    )
                }
                loadArtworks(enrichedTracks)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun toggleBookmark() {
        val newState = !_uiState.value.isBookmarked
        viewModelScope.launch {
            val result = if (newState) {
                musicRepository.bookmarkPlaylist(playlistId)
            } else {
                musicRepository.unbookmarkPlaylist(playlistId)
            }
            result.onSuccess {
                _uiState.update { it.copy(isBookmarked = newState) }
            }
        }
    }

    fun editPlaylist(title: String, description: String) {
        viewModelScope.launch {
            musicRepository.editPlaylist(playlistId, title, description)
                .onSuccess {
                    loadTracks(isRefresh = true)
                }
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
                .onSuccess {
                    _uiState.update { it.copy(playlistDeleted = true) }
                }
        }
    }

    fun addTrack(track: AudioTrack) {
        viewModelScope.launch {
            musicRepository.addAudioToPlaylist(playlistId, track.stableId)
                .onSuccess {
                    loadTracks(isRefresh = true)
                }
        }
    }

    fun removeTrack(track: AudioTrack) {
        viewModelScope.launch {
            musicRepository.removeAudioFromPlaylist(playlistId, track)
                .onSuccess {
                    loadTracks(isRefresh = true)
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        if (settingsViewModel.offlineMode.value) return
        val pageSize = settingsViewModel.tracksPerPage.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val currentOffset = state.tracks.size
            musicRepository.loadPlaylistTracks(ownerId, playlistId, offset = currentOffset, count = pageSize)
                .onSuccess { tracks ->
                    _uiState.update {
                        it.copy(
                            tracks = (it.tracks + tracks).distinctBy { it.stableId },
                            isLoadingMore = false,
                            canLoadMore = tracks.size >= pageSize
                        )
                    }
                    loadArtworks(tracks)
                }
                .onFailure { error ->
                    if (AppEvents.isNetworkError(error.message)) {
                        AppEvents.emitNetworkError()
                    }
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun toggleTrackAdded(track: AudioTrack) {
        viewModelScope.launch {
            val result = if (track.isAdded) {
                musicRepository.deleteAudio(track.id, track.ownerId)
            } else {
                musicRepository.addAudio(track.id, track.ownerId)
            }

            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        tracks = state.tracks.map {
                            if (it.id == track.id && it.ownerId == track.ownerId) {
                                it.copy(isAdded = !track.isAdded)
                            } else it
                        }
                    )
                }
            }
        }
    }

    private fun loadArtworks(tracks: List<AudioTrack>) {
        viewModelScope.launch {
            tracks.forEach { track ->
                if (track.artworkUrl == null) {
                    artworkRepository.getArtworkUrl(track.artist, track.title)
                        .onSuccess { url ->
                            if (url != null) {
                                _uiState.update { state ->
                                    state.copy(
                                        tracks = state.tracks.map {
                                            if (it.id == track.id && it.ownerId == track.ownerId) {
                                                it.copy(artworkUrl = url)
                                            } else it
                                        }
                                    )
                                }
                            }
                        }
                }
            }
        }
    }


    companion object {
        fun factory(ownerId: Int, playlistId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                val settingsViewModel = SettingsViewModel(app)
                return PlaylistDetailsViewModel(
                    app.musicRepository,
                    app.profileRepository,
                    app.artworkRepository,
                    settingsViewModel,
                    ownerId,
                    playlistId
                ) as T
            }
        }
    }
}

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isArchive: Boolean = false,
    val hasMore: Boolean = true,
    val nextFrom: String? = null,
    val error: String? = null,
)

class NotificationsViewModel(
    private val repository: com.deliriousvoid.openvkmatcha.data.repository.NotificationsRepository,
    private val commentsRepository: com.deliriousvoid.openvkmatcha.data.repository.CommentsRepository,
    private val settingsViewModel: SettingsViewModel,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadingJob: Job? = null
    private var pollingJob: Job? = null

    init {
        loadNotifications(refresh = true)
        refreshUnreadCount()
        viewModelScope.launch {
            AppEvents.refreshNotifications.collect {
                refreshUnreadCount()
                if (!_uiState.value.isArchive) {
                    delay(1000) // Small delay to allow the server to index the notification
                    loadNotifications(refresh = true)
                }
            }
        }
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { NotificationsUiState(isLoading = true) }
                loadNotifications(refresh = true)
                refreshUnreadCount()
            }
        }
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(30_000) // Poll every 30 seconds as fallback
                if (settingsViewModel.offlineMode.value) continue
                refreshUnreadCount()
            }
        }
    }

    fun setArchive(archive: Boolean) {
        if (_uiState.value.isArchive == archive && _uiState.value.notifications.isNotEmpty()) return
        _uiState.update { 
            it.copy(
                isArchive = archive, 
                notifications = emptyList(), 
                isLoading = true,
                nextFrom = null,
                hasMore = true
            ) 
        }
        loadNotifications(refresh = true)
    }

    fun loadNotifications(refresh: Boolean = false, isManual: Boolean = false) {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }
        
        if (!refresh && (!_uiState.value.hasMore || _uiState.value.isLoading)) return

        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            val startFrom = if (refresh) null else _uiState.value.nextFrom
            
            _uiState.update { state ->
                state.copy(
                    isLoading = refresh && state.notifications.isEmpty(),
                    isRefreshing = isManual
                )
            }

            repository.loadNotifications(
                startFrom = startFrom, 
                archived = _uiState.value.isArchive
            ).onSuccess { response ->
                _uiState.update { state ->
                    val newItems = if (refresh) response.items else state.notifications + response.items
                    
                    state.copy(
                        notifications = newItems,
                        nextFrom = response.nextFrom,
                        hasMore = response.nextFrom != null && response.items.isNotEmpty(),
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }
                if (refresh && !_uiState.value.isArchive) refreshUnreadCount()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = error.message) }
            }
        }
    }

    fun refreshUnreadCount() {
        if (settingsViewModel.offlineMode.value) return
        viewModelScope.launch {
            repository.getUnreadCount().onSuccess { count ->
                _uiState.update { it.copy(unreadCount = count) }
                android.util.Log.d("NotificationsViewModel", "Unread count updated: $count")
            }.onFailure {
                android.util.Log.e("NotificationsViewModel", "Failed to get unread count: ${it.message}")
            }
        }
    }

    fun markAsRead() {
        viewModelScope.launch {
            repository.markAsViewed()
            refreshUnreadCount()
            _uiState.update { state -> 
                state.copy(
                    notifications = state.notifications.map { it.copy(isRead = true) }
                )
            }
        }
    }

    fun loadCommentDetails(notification: Notification) {
        if (notification.isDetailsLoaded || notification.itemId == 0 || notification.ownerId == 0) return
        val typesWithComments = listOf("comment_post", "comment_photo", "mention", "reply_comment", "reply_post")
        if (notification.type !in typesWithComments) return

        viewModelScope.launch {
            commentsRepository.getComments(notification.ownerId, notification.itemId).onSuccess { response ->
                val matchingComment = response.items.minByOrNull { kotlin.math.abs(it.date - notification.date) }
                    ?.takeIf { kotlin.math.abs(it.date - notification.date) <= 5 }

                if (matchingComment != null) {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { n ->
                                if (n.id == notification.id) {
                                    n.copy(
                                        parentText = matchingComment.text,
                                        isDetailsLoaded = true
                                    )
                                } else n
                            }
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { n ->
                                if (n.id == notification.id) n.copy(isDetailsLoaded = true) else n
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
    }



    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return NotificationsViewModel(
                    app.notificationsRepository,
                    app.commentsRepository,
                    SettingsViewModel(app)
                ) as T
            }
        }
    }
}

data class CreatePostUiState(
    val inputText: String = "",
    val pendingAttachments: List<PendingAttachment> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val isGroup: Boolean = false,
    val isAdmin: Boolean = false,
    val fromGroup: Boolean = false,
    val signed: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val pollQuestion: String? = null,
    val pollAnswers: List<String> = emptyList(),
    val pollAnonymous: Boolean = false,
    val pollMultiple: Boolean = false,
    val pollDisableUnvote: Boolean = false,
    val pollEndDate: Long? = null,
    val copyright: String? = null,
    val isNsfw: Boolean = false,
    val isDeveloperMode: Boolean = false
)

class CreatePostViewModel(
    private val feedRepository: FeedRepository,
    private val profileRepository: ProfileRepository,
    private val attachmentsRepository: com.deliriousvoid.openvkmatcha.data.repository.AttachmentsRepository,
    private val ownerId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState(
        isGroup = ownerId < 0,
        isDeveloperMode = OpenVKMatchaApp.instance.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("experimental_features", false)
    ))
    val uiState = _uiState.asStateFlow()

    init {
        if (ownerId < 0) {
            checkAdmin()
        }
    }

    private fun checkAdmin() {
        viewModelScope.launch {
            profileRepository.loadProfile(ownerId.toString()).onSuccess { profile ->
                _uiState.update { it.copy(isAdmin = profile.isAdmin, fromGroup = profile.isAdmin) }
            }
        }
    }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }
    
    fun setFromGroup(value: Boolean) = _uiState.update { it.copy(fromGroup = value) }
    
    fun setSigned(value: Boolean) = _uiState.update { it.copy(signed = value) }

    fun setLocation(lat: Double?, lon: Double?, name: String? = null) = 
        _uiState.update { it.copy(latitude = lat, longitude = lon, locationName = name) }

    fun updateLocationName(name: String?) = _uiState.update { it.copy(locationName = name) }

    fun updatePoll(
        question: String?,
        answers: List<String>,
        anonymous: Boolean,
        multiple: Boolean,
        disableUnvote: Boolean = false,
        endDate: Long? = null
    ) = _uiState.update { 
        it.copy(
            pollQuestion = question, 
            pollAnswers = answers, 
            pollAnonymous = anonymous, 
            pollMultiple = multiple,
            pollDisableUnvote = disableUnvote,
            pollEndDate = endDate
        ) 
    }

    fun removePoll() = _uiState.update { it.copy(pollQuestion = null, pollAnswers = emptyList()) }

    fun updateCopyright(url: String?) = _uiState.update { it.copy(copyright = url) }

    fun setNsfw(value: Boolean) = _uiState.update { it.copy(isNsfw = value) }

    fun addAttachment(uri: android.net.Uri, type: AttachmentType, name: String = "", size: Long = 0) {
        if (_uiState.value.pendingAttachments.size >= 10) return
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + PendingAttachment(uri, type, name, size)) }
    }

    fun addExistingAttachment(attachmentString: String, type: AttachmentType, name: String) {
        if (_uiState.value.pendingAttachments.size >= 10) return
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments + PendingAttachment(
                type = type,
                name = name,
                isExisting = true,
                attachmentString = attachmentString
            ))
        }
    }

    fun removeAttachment(attachment: PendingAttachment) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments - attachment) }
    }

    fun post() {
        val state = _uiState.value
        if (state.inputText.trim().isEmpty() && state.pendingAttachments.isEmpty() && state.pollQuestion == null) return
        if (state.isSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            
            val attachmentStrings = mutableListOf<String>()
            var uploadError: String? = null
            
            for (pending in state.pendingAttachments) {
                if (pending.isExisting) {
                    attachmentStrings.add(pending.attachmentString!!)
                    continue
                }
                attachmentsRepository.uploadAttachment(pending).fold(
                    onSuccess = { attachmentStrings.add(it) },
                    onFailure = { 
                        uploadError = "Ошибка загрузки вложения: ${it.message}"
                        return@fold
                    }
                )
                if (uploadError != null) break
            }

            if (uploadError != null) {
                _uiState.update { it.copy(isSending = false, error = uploadError) }
                return@launch
            }

            if (state.pollQuestion != null) {
                val pollResult = feedRepository.createPoll(
                    question = state.pollQuestion,
                    answers = state.pollAnswers,
                    anonymous = state.pollAnonymous,
                    multiple = state.pollMultiple,
                    disableUnvote = state.pollDisableUnvote,
                    endDate = state.pollEndDate
                )
                
                if (pollResult.isFailure) {
                    val pollError = pollResult.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    _uiState.update { it.copy(isSending = false, error = "Ошибка создания опроса: $pollError") }
                    return@launch
                }
                
                val poll = pollResult.getOrThrow()
                // According to OpenVK docs: attachments=poll{returned_poll_id}
                attachmentStrings.add("poll${poll.id}")
            }

            feedRepository.createPost(
                ownerId = ownerId,
                message = state.inputText,
                attachments = if (attachmentStrings.isNotEmpty()) attachmentStrings.joinToString(",") else null,
                fromGroup = state.fromGroup,
                signed = state.signed,
                lat = state.latitude,
                long = state.longitude,
                placeName = state.locationName,
                copyright = state.copyright,
                isNsfw = state.isNsfw
            ).onSuccess {
                _uiState.update { it.copy(isSending = false, success = true) }
                AppEvents.emitRefreshProfile(ownerId)
            }.onFailure { error ->
                _uiState.update { it.copy(isSending = false, error = error.message) }
            }
        }
    }

    companion object {
        fun factory(ownerId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return CreatePostViewModel(
                    app.feedRepository,
                    app.profileRepository,
                    app.attachmentsRepository,
                    ownerId
                ) as T
            }
        }
    }
}

data class CommentsUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val threadedComments: List<CommentThreadItem<Comment>> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val inputText: String = "",
    val pendingAttachments: List<PendingAttachment> = emptyList(),
    val editingComment: Comment? = null,
    val replyingTo: Comment? = null,
    val currentUserId: Int? = null,
    val isDeveloperMode: Boolean = false,
    val isAdmin: Boolean = false,
    val fromGroup: Boolean = false,
)

data class RepostUiState(
    val adminGroups: List<UserProfile> = emptyList(),
    val isLoadingGroups: Boolean = false,
    val isReposting: Boolean = false,
    val pendingAttachments: List<PendingAttachment> = emptyList(),
    val error: String? = null,
)

class RepostViewModel(
    private val feedRepository: FeedRepository,
    private val profileRepository: ProfileRepository,
    private val attachmentsRepository: com.deliriousvoid.openvkmatcha.data.repository.AttachmentsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepostUiState())
    val uiState = _uiState.asStateFlow()

    fun loadAdminGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGroups = true) }
            profileRepository.getAdminGroups().onSuccess { groups ->
                _uiState.update { it.copy(adminGroups = groups, isLoadingGroups = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoadingGroups = false, error = error.message) }
            }
        }
    }

    fun addAttachment(uri: android.net.Uri, type: AttachmentType, name: String = "", size: Long = 0) {
        if (_uiState.value.pendingAttachments.size >= 10) return
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + PendingAttachment(uri, type, name, size)) }
    }

    fun addExistingAttachment(attachmentString: String, type: AttachmentType, name: String) {
        if (_uiState.value.pendingAttachments.size >= 10) return
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments + PendingAttachment(
                type = type,
                name = name,
                isExisting = true,
                attachmentString = attachmentString
            ))
        }
    }

    fun removeAttachment(attachment: PendingAttachment) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments - attachment) }
    }

    fun repost(post: Post, message: String, groupId: Int? = null, onSuccess: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isReposting = true, error = null) }
            
            val attachmentStrings = mutableListOf<String>()
            var uploadError: String? = null
            
            for (pending in state.pendingAttachments) {
                if (pending.isExisting) {
                    attachmentStrings.add(pending.attachmentString!!)
                    continue
                }
                attachmentsRepository.uploadAttachment(pending).fold(
                    onSuccess = { attachmentStrings.add(it) },
                    onFailure = { 
                        uploadError = "Ошибка загрузки вложения: ${it.message}"
                        return@fold
                    }
                )
                if (uploadError != null) break
            }

            if (uploadError != null) {
                _uiState.update { it.copy(isReposting = false, error = uploadError) }
                return@launch
            }

            val attachmentsParam = if (attachmentStrings.isNotEmpty()) attachmentStrings.joinToString(",") else null

            feedRepository.repost(post.ownerId, post.id, message, groupId, attachmentsParam).onSuccess {
                _uiState.update { it.copy(isReposting = false, pendingAttachments = emptyList()) }
                onSuccess()
            }.onFailure { error ->
                _uiState.update { it.copy(isReposting = false, error = error.message) }
            }
        }
    }


    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return RepostViewModel(app.feedRepository, app.profileRepository, app.attachmentsRepository) as T
            }
        }
    }
}

class CommentsViewModel(
    private val repository: com.deliriousvoid.openvkmatcha.data.repository.CommentsRepository,
    private val feedRepository: FeedRepository,
    private val profileRepository: ProfileRepository,
    private val attachmentsRepository: com.deliriousvoid.openvkmatcha.data.repository.AttachmentsRepository,
    private val settingsViewModel: SettingsViewModel,
    private val ownerId: Int,
    private val postId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState(
        isLoading = true,
        isDeveloperMode = OpenVKMatchaApp.instance.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("experimental_features", false)
    ))
    val uiState = _uiState.asStateFlow()

    init {
        loadCurrentUserId()
        loadPost()
        loadComments()
        checkAdminStatus()
    }

    private fun checkAdminStatus() {
        if (ownerId >= 0) return
        viewModelScope.launch {
            profileRepository.loadProfile(ownerId.toString()).onSuccess { profile ->
                _uiState.update { it.copy(isAdmin = profile.isAdmin, fromGroup = profile.isAdmin) }
            }
        }
    }

    fun setFromGroup(enabled: Boolean) = _uiState.update { it.copy(fromGroup = enabled) }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            profileRepository.loadCurrentUser().onSuccess { user ->
                _uiState.update { it.copy(currentUserId = user.id) }
            }
        }
    }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun addAttachment(uri: android.net.Uri, type: AttachmentType, name: String = "", size: Long = 0) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + PendingAttachment(uri, type, name, size)) }
    }

    fun addExistingAttachment(attachmentString: String, type: AttachmentType, name: String) {
        _uiState.update {
            it.copy(pendingAttachments = it.pendingAttachments + PendingAttachment(
                type = type,
                name = name,
                isExisting = true,
                attachmentString = attachmentString
            ))
        }
    }

    fun removeAttachment(attachment: PendingAttachment) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments - attachment) }
    }

    fun replyTo(comment: Comment) {
        val idStr = if (comment.fromId < 0) "club${comment.fromId.absoluteValue}" else "id${comment.fromId}"
        val mention = "[$idStr|${comment.authorName}], "
        _uiState.update { 
            it.copy(
                inputText = it.inputText + mention,
                replyingTo = comment,
                editingComment = null
            ) 
        }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingTo = null) }
    }

    fun startEditing(comment: Comment) {
        _uiState.update { 
            it.copy(
                editingComment = comment, 
                inputText = comment.text,
                replyingTo = null
            ) 
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(editingComment = null, inputText = "") }
    }

    private fun loadPost() {
        viewModelScope.launch {
            feedRepository.getPostById(ownerId, postId).onSuccess { post ->
                _uiState.update { it.copy(post = post) }
            }
        }
    }

    private fun buildThreadedComments(comments: List<Comment>): List<CommentThreadItem<Comment>> {
        val roots = comments.filter { it.replyToComment == null }
        val replies = comments.filter { it.replyToComment != null }
        
        val result = mutableListOf<CommentThreadItem<Comment>>()
        val processedIds = mutableSetOf<Int>()

        // 1. Add roots and their direct/indirect replies
        for (root in roots) {
            val treeReplies = findDescendants(root.id, replies, comments).sortedBy { it.date }
            result.add(CommentThreadItem(
                item = root,
                level = 0,
                isLastInThread = false,
                hasNextInThread = treeReplies.isNotEmpty()
            ))
            processedIds.add(root.id)
            
            for (i in treeReplies.indices) {
                val reply = treeReplies[i]
                if (!processedIds.contains(reply.id)) {
                    result.add(CommentThreadItem(
                        item = reply,
                        level = 1,
                        isLastInThread = i == treeReplies.size - 1,
                        hasNextInThread = i < treeReplies.size - 1
                    ))
                    processedIds.add(reply.id)
                }
            }
        }

        // 2. Add remaining replies that might have missing parents in the current list
        val remaining = replies.filter { !processedIds.contains(it.id) }.sortedBy { it.date }
        for (rem in remaining) {
            result.add(CommentThreadItem(
                item = rem,
                level = 0,
                isLastInThread = false,
                hasNextInThread = false
            ))
        }

        return result
    }

    private fun findDescendants(parentId: Int, allReplies: List<Comment>, allComments: List<Comment>): List<Comment> {
        val descendants = mutableListOf<Comment>()
        val queue = mutableListOf(parentId)
        val visited = mutableSetOf(parentId)

        var i = 0
        while (i < queue.size) {
            val currentId = queue[i++]
            val children = allReplies.filter { it.replyToComment == currentId }
            for (child in children) {
                if (!visited.contains(child.id)) {
                    descendants.add(child)
                    visited.add(child.id)
                    queue.add(child.id)
                }
            }
        }
        return descendants
    }

    private fun updateUiStateWithComments(update: (CommentsUiState) -> CommentsUiState) {
        _uiState.update { state ->
            val newState = update(state)
            if (newState.comments != state.comments) {
                newState.copy(threadedComments = buildThreadedComments(newState.comments))
            } else {
                newState
            }
        }
    }

    fun loadComments(refresh: Boolean = false) {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !refresh && it.comments.isEmpty(), error = null) }
            repository.getComments(ownerId, postId, offset = if (refresh) 0 else _uiState.value.comments.size)
                .onSuccess { response ->
                    updateUiStateWithComments {
                        val currentComments = if (refresh) emptyList() else it.comments
                        val newComments = (currentComments + response.items).distinctBy { c -> c.id }
                        it.copy(
                            comments = newComments,
                            isLoading = false,
                            canLoadMore = (if (refresh) 0 else it.comments.size) + response.items.size < response.count
                        )
                    }
                    loadMissingAuthors(response.items)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun loadMissingAuthors(comments: List<Comment>) {
        viewModelScope.launch {
            // Suspect positive IDs might be groups if name is DELETED or matches groupId
            // Regular comments don't have groupId in item, but we have ownerId (if it's a community wall)
            val communityId = if (ownerId < 0) ownerId else 0
            
            val suspectedGroupIds = comments.filter { 
                it.fromId > 0 && (it.authorName == "DELETED" || it.authorName == "DELETED " || (communityId != 0 && (it.fromId == communityId.absoluteValue))) 
            }.map { it.fromId }.toSet()

            val missingUserIds = comments.filter { 
                it.fromId > 0 && it.authorAvatar.isEmpty() && !suspectedGroupIds.contains(it.fromId) 
            }.map { it.fromId }.distinct()
            
            val missingGroupIds = (comments.filter { 
                it.fromId < 0 && it.authorAvatar.isEmpty() 
            }.map { it.fromId.absoluteValue } + suspectedGroupIds).distinct()

            if (missingUserIds.isEmpty() && missingGroupIds.isEmpty()) return@launch

            val users = if (missingUserIds.isNotEmpty()) {
                profileRepository.loadUsers(missingUserIds).getOrDefault(emptyList())
            } else emptyList()

            val groups = if (missingGroupIds.isNotEmpty()) {
                profileRepository.loadGroupsByIds(missingGroupIds.map { -it.absoluteValue }).getOrDefault(emptyList())
            } else emptyList()

            if (users.isEmpty() && groups.isEmpty()) return@launch

            updateUiStateWithComments { state ->
                val newComments = state.comments.map { comment ->
                    // Try group match first for suspected IDs
                    val group = groups.find { it.id == -comment.fromId.absoluteValue }
                    val user = if (group == null) users.find { it.id == comment.fromId } else null
                    
                    when {
                        group != null -> comment.copy(
                            fromId = group.id, // Fix ID to be negative
                            authorName = group.firstName, 
                            authorAvatar = group.photo200
                        )
                        user != null -> comment.copy(
                            authorName = user.fullName, 
                            authorAvatar = user.photo200
                        )
                        else -> comment
                    }
                }
                state.copy(comments = newComments)
            }
        }
    }

    fun refresh() {
        loadPost()
        loadComments(refresh = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        if (settingsViewModel.offlineMode.value) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.getComments(ownerId, postId, offset = state.comments.size)
                .onSuccess { response ->
                    updateUiStateWithComments {
                        val newComments = (it.comments + response.items).distinctBy { c -> c.id }
                        it.copy(
                            comments = newComments,
                            isLoadingMore = false,
                            canLoadMore = it.comments.size + response.items.size < response.count
                        )
                    }
                    loadMissingAuthors(response.items)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun postComment() {
        val text = _uiState.value.inputText.trim()
        val attachments = _uiState.value.pendingAttachments
        if (text.isBlank() && attachments.isEmpty()) return
        val editing = _uiState.value.editingComment
        val replying = _uiState.value.replyingTo

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            
            // Upload attachments first
            val attachmentStrings = mutableListOf<String>()
            var uploadError: String? = null
            
            for (pending in attachments) {
                if (pending.isExisting) {
                    attachmentStrings.add(pending.attachmentString!!)
                    continue
                }
                val result = attachmentsRepository.uploadAttachment(pending)
                if (result.isFailure) {
                    uploadError = "Ошибка загрузки вложения: ${result.exceptionOrNull()?.message}"
                    break
                }
                attachmentStrings.add(result.getOrThrow())
            }
            
            if (uploadError != null) {
                _uiState.update { it.copy(isSending = false, error = uploadError) }
                return@launch
            }

            val attachmentsParam = if (attachmentStrings.isNotEmpty()) attachmentStrings.joinToString(",") else null
            android.util.Log.d("Comments", "Posting comment with attachments: $attachmentsParam")
            
            val result = if (editing != null) {
                repository.editComment(editing.id, text, attachmentsParam)
            } else {
                repository.createComment(ownerId, postId, text, attachmentsParam, _uiState.value.fromGroup, replyToComment = replying?.id)
            }

            result.onSuccess {
                _uiState.update { it.copy(inputText = "", pendingAttachments = emptyList(), isSending = false, editingComment = null, replyingTo = null) }
                loadComments(refresh = true)
            }.onFailure { err ->
                _uiState.update { it.copy(isSending = false, error = err.message) }
            }
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            repository.deleteComment(comment.id).onSuccess {
                updateUiStateWithComments { state ->
                    state.copy(comments = state.comments.filter { it.id != comment.id })
                }
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            feedRepository.deletePost(post.ownerId, post.id).onSuccess {
                _uiState.update { it.copy(post = null) }
            }
        }
    }

    fun votePoll(comment: Comment, answerIds: List<Int>) {
        val poll = comment.poll ?: return
        viewModelScope.launch {
            feedRepository.addPollVote(poll.ownerId, poll.id, answerIds).onSuccess {
                loadComments(refresh = true)
            }
        }
    }

    fun editPost(post: Post, text: String, fromGroup: Boolean = true, isNsfw: Boolean = false) {
        viewModelScope.launch {
            feedRepository.editPost(post.ownerId, post.id, text, fromGroup, isNsfw).onSuccess {
                _uiState.update { it.copy(post = it.post?.copy(text = text, isNsfw = isNsfw)) }
            }
        }
    }

    fun toggleLikePost(post: Post) {
        viewModelScope.launch {
            feedRepository.toggleLike(post).onSuccess {
                _uiState.update { state ->
                    if (state.post?.id == post.id && state.post?.ownerId == post.ownerId) {
                        state.copy(
                            post = state.post?.copy(
                                isLiked = !post.isLiked,
                                likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                            )
                        )
                    } else state
                }
            }
        }
    }

    fun toggleLike(comment: Comment) {
        viewModelScope.launch {
            repository.toggleLike(comment).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        comments = state.comments.map { item ->
                            if (item.id == comment.id) {
                                item.copy(
                                    isLiked = !item.isLiked,
                                    likeCount = if (item.isLiked) item.likeCount - 1 else item.likeCount + 1
                                )
                            } else item
                        }
                    )
                }
            }
        }
    }

    fun report(type: String, ownerId: Int, itemId: Int? = null, comment: String? = null) {
        viewModelScope.launch {
            OpenVKMatchaApp.instance.reportsRepository.report(type, ownerId, itemId, comment)
                .onSuccess {
                    AppEvents.showSnackbar("Жалоба успешно отправлена")
                }
        }
    }


    companion object {
        fun factory(ownerId: Int, postId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return CommentsViewModel(
                    app.commentsRepository,
                    app.feedRepository,
                    app.profileRepository,
                    app.attachmentsRepository,
                    SettingsViewModel(app),
                    ownerId,
                    postId
                ) as T
            }
        }
    }
}

class MusicPickerViewModel(
    private val musicRepository: MusicRepository,
    private val profileRepository: ProfileRepository,
    private val settingsViewModel: SettingsViewModel,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MusicUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    private var userId: Int? = null

    init {
        loadMusic()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadMusic(isRefresh = true)
    }

    fun loadMusic(isRefresh: Boolean = false) {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            val currentUserId = userId ?: profileRepository.loadCurrentUser().getOrNull()?.id.also { userId = it }
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Ошибка профиля") }
                return@launch
            }

            val query = _uiState.value.searchQuery
            val offset = if (isRefresh) 0 else _uiState.value.tracks.size

            val result = if (query.isNotBlank()) {
                musicRepository.searchAudio(query, offset = offset)
            } else {
                musicRepository.loadMyAudio(currentUserId, offset = offset)
            }

            result.onSuccess { tracks ->
                _uiState.update {
                    it.copy(
                        tracks = if (isRefresh) tracks else (it.tracks + tracks).distinctBy { t -> t.stableId },
                        isLoading = false,
                        canLoadMore = tracks.size >= 30
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return
        loadMusic(isRefresh = false)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return MusicPickerViewModel(app.musicRepository, app.profileRepository, SettingsViewModel(app)) as T
            }
        }
    }
}

data class FriendsUiState(
    val friends: List<UserProfile> = emptyList(),
    val onlineFriends: List<UserProfile> = emptyList(),
    val requests: List<UserProfile> = emptyList(),
    val followers: List<UserProfile> = emptyList(),
    val searchResults: List<UserProfile> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val isSearching: Boolean = false,
    val error: String? = null,
    val isMe: Boolean = false,
)

class FriendsViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsViewModel: SettingsViewModel,
    private val userId: Int,
    private val currentUserId: Int?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FriendsUiState(isLoading = true, isMe = userId == currentUserId))
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
        if (userId == currentUserId) {
            loadRequests()
        } else {
            loadFollowers()
        }
        viewModelScope.launch {
            AppEvents.searchQuery.collect { query ->
                updateSearchQuery(query)
            }
        }
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { FriendsUiState(isLoading = true, isMe = userId == currentUserId) }
                loadFriends()
                if (userId == currentUserId) loadRequests() else loadFollowers()
            }
        }
    }

    fun loadFriends() {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, canLoadMore = true) }
            profileRepository.getFriends(userId, offset = 0).onSuccess { friends ->
                _uiState.update { state ->
                    state.copy(
                        friends = friends,
                        onlineFriends = friends.filter { it.online },
                        isLoading = false,
                        canLoadMore = friends.size >= 1000
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(canLoadMore = true) }
            profileRepository.getFriendRequests(offset = 0).onSuccess { requests ->
                _uiState.update { it.copy(requests = requests, canLoadMore = requests.size >= 50) }
            }
        }
    }

    fun loadFollowers() {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(canLoadMore = true) }
            profileRepository.getFollowers(userId, offset = 0).onSuccess { followers ->
                _uiState.update { it.copy(followers = followers, canLoadMore = followers.size >= 50) }
            }
        }
    }

    fun loadMore(currentTab: Int) {
        val state = _uiState.value
        if (state.isLoadingMore || !state.canLoadMore || state.searchQuery.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            
            val result = when (currentTab) {
                0 -> profileRepository.getFriends(userId, offset = state.friends.size)
                1 -> profileRepository.getFriends(userId, offset = state.friends.size) // This might be tricky if we want ONLY online pagination, but usually it's better to load more general friends and filter
                2 -> if (state.isMe) profileRepository.getFriendRequests(offset = state.requests.size) 
                     else profileRepository.getFollowers(userId, offset = state.followers.size)
                else -> Result.success(emptyList())
            }

            result.onSuccess { newItems ->
                _uiState.update { currentState ->
                    when (currentTab) {
                        0, 1 -> {
                            val updatedFriends = (currentState.friends + newItems).distinctBy { it.id }
                            currentState.copy(
                                friends = updatedFriends,
                                onlineFriends = updatedFriends.filter { it.online },
                                isLoadingMore = false,
                                canLoadMore = newItems.size >= 1000
                            )
                        }
                        2 -> {
                            if (currentState.isMe) {
                                currentState.copy(
                                    requests = (currentState.requests + newItems).distinctBy { it.id },
                                    isLoadingMore = false,
                                    canLoadMore = newItems.size >= 50
                                )
                            } else {
                                currentState.copy(
                                    followers = (currentState.followers + newItems).distinctBy { it.id },
                                    isLoadingMore = false,
                                    canLoadMore = newItems.size >= 50
                                )
                            }
                        }
                        else -> currentState.copy(isLoadingMore = false)
                    }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoadingMore = false, error = err.message) }
            }
        }
    }

    fun acceptRequest(user: UserProfile) {
        viewModelScope.launch {
            profileRepository.addFriend(user.id).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        requests = state.requests.filter { it.id != user.id },
                        friends = listOf(user) + state.friends
                    )
                }
            }.onFailure { error ->
                if (error is com.deliriousvoid.openvkmatcha.data.api.ApiException && error.errorCode == 15) {
                    loadFriends()
                    loadRequests()
                } else {
                    AppEvents.showSnackbar(error.message ?: "Ошибка при принятии заявки")
                }
            }
        }
    }

    fun deleteFriend(user: UserProfile) {
        viewModelScope.launch {
            profileRepository.deleteFriend(user.id).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        friends = state.friends.filter { it.id != user.id },
                        onlineFriends = state.onlineFriends.filter { it.id != user.id },
                        requests = state.requests.filter { it.id != user.id },
                        searchResults = state.searchResults.map {
                            if (it.id == user.id) it.copy(friendStatus = 0) else it
                        }
                    )
                }
            }.onFailure { error ->
                if (error is com.deliriousvoid.openvkmatcha.data.api.ApiException && error.errorCode == 15) {
                    loadFriends()
                    loadRequests()
                } else {
                    AppEvents.showSnackbar(error.message ?: "Ошибка при удалении")
                }
            }
        }
    }

    fun toggleFriendship(user: UserProfile) {
        viewModelScope.launch {
            val result = when (user.friendStatus) {
                0, null -> profileRepository.addFriend(user.id) // Not a friend -> Add
                1 -> profileRepository.deleteFriend(user.id)  // Outgoing request -> Cancel
                2 -> profileRepository.addFriend(user.id)     // Incoming request -> Accept
                3 -> profileRepository.deleteFriend(user.id)  // Friends -> Delete
                else -> return@launch
            }

            result.onSuccess {
                // Update search results or local lists
                val newStatus = when (user.friendStatus) {
                    0, null -> 1 // Now outgoing
                    1 -> 0       // Now nothing
                    2 -> 3       // Now friends
                    3 -> 0       // Now nothing
                    else -> user.friendStatus
                }
                
                _uiState.update { state ->
                    state.copy(
                        searchResults = state.searchResults.map {
                            if (it.id == user.id) it.copy(friendStatus = newStatus) else it
                        },
                        friends = if (newStatus == 3) (state.friends + user.copy(friendStatus = 3)).distinctBy { it.id } 
                                 else state.friends.filter { it.id != user.id },
                        requests = state.requests.filter { it.id != user.id }
                    )
                }
            }.onFailure { error ->
                if (error is com.deliriousvoid.openvkmatcha.data.api.ApiException && error.errorCode == 15) {
                    loadFriends()
                    loadRequests()
                } else {
                    AppEvents.showSnackbar(error.message ?: "Ошибка при выполнении действия")
                }
            }
        }
    }

    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        search(query)
    }

    private fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.update { it.copy(isSearching = true) }
            profileRepository.searchUsers(query).onSuccess { list ->
                _uiState.update { it.copy(searchResults = list, isSearching = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSearching = false, error = e.message) }
            }
        }
    }


    companion object {
        fun factory(userId: Int, currentUserId: Int?): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return FriendsViewModel(app.profileRepository, SettingsViewModel(app), userId, currentUserId) as T
            }
        }
    }
}

data class GroupsUiState(
    val groups: List<UserProfile> = emptyList(),
    val managedGroups: List<UserProfile> = emptyList(),
    val searchResults: List<UserProfile> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val isSearching: Boolean = false,
    val error: String? = null,
    val isMe: Boolean = false,
)

class GroupsViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsViewModel: SettingsViewModel,
    private val userId: Int,
    private val currentUserId: Int?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GroupsUiState(isLoading = true, isMe = userId == currentUserId))
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
        if (userId == currentUserId) {
            loadManagedGroups()
        }
        viewModelScope.launch {
            AppEvents.searchQuery.collect { query ->
                updateSearchQuery(query)
            }
        }
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { GroupsUiState(isLoading = true, isMe = userId == currentUserId) }
                loadGroups()
                if (userId == currentUserId) loadManagedGroups()
            }
        }
    }

    fun loadGroups() {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, canLoadMore = true) }
            profileRepository.getUserGroups(userId, offset = 0).onSuccess { groups ->
                _uiState.update { it.copy(groups = groups, isLoading = false, canLoadMore = groups.size >= 50) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun loadManagedGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(canLoadMore = true) }
            profileRepository.getAdminGroups(offset = 0).onSuccess { groups ->
                _uiState.update { it.copy(managedGroups = groups, canLoadMore = groups.size >= 50) }
            }
        }
    }

    fun loadMore(currentTab: Int) {
        val state = _uiState.value
        if (state.isLoadingMore || !state.canLoadMore || state.searchQuery.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            
            val result = when (currentTab) {
                0 -> profileRepository.getUserGroups(userId, offset = state.groups.size)
                1 -> profileRepository.getAdminGroups(offset = state.managedGroups.size)
                else -> Result.success(emptyList())
            }

            result.onSuccess { newItems ->
                _uiState.update { currentState ->
                    when (currentTab) {
                        0 -> currentState.copy(
                            groups = (currentState.groups + newItems).distinctBy { it.id },
                            isLoadingMore = false,
                            canLoadMore = newItems.size >= 50
                        )
                        1 -> currentState.copy(
                            managedGroups = (currentState.managedGroups + newItems).distinctBy { it.id },
                            isLoadingMore = false,
                            canLoadMore = newItems.size >= 50
                        )
                        else -> currentState.copy(isLoadingMore = false)
                    }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoadingMore = false, error = err.message) }
            }
        }
    }

    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        search(query)
    }

    private fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.update { it.copy(isSearching = true) }
            profileRepository.searchGroups(query).onSuccess { list ->
                _uiState.update { it.copy(searchResults = list, isSearching = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSearching = false, error = e.message) }
            }
        }
    }

    fun toggleGroupMembership(group: UserProfile) {
        viewModelScope.launch {
            val result = if (group.isMember) {
                profileRepository.leaveGroup(group.id)
            } else {
                profileRepository.joinGroup(group.id)
            }

            result.onSuccess {
                val newIsMember = !group.isMember
                _uiState.update { state ->
                    state.copy(
                        searchResults = state.searchResults.map {
                            if (it.id == group.id) it.copy(isMember = newIsMember) else it
                        },
                        groups = if (newIsMember) (state.groups + group.copy(isMember = true)).distinctBy { it.id }
                                 else state.groups.filter { it.id != group.id }
                    )
                }
            }
        }
    }


    companion object {
        fun factory(userId: Int, currentUserId: Int?): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return GroupsViewModel(app.profileRepository, SettingsViewModel(app), userId, currentUserId) as T
            }
        }
    }
}

data class GiftsUiState(
    val gifts: List<Gift> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: Int? = null,
)

class GiftsViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsViewModel: SettingsViewModel,
    private val userId: Int,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GiftsUiState(isLoading = true))
    val uiState: StateFlow<GiftsUiState> = _uiState.asStateFlow()

    init {
        loadGifts()
        loadCurrentUserId()
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { it.copy(gifts = emptyList(), isLoading = true) }
                loadGifts()
                loadCurrentUserId()
            }
        }
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            profileRepository.loadCurrentUser().onSuccess { user ->
                _uiState.update { it.copy(currentUserId = user.id) }
            }
        }
    }

    fun loadGifts() {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileRepository.getUserGifts(userId).onSuccess { response ->
                _uiState.update { it.copy(gifts = response.items, isLoading = false) }
                fetchMissingNames(response.items)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    private fun fetchMissingNames(gifts: List<Gift>) {
        val missingUserIds = gifts.filter { it.fromId > 0 && it.senderName == null }.map { it.fromId }.distinct()
        val missingGroupIds = gifts.filter { it.fromId < 0 && it.senderName == null }.map { it.fromId }.distinct()

        if (missingUserIds.isEmpty() && missingGroupIds.isEmpty()) return

        viewModelScope.launch {
            val userProfiles = if (missingUserIds.isNotEmpty()) {
                profileRepository.loadUsers(missingUserIds).getOrNull() ?: emptyList()
            } else emptyList()

            val groupProfiles = if (missingGroupIds.isNotEmpty()) {
                profileRepository.loadGroupsByIds(missingGroupIds).getOrNull() ?: emptyList()
            } else emptyList()

            val nameMap = (userProfiles + groupProfiles).associate { profile ->
                profile.id to if (profile.isGroup) profile.firstName else "${profile.firstName} ${profile.lastName}".trim()
            }

            if (nameMap.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(gifts = state.gifts.map { gift ->
                        if (gift.senderName == null && nameMap.containsKey(gift.fromId)) {
                            gift.copy(senderName = nameMap[gift.fromId])
                        } else gift
                    })
                }
            }
        }
    }


    companion object {
        fun factory(userId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return GiftsViewModel(app.profileRepository, SettingsViewModel(app), userId) as T
            }
        }
    }
}

data class SendGiftUiState(
    val isLoading: Boolean = false,
    val categoriesWithGifts: List<CategoryWithGifts> = emptyList(),
    val friends: List<UserProfile> = emptyList(),
    val selectedUserId: Int? = null,
    val selectedGift: SelectableGift? = null,
    val message: String = "",
    val isAnonymous: Boolean = false,
    val isSent: Boolean = false,
    val error: String? = null
)

data class CategoryWithGifts(
    val category: GiftCategory,
    val gifts: List<SelectableGift>
)

class SendGiftViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsViewModel: SettingsViewModel,
    private val initialUserId: Int?
) : ViewModel() {
    private val _uiState = MutableStateFlow(SendGiftUiState(selectedUserId = initialUserId))
    val uiState: StateFlow<SendGiftUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUserId()
        loadAllGifts()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            profileRepository.loadCurrentUser().onSuccess { user ->
                loadFriends(user.id)
            }.onFailure {
                loadFriends(0)
            }
        }
    }

    fun loadFriends(userId: Int) {
        viewModelScope.launch {
            profileRepository.getFriends(userId).onSuccess { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
        }
    }

    fun loadAllGifts() {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileRepository.getGiftCategories().onSuccess { categories ->
                val categoriesWithGifts = categories.map { category ->
                    val gifts = profileRepository.getGiftsInCategory(category.id).getOrNull() ?: emptyList()
                    CategoryWithGifts(category, gifts)
                }
                _uiState.update { it.copy(categoriesWithGifts = categoriesWithGifts, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectUser(userId: Int) {
        _uiState.update { it.copy(selectedUserId = userId) }
    }

    fun selectGift(gift: SelectableGift) {
        android.util.Log.d("SendGift", "Selected gift ID: ${gift.id}")
        _uiState.update { it.copy(selectedGift = gift) }
    }

    fun setMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun setAnonymous(anonymous: Boolean) {
        _uiState.update { it.copy(isAnonymous = anonymous) }
    }

    fun send() {
        val state = _uiState.value
        val userId = state.selectedUserId ?: return
        val gift = state.selectedGift ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileRepository.sendGift(
                userId = userId,
                giftId = gift.id,
                message = state.message,
                privacy = if (state.isAnonymous) 1 else 0
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false, isSent = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    companion object {
        fun factory(userId: Int?): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return SendGiftViewModel(app.profileRepository, SettingsViewModel(app), userId) as T
            }
        }
    }
}

data class FollowersUiState(
    val users: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val currentUserId: Int? = null,
)

class FollowersViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsViewModel: SettingsViewModel,
    private val id: Int,
    private val isGroup: Boolean,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FollowersUiState(isLoading = true))
    val uiState: StateFlow<FollowersUiState> = _uiState.asStateFlow()
    private val pageSize = 50

    init {
        loadFollowers(isRefresh = true)
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { FollowersUiState(isLoading = true) }
                loadFollowers(isRefresh = true)
            }
        }
    }

    fun loadFollowers(isRefresh: Boolean = false) {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isLoading = true, error = null, canLoadMore = true) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true, error = null) }
            }

            val offset = if (isRefresh) 0 else _uiState.value.users.size
            val result = if (isGroup) {
                profileRepository.getMembers(id, offset = offset, count = pageSize)
            } else {
                profileRepository.getFollowers(id, offset = offset, count = pageSize)
            }
            
            result.onSuccess { users ->
                _uiState.update { state ->
                    val newList = if (isRefresh) users else (state.users + users)
                    state.copy(
                        users = newList,
                        isLoading = false,
                        isLoadingMore = false,
                        canLoadMore = users.size >= pageSize
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = err.message) }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || !_uiState.value.canLoadMore) return
        loadFollowers(isRefresh = false)
    }

    companion object {
        fun factory(id: Int, isGroup: Boolean): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return FollowersViewModel(app.profileRepository, SettingsViewModel(app), id, isGroup) as T
            }
        }
    }
}

data class PhotosUiState(
    val photos: List<Photo> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val reversed: Boolean = false,
    val error: String? = null,
    val currentUserId: Int? = null,
)

class PhotosViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsViewModel: SettingsViewModel,
    private val userId: Int,
    private val albumId: Int? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotosUiState(isLoading = true))
    val uiState: StateFlow<PhotosUiState> = _uiState.asStateFlow()

    private val pageSize = 50
    private var apiIsNewestFirst: Boolean? = null

    init {
        loadCurrentUserId()
        loadPhotos()
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { PhotosUiState(isLoading = true) }
                loadCurrentUserId()
                loadPhotos(isRefresh = true)
            }
        }
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            profileRepository.loadCurrentUser().onSuccess { user ->
                _uiState.update { it.copy(currentUserId = user.id) }
            }
        }
    }

    fun toggleSort() {
        val newReversed = !_uiState.value.reversed
        _uiState.update { it.copy(reversed = newReversed, photos = emptyList(), isLoading = true) }
        loadPhotos(isRefresh = true, reversedOverride = newReversed)
    }

    fun loadPhotos(isRefresh: Boolean = false, isManual: Boolean = false, reversedOverride: Boolean? = null) {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }
        viewModelScope.launch {
            val desiredReversed = reversedOverride ?: _uiState.value.reversed
            _uiState.update {
                it.copy(
                    isLoading = if (isManual) false else (isRefresh || it.photos.isEmpty()),
                    isRefreshing = isManual,
                    canLoadMore = if (isRefresh) true else it.canLoadMore,
                    error = null,
                    photos = if (isRefresh && !isManual) emptyList() else it.photos,
                    reversed = desiredReversed
                )
            }

            // 1. Get probe page to determine total count and API order
            profileRepository.getUserPhotos(userId, albumId, offset = 0, count = pageSize, reversed = false).onSuccess { probeResponse: PhotosResponse ->
                val total = probeResponse.count
                if (probeResponse.items.isEmpty()) {
                    _uiState.update { it.copy(photos = emptyList(), totalCount = 0, isLoading = false, isRefreshing = false, canLoadMore = false) }
                    return@onSuccess
                }

                // API order: true if first is newer than last
                val newestFirstInApi = if (probeResponse.items.size > 1) {
                    probeResponse.items.first().date >= probeResponse.items.last().date
                } else true
                apiIsNewestFirst = newestFirstInApi

                // desiredReversed: false = Newest First, true = Oldest First
                val needsManualReverse = (desiredReversed && newestFirstInApi) || (!desiredReversed && !newestFirstInApi)

                if (needsManualReverse) {
                    // We want the other end of the list
                    val offset = (total - pageSize).coerceAtLeast(0)
                    profileRepository.getUserPhotos(userId, albumId, offset = offset, count = pageSize, reversed = false).onSuccess { revResponse: PhotosResponse ->
                        _uiState.update {
                            it.copy(
                                photos = revResponse.items.reversed(),
                                totalCount = total,
                                isLoading = false,
                                isRefreshing = false,
                                canLoadMore = total > pageSize
                            )
                        }
                    }.onFailure { err ->
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = err.message) }
                    }
                } else {
                    // API order is what we want
                    _uiState.update {
                        it.copy(
                            photos = probeResponse.items,
                            totalCount = total,
                            isLoading = false,
                            isRefreshing = false,
                            canLoadMore = total > probeResponse.items.size
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = err.message) }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore || apiIsNewestFirst == null) return
        if (settingsViewModel.offlineMode.value) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val newestFirstInApi = apiIsNewestFirst!!
            val desiredReversed = state.reversed // false = Newest, true = Oldest
            val needsManualReverse = (desiredReversed && newestFirstInApi) || (!desiredReversed && !newestFirstInApi)

            if (needsManualReverse) {
                // Loading "backwards" from the end
                val currentLoaded = state.photos.size
                val offset = (state.totalCount - currentLoaded - pageSize).coerceAtLeast(0)
                val requestedCount = if (state.totalCount - currentLoaded < pageSize) {
                    state.totalCount - currentLoaded
                } else pageSize

                if (requestedCount <= 0) {
                    _uiState.update { it.copy(isLoadingMore = false, canLoadMore = false) }
                    return@launch
                }

                profileRepository.getUserPhotos(userId, albumId, offset = offset, count = requestedCount, reversed = false).onSuccess { nextBatchResponse: PhotosResponse ->
                    _uiState.update {
                        val newBatch = nextBatchResponse.items.reversed()
                        val combined = (it.photos + newBatch).distinctBy { photo: Photo -> photo.id }
                        it.copy(
                            photos = combined,
                            isLoadingMore = false,
                            canLoadMore = combined.size < it.totalCount
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { it.copy(isLoadingMore = false, error = err.message) }
                }
            } else {
                // Loading "forwards"
                profileRepository.getUserPhotos(userId, albumId, offset = state.photos.size, count = pageSize, reversed = false).onSuccess { nextBatchResponse: PhotosResponse ->
                    _uiState.update {
                        val combined = (it.photos + nextBatchResponse.items).distinctBy { photo: Photo -> photo.id }
                        it.copy(
                            photos = combined,
                            isLoadingMore = false,
                            canLoadMore = combined.size < it.totalCount
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { it.copy(isLoadingMore = false, error = err.message) }
                }
            }
        }
    }

    fun deletePhotos(photoIds: List<Int>) {
        viewModelScope.launch {
            photoIds.forEach { id ->
                profileRepository.deletePhoto(userId, id)
            }
            loadPhotos(isRefresh = true)
        }
    }

    fun editPhoto(photoId: Int, caption: String) {
        viewModelScope.launch {
            profileRepository.editPhoto(userId, photoId, caption).onSuccess {
                loadPhotos(isRefresh = true)
            }
        }
    }

    companion object {
        fun factory(userId: Int, albumId: Int? = null): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return PhotosViewModel(app.profileRepository, SettingsViewModel(app), userId, albumId) as T
            }
        }
    }
}

data class PhotoAlbumsUiState(
    val albums: List<com.deliriousvoid.openvkmatcha.data.model.PhotoAlbum> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: Int? = null,
)

class PhotoAlbumsViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsViewModel: SettingsViewModel,
    private val userId: Int,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoAlbumsUiState(isLoading = true))
    val uiState: StateFlow<PhotoAlbumsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUserId()
        loadAlbums()
        viewModelScope.launch {
            AppEvents.accountChanged.collect {
                _uiState.update { PhotoAlbumsUiState(isLoading = true) }
                loadCurrentUserId()
                loadAlbums()
            }
        }
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            profileRepository.loadCurrentUser().onSuccess { user ->
                _uiState.update { it.copy(currentUserId = user.id) }
            }
        }
    }

    fun loadAlbums() {
        if (settingsViewModel.offlineMode.value) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            profileRepository.getAlbums(userId).onSuccess { albums ->
                _uiState.update { it.copy(albums = albums, isLoading = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun createAlbum(title: String, description: String) {
        viewModelScope.launch {
            profileRepository.createAlbum(title, description).onSuccess {
                loadAlbums()
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message) }
            }
        }
    }

    fun deleteAlbum(albumId: Int) {
        viewModelScope.launch {
            profileRepository.deleteAlbum(albumId).onSuccess {
                loadAlbums()
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message) }
            }
        }
    }

    fun editAlbum(albumId: Int, title: String, description: String) {
        viewModelScope.launch {
            profileRepository.editAlbum(albumId, title, description).onSuccess {
                loadAlbums()
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message) }
            }
        }
    }

    companion object {
        fun factory(userId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return PhotoAlbumsViewModel(app.profileRepository, SettingsViewModel(app), userId) as T
            }
        }
    }
}
