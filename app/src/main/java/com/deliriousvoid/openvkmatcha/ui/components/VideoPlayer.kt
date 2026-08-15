package com.deliriousvoid.openvkmatcha.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.data.model.Video
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.LocalFullScreenVideoHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    video: Video,
    modifier: Modifier = Modifier,
    initiallyFullscreen: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val playerManager = OpenVKMatchaApp.instance.playerManager
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
    val pauseVideoOnScroll by settingsViewModel.pauseVideoOnScroll.collectAsState()
    
    val isInPipMode by AppEvents.isInPipMode.collectAsState()
    
    val isExternal = video.isExternal
    val activeVideo by AppEvents.activeVideo.collectAsState()
    val isThisVideoActive = activeVideo?.id == video.id
    
    var resolvedUrl by remember(video.id) { mutableStateOf<String?>(null) }
    var isResolving by remember(video.id) { mutableStateOf(false) }

    LaunchedEffect(video.id) {
        if (isExternal && video.playerUrl != null) {
            isResolving = true
            resolvedUrl = com.deliriousvoid.openvkmatcha.util.VideoResolver.resolveDirectUrl(video.playerUrl)
            isResolving = false
        }
    }

    val exoPlayer = remember(video.id, resolvedUrl) {
        if (isExternal && resolvedUrl == null) return@remember null
        val activePlayer = AppEvents.activeExoPlayer.value
        val activeVid = AppEvents.activeVideo.value
        if (activeVid?.id == video.id && activePlayer != null) {
            activePlayer
        } else {
            ExoPlayer.Builder(context)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()
                setAudioAttributes(audioAttributes, true)
                
                val urlToUse = if (isExternal) resolvedUrl else video.videoUrl
                urlToUse?.let {
                    val metadata = MediaMetadata.Builder()
                        .setTitle(video.title)
                        .setArtist(video.ownerName ?: "OpenVK Video")
                        .setArtworkUri(video.thumbnailUrl?.let { Uri.parse(it) })
                        .build()
                    val mediaItem = MediaItem.Builder()
                        .setUri(it)
                        .setMediaMetadata(metadata)
                        .build()
                    setMediaItem(mediaItem)
                    prepare()
                }
            }
        }
    }

    var isPlaying by remember(exoPlayer) { mutableStateOf(exoPlayer?.isPlaying ?: false) }
    var isStarted by remember(video.id, exoPlayer) { 
        mutableStateOf(exoPlayer?.playbackState?.let { it != Player.STATE_IDLE } ?: (AppEvents.activeVideo.value?.id == video.id)) 
    }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember(video.id, exoPlayer) { mutableLongStateOf(exoPlayer?.currentPosition ?: 0L) }
    var duration by remember(video.id, exoPlayer) { mutableLongStateOf(exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L) }
    var isFullscreen by rememberSaveable { mutableStateOf(initiallyFullscreen) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedSelector by remember { mutableStateOf(false) }
    var seekIndicator by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (!AppEvents.isInPipMode.value && !AppEvents.shouldEnterPip.value && !AppEvents.isVideoFloating.value) {
                    exoPlayer?.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(exoPlayer) {
        if (exoPlayer == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    playerManager.pause()
                    if (AppEvents.activeVideo.value?.id != video.id) {
                        AppEvents.setActiveVideo(context, video, exoPlayer)
                    }
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.contentDuration
                } else if (state == Player.STATE_ENDED) {
                    showControls = true
                }
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                currentPosition = newPosition.positionMs
            }
        }
        exoPlayer.addListener(listener)
        isPlaying = exoPlayer.isPlaying
        currentPosition = exoPlayer.currentPosition
        duration = exoPlayer.duration.coerceAtLeast(0L)

        onDispose {
            exoPlayer.removeListener(listener)
            
            val isCurrentlyPlaying = exoPlayer.isPlaying
            val isGlobalFullScreen = AppEvents.isFullScreenOpened.value

            if (!initiallyFullscreen && !isGlobalFullScreen && !AppEvents.isInPipMode.value && !AppEvents.shouldEnterPip.value) {
                if (isCurrentlyPlaying) {
                    if (pauseVideoOnScroll) {
                        exoPlayer.pause()
                    } else {
                        AppEvents.setVideoFloating(true)
                    }
                } else {
                    if (AppEvents.activeVideo.value?.id != video.id) {
                        exoPlayer.release()
                    }
                }
            }
        }
    }

    val fullScreenHandler = LocalFullScreenVideoHandler.current

    LaunchedEffect(playbackSpeed) {
        exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    val musicIsPlaying by playerManager.isPlaying.collectAsState()
    LaunchedEffect(musicIsPlaying) {
        if (musicIsPlaying && isPlaying) {
            exoPlayer?.pause()
        }
    }


    LaunchedEffect(isPlaying) {
        if (isPlaying && exoPlayer != null) {
            while (true) {
                currentPosition = exoPlayer.currentPosition
                delay(500)
            }
        }
    }

    LaunchedEffect(showControls, isPlaying, showSpeedSelector) {
        if (showControls && isPlaying && !showSpeedSelector) {
            delay(3000)
            showControls = false
        }
    }
    
    val scope = rememberCoroutineScope()
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    val isGlobalFullScreen by AppEvents.isFullScreenOpened.collectAsState()
    val isVideoFloating by AppEvents.isVideoFloating.collectAsState()

    val playerContent = @Composable { isFull: Boolean ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(isFull) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = if (isFull) { offset ->
                            val isForward = offset.x > size.width / 2
                            val seekAmount = 10000L
                            val newPos = if (isForward) {
                                ((exoPlayer?.currentPosition ?: 0L) + seekAmount).coerceAtMost(duration)
                            } else {
                                ((exoPlayer?.currentPosition ?: 0L) - seekAmount).coerceAtLeast(0)
                            }
                            exoPlayer?.seekTo(newPos)
                            currentPosition = newPos

                            seekIndicator = seekAmount / 1000 to isForward
                            scope.launch {
                                delay(600)
                                seekIndicator = null
                            }
                        } else null
                    )
                }
        ) {
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else if (isExternal && resolvedUrl == null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Не удалось загрузить видео",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = {
                        video.playerUrl?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }) {
                        Text("Открыть в браузере")
                    }
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        val view = LayoutInflater.from(ctx).inflate(R.layout.matcha_player_view, null) as PlayerView
                        view.apply {
                            val shouldShowPlayer = lifecycleState.isAtLeast(Lifecycle.State.STARTED) && 
                                !isInPipMode && !isVideoFloating && !(isGlobalFullScreen && !isFull)
                            player = if (shouldShowPlayer) exoPlayer else null
                            setBackgroundColor(android.graphics.Color.BLACK)
                            setEnableComposeSurfaceSyncWorkaround(true)
                        }
                    },
                    update = {
                        val shouldShowPlayer = lifecycleState.isAtLeast(Lifecycle.State.STARTED) && 
                            !isInPipMode && !isVideoFloating && !(isGlobalFullScreen && !isFull)
                        it.player = if (shouldShowPlayer) exoPlayer else null
                    },
                    onRelease = {
                        it.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isFull && (resolvedUrl != null || !isExternal)) {
                seekIndicator?.let { (amount, isForward) ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp),
                        contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "${if (isForward) "+" else "-"}$amount сек",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                VideoControls(
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    isFullscreen = isFull,
                    playbackSpeed = playbackSpeed,
                    showSpeedSelector = showSpeedSelector,
                    onShowSpeedSelector = { showSpeedSelector = it },
                    onPlayPause = {
                        if (isPlaying) {
                            exoPlayer?.pause()
                        } else {
                            if (exoPlayer?.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                            }
                            exoPlayer?.play()
                        }
                    },
                    onSeek = {
                        exoPlayer?.seekTo(it)
                        currentPosition = it
                    },
                    onDownload = {
                        video.videoUrl?.let { url ->
                            val fileName = "video_${video.ownerId}_${video.id}.mp4"
                            OpenVKMatchaApp.instance.downloadRepository.downloadFile(url, fileName)
                        }
                    },
                    onToggleFullscreen = {
                        if (fullScreenHandler != null) {
                            fullScreenHandler(video, !isFull, exoPlayer)
                        } else {
                            isFullscreen = !isFullscreen
                            if (!isFullscreen && initiallyFullscreen) onDismiss?.invoke()
                        }
                    },
                    onPip = {
                        AppEvents.triggerEnterPip()
                    },
                    onSetSpeed = { playbackSpeed = it },
                    isExternal = isExternal && resolvedUrl == null,
                    canDownload = !video.videoUrl.isNullOrBlank(),
                    isYouTube = com.deliriousvoid.openvkmatcha.util.VideoResolver.isYouTube(video.playerUrl),
                    hasPlayerUrl = video.playerUrl != null,
                    onOpenExternal = {
                        video.playerUrl?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }

    Box(
        modifier = if (isFullscreen) {
            Modifier.fillMaxSize().background(Color.Black)
        } else {
            modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .background(Color.Black)
        }
    ) {
        if (isThisVideoActive && (isInPipMode || isVideoFloating)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isVideoFloating) "Видео воспроизводится в плавающем окне" else "Видео воспроизводится в отдельном окне",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    TextButton(onClick = {
                        if (isVideoFloating) {
                            AppEvents.setVideoFloating(false)
                        } else {
                            AppEvents.setInPipMode(false)
                        }
                    }) {
                        Text("Вернуть")
                    }
                }
            }
        } else if (!isStarted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        isStarted = true
                        exoPlayer?.play()
                    }
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize()
                        )
                    }
                }
            }
        } else {
            playerContent(isFullscreen)
        }
    }

    if (isFullscreen && !isInPipMode && fullScreenHandler == null) {
        Dialog(
            onDismissRequest = { 
                isFullscreen = false
                if (initiallyFullscreen) onDismiss?.invoke()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            BackHandler {
                isFullscreen = false
                if (initiallyFullscreen) onDismiss?.invoke()
            }
            val view = LocalView.current
            val activity = context.findActivity()
            
            SideEffect {
                var parent = view.parent
                while (parent != null && parent !is DialogWindowProvider) {
                    parent = parent.parent
                }
                val window = (parent as? DialogWindowProvider)?.window ?: activity?.window
                if (window != null) {
                    val controller = WindowInsetsControllerCompat(window, view)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                playerContent(true)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PipPlayerView() {
    val exoPlayer by AppEvents.activeExoPlayer.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        exoPlayer?.let { player ->
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx).inflate(R.layout.matcha_player_view, null) as PlayerView
                    view.apply {
                        this.player = player
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setEnableComposeSurfaceSyncWorkaround(true)
                    }
                },
                update = {
                    it.player = player
                },
                onRelease = {
                    it.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun VideoControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isFullscreen: Boolean,
    playbackSpeed: Float,
    showSpeedSelector: Boolean,
    onShowSpeedSelector: (Boolean) -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onDownload: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onPip: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    isExternal: Boolean = false,
    canDownload: Boolean = true,
    isYouTube: Boolean = false,
    hasPlayerUrl: Boolean = false,
    onOpenExternal: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .then(
                if (showSpeedSelector) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onShowSpeedSelector(false) }
                } else Modifier
            )
    ) {
        if (showSpeedSelector) {
            // Speed Slider Panel
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume clicks */ },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Скорость воспроизведения: ${"%.2f".format(playbackSpeed)}x",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { onShowSpeedSelector(false) }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.50x", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${"%.2f".format(playbackSpeed)}x",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("2.00x", style = MaterialTheme.typography.labelMedium)
                }
                
                Slider(
                    value = playbackSpeed,
                    onValueChange = { 
                        val roundedSpeed = Math.round(it * 100f) / 100f
                        onSetSpeed(roundedSpeed)
                    },
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        FilterChip(
                            selected = (playbackSpeed == speed),
                            onClick = { 
                                val roundedSpeed = Math.round(speed * 100f) / 100f
                                onSetSpeed(roundedSpeed)
                            },
                            label = { Text("${"%.2f".format(speed)}x") }
                        )
                    }
                }
            }
        } else {
            // Top left back button (only if full)
            if (isFullscreen) {
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            }

            // Top right controls
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFullscreen && !isExternal) {
                    IconButton(onClick = onPip) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = "PiP",
                            tint = Color.White
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        var needsDivider = false
                        if (hasPlayerUrl) {
                            DropdownMenuItem(
                                text = { Text(if (isYouTube) "Смотреть на YouTube" else "Открыть в браузере") },
                                onClick = {
                                    showMenu = false
                                    onOpenExternal()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) }
                            )
                            needsDivider = true
                        }
                        
                        if (canDownload) {
                            if (needsDivider) HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Скачать") },
                                onClick = {
                                    showMenu = false
                                    onDownload()
                                },
                                leadingIcon = { Icon(Icons.Default.Download, null) }
                            )
                            needsDivider = true
                        }

                        if (isFullscreen && !isExternal) {
                            if (needsDivider) HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Скорость воспроизведения") },
                                onClick = {
                                    showMenu = false
                                    onShowSpeedSelector(true)
                                },
                                leadingIcon = { Icon(Icons.Default.Speed, null) },
                                trailingIcon = { Text("${"%.2f".format(playbackSpeed)}x", style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
            }

            // Center Play/Pause
            if (!isExternal) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Bottom Bar
            if (!isExternal) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            } else {
                // For external videos, only show the fullscreen toggle if needed, or just let them use internal controls
                // But we still need a way to exit fullscreen if we are in it
                if (isFullscreen) {
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
