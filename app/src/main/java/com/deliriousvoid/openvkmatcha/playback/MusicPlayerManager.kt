package com.deliriousvoid.openvkmatcha.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource
import com.deliriousvoid.openvkmatcha.data.repository.MusicRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MusicPlayerManager(
    private val context: Context,
    private val musicRepository: MusicRepository
) {
    private val playerPrefs = context.getSharedPreferences("player_state", Context.MODE_PRIVATE)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _playlistSource = MutableStateFlow<PlaylistSource>(PlaylistSource.Unknown)
    val playlistSource = _playlistSource.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playWhenReady = MutableStateFlow(false)
    val playWhenReady = _playWhenReady.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState = _playbackState.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<AudioTrack>>(emptyList())
    val queue = _queue.asStateFlow()

    private var currentPlaylist: List<AudioTrack> = emptyList()
    private var originalPlaylist: List<AudioTrack> = emptyList()

    private var activeTrackId: String? = null
    private var lastScrobbledId: String? = null
    private var lastNowPlayingId: String? = null
    private var accumulatedTimeMs: Long = 0
    private var lastTickTime: Long = 0

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var isInternalUpdate = false
    private var lastSeekTime: Long = 0

    // Cached settings for performance
    private var lbEnabled: Boolean? = null
    private var lbToken: String? = null
    private var lastSettingsRefresh: Long = 0
    private var saveJob: Job? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            setupController()
        }, MoreExecutors.directExecutor())

        scope.launch {
            currentTrack.collect { track ->
                if (track != null) {
                    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    if (prefs.getBoolean("auto_download", false)) {
                        OpenVKMatchaApp.instance.downloadRepository.downloadTrack(track)
                    }
                }
            }
        }
    }

    private var lastLbErrorTime: Long = 0

    private suspend fun checkAndScrobble() {
        val track = currentTrack.value ?: return
        
        val now = System.currentTimeMillis()
        if (now - lastSettingsRefresh > 10000 || lbEnabled == null) {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            lbEnabled = prefs.getBoolean("lb_enabled", false)
            lbToken = (prefs.getString("lb_token", "") ?: "").trim()
            lastSettingsRefresh = now
        }

        val enabled = lbEnabled == true
        val token = lbToken ?: ""
        
        if (!enabled) {
            // Log once per track if disabled
            if (activeTrackId != track.stableId) Log.d("ListenBrainz", "Scrobbling disabled in settings")
            return
        }
        if (token.isBlank()) {
            if (activeTrackId != track.stableId) Log.d("ListenBrainz", "Token is blank, skipping scrobble")
            return
        }

        // Detect physical track change
        if (activeTrackId != track.stableId) {
            Log.d("ListenBrainz", "Track changed to ${track.stableId} (${track.title}), resetting flags. Token: ${token.take(5)}...")
            activeTrackId = track.stableId
            lastNowPlayingId = null
            lastScrobbledId = null
            accumulatedTimeMs = 0
            lastLbErrorTime = 0
        }

        // Rate limit retries on error (every 30 seconds)
        if (lastLbErrorTime > 0 && now - lastLbErrorTime < 30000) return

        // Handle "Now Playing"
        if (lastNowPlayingId != track.stableId) {
            Log.d("ListenBrainz", "Sending playing_now for ${track.title}...")
            OpenVKMatchaApp.instance.listenBrainzRepository.submitListen(
                token = token,
                track = track,
                type = "playing_now"
            ).onSuccess {
                Log.d("ListenBrainz", "playing_now SUCCESS")
                lastNowPlayingId = track.stableId
                lastLbErrorTime = 0
            }.onFailure { e ->
                Log.e("ListenBrainz", "playing_now FAILED: ${e.message}")
                lastLbErrorTime = now
            }
        }

        // Handle the actual scrobble
        if (lastScrobbledId != track.stableId) {
            val durationMs = track.duration * 1000L
            val thresholdMs = minOf(durationMs / 2, 4 * 60 * 1000L)
            
            if (accumulatedTimeMs >= thresholdMs) {
                Log.d("ListenBrainz", "Threshold reached (${accumulatedTimeMs/1000}s / ${thresholdMs/1000}s). Sending scrobble...")
                OpenVKMatchaApp.instance.listenBrainzRepository.submitListen(
                    token = token,
                    track = track,
                    type = "single",
                    listenedAt = System.currentTimeMillis() / 1000
                ).onSuccess {
                    Log.d("ListenBrainz", "Scrobble SUCCESS")
                    lastScrobbledId = track.stableId
                    lastLbErrorTime = 0
                }.onFailure { e ->
                    Log.e("ListenBrainz", "Scrobble FAILED: ${e.message}")
                    lastLbErrorTime = now
                }
            }
        }
    }

    private fun setupController() {
        val controller = controller ?: return
        
        // Load saved state before anything else
        val savedShuffle = playerPrefs.getBoolean("shuffle_mode", false)
        val savedRepeat = playerPrefs.getInt("repeat_mode", Player.REPEAT_MODE_OFF)
        _shuffleMode.value = savedShuffle
        _repeatMode.value = savedRepeat
        
        val currentQueueJson = playerPrefs.getString("current_queue_json", "") ?: ""
        val originalQueueJson = playerPrefs.getString("original_queue_json", "") ?: ""
        currentPlaylist = currentQueueJson.toAudioTrackList()
        originalPlaylist = originalQueueJson.toAudioTrackList()

        controller.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentPosition.value = 0L
                lastSeekTime = System.currentTimeMillis()
                updateCurrentTrack(mediaItem)
                savePlaybackState()
                
                // Dynamic loading and preloading
                handleMediaTransition()
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val current = _currentTrack.value ?: return
                val newTitle = mediaMetadata.title?.toString()
                val newArtist = mediaMetadata.artist?.toString()
                
                if ((newTitle != null && newTitle != current.title) || 
                    (newArtist != null && newArtist != current.artist)) {
                    _currentTrack.value = current.copy(
                        title = newTitle ?: current.title,
                        artist = newArtist ?: current.artist
                    )
                }
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                updateQueue()
                savePlaybackState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressUpdate() else {
                    stopProgressUpdate()
                    savePlaybackState()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.value = state
                val d = controller.duration
                if (d > 0) {
                    _duration.value = d
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleMode.value = shuffleModeEnabled
                playerPrefs.edit().putBoolean("shuffle_mode", shuffleModeEnabled).apply()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                _playWhenReady.value = playWhenReady
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
                playerPrefs.edit().putInt("repeat_mode", repeatMode).apply()
            }
        })

        if (currentPlaylist.isEmpty() && controller.mediaItemCount > 0) {
            val list = mutableListOf<AudioTrack>()
            for (i in 0 until controller.mediaItemCount) {
                (controller.getMediaItemAt(i).localConfiguration?.tag as? AudioTrack)?.let {
                    list.add(it)
                }
            }
            currentPlaylist = list
        }

        // Restore Shuffle/Repeat
        controller.shuffleModeEnabled = false // We use manual shuffle
        controller.repeatMode = savedRepeat

        _isPlaying.value = controller.isPlaying
        _playWhenReady.value = controller.playWhenReady
        _playbackState.value = controller.playbackState
        
        if (controller.mediaItemCount == 0) {
            restoreLastTrack(controller)
        } else {
            // Re-sync currentPlaylist from controller if it was empty but service was running
            if (currentPlaylist.isEmpty()) {
                val list = mutableListOf<AudioTrack>()
                for (i in 0 until controller.mediaItemCount) {
                    val item = controller.getMediaItemAt(i)
                    (item.localConfiguration?.tag as? AudioTrack)?.let {
                        list.add(it)
                    }
                }
                currentPlaylist = list
                if (originalPlaylist.isEmpty()) originalPlaylist = list
            }
            updateCurrentTrack(controller.currentMediaItem)
            if (controller.duration > 0) {
                _duration.value = controller.duration
            }
        }
        
        updateQueue()
        if (controller.isPlaying) startProgressUpdate()
    }

    private fun savePlaybackState() {
        val controller = controller ?: return
        val track = _currentTrack.value ?: return
        val currentPosition = controller.currentPosition
        val currentMediaItemIndex = controller.currentMediaItemIndex
        val playlistSource = _playlistSource.value
        val currentList = currentPlaylist.toList()
        val originalList = originalPlaylist.toList()

        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            // Debounce saves slightly to avoid hammering the disk during seek
            delay(500)
            
            val currentQueueJson = currentList.toJsonArrayString()
            val originalQueueJson = originalList.toJsonArrayString()
            val sourceJson = playlistSource.toJsonString()

            playerPrefs?.edit()?.apply {
                putInt("last_track_id", track.id)
                putInt("last_track_owner_id", track.ownerId)
                putString("last_track_artist", track.artist)
                putString("last_track_title", track.title)
                putInt("last_track_duration", track.duration)
                putString("last_track_url", track.url)
                putString("last_track_remote_url", track.remoteUrl)
                putString("last_track_artwork_url", track.artworkUrl)
                putLong("last_position", currentPosition)
                putInt("last_index", currentMediaItemIndex)
                putString("last_source", sourceJson)
                putString("current_queue_json", currentQueueJson)
                putString("original_queue_json", originalQueueJson)
                apply()
            }
        }
    }

    private fun List<AudioTrack>.toJsonArrayString(): String {
        val array = JSONArray()
        forEach { track ->
            array.put(JSONObject().apply {
                put("id", track.id)
                put("ownerId", track.ownerId)
                put("artist", track.artist)
                put("title", track.title)
                put("duration", track.duration)
                put("url", track.url ?: "")
                put("remoteUrl", track.remoteUrl ?: "")
                put("artworkUrl", track.artworkUrl ?: "")
                put("isAdded", track.isAdded)
            })
        }
        return array.toString()
    }

    private fun String.toAudioTrackList(): List<AudioTrack> {
        if (isBlank()) return emptyList()
        return try {
            val array = JSONArray(this)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                AudioTrack(
                    id = obj.getInt("id"),
                    ownerId = obj.getInt("ownerId"),
                    artist = obj.getString("artist"),
                    title = obj.getString("title"),
                    duration = obj.getInt("duration"),
                    url = obj.getString("url").takeIf { it.isNotBlank() },
                    remoteUrl = obj.getString("remoteUrl").takeIf { it.isNotBlank() },
                    artworkUrl = obj.getString("artworkUrl").takeIf { it.isNotBlank() },
                    isAdded = obj.optBoolean("isAdded", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun restoreLastTrack(controller: MediaController) {
        val currentQueueJson = playerPrefs.getString("current_queue_json", "") ?: ""
        val originalQueueJson = playerPrefs.getString("original_queue_json", "") ?: ""
        val lastSourceStr = playerPrefs.getString("last_source", null)
        
        _playlistSource.value = PlaylistSource.fromJsonString(lastSourceStr)
        
        val restoredCurrent = currentQueueJson.toAudioTrackList()
        val restoredOriginal = originalQueueJson.toAudioTrackList()
        
        if (restoredCurrent.isNotEmpty()) {
            currentPlaylist = restoredCurrent
            originalPlaylist = if (restoredOriginal.isNotEmpty()) restoredOriginal else restoredCurrent
            
            // Set queue to UI early so it's not empty while loading
            _queue.value = restoredCurrent

            val lastIndex = playerPrefs.getInt("last_index", 0).coerceIn(0, restoredCurrent.size - 1)
            val lastPosition = playerPrefs.getLong("last_position", 0L)
            
            val mediaItems = restoredCurrent.map { it.toMediaItem() }
            controller.setMediaItems(mediaItems, lastIndex, lastPosition)
            controller.prepare()
            
            _currentTrack.value = restoredCurrent.getOrNull(lastIndex)
        } else {
            // Legacy single track restore for backward compatibility or if queue was empty
            val trackId = playerPrefs.getInt("last_track_id", -1)
            if (trackId == -1) return

            val track = AudioTrack(
                id = trackId,
                ownerId = playerPrefs.getInt("last_track_owner_id", 0),
                artist = playerPrefs.getString("last_track_artist", "") ?: "",
                title = playerPrefs.getString("last_track_title", "") ?: "",
                duration = playerPrefs.getInt("last_track_duration", 0),
                url = playerPrefs.getString("last_track_url", null),
                remoteUrl = playerPrefs.getString("last_track_remote_url", null),
                artworkUrl = playerPrefs.getString("last_track_artwork_url", null),
                isAdded = true
            )

            _currentTrack.value = track
            _duration.value = track.duration * 1000L

            val mediaItem = track.toMediaItem()
            controller.setMediaItem(mediaItem)
            controller.prepare()
        }
    }

    private fun updateCurrentTrack(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId
        if (mediaId == null) {
            // Only nullify if we're not in the middle of a restoration
            // or if the controller explicitly has no items
            if (controller?.mediaItemCount == 0) {
                _currentTrack.value = null
            }
            return
        }

        val track = currentPlaylist.find { it.stableId == mediaId }
            ?: (mediaItem.localConfiguration?.tag as? AudioTrack)
        
        if (track != null) {
            val enriched = OpenVKMatchaApp.instance.downloadRepository.enrichTrack(track)
            _currentTrack.value = enriched
            _duration.value = enriched.duration * 1000L
        }
    }

    private fun updateQueue() {
        val controller = controller ?: return
        if (isInternalUpdate) return
        
        val count = controller.mediaItemCount
        
        if (count > 0) {
            val list = mutableListOf<AudioTrack>()
            val downloadRepository = OpenVKMatchaApp.instance.downloadRepository
            for (i in 0 until count) {
                val item = controller.getMediaItemAt(i)
                // Search in BOTH playlists to be sure, and fallback to metadata
                val track = currentPlaylist.find { it.stableId == item.mediaId }
                    ?: originalPlaylist.find { it.stableId == item.mediaId }
                    ?: (item.localConfiguration?.tag as? AudioTrack)
                
                if (track != null) {
                    list.add(downloadRepository.enrichTrack(track))
                } else {
                    // Fallback to media item metadata if track object is lost
                    val fallback = AudioTrack(
                        id = item.mediaId.split("_").lastOrNull()?.toIntOrNull() ?: 0,
                        ownerId = item.mediaId.split("_").firstOrNull()?.toIntOrNull() ?: 0,
                        artist = item.mediaMetadata.artist?.toString() ?: "Unknown",
                        title = item.mediaMetadata.title?.toString() ?: "Unknown",
                        duration = 0,
                        url = item.localConfiguration?.uri?.toString(),
                        isAdded = false
                    )
                    list.add(downloadRepository.enrichTrack(fallback))
                }
            }
            
            // Sync currentPlaylist to the actual order in controller
            currentPlaylist = list
            _queue.value = list
        } else if (currentPlaylist.isEmpty()) {
            _queue.value = emptyList()
        } else {
            // Keep the UI queue as currentPlaylist during transient empty states
            _queue.value = currentPlaylist
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        lastTickTime = System.currentTimeMillis()
        progressJob = scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val delta = now - lastTickTime
                lastTickTime = now
                accumulatedTimeMs += delta

                controller?.let {
                    if (System.currentTimeMillis() - lastSeekTime > 500) {
                        _currentPosition.value = it.currentPosition
                    }
                }
                
                checkAndScrobble()

                delay(100)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    fun play(tracks: List<AudioTrack>, startIndex: Int = 0, source: PlaylistSource = PlaylistSource.Unknown) {
        val controller = controller ?: return
        originalPlaylist = tracks
        _playlistSource.value = source
        
        val finalTracks = if (_shuffleMode.value && tracks.size > 1) {
            val current = tracks.getOrNull(startIndex)
            val others = tracks.toMutableList().apply { if (startIndex in indices) removeAt(startIndex) }
            others.shuffle()
            if (current != null) listOf(current) + others else others
        } else {
            tracks
        }
        
        currentPlaylist = finalTracks
        _queue.value = finalTracks
        
        val mediaItems = finalTracks.map { it.toMediaItem() }
        val finalStartIndex = if (_shuffleMode.value) 0 else startIndex
        
        controller.setMediaItems(mediaItems, finalStartIndex, 0L)
        controller.prepare()
        controller.play()
        savePlaybackState()

        // If it's a dynamic source, start fetching the rest of the playlist in background
        if (source !is PlaylistSource.LocalAudio && source !is PlaylistSource.Unknown) {
            fetchAllTracks(source)
        }
    }

    private var fetchJob: Job? = null
    private fun fetchAllTracks(source: PlaylistSource) {
        fetchJob?.cancel()
        fetchJob = scope.launch(Dispatchers.IO) {
            var offset = originalPlaylist.size
            val pageSize = 500 // Efficient loading
            var hasMore = true

            while (hasMore) {
                val result = when (source) {
                    is PlaylistSource.UserAudio -> musicRepository.loadMyAudio(source.userId, offset, pageSize)
                    is PlaylistSource.PlaylistAudio -> musicRepository.loadPlaylistTracks(source.ownerId, source.playlistId, offset, pageSize)
                    is PlaylistSource.SearchAudio -> musicRepository.searchAudio(source.query, offset, pageSize)
                    else -> Result.failure(Exception("Unsupported source for background fetch"))
                }

                result.onSuccess { newTracks ->
                    if (newTracks.isEmpty()) {
                        hasMore = false
                    } else {
                        offset += newTracks.size
                        // Update original playlist
                        originalPlaylist = originalPlaylist + newTracks
                        
                        // If shuffle is OFF, we can append to controller right away
                        // If shuffle is ON, we should probably add them to the "shuffled" part of the queue
                        launch(Dispatchers.Main) {
                            appendTracksToController(newTracks)
                        }
                    }
                }.onFailure {
                    hasMore = false
                }
                
                if (hasMore) delay(1000) // Rate limit
            }
        }
    }

    private fun appendTracksToController(newTracks: List<AudioTrack>) {
        val controller = controller ?: return
        
        if (_shuffleMode.value) {
            // Add to original playlist is already done.
            // For current (shuffled) playlist, we just append them shuffled at the end
            val shuffledNew = newTracks.shuffled()
            currentPlaylist = currentPlaylist + shuffledNew
            _queue.value = currentPlaylist
            controller.addMediaItems(shuffledNew.map { it.toMediaItem() })
        } else {
            currentPlaylist = currentPlaylist + newTracks
            _queue.value = currentPlaylist
            controller.addMediaItems(newTracks.map { it.toMediaItem() })
        }
    }

    private fun handleMediaTransition() {
        val controller = controller ?: return
        val currentIndex = controller.currentMediaItemIndex
        val totalItems = controller.mediaItemCount

        // 1. Preload next tracks
        val nextTracks = mutableListOf<AudioTrack>()
        for (i in 1..3) {
            val nextIndex = currentIndex + i
            if (nextIndex < totalItems) {
                currentPlaylist.getOrNull(nextIndex)?.let { nextTracks.add(it) }
            } else if (_repeatMode.value == Player.REPEAT_MODE_ALL && totalItems > 0) {
                currentPlaylist.getOrNull(nextIndex % totalItems)?.let { nextTracks.add(it) }
            }
        }
        if (nextTracks.isNotEmpty()) {
            OpenVKMatchaApp.instance.preloadManager.preload(nextTracks)
        }

        // 2. Load more tracks if we're near the end (fallback if fetchAllTracks isn't finished or failed)
        // Actually fetchAllTracks handles most of it now, but we might want to check for errors here
    }

    fun playPause() {
        val controller = controller ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0, 0)
            }
            controller.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun resume() {
        controller?.play()
    }

    fun seekTo(position: Long) {
        val controller = controller ?: return
        lastSeekTime = System.currentTimeMillis()
        controller.seekTo(position)
        _currentPosition.value = position
        savePlaybackState()
    }

    fun skipToNext() {
        val controller = controller ?: return
        controller.seekToNext()
        controller.play()
    }

    fun skipToPrevious() {
        val controller = controller ?: return
        controller.seekToPreviousMediaItem()
        controller.play()
    }

    fun toggleShuffle() {
        val controller = controller ?: return
        val currentShuffle = _shuffleMode.value
        val newShuffle = !currentShuffle
        
        scope.launch {
            isInternalUpdate = true
            
            if (newShuffle) {
                // Smart Shuffle: Shuffle the queue without resetting current item
                val currentIndex = controller.currentMediaItemIndex
                val totalItems = controller.mediaItemCount
                
                if (totalItems > 1) {
                    val (newTrackOrder, otherMediaItems) = withContext(Dispatchers.Default) {
                        val currentTrack = currentPlaylist.getOrNull(currentIndex)
                        val otherTracks = currentPlaylist.toMutableList().apply { 
                            if (currentIndex in indices) removeAt(currentIndex) 
                        }
                        otherTracks.shuffle()
                        
                        val order = if (currentTrack != null) {
                            listOf(currentTrack) + otherTracks
                        } else {
                            otherTracks
                        }
                        
                        // Heavy MediaItem creation in background
                        val items = otherTracks.map { it.toMediaItem() }
                        Pair(order, items)
                    }
                    
                    // Update UI state immediately
                    currentPlaylist = newTrackOrder
                    _queue.value = newTrackOrder
                    
                    if (currentIndex != 0) {
                        controller.moveMediaItem(currentIndex, 0)
                    }
                    controller.removeMediaItems(1, totalItems)
                    controller.addMediaItems(otherMediaItems)
                }
            } else {
                // Restore original order
                if (originalPlaylist.isNotEmpty()) {
                    val currentTrackId = _currentTrack.value?.stableId
                    val currentIndexInOriginal = originalPlaylist.indexOfFirst { it.stableId == currentTrackId }
                    
                    if (currentIndexInOriginal != -1) {
                        val totalItems = controller.mediaItemCount
                        val currentControllerIndex = controller.currentMediaItemIndex
                        
                        val (beforeItems, afterItems) = withContext(Dispatchers.Default) {
                            val before = if (currentIndexInOriginal > 0) {
                                originalPlaylist.subList(0, currentIndexInOriginal).map { it.toMediaItem() }
                            } else emptyList()
                            
                            val after = if (currentIndexInOriginal < originalPlaylist.size - 1) {
                                originalPlaylist.subList(currentIndexInOriginal + 1, originalPlaylist.size).map { it.toMediaItem() }
                            } else emptyList()
                            
                            Pair(before, after)
                        }

                        // Update UI state
                        currentPlaylist = originalPlaylist
                        _queue.value = originalPlaylist
                        
                        // Batch update the controller
                        if (currentControllerIndex != 0) {
                            controller.moveMediaItem(currentControllerIndex, 0)
                        }
                        controller.removeMediaItems(1, totalItems)
                        
                        if (beforeItems.isNotEmpty()) {
                            controller.addMediaItems(0, beforeItems)
                        }
                        if (afterItems.isNotEmpty()) {
                            controller.addMediaItems(afterItems)
                        }
                    }
                }
            }
            
            isInternalUpdate = false
            _shuffleMode.value = newShuffle
            playerPrefs?.edit()?.putBoolean("shuffle_mode", newShuffle)?.apply()
            savePlaybackState()
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val controller = controller ?: return
        
        // Update local lists immediately for smooth UI
        val newList = currentPlaylist.toMutableList()
        if (fromIndex in newList.indices && toIndex in newList.indices) {
            val item = newList.removeAt(fromIndex)
            newList.add(toIndex, item)
            currentPlaylist = newList
            _queue.value = newList
        }

        controller.moveMediaItem(fromIndex, toIndex)
        
        // Also update our internal track of original order if we want to maintain consistency
        if (!_shuffleMode.value) {
            val originalList = originalPlaylist.toMutableList()
            if (fromIndex in originalList.indices && toIndex in originalList.indices) {
                val item = originalList.removeAt(fromIndex)
                originalList.add(toIndex, item)
                originalPlaylist = originalList
            }
        }
        savePlaybackState()
    }

    fun skipToQueueItem(index: Int) {
        val controller = controller ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    fun toggleRepeat() {
        val controller = controller ?: return
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun addToQueue(track: AudioTrack) {
        val controller = controller ?: return
        val mediaItem = track.toMediaItem()
        controller.addMediaItem(mediaItem)
        
        // Add to both lists for consistency
        currentPlaylist = currentPlaylist + track
        originalPlaylist = originalPlaylist + track
        _queue.value = currentPlaylist
        savePlaybackState()
    }

    fun playNext(track: AudioTrack) {
        val controller = controller ?: return
        val mediaItem = track.toMediaItem()
        val nextIndex = if (controller.mediaItemCount > 0) controller.currentMediaItemIndex + 1 else 0
        controller.addMediaItem(nextIndex, mediaItem)
        
        // Update current playlist (the one visible in UI)
        val newList = currentPlaylist.toMutableList()
        if (nextIndex >= 0 && nextIndex <= newList.size) {
            newList.add(nextIndex, track)
        } else {
            newList.add(track)
        }
        currentPlaylist = newList
        _queue.value = newList
        
        // Update original playlist (add after current track's original position)
        val currentTrackId = _currentTrack.value?.stableId
        val originalIndex = originalPlaylist.indexOfFirst { it.stableId == currentTrackId }
        val newOriginal = originalPlaylist.toMutableList()
        if (originalIndex != -1) {
            newOriginal.add(originalIndex + 1, track)
        } else {
            newOriginal.add(track)
        }
        originalPlaylist = newOriginal
        
        savePlaybackState()
    }

    fun removeFromQueue(index: Int) {
        val controller = controller ?: return
        val trackToRemove = currentPlaylist.getOrNull(index)
        controller.removeMediaItem(index)
        
        if (trackToRemove != null) {
            currentPlaylist = currentPlaylist.filterIndexed { i, _ -> i != index }
            originalPlaylist = originalPlaylist.filter { it.stableId != trackToRemove.stableId }
            _queue.value = currentPlaylist
        }
        
        savePlaybackState()
    }

    private fun AudioTrack.toMediaItem(): MediaItem {
        val enriched = OpenVKMatchaApp.instance.downloadRepository.enrichTrack(this)
        return MediaItem.Builder()
            .setMediaId(enriched.stableId)
            .setUri(enriched.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(enriched.title)
                    .setArtist(enriched.artist)
                    .build()
            )
            .setTag(enriched)
            .build()
    }

    fun release() {
        stopProgressUpdate()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
