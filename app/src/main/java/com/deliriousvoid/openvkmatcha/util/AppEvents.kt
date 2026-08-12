package com.deliriousvoid.openvkmatcha.util

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.RowScope
import com.deliriousvoid.openvkmatcha.data.model.Video
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.deliriousvoid.openvkmatcha.playback.PlaybackService
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class QrData(val url: String, val title: String, val avatarUrl: String)

data class TopBarState(
    val title: String = "",
    val navigationIcon: (@Composable () -> Unit)? = null,
    val actions: (@Composable RowScope.() -> Unit)? = null,
    val isSearchActive: Boolean = false,
    val customContent: (@Composable () -> Unit)? = null,
    val customTopBar: (@Composable () -> Unit)? = null,
    val tag: String? = null
)

val LocalFullScreenVideoHandler = staticCompositionLocalOf<((Video, Boolean, ExoPlayer?) -> Unit)?> { null }

object AppEvents {
    private val _networkError = MutableSharedFlow<Unit>()
    val networkError = _networkError.asSharedFlow()

    private val _refreshFeed = MutableSharedFlow<Unit>()
    val refreshFeed = _refreshFeed.asSharedFlow()

    private val _refreshMusic = MutableSharedFlow<Unit>()
    val refreshMusic = _refreshMusic.asSharedFlow()

    private val _refreshProfile = MutableSharedFlow<Int?>(extraBufferCapacity = 1)
    val refreshProfile = _refreshProfile.asSharedFlow()

    private val _refreshNotes = MutableSharedFlow<Unit>()
    val refreshNotes = _refreshNotes.asSharedFlow()

    private val _refreshNotifications = MutableSharedFlow<Unit>()
    val refreshNotifications = _refreshNotifications.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val _activeVideo = MutableStateFlow<Video?>(null)
    val activeVideo = _activeVideo.asStateFlow()

    private val _activeExoPlayer = MutableStateFlow<ExoPlayer?>(null)
    val activeExoPlayer = _activeExoPlayer.asStateFlow()

    private val _isVideoFloating = MutableStateFlow(false)
    val isVideoFloating = _isVideoFloating.asStateFlow()

    private var videoMediaSession: MediaSession? = null
    fun getVideoSession(): MediaSession? = videoMediaSession
    fun setVideoSession(session: MediaSession?) {
        videoMediaSession = session
    }

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode = _isInPipMode.asStateFlow()

    private val _shouldEnterPip = MutableStateFlow(false)
    val shouldEnterPip = _shouldEnterPip.asStateFlow()

    private val _customTitle = MutableStateFlow<String?>(null)
    val customTitle = _customTitle.asStateFlow()

    private val _topBarState = MutableStateFlow<TopBarState?>(null)
    val topBarState = _topBarState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchCategory = MutableStateFlow(SearchCategory.PEOPLE)
    val searchCategory = _searchCategory.asStateFlow()

    private val _currentQrData = MutableStateFlow<QrData?>(null)
    val currentQrData = _currentQrData.asStateFlow()

    fun setCurrentQrData(data: QrData?) {
        _currentQrData.value = data
    }

    fun setCustomTitle(title: String?) {
        _customTitle.value = title
    }

    fun setTopBarState(state: TopBarState?) {
        _topBarState.value = state
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchCategory(category: SearchCategory) {
        _searchCategory.value = category
    }

    fun setActiveVideo(context: Context, video: Video?, player: ExoPlayer? = null) {
        if (_activeExoPlayer.value != null && _activeExoPlayer.value != player) {
            _activeExoPlayer.value?.release()
        }
        
        _activeVideo.value = video
        _activeExoPlayer.value = player

        // Delegate MediaSession creation to PlaybackService
        val serviceIntent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_UPDATE_VIDEO_SESSION
        }
        context.startService(serviceIntent)
    }

    fun releaseActivePlayer(context: Context? = null, force: Boolean = false) {
        if (_isInPipMode.value && !force) return
        _activeExoPlayer.value?.release()
        _activeVideo.value = null
        _activeExoPlayer.value = null

        context?.let {
            val serviceIntent = Intent(it, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_UPDATE_VIDEO_SESSION
            }
            it.startService(serviceIntent)
        }
    }

    private val _isFullScreenOpened = MutableStateFlow(false)
    val isFullScreenOpened = _isFullScreenOpened.asStateFlow()

    fun setInPipMode(inPip: Boolean) {
        _isInPipMode.value = inPip
        if (!inPip) {
            _shouldEnterPip.value = false
        }
    }

    fun setVideoFloating(floating: Boolean) {
        _isVideoFloating.value = floating
    }

    fun setFullScreenOpened(opened: Boolean) {
        _isFullScreenOpened.value = opened
    }

    fun triggerEnterPip() {
        _shouldEnterPip.value = true
    }

    suspend fun emitNetworkError() {
        _networkError.emit(Unit)
    }

    suspend fun emitRefreshFeed() {
        _refreshFeed.emit(Unit)
    }

    suspend fun emitRefreshMusic() {
        _refreshMusic.emit(Unit)
    }

    suspend fun emitRefreshProfile(ownerId: Int? = null) {
        _refreshProfile.emit(ownerId)
    }

    suspend fun emitRefreshNotes() {
        _refreshNotes.emit(Unit)
    }

    suspend fun emitRefreshNotifications() {
        _refreshNotifications.emit(Unit)
    }

    suspend fun showSnackbar(message: String) {
        _snackbarMessage.emit(message)
    }

    fun isNetworkError(message: String?): Boolean {
        return message?.contains("Unable to resolve host", ignoreCase = true) == true ||
               message?.contains("failed to connect", ignoreCase = true) == true
    }
}

enum class SearchCategory(val title: String) {
    PEOPLE("Люди"),
    GROUPS("Сообщества"),
    MUSIC("Музыка"),
    PLAYLISTS("Плейлисты")
}
