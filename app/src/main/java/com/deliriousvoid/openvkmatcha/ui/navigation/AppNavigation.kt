package com.deliriousvoid.openvkmatcha.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.deliriousvoid.openvkmatcha.data.model.PlaylistSource
import com.deliriousvoid.openvkmatcha.data.model.Video
import com.deliriousvoid.openvkmatcha.ui.screens.board.TopicCommentsScreen
import com.deliriousvoid.openvkmatcha.ui.screens.board.TopicsScreen
import com.deliriousvoid.openvkmatcha.ui.screens.comments.CommentsScreen
import com.deliriousvoid.openvkmatcha.ui.screens.explore.*
import com.deliriousvoid.openvkmatcha.ui.screens.login.LoginScreen
import com.deliriousvoid.openvkmatcha.ui.screens.login.TwoFactorScreen
import com.deliriousvoid.openvkmatcha.ui.screens.main.MainScreen
import com.deliriousvoid.openvkmatcha.ui.screens.map.MapPickerScreen
import com.deliriousvoid.openvkmatcha.ui.screens.messages.ChatScreen
import com.deliriousvoid.openvkmatcha.ui.screens.music.MusicScreen
import com.deliriousvoid.openvkmatcha.ui.screens.music.PlaylistDetailsScreen
import com.deliriousvoid.openvkmatcha.ui.screens.music.UploadAudioScreen
import com.deliriousvoid.openvkmatcha.ui.screens.notifications.NotificationsScreen
import com.deliriousvoid.openvkmatcha.ui.screens.profile.*
import com.deliriousvoid.openvkmatcha.ui.screens.qr.QRDisplayScreen
import com.deliriousvoid.openvkmatcha.ui.screens.qr.QRScannerScreen
import com.deliriousvoid.openvkmatcha.ui.screens.search.GlobalSearchScreen
import com.deliriousvoid.openvkmatcha.ui.screens.settings.*
import com.deliriousvoid.openvkmatcha.ui.screens.splash.SplashScreen
import com.deliriousvoid.openvkmatcha.ui.screens.webview.WebViewScreen
import com.deliriousvoid.openvkmatcha.ui.util.LinkHandler
import com.deliriousvoid.openvkmatcha.ui.viewmodel.*
import com.deliriousvoid.openvkmatcha.util.AppEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel,
    feedViewModel: FeedViewModel,
    playerViewModel: PlayerViewModel,
    notificationsViewModel: NotificationsViewModel,
    settingsViewModel: SettingsViewModel,
    currentUserId: Int?,
    isOfflineMode: Boolean,
    selectedTab: MainTab,
    onTabChange: (MainTab) -> Unit,
    showCreatePlaylistDialog: Boolean,
    onShowCreatePlaylistDialogChange: (Boolean) -> Unit,
    onOpenVideo: (Video) -> Unit,
    coroutineScope: CoroutineScope,
    @Suppress("DEPRECATION")
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier.fillMaxSize()
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateTo2FA = { username, password, instance ->
                    navController.navigate(Routes.twoFactorRoute(username, password, instance))
                }
            )
        }

        composable(
            route = Routes.TWO_FACTOR,
            arguments = listOf(
                navArgument("username") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType },
                navArgument("instance") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            val instance = backStackEntry.arguments?.getString("instance") ?: ""
            TwoFactorScreen(
                username = username,
                password = password,
                instance = instance,
                onSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SEARCH) {
            val searchViewModel: GlobalSearchViewModel = viewModel(factory = GlobalSearchViewModel.factory())
            val downloadedTracksSet by musicViewModel.downloadedTracks.collectAsState()
            val currentTrack by playerViewModel.currentTrack.collectAsState()

            GlobalSearchScreen(
                onOpenProfile = { id -> navController.navigate(Routes.profileRoute(id)) },
                onOpenPlaylist = { ownerId, playlistId, title ->
                    navController.navigate(Routes.playlistRoute(ownerId, playlistId, title))
                },
                onPlayTrack = { tracks, index ->
                    val query = searchViewModel.uiState.value.searchQuery
                    playerViewModel.play(tracks, index, PlaylistSource.SearchAudio(query))
                },
                onToggleTrackAdded = { track -> playerViewModel.toggleTrackAdded(track) },
                onDownloadTrack = { track -> musicViewModel.downloadTrack(track) },
                onShareTrack = { track ->
                    val url = track.remoteUrl ?: track.url ?: ""
                    if (url.isNotEmpty()) clipboardManager.setText(AnnotatedString(url))
                },
                onAddToQueue = { track -> playerViewModel.playerManager.addToQueue(track) },
                onPlayNext = { track -> playerViewModel.playerManager.playNext(track) },
                currentTrackId = currentTrack?.id,
                downloadedTracks = downloadedTracksSet,
                viewModel = searchViewModel
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                selectedTab = selectedTab,
                onTabChange = onTabChange,
                musicViewModel = musicViewModel,
                feedViewModel = feedViewModel,
                isOfflineMode = isOfflineMode,
                onOpenChat = { peerId: Int, title: String ->
                    navController.navigate(Routes.chatRoute(peerId, title))
                },
                onOpenPlaylist = { ownerId: Int, playlistId: Int, title: String ->
                    navController.navigate(Routes.playlistRoute(ownerId, playlistId, title))
                },
                onOpenProfile = { target: Any ->
                    navController.navigate(Routes.profileRoute(target))
                },
                onOpenComments = { ownerId: Int, postId: Int ->
                    navController.navigate(Routes.commentsRoute(ownerId, postId))
                },
                onOpenFriends = { id: Int, name: String ->
                    navController.navigate(Routes.friendsRoute(id, name))
                },
                onOpenGroups = { id: Int, name: String ->
                    navController.navigate(Routes.groupsRoute(id, name))
                },
                onOpenMusic = { id: Int, name: String ->
                    navController.navigate(Routes.userMusicRoute(id, name))
                },
                onOpenGifts = { id: Int, name: String ->
                    navController.navigate(Routes.giftsRoute(id, name))
                },
                onOpenTopics = { id: Int, name: String ->
                    navController.navigate(Routes.topicsRoute(id, name))
                },
                onOpenFollowers = { id: Int, isGroup: Boolean, name: String ->
                    if (!isGroup) {
                        navController.navigate(Routes.friendsRoute(id, name, initialTab = 2))
                    } else {
                        navController.navigate(Routes.followersRoute(id, true, name))
                    }
                },
                onOpenPhotos = { id: Int, name: String ->
                    navController.navigate(Routes.photoAlbumsRoute(id, name))
                },
                onOpenEditProfile = {
                    navController.navigate(Routes.EDIT_PROFILE)
                },
                onOpenEditGroup = { groupId ->
                    navController.navigate(Routes.editGroupRoute(groupId))
                },
                onOpenCreatePost = { ownerId ->
                    navController.navigate(Routes.createPostRoute(ownerId))
                },
                onOpenWebView = { url, title ->
                    navController.navigate(Routes.webviewRoute(url, title))
                },
                onOpenVideos = { userId ->
                    navController.navigate(Routes.videosRoute(userId))
                },
                onOpenDocs = { userId ->
                    navController.navigate(Routes.documentsRoute(userId))
                },
                onOpenNotes = { userId ->
                    navController.navigate(Routes.notesRoute(userId))
                },
                onOpenNoteDetails = { ownerId, noteId ->
                    navController.navigate(Routes.noteDetailsRoute(ownerId, noteId))
                },
                onCreateNote = { ownerId ->
                    navController.navigate(Routes.createEditNoteRoute(ownerId))
                },
                onOpenEvents = { userId ->
                    navController.navigate(Routes.eventsRoute(userId))
                },
                onOpenTransfer = {
                    navController.navigate(Routes.TRANSFER)
                },
                currentUserId = currentUserId
            )

            if (showCreatePlaylistDialog) {
                com.deliriousvoid.openvkmatcha.ui.components.EditPlaylistDialog(
                    onDismiss = { onShowCreatePlaylistDialogChange(false) },
                    onConfirm = { title, desc ->
                        musicViewModel.createPlaylist(title, desc)
                        onShowCreatePlaylistDialogChange(false)
                    }
                )
            }
        }

        composable(Routes.TRANSFER) {
            val tViewModel: TransferViewModel =
                viewModel(factory = TransferViewModel.factory(currentUserId ?: 0))
            TransferScreen(
                viewModel = tViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.popBackStack()
                    coroutineScope.launch {
                        AppEvents.emitRefreshProfile() // Refresh balance after success
                    }
                }
            )
        }

        composable(
            route = Routes.VIDEOS,
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0
            val vViewModel: VideosViewModel =
                viewModel(factory = VideosViewModel.factory(userId))
            VideosScreen(
                viewModel = vViewModel,
                onBack = { navController.popBackStack() },
                onOpenVideo = onOpenVideo,
                onOpenWebView = { url, title ->
                    navController.navigate(Routes.webviewRoute(url, title))
                }
            )
        }

        composable(
            route = Routes.DOCUMENTS,
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0
            val dViewModel: DocsViewModel =
                viewModel(factory = DocsViewModel.factory(userId))
            DocumentsScreen(
                viewModel = dViewModel,
                onBack = { navController.popBackStack() },
                onOpenWebView = { url, title ->
                    navController.navigate(Routes.webviewRoute(url, title))
                }
            )
        }

        composable(
            route = Routes.NOTES,
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0
            val nViewModel: NotesViewModel =
                viewModel(factory = NotesViewModel.factory(userId))
            NotesScreen(
                viewModel = nViewModel,
                onBack = { navController.popBackStack() },
                onOpenNote = { note ->
                    navController.navigate(Routes.noteDetailsRoute(note.ownerId, note.id))
                },
                onCreateNote = {
                    navController.navigate(Routes.createEditNoteRoute())
                }
            )
        }

        composable(
            route = Routes.NOTE_DETAILS,
            arguments = listOf(
                navArgument("ownerId") { type = NavType.IntType },
                navArgument("noteId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getInt("ownerId") ?: 0
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: 0
            val ndViewModel: NoteDetailsViewModel =
                viewModel(factory = NoteDetailsViewModel.factory(ownerId, noteId))
            NoteDetailsScreen(
                viewModel = ndViewModel,
                onBack = { navController.popBackStack() },
                onEditNote = { note ->
                    navController.navigate(Routes.createEditNoteRoute(note.ownerId, note.id))
                },
                onOpenProfile = { target ->
                    navController.navigate(Routes.profileRoute(target))
                },
                currentUserId = currentUserId ?: 0
            )
        }

        composable(
            route = Routes.CREATE_EDIT_NOTE,
            arguments = listOf(
                navArgument("ownerId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("noteId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getString("ownerId")?.toIntOrNull()
            val noteId = backStackEntry.arguments?.getString("noteId")?.toIntOrNull()
            val ceViewModel: CreateEditNoteViewModel =
                viewModel(factory = CreateEditNoteViewModel.factory(ownerId, noteId))
            CreateEditNoteScreen(
                viewModel = ceViewModel,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EVENTS,
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0
            val eViewModel: EventsViewModel =
                viewModel(factory = EventsViewModel.factory(userId))
            EventsScreen(
                viewModel = eViewModel,
                onBack = { navController.popBackStack() },
                onOpenEvent = { eventId ->
                    navController.navigate(Routes.profileRoute(eventId))
                }
            )
        }

        composable(
            route = Routes.WEBVIEW,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            WebViewScreen(
                url = url,
                title = title,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CREATE_POST,
            arguments = listOf(navArgument("ownerId") { type = NavType.IntType })
        ) { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getInt("ownerId") ?: 0
            val locationResult = backStackEntry.savedStateHandle
                .getStateFlow<Triple<Double, Double, String?>?>("location", null)
                .collectAsState()
            val graffitiResult = backStackEntry.savedStateHandle
                .getStateFlow<Uri?>("graffiti", null)
                .collectAsState()

            CreatePostScreen(
                ownerId = ownerId,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() },
                onOpenMap = { lat, lon ->
                    val route = if ((lat != null) && (lon != null)) {
                        "map_picker?lat=$lat&lon=$lon"
                    } else {
                        "map_picker"
                    }
                    navController.navigate(route)
                },
                onOpenGraffiti = {
                    navController.navigate(Routes.GRAFFITI)
                },
                resultLocation = locationResult.value,
                resultGraffiti = graffitiResult.value
            )
        }

        composable(
            route = Routes.MAP_PICKER,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType; nullable = true },
                navArgument("lon") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
            MapPickerScreen(
                initialLat = lat,
                initialLon = lon,
                onBack = { navController.popBackStack() },
                onConfirm = { latitude, longitude, address ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("location", Triple(latitude, longitude, address))
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onOpenProfile = { target ->
                    navController.navigate(Routes.profileRoute(target))
                },
                onOpenPost = { ownerId, postId ->
                    navController.navigate(Routes.commentsRoute(ownerId, postId))
                },
                viewModel = notificationsViewModel
            )
        }

        composable(
            route = Routes.COMMENTS,
            arguments = listOf(
                navArgument("ownerId") { type = NavType.IntType },
                navArgument("postId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getInt("ownerId") ?: 0
            val postId = backStackEntry.arguments?.getInt("postId") ?: 0
            CommentsScreen(
                ownerId = ownerId,
                postId = postId,
                onOpenProfile = { target ->
                    navController.navigate(Routes.profileRoute(target))
                },
                onOpenComments = { oId, pId ->
                    navController.navigate(Routes.commentsRoute(oId, pId))
                },
                onOpenPlaylist = { oId, pId, title ->
                    navController.navigate(Routes.playlistRoute(oId, pId, title))
                },
                onOpenMusic = { targetId, name ->
                    navController.navigate(Routes.userMusicRoute(targetId, name))
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToGeneral = { navController.navigate(Routes.SETTINGS_GENERAL) },
                onNavigateToAppearance = { navController.navigate(Routes.SETTINGS_APPEARANCE) },
                onNavigateToMusic = { navController.navigate(Routes.SETTINGS_MUSIC) },
                onNavigateToDeveloper = { navController.navigate(Routes.SETTINGS_DEVELOPER) },
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SETTINGS_ACCOUNTS) {
            AccountManagementScreen(
                onAddAccount = {
                    navController.navigate(Routes.LOGIN)
                },
                onAccountSwitched = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SETTINGS_GENERAL) {
            GeneralSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToAccounts = { navController.navigate(Routes.SETTINGS_ACCOUNTS) },
                onNavigateToIgnored = { navController.navigate(Routes.SETTINGS_IGNORED) },
                onNavigateToAboutInstance = { navController.navigate(Routes.SETTINGS_ABOUT_INSTANCE) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_ABOUT_INSTANCE) {
            AboutInstanceScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { id -> navController.navigate(Routes.profileRoute(id)) }
            )
        }

        composable(Routes.SETTINGS_APPEARANCE) {
            AppearanceSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToNavigationSettings = { navController.navigate(Routes.SETTINGS_NAVIGATION) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_MUSIC) {
            MusicSettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_DEVELOPER) {
            DeveloperSettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_NAVIGATION) {
            NavigationSettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_IGNORED) {
            IgnoredSourcesScreen(
                onOpenProfile = { target: Any ->
                    navController.navigate(Routes.profileRoute(target))
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("peerId") { type = NavType.IntType },
                navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getInt("peerId") ?: 0
            val title = backStackEntry.arguments?.getString("title") ?: ""
            ChatScreen(
                peerId = peerId,
                title = title,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PLAYLIST_DETAILS,
            arguments = listOf(
                navArgument("ownerId") { type = NavType.IntType },
                navArgument("playlistId") { type = NavType.IntType },
                navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getInt("ownerId") ?: 0
            val playlistId = backStackEntry.arguments?.getInt("playlistId") ?: 0
            val title = backStackEntry.arguments?.getString("title") ?: ""
            PlaylistDetailsScreen(
                ownerId = ownerId,
                playlistId = playlistId,
                title = title,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PROFILE,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            ProfileScreen(
                userIdOrName = id,
                onOpenProfile = { target: Any ->
                    navController.navigate(Routes.profileRoute(target))
                },
                onOpenComments = { ownerId: Int, postId: Int ->
                    navController.navigate(Routes.commentsRoute(ownerId, postId))
                },
                onOpenPlaylist = { ownerId: Int, playlistId: Int, title: String ->
                    navController.navigate(Routes.playlistRoute(ownerId, playlistId, title))
                },
                onOpenFriends = { targetId: Int, name: String ->
                    navController.navigate(Routes.friendsRoute(targetId, name))
                },
                onOpenGroups = { targetId: Int, name: String ->
                    navController.navigate(Routes.groupsRoute(targetId, name))
                },
                onOpenMusic = { targetId: Int, name: String ->
                    navController.navigate(Routes.userMusicRoute(targetId, name))
                },
                onOpenGifts = { targetId: Int, name: String ->
                    navController.navigate(Routes.giftsRoute(targetId, name))
                },
                onOpenTopics = { targetId: Int, name: String ->
                    navController.navigate(Routes.topicsRoute(targetId, name))
                },
                onOpenChat = { peerId: Int, title: String ->
                    navController.navigate(Routes.chatRoute(peerId, title))
                },
                onOpenFollowers = { targetId: Int, isGroup: Boolean, name: String ->
                    if (!isGroup) {
                        navController.navigate(Routes.friendsRoute(targetId, name, initialTab = 2))
                    } else {
                        navController.navigate(Routes.followersRoute(targetId, isGroup, name))
                    }
                },
                onOpenPhotos = { targetId: Int, name: String ->
                    navController.navigate(Routes.photoAlbumsRoute(targetId, name))
                },
                onOpenEditProfile = {
                    navController.navigate(Routes.EDIT_PROFILE)
                },
                onOpenEditGroup = { groupId ->
                    navController.navigate(Routes.editGroupRoute(groupId))
                },
                onOpenCreatePost = { ownerId ->
                    navController.navigate(Routes.createPostRoute(ownerId))
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_GROUP,
            arguments = listOf(
                navArgument("groupId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 0
            EditGroupScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PHOTO_ALBUMS,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val name = backStackEntry.arguments?.getString("name") ?: ""
            PhotoAlbumsScreen(
                userId = id,
                name = name,
                onBack = { navController.popBackStack() },
                onOpenAlbum = { albumId: Int?, title: String ->
                    navController.navigate(Routes.photosRoute(id, title, albumId))
                }
            )
        }

        composable(
            route = Routes.PHOTOS,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("title") { type = NavType.StringType },
                navArgument("albumId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val albumId = backStackEntry.arguments?.getString("albumId")?.toIntOrNull()
            PhotosScreen(
                userId = id,
                title = title,
                albumId = albumId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.FRIENDS,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType },
                navArgument("initialTab") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
            FriendsScreen(
                userId = id,
                currentUserId = currentUserId,
                initialTab = initialTab,
                onOpenProfile = { target ->
                    navController.navigate(Routes.profileRoute(target))
                }
            )
        }

        composable(
            route = Routes.GROUPS,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            GroupsScreen(
                userId = id,
                currentUserId = currentUserId,
                onOpenProfile = { target ->
                    navController.navigate(Routes.profileRoute(target))
                }
            )
        }

        composable(
            route = Routes.GIFTS,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val name = backStackEntry.arguments?.getString("name")
            GiftsScreen(
                userId = id,
                userName = name,
                onOpenProfile = { target ->
                    navController.navigate(Routes.profileRoute(target))
                },
                onSendGift = { userId ->
                    navController.navigate(Routes.sendGiftRoute(userId))
                }
            )
        }

        composable(
            route = Routes.SEND_GIFT,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
            SendGiftScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.USER_MUSIC,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            MusicScreen(
                userId = id,
                onOpenPlaylist = { ownerId, playlistId, title ->
                    navController.navigate(Routes.playlistRoute(ownerId, playlistId, title))
                }
            )
        }

        composable(
            route = Routes.FOLLOWERS,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("isGroup") { type = NavType.BoolType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
            FollowersScreen(
                id = id,
                isGroup = isGroup,
                onOpenProfile = { target ->
                    navController.navigate(Routes.profileRoute(target))
                }
            )
        }

        composable(Routes.UPLOAD_AUDIO) {
            UploadAudioScreen()
        }

        composable(
            route = Routes.TOPICS,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val name = backStackEntry.arguments?.getString("name") ?: ""
            TopicsScreen(
                groupId = id,
                groupName = name,
                onOpenTopic = { topicId, topicTitle, vid ->
                    navController.navigate(Routes.topicCommentsRoute(id, topicId, topicTitle, vid))
                }
            )
        }

        composable(
            route = Routes.TOPIC_COMMENTS,
            arguments = listOf(
                navArgument("groupId") { type = NavType.IntType },
                navArgument("topicId") { type = NavType.IntType },
                navArgument("title") { type = NavType.StringType },
                navArgument("vid") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 0
            val topicId = backStackEntry.arguments?.getInt("topicId") ?: 0
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val vid = backStackEntry.arguments?.getInt("vid")?.takeIf { it != 0 }
            TopicCommentsScreen(
                groupId = groupId,
                topicId = topicId,
                title = title,
                vidGuess = vid,
                onOpenProfile = { targetId ->
                    navController.navigate(Routes.profileRoute(targetId))
                },
                onOpenComments = { oId, pId ->
                    navController.navigate(Routes.commentsRoute(oId, pId))
                },
                onOpenPlaylist = { oId, pId, t ->
                    navController.navigate(Routes.playlistRoute(oId, pId, t))
                },
                onOpenMusic = { targetId, name ->
                    navController.navigate(Routes.userMusicRoute(targetId, name))
                }
            )
        }

        composable(
            route = Routes.QR_DISPLAY,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("avatarUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val avatarUrl = backStackEntry.arguments?.getString("avatarUrl") ?: ""
            QRDisplayScreen(
                url = url,
                title = title,
                avatarUrl = avatarUrl,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QR_SCANNER) {
            QRScannerScreen(
                onScanResult = { result ->
                    val route = LinkHandler.getRouteForUrl(result)
                    if (route != null) {
                        navController.navigate(route) {
                            popUpTo(Routes.QR_SCANNER) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GRAFFITI) {
            GraffitiScreen(
                onBack = { navController.popBackStack() },
                onConfirm = { uri ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("graffiti", uri)
                    navController.popBackStack()
                }
            )
        }
    }
}
