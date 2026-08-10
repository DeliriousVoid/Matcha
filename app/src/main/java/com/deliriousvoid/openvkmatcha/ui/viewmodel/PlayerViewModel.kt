package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.LrcLine
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource
import com.deliriousvoid.openvkmatcha.data.repository.ArtworkRepository
import com.deliriousvoid.openvkmatcha.data.repository.LyricsRepository
import com.deliriousvoid.openvkmatcha.playback.LrcParser
import com.deliriousvoid.openvkmatcha.playback.MusicPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    val playerManager: MusicPlayerManager,
    private val lyricsRepository: LyricsRepository,
    private val artworkRepository: ArtworkRepository,
    private val musicRepository: com.deliriousvoid.openvkmatcha.data.repository.MusicRepository
) : ViewModel() {

    private val _trackWithArtwork = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _trackWithArtwork.asStateFlow()
    val isPlaying = playerManager.isPlaying
    val playWhenReady = playerManager.playWhenReady
    val playbackState = playerManager.playbackState
    val shuffleMode = playerManager.shuffleMode
    val repeatMode = playerManager.repeatMode
    val currentPosition = playerManager.currentPosition
    val duration = playerManager.duration
    val queue = playerManager.queue

    private val _syncedLyrics = MutableStateFlow<List<LrcLine>>(emptyList())
    val syncedLyrics: StateFlow<List<LrcLine>> = _syncedLyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    val currentLineIndex: StateFlow<Int> = combine(syncedLyrics, currentPosition) { lyrics, pos ->
        if (lyrics.isEmpty()) -1
        else lyrics.indexOfLast { it.timestampMs <= pos }.coerceAtLeast(0)
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    init {
        viewModelScope.launch {
            playerManager.currentTrack.collect { track ->
                if (track != null) {
                    _trackWithArtwork.value = track
                    loadLyrics(track)
                    loadArtwork(track)
                } else {
                    _trackWithArtwork.value = null
                    _syncedLyrics.value = emptyList()
                }
            }
        }
    }

    private fun loadArtwork(track: AudioTrack) {
        if (track.artworkUrl != null) return
        viewModelScope.launch {
            artworkRepository.getArtworkUrl(track.artist, track.title)
                .onSuccess { url ->
                    if (url != null && _trackWithArtwork.value?.id == track.id) {
                        _trackWithArtwork.value = track.copy(artworkUrl = url)
                    }
                }
        }
    }

    private fun loadLyrics(track: AudioTrack) {
        viewModelScope.launch {
            _lyricsLoading.value = true
            lyricsRepository.getLyrics(track.title, track.artist, track.duration)
                .onSuccess { record ->
                    _syncedLyrics.value = LrcParser.parse(record?.syncedLyrics)
                }
                .onFailure {
                    _syncedLyrics.value = emptyList()
                }
            _lyricsLoading.value = false
        }
    }

    fun play(tracks: List<AudioTrack>, startIndex: Int = 0, source: PlaylistSource = PlaylistSource.Unknown) {
        playerManager.play(tracks, startIndex, source)
    }

    fun playPause() = playerManager.playPause()
    fun skipToNext() = playerManager.skipToNext()
    fun skipToPrevious() = playerManager.skipToPrevious()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun toggleRepeat() = playerManager.toggleRepeat()
    fun seekTo(position: Long) = playerManager.seekTo(position)
    fun removeFromQueue(index: Int) = playerManager.removeFromQueue(index)

    fun moveItemInQueue(from: Int, to: Int) = playerManager.moveItem(from, to)

    fun skipToQueueItem(index: Int) = playerManager.skipToQueueItem(index)

    fun toggleTrackAdded(track: AudioTrack) {
        val newState = !track.isAdded
        // Optimistic update
        if (_trackWithArtwork.value?.id == track.id && _trackWithArtwork.value?.ownerId == track.ownerId) {
            _trackWithArtwork.value = track.copy(isAdded = newState)
        }

        viewModelScope.launch {
            val result = if (newState) {
                musicRepository.addAudio(track.id, track.ownerId)
            } else {
                musicRepository.deleteAudio(track.id, track.ownerId)
            }

            result.onFailure {
                // Revert on failure
                if (_trackWithArtwork.value?.id == track.id && _trackWithArtwork.value?.ownerId == track.ownerId) {
                    _trackWithArtwork.value = track
                }
            }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return PlayerViewModel(
                    app.playerManager, 
                    app.lyricsRepository, 
                    app.artworkRepository,
                    app.musicRepository
                ) as T
            }
        }
    }
}
