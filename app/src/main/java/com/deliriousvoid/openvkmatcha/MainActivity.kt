package com.deliriousvoid.openvkmatcha

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.Player
import com.deliriousvoid.openvkmatcha.data.model.Video
import com.deliriousvoid.openvkmatcha.ui.components.FullPlayer
import com.deliriousvoid.openvkmatcha.ui.components.MiniPlayer
import com.deliriousvoid.openvkmatcha.ui.components.PipPlayerView
import com.deliriousvoid.openvkmatcha.ui.components.VideoPlayer
import com.deliriousvoid.openvkmatcha.ui.navigation.AppNavigation
import com.deliriousvoid.openvkmatcha.ui.navigation.MainTab
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.ui.theme.OpenVKMatchaTheme
import com.deliriousvoid.openvkmatcha.ui.util.LinkHandler
import com.deliriousvoid.openvkmatcha.ui.viewmodel.*
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.SearchCategory
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    companion object {
        const val ACTION_OPEN_PLAYER = "com.deliriousvoid.openvkmatcha.ACTION_OPEN_PLAYER"
        const val ACTION_OPEN_VIDEO_PLAYER = "com.deliriousvoid.openvkmatcha.ACTION_OPEN_VIDEO_PLAYER"
        private const val ACTION_PIP_PLAY = "com.deliriousvoid.openvkmatcha.ACTION_PIP_PLAY"
        private const val ACTION_PIP_PAUSE = "com.deliriousvoid.openvkmatcha.ACTION_PIP_PAUSE"
    }

    private var openPlayerTrigger by mutableStateOf(value = false)
    private var deepLinkRoute by mutableStateOf<String?>(null)

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val player = AppEvents.activeExoPlayer.value ?: return
            when (intent.action) {
                ACTION_PIP_PLAY -> player.play()
                ACTION_PIP_PAUSE -> player.pause()
            }
            updatePipParams()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        AppEvents.setInPipMode(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PIP_PLAY)
                addAction(ACTION_PIP_PAUSE)
            }
            ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
            updatePipParams()
        } else {
            try {
                unregisterReceiver(pipReceiver)
            } catch (_: Exception) {}
        }
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val player = AppEvents.activeExoPlayer.value
            val isPlaying = player?.isPlaying == true
            
            val actions = mutableListOf<RemoteAction>()
            
            val playPauseAction = if (isPlaying) {
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                    "Пауза", "Пауза",
                    PendingIntent.getBroadcast(this, 1, Intent(ACTION_PIP_PAUSE), PendingIntent.FLAG_IMMUTABLE),
                )
            } else {
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_play),
                    "Воспроизвести", "Воспроизвести",
                    PendingIntent.getBroadcast(this, 2, Intent(ACTION_PIP_PLAY), PendingIntent.FLAG_IMMUTABLE),
                )
            }
            actions.add(playPauseAction)

            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(actions)
                .build()
            setPictureInPictureParams(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val activeVideo = AppEvents.activeVideo.value
        if (activeVideo != null) {
            AppEvents.triggerEnterPip()
        }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updatePipParams()
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onStop() {
        super.onStop()
        AppEvents.releaseActivePlayer(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val data: Uri? = intent.data
        data?.toString()?.let { LinkHandler.getRouteForUrl(it) }?.let { route ->
            deepLinkRoute = route
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppEvents.releaseActivePlayer(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
            val theme by settingsViewModel.theme.collectAsState()
            val accent by settingsViewModel.accent.collectAsState()

            val isInPipMode by AppEvents.isInPipMode.collectAsState()
            var activeVideoToPlay by remember { mutableStateOf<Video?>(null) }
            var showFullPlayer by remember { mutableStateOf(value = false) }

            OpenVKMatchaTheme(theme = theme, accent = accent) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = if (isInPipMode) 0f else 1f },
                    ) {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        val isMainScreen = currentRoute == Routes.MAIN
                        val isPlaylistDetails = currentRoute?.startsWith("playlist/") == true
                        val isChat = currentRoute?.startsWith("chat/") == true
                        val isComments = currentRoute?.startsWith("comments/") == true
                        val isNotifications = currentRoute == Routes.NOTIFICATIONS
                        val isUpload = currentRoute == Routes.UPLOAD_AUDIO
                        val isProfile = currentRoute?.startsWith("profile/") == true
                        val isFriends = currentRoute?.startsWith("friends/") == true
                        val isGroups = currentRoute?.startsWith("groups/") == true
                        val isGifts = currentRoute?.startsWith("gifts/") == true
                        val isUserMusic = currentRoute?.startsWith("user_music/") == true
                        val isFollowers = currentRoute?.startsWith("followers/") == true
                        val isSearch = currentRoute == Routes.SEARCH
                        val isTopics = currentRoute?.startsWith("topics/") == true
                        val isTopicComments = currentRoute?.startsWith("topic_comments/") == true
                        val isSendGift = currentRoute?.startsWith("send_gift") == true
                        val isCreatePost = currentRoute?.startsWith("create_post/") == true
                        val isVideos = currentRoute?.startsWith("videos/") == true
                        val isDocuments = currentRoute?.startsWith("documents/") == true
                        val isNotes = currentRoute?.startsWith("notes/") == true
                        val isNoteDetails = currentRoute?.startsWith("note/") == true
                        val isEvents = currentRoute?.startsWith("events/") == true
                        val isWebView = currentRoute?.startsWith("webview") == true
                        val isTransfer = currentRoute == Routes.TRANSFER
                        val isEditProfile = currentRoute == Routes.EDIT_PROFILE
                        val isEditGroup = currentRoute?.startsWith("edit_group/") == true
                        val isPhotoAlbums = currentRoute?.startsWith("photo_albums/") == true
                        val isPhotos = currentRoute?.startsWith("photos/") == true
                        val isQr = (currentRoute?.startsWith("qr_display/") == true) || (currentRoute == Routes.QR_SCANNER)

                        val topBarState by AppEvents.topBarState.collectAsState()
                        val showOuterTopBar = (currentRoute != Routes.SPLASH) && (currentRoute != Routes.LOGIN) && (currentRoute != Routes.GRAFFITI) && !isQr

                        LaunchedEffect(deepLinkRoute, currentRoute) {
                            if ((deepLinkRoute != null) && (currentRoute != null) && (currentRoute != Routes.SPLASH)) {
                                navController.navigate(deepLinkRoute!!)
                                deepLinkRoute = null
                            }
                        }

                        var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
                        var showCreatePlaylistDialog by remember { mutableStateOf(value = false) }
                        val coroutineScope = rememberCoroutineScope()
                        val snackbarHostState = remember { SnackbarHostState() }

                        LaunchedEffect(Unit) {
                            AppEvents.snackbarMessage.collect { message ->
                                snackbarHostState.showSnackbar(message)
                            }
                        }

                        val shouldEnterPip by AppEvents.shouldEnterPip.collectAsState()
                        LaunchedEffect(shouldEnterPip) {
                            if (shouldEnterPip) {
                                enterPip()
                            }
                        }

                        val activeExoPlayer by AppEvents.activeExoPlayer.collectAsState()
                        LaunchedEffect(activeExoPlayer, isInPipMode) {
                            val player = activeExoPlayer
                            if ((isInPipMode) && (player != null)) {
                                val listener = object : Player.Listener {
                                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                                        updatePipParams()
                                    }
                                }
                                player.addListener(listener)
                                updatePipParams()
                                try {
                                    kotlinx.coroutines.awaitCancellation()
                                } finally {
                                    player.removeListener(listener)
                                }
                            }
                        }

                        LaunchedEffect(intent.action, openPlayerTrigger) {
                            if (intent.action == ACTION_OPEN_PLAYER || openPlayerTrigger) {
                                showFullPlayer = true
                                openPlayerTrigger = false
                                intent.action = null
                            } else if (intent.action == ACTION_OPEN_VIDEO_PLAYER) {
                                activeVideoToPlay = AppEvents.activeVideo.value
                                intent.action = null
                            }
                        }

                        val playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory())
                        val musicViewModel: MusicViewModel = viewModel(factory = MusicViewModel.factory())
                        val feedViewModel: FeedViewModel = viewModel(factory = FeedViewModel.factory())
                        val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory())
                        val notificationsViewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.factory())

                        val isOfflineMode by settingsViewModel.offlineMode.collectAsState()

                        LaunchedEffect(isOfflineMode, currentRoute) {
                            if (isOfflineMode) {
                                selectedTab = MainTab.Music
                                musicViewModel.setMode(MusicMode.Downloaded)
                                if (currentRoute != Routes.MAIN && currentRoute != Routes.SETTINGS && currentRoute?.startsWith("settings/") != true) {
                                    navController.navigate(Routes.MAIN) {
                                        popUpTo(Routes.MAIN) { inclusive = true }
                                    }
                                }
                            }
                        }

                        val lifecycleOwner = LocalLifecycleOwner.current
                        LaunchedEffect(lifecycleOwner) {
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                    coroutineScope.launch {
                                        AppEvents.emitRefreshNotifications()
                                    }
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            try {
                                kotlinx.coroutines.awaitCancellation()
                            } finally {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                            }
                        }

                        val currentTrack by playerViewModel.currentTrack.collectAsState()
                        val isPlaying by playerViewModel.isPlaying.collectAsState()
                        val playbackState by playerViewModel.playbackState.collectAsState()

                        @Suppress("DEPRECATION")
                        val clipboardManager = LocalClipboardManager.current
                        val profileState by profileViewModel.uiState.collectAsState()
                        val currentUserId = profileState.currentUserId

                        val isBuffering = playbackState == Player.STATE_BUFFERING
                        val repeatMode by playerViewModel.repeatMode.collectAsState()
                        val shuffleMode by playerViewModel.shuffleMode.collectAsState()
                        val queue by playerViewModel.queue.collectAsState()
                        val hidePlayedTracks by settingsViewModel.hidePlayedTracks.collectAsState()

                        val musicState by musicViewModel.uiState.collectAsState()

                        var previousRoute by remember { mutableStateOf<String?>(null) }
                        var isSearchActive by rememberSaveable { mutableStateOf(false) }
                        var showCategoryMenu by remember { mutableStateOf(false) }
                        val focusRequester = remember { FocusRequester() }
                        val keyboardController = LocalSoftwareKeyboardController.current
                        val globalSearchQuery by AppEvents.searchQuery.collectAsState()
                        val globalSearchCategory by AppEvents.searchCategory.collectAsState()

                        LaunchedEffect(selectedTab) { isSearchActive = false }
                        LaunchedEffect(isSearchActive, currentRoute) {
                            if (isSearchActive || currentRoute == Routes.SEARCH) focusRequester.requestFocus()
                        }
                        LaunchedEffect(currentRoute) {
                            if (currentRoute == Routes.MAIN && previousRoute == Routes.UPLOAD_AUDIO) {
                                musicViewModel.loadMusic(isRefresh = true, isManual = false)
                            }
                            previousRoute = currentRoute
                        }

                        if (showFullPlayer) {
                            FullPlayer(
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                isBuffering = isBuffering,
                                repeatMode = repeatMode,
                                shuffleMode = shuffleMode,
                                queue = queue,
                                hidePlayedTracks = hidePlayedTracks,
                                playerViewModel = playerViewModel,
                                onPlayPause = { playerViewModel.playPause() },
                                onNext = { playerViewModel.skipToNext() },
                                onPrevious = { playerViewModel.skipToPrevious() },
                                onToggleRepeat = { playerViewModel.toggleRepeat() },
                                onToggleShuffle = { playerViewModel.toggleShuffle() },
                                onSeek = { playerViewModel.seekTo(it) },
                                onToggleAdded = { currentTrack?.let { playerViewModel.toggleTrackAdded(it) } },
                                onRemoveFromQueue = { playerViewModel.removeFromQueue(it) },
                                onMoveInQueue = { from, to -> playerViewModel.moveItemInQueue(from, to) },
                                onShare = {
                                    val shareUrl = currentTrack?.remoteUrl ?: currentTrack?.url ?: ""
                                    if (shareUrl.isNotEmpty()) clipboardManager.setText(AnnotatedString(shareUrl))
                                },
                                onSkipToQueueItem = { playerViewModel.skipToQueueItem(it) },
                            ) { showFullPlayer = false }
                        }

                        activeVideoToPlay?.let { video ->
                            VideoPlayer(
                                video = video,
                                initiallyFullscreen = true,
                            ) { activeVideoToPlay = null }
                        }

                        Scaffold(
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                            topBar = {
                                if (showOuterTopBar) {
                                    if (topBarState?.customTopBar != null) {
                                        topBarState?.customTopBar?.invoke()
                                    } else {
                                        CenterAlignedTopAppBar(
                                            title = {
                                                val feedState by feedViewModel.uiState.collectAsState()
                                                var showFeedMenu by remember { mutableStateOf(false) }
                                                val customTitle by AppEvents.customTitle.collectAsState()

                                                if (topBarState?.customContent != null) {
                                                    topBarState?.customContent?.invoke()
                                                } else if (topBarState?.isSearchActive == true || (isSearchActive && ((isMainScreen && selectedTab == MainTab.Music) || isFriends || isGroups)) || isSearch) {
                                                    val placeholderText = when {
                                                        isSearch -> "Поиск ${globalSearchCategory.title.lowercase()}..."
                                                        isMainScreen && selectedTab == MainTab.Music -> if (musicState.mode == MusicMode.Tracks) "Поиск музыки..." else "Поиск плейлистов..."
                                                        isFriends -> "Поиск друзей..."
                                                        isGroups -> "Поиск сообществ..."
                                                        else -> "Поиск..."
                                                    }
                                                    
                                                    TextField(
                                                        value = globalSearchQuery,
                                                        onValueChange = { AppEvents.setSearchQuery(it) },
                                                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                                        placeholder = { Text(placeholderText) },
                                                        singleLine = true,
                                                        colors = TextFieldDefaults.colors(
                                                            focusedContainerColor = Color.Transparent,
                                                            unfocusedContainerColor = Color.Transparent,
                                                            disabledContainerColor = Color.Transparent,
                                                            focusedIndicatorColor = Color.Transparent,
                                                            unfocusedIndicatorColor = Color.Transparent,
                                                        ),
                                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                                                    )
                                                } else {
                                                    val titleStr = when {
                                                        topBarState?.title != null -> topBarState!!.title
                                                        customTitle != null -> customTitle!!
                                                        isMainScreen -> {
                                                            if (selectedTab == MainTab.Home) {
                                                                val currentTitle = if (feedState.feedType == FeedType.GLOBAL) "OpenVK" else "Подписки"
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.clickable { showFeedMenu = true }
                                                                ) {
                                                                    Text(currentTitle, maxLines = 1, modifier = Modifier.basicMarquee())
                                                                    Icon(Icons.Default.ArrowDropDown, null)
                                                                    DropdownMenu(expanded = showFeedMenu, onDismissRequest = { showFeedMenu = false }) {
                                                                        DropdownMenuItem(
                                                                            text = { Text("Глобальная") },
                                                                            onClick = { feedViewModel.setFeedType(FeedType.GLOBAL); showFeedMenu = false },
                                                                            trailingIcon = { if (feedState.feedType == FeedType.GLOBAL) Icon(Icons.Default.Check, null) }
                                                                        )
                                                                        DropdownMenuItem(
                                                                            text = { Text("Подписки") },
                                                                            onClick = { feedViewModel.setFeedType(FeedType.SUBSCRIPTIONS); showFeedMenu = false },
                                                                            trailingIcon = { if (feedState.feedType == FeedType.SUBSCRIPTIONS) Icon(Icons.Default.Check, null) }
                                                                        )
                                                                    }
                                                                }
                                                                ""
                                                            } else selectedTab.title
                                                        }
                                                        isPlaylistDetails -> navBackStackEntry?.arguments?.getString("title") ?: "Плейлист"
                                                        isChat -> navBackStackEntry?.arguments?.getString("title") ?: "Чат"
                                                        isNotifications && !isOfflineMode -> "Ответы"
                                                        isComments -> "Комментарии"
                                                        currentRoute == Routes.SETTINGS -> "Настройки"
                                                        currentRoute == Routes.SETTINGS_GENERAL -> "Основные"
                                                        currentRoute == Routes.SETTINGS_APPEARANCE -> "Внешний вид"
                                                        currentRoute == Routes.SETTINGS_MUSIC -> "Музыка"
                                                        currentRoute == Routes.SETTINGS_DEVELOPER -> "Для разработчика"
                                                        currentRoute == Routes.SETTINGS_ABOUT_INSTANCE -> "Об инстанции"
                                                        isUpload -> "Загрузка музыки"
                                                        isProfile -> "Профиль"
                                                        isFriends -> {
                                                            val name = navBackStackEntry?.arguments?.getString("name") ?: ""
                                                            if (name.isNotEmpty()) "Друзья $name" else "Друзья"
                                                        }
                                                        isGroups -> {
                                                            val name = navBackStackEntry?.arguments?.getString("name") ?: ""
                                                            if (name.isNotEmpty()) "Сообщества $name" else "Сообщества"
                                                        }
                                                        isGifts -> {
                                                            val name = navBackStackEntry?.arguments?.getString("name") ?: ""
                                                            if (name.isNotEmpty()) "Подарки $name" else "Подарки"
                                                        }
                                                        isUserMusic -> {
                                                            val name = navBackStackEntry?.arguments?.getString("name") ?: ""
                                                            "Музыка $name"
                                                        }
                                                        currentRoute == Routes.SETTINGS_IGNORED -> "Игнорируемые"
                                                        isFollowers -> {
                                                            val name = navBackStackEntry?.arguments?.getString("name") ?: ""
                                                            val isGroup = navBackStackEntry?.arguments?.getBoolean("isGroup") ?: false
                                                            val label = if (isGroup) "Участники" else "Подписчики"
                                                            if (name.isNotEmpty()) "$label $name" else label
                                                        }
                                                        isTopics -> "Обсуждения"
                                                        isTopicComments -> navBackStackEntry?.arguments?.getString("title") ?: "Обсуждение"
                                                        currentRoute?.startsWith("create_post/") == true -> "Новая запись"
                                                        isVideos -> "Видео"
                                                        isDocuments -> "Документы"
                                                        isNotes -> "Заметки"
                                                        isNoteDetails -> "Заметка"
                                                        isEvents -> "События"
                                                        isWebView -> navBackStackEntry?.arguments?.getString("title") ?: ""
                                                        isTransfer -> "Перевод"
                                                        isPhotoAlbums -> {
                                                            val name = navBackStackEntry?.arguments?.getString("name") ?: ""
                                                            if (name.isNotEmpty()) "Альбомы $name" else "Альбомы"
                                                        }
                                                        isPhotos -> navBackStackEntry?.arguments?.getString("title") ?: "Фото"
                                                        else -> ""
                                                    }
                                                    if (titleStr.isNotEmpty()) Text(titleStr, maxLines = 1, modifier = Modifier.basicMarquee())
                                                }
                                            },
                                            navigationIcon = {
                                                if (topBarState?.navigationIcon != null) {
                                                    topBarState?.navigationIcon?.invoke()
                                                } else {
                                                    val qrData by AppEvents.currentQrData.collectAsState()
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (isSearch || (isSearchActive && ((isMainScreen && selectedTab == MainTab.Music) || isFriends || isGroups))) {
                                                            IconButton(
                                                                onClick = { 
                                                                    if (isSearch) {
                                                                        navController.popBackStack()
                                                                    } else {
                                                                        if (globalSearchQuery.isEmpty() && (isFriends || isGroups)) navController.popBackStack()
                                                                        isSearchActive = false
                                                                    }
                                                                    AppEvents.setSearchQuery("")
                                                                },
                                                            ) {
                                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Закрыть поиск")
                                                            }
                                                        } else if (((isMainScreen && selectedTab == MainTab.Music) || isFriends || isGroups) && !isSearchActive && !isOfflineMode) {
                                                            if (isFriends || isGroups) {
                                                                IconButton(onClick = { navController.popBackStack() }) {
                                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                                                                }
                                                            }
                                                            IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Default.Search, "Поиск") }
                                                        } else if (isMainScreen && selectedTab == MainTab.Explore && !isOfflineMode) {
                                                            IconButton(onClick = { navController.navigate(Routes.SEARCH) }) { Icon(Icons.Default.Search, "Поиск") }
                                                            IconButton(onClick = { navController.navigate(Routes.qrScannerRoute()) }) { Icon(Icons.Default.QrCodeScanner, "Сканировать QR") }
                                                        } else if (isProfile || (isMainScreen && selectedTab == MainTab.Profile)) {
                                                            if (isProfile) {
                                                                IconButton(onClick = { navController.popBackStack() }) {
                                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                                                                }
                                                            }
                                                            IconButton(onClick = {
                                                                qrData?.let { navController.navigate(Routes.qrDisplayRoute(it.url, it.title, it.avatarUrl)) }
                                                            }) { Icon(Icons.Default.QrCode, "Показать QR") }
                                                        } else if (!isMainScreen) {
                                                            IconButton(onClick = { 
                                                                if (isUpload) musicViewModel.loadMusic()
                                                                navController.popBackStack() 
                                                            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                                                        }
                                                    }
                                                }
                                            },
                                            actions = {
                                                if (topBarState?.actions != null) {
                                                    topBarState?.actions?.invoke(this)
                                                } else {
                                                    if (isSearch) {
                                                        Box {
                                                            IconButton(onClick = { showCategoryMenu = true }) { Icon(Icons.Filled.Tune, "Категория поиска") }
                                                            DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                                                                for (cat in SearchCategory.entries) {
                                                                    DropdownMenuItem(
                                                                        text = { Text(cat.title) },
                                                                        onClick = { AppEvents.setSearchCategory(cat); showCategoryMenu = false },
                                                                        trailingIcon = { if (globalSearchCategory == cat) Icon(Icons.Default.Check, null) }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if ((isSearch || (isSearchActive && ((isMainScreen && selectedTab == MainTab.Music) || isFriends || isGroups))) && globalSearchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { AppEvents.setSearchQuery("") }) { Icon(Icons.Default.Clear, "Очистить") }
                                                    }
                                                    if (isMainScreen || isProfile || isFriends || isGroups || isUserMusic || isGifts || isFollowers || isTopics || isTopicComments) {
                                                        if (isMainScreen && !isOfflineMode) {
                                                            val notificationsState by notificationsViewModel.uiState.collectAsState()
                                                            val unreadCount = notificationsState.unreadCount
                                                            IconButton(onClick = { 
                                                                notificationsViewModel.setArchive(unreadCount == 0)
                                                                navController.navigate(Routes.NOTIFICATIONS) 
                                                            }) {
                                                                BadgedBox(badge = { if (unreadCount > 0) Badge(containerColor = Color.Red, contentColor = Color.White) { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) } }) {
                                                                    Icon(Icons.Default.Notifications, "Ответы")
                                                                }
                                                            }
                                                        }
                                                        IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) { Icon(Icons.Default.Settings, "Настройки") }
                                                        
                                                        var showMenu by remember { mutableStateOf(false) }
                                                        val context = LocalContext.current
                                                        val baseUrl = OpenVKMatchaApp.instance.api.baseUrl
                                                        if (isChat) {
                                                            Box {
                                                                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Ещё") }
                                                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                                                    val peerId = navBackStackEntry?.arguments?.getInt("peerId") ?: 0
                                                                    val currentUrl = "$baseUrl/im?sel=$peerId"
                                                                    DropdownMenuItem(text = { Text("Копировать ссылку") }, onClick = { showMenu = false; clipboardManager.setText(AnnotatedString(currentUrl)) })
                                                                    DropdownMenuItem(text = { Text("Открыть в браузере") }, onClick = { showMenu = false; context.startActivity(Intent(Intent.ACTION_VIEW, currentUrl.toUri())) })
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                                        )
                                    }
                                }
                            },
                            floatingActionButton = {
                                if (isMainScreen && selectedTab == MainTab.Music && !isOfflineMode) {
                                    val onClick = when (musicState.mode) {
                                        MusicMode.Tracks -> { { navController.navigate(Routes.UPLOAD_AUDIO) } }
                                        MusicMode.Playlists -> { { showCreatePlaylistDialog = true } }
                                        else -> null
                                    }
                                    onClick?.let { nonNullOnClick ->
                                        FloatingActionButton(
                                            onClick = nonNullOnClick,
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Icon(Icons.Default.Add, null)
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                val shouldShowNavBar = (isMainScreen || isSearch || isPlaylistDetails || isProfile || isFriends || isGroups || isUserMusic || isGifts || isFollowers || isSendGift || isCreatePost || isVideos || isDocuments || isNotes || isNoteDetails || isEvents || isWebView || isTransfer || isTopics || isTopicComments || isEditProfile || isEditGroup || isPhotoAlbums || isPhotos) && !isQr && currentRoute != Routes.GRAFFITI
                                val shouldShowPlayer = currentRoute != Routes.SPLASH && currentRoute != Routes.LOGIN && currentRoute != Routes.SETTINGS && currentRoute?.startsWith("settings/") != true && currentRoute != Routes.NOTIFICATIONS && currentRoute?.startsWith("comments/") != true && currentRoute?.startsWith("topic_comments/") != true && !isEditProfile && !isEditGroup && !isPhotoAlbums && !isPhotos && !isQr && currentRoute != Routes.GRAFFITI && currentTrack != null
                                
                                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                                    Column {
                                        if (shouldShowPlayer) {
                                            MiniPlayer(
                                                currentTrack = currentTrack,
                                                isPlaying = isPlaying,
                                                isBuffering = isBuffering,
                                                onPlayPause = { playerViewModel.playPause() },
                                                onToggleAdded = { currentTrack?.let { playerViewModel.toggleTrackAdded(it) } },
                                                onClick = { showFullPlayer = true }
                                            )
                                        }
                                        if (shouldShowNavBar) {
                                            NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                                                val navigationTabs by settingsViewModel.navigationTabs.collectAsState()
                                                val navBarLabelsVisible by settingsViewModel.navBarLabelsVisible.collectAsState()
                                                val visibleTabs = if (isOfflineMode) listOf(MainTab.Music) else navigationTabs
                                                visibleTabs.forEach { tab ->
                                                    val selected = (isMainScreen && selectedTab == tab) || 
                                                        (tab == MainTab.Music && (isPlaylistDetails || isUserMusic)) || 
                                                        (tab == MainTab.Profile && (isProfile || isGifts || isFollowers || isSendGift || isCreatePost || isTopics || isTopicComments)) || 
                                                        (tab == MainTab.Explore && (isVideos || isDocuments || isEvents || isWebView || isTransfer || isSearch)) ||
                                                        (tab == MainTab.Friends && isFriends) ||
                                                        (tab == MainTab.Groups && isGroups) ||
                                                        (tab == MainTab.Notes && (isNotes || isNoteDetails))

                                                    NavigationBarItem(
                                                        selected = selected,
                                                        onClick = {
                                                            if (isPlaylistDetails || isProfile || isFriends || isGroups || isUserMusic || isGifts || isFollowers || isSendGift || isCreatePost || isVideos || isDocuments || isNotes || isNoteDetails || isEvents || isWebView || isTransfer || isTopics || isTopicComments || isSearch) {
                                                                navController.popBackStack(Routes.MAIN, false)
                                                            }
                                                            isSearchActive = false
                                                            AppEvents.setSearchQuery("")
                                                            if (tab == MainTab.Home) coroutineScope.launch { AppEvents.emitRefreshFeed() }
                                                            if (tab == MainTab.Profile) coroutineScope.launch { AppEvents.emitRefreshProfile() }
                                                            selectedTab = tab
                                                        },
                                                        icon = { Icon(tab.icon(selected), tab.title) },
                                                        label = if (navBarLabelsVisible) { { Text(tab.title) } } else null,
                                                        alwaysShowLabel = navBarLabelsVisible
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        ) { padding ->
                            AppNavigation(
                                navController = navController,
                                modifier = Modifier.padding(padding),
                                musicViewModel = musicViewModel,
                                feedViewModel = feedViewModel,
                                playerViewModel = playerViewModel,
                                notificationsViewModel = notificationsViewModel,
                                settingsViewModel = settingsViewModel,
                                currentUserId = currentUserId,
                                isOfflineMode = isOfflineMode,
                                selectedTab = selectedTab,
                                onTabChange = { selectedTab = it },
                                showCreatePlaylistDialog = showCreatePlaylistDialog,
                                onShowCreatePlaylistDialogChange = { showCreatePlaylistDialog = it },
                                onOpenVideo = { activeVideoToPlay = it },
                                coroutineScope = coroutineScope,
                                clipboardManager = clipboardManager
                            )
                        }
                    }

                    if (isInPipMode) {
                        PipPlayerView()
                    }

                    val isVideoFloating by AppEvents.isVideoFloating.collectAsState()
                    if (isVideoFloating && !isInPipMode) {
                        var offset by remember { mutableStateOf(Offset.Zero) }
                        var scale by remember { mutableFloatStateOf(1f) }
                        var showFloatingControls by remember { mutableStateOf(true) }
                        val activePlayerState = AppEvents.activeExoPlayer.collectAsState()
                        val activePlayer = activePlayerState.value
                        var isPlaying by remember { mutableStateOf(activePlayer?.isPlaying ?: false) }

                        LaunchedEffect(activePlayer) {
                            activePlayer?.addListener(object : Player.Listener {
                                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                            })
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                                    .width(200.dp * scale)
                                    .aspectRatio(16 / 9f)
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                                            offset += pan
                                        }
                                    }
                                    .clickable { showFloatingControls = !showFloatingControls },
                                shape = MaterialTheme.shapes.medium,
                                color = Color.Black,
                                tonalElevation = 8.dp,
                                shadowElevation = 8.dp
                            ) {
                                Box {
                                    PipPlayerView()
                                    AnimatedVisibility(visible = showFloatingControls, enter = fadeIn(), exit = fadeOut()) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))) {
                                            IconButton(onClick = { AppEvents.activeExoPlayer.value?.pause(); AppEvents.setVideoFloating(false) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(onClick = { val player = AppEvents.activeExoPlayer.value; if (player?.isPlaying == true) player.pause() else player?.play() }, modifier = Modifier.align(Alignment.Center).size(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                                                Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { activeVideoToPlay = AppEvents.activeVideo.value; AppEvents.setVideoFloating(false) }, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                                                Icon(Icons.Default.Fullscreen, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
