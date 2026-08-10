package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.repository.ArtworkRepository
import com.deliriousvoid.openvkmatcha.data.repository.DownloadRepository
import com.deliriousvoid.openvkmatcha.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicActionsViewModel(
    private val musicRepository: MusicRepository,
    private val downloadRepository: DownloadRepository,
    private val artworkRepository: ArtworkRepository,
) : ViewModel() {

    private val _trackStates = MutableStateFlow<Map<String, AudioTrack>>(emptyMap())
    val trackStates: StateFlow<Map<String, AudioTrack>> = _trackStates.asStateFlow()

    val downloadedTracks = downloadRepository.downloadedTracksFlow

    fun toggleTrackAdded(track: AudioTrack) {
        val currentTrack = _trackStates.value[track.stableId] ?: track
        val newState = !currentTrack.isAdded
        
        // Optimistic update
        _trackStates.update { it + (track.stableId to currentTrack.copy(isAdded = newState)) }
        
        viewModelScope.launch {
            val result = if (newState) {
                musicRepository.addAudio(currentTrack.id, currentTrack.ownerId)
            } else {
                musicRepository.deleteAudio(currentTrack.id, currentTrack.ownerId)
            }

            result.onFailure {
                // Revert on failure
                _trackStates.update { it + (track.stableId to currentTrack) }
            }
        }
    }

    fun downloadTrack(track: AudioTrack) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
            // Trigger UI update for downloaded status
            _trackStates.update { it + (track.stableId to (it[track.stableId] ?: track)) }
        }
    }

    fun addToQueue(track: AudioTrack) {
        OpenVKMatchaApp.instance.playerManager.addToQueue(track)
    }

    fun playNext(track: AudioTrack) {
        OpenVKMatchaApp.instance.playerManager.playNext(track)
    }

    fun isDownloaded(trackId: Int, ownerId: Int): Boolean {
        val stableId = "${ownerId}_$trackId"
        if (downloadedTracks.value.contains(stableId)) return true
        val track = _trackStates.value[stableId]
        return track?.url?.startsWith("/") == true
    }

    fun loadArtworks(tracks: List<AudioTrack>) {
        viewModelScope.launch {
            tracks.forEach { track ->
                val cached = _trackStates.value[track.stableId]
                if (track.artworkUrl == null && cached?.artworkUrl == null) {
                    artworkRepository.getArtworkUrl(track.artist, track.title)
                        .onSuccess { url ->
                            if (url != null) {
                                _trackStates.update { state ->
                                    val t = state[track.stableId] ?: track
                                    state + (track.stableId to t.copy(artworkUrl = url))
                                }
                            }
                        }
                }
            }
        }
    }

    fun getTrack(track: AudioTrack): AudioTrack {
        val cached = _trackStates.value[track.stableId] ?: track
        return OpenVKMatchaApp.instance.downloadRepository.enrichTrack(cached)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = OpenVKMatchaApp.instance
                return MusicActionsViewModel(
                    app.musicRepository,
                    app.downloadRepository,
                    app.artworkRepository
                ) as T
            }
        }
    }
}
