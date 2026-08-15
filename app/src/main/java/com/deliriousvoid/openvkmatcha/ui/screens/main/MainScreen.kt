package com.deliriousvoid.openvkmatcha.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.deliriousvoid.openvkmatcha.ui.navigation.MainTab
import com.deliriousvoid.openvkmatcha.ui.screens.home.HomeScreen
import com.deliriousvoid.openvkmatcha.ui.screens.explore.ExploreScreen
import com.deliriousvoid.openvkmatcha.ui.screens.messages.MessagesScreen
import com.deliriousvoid.openvkmatcha.ui.screens.music.MusicScreen
import com.deliriousvoid.openvkmatcha.ui.screens.profile.ProfileScreen
import com.deliriousvoid.openvkmatcha.ui.screens.profile.FriendsScreen
import com.deliriousvoid.openvkmatcha.ui.screens.profile.GroupsScreen
import com.deliriousvoid.openvkmatcha.ui.screens.explore.NotesScreen
import com.deliriousvoid.openvkmatcha.ui.viewmodel.MusicViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.FeedViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.ExploreViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.NotesViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.runtime.key

@Composable
fun MainScreen(
    selectedTab: MainTab,
    onTabChange: (MainTab) -> Unit = {},
    onOpenChat: (Int, String) -> Unit,
    onOpenPlaylist: (Int, Int, String) -> Unit,
    onOpenProfile: (Any) -> Unit,
    onOpenComments: (Int, Int) -> Unit,
    onOpenFriends: (Int, String) -> Unit = { _, _ -> },
    onOpenGroups: (Int, String) -> Unit = { _, _ -> },
    onOpenMusic: (Int, String) -> Unit = { _, _ -> },
    onOpenGifts: (Int, String) -> Unit = { _, _ -> },
    onOpenTopics: (Int, String) -> Unit = { _, _ -> },
    onOpenFollowers: (Int, Boolean, String) -> Unit = { _, _, _ -> },
    onOpenPhotos: (Int, String) -> Unit = { _, _ -> },
    onOpenEditProfile: () -> Unit = {},
    onOpenEditGroup: (Int) -> Unit = {},
    onOpenCreatePost: (Int) -> Unit = {},
    onOpenWebView: (String, String) -> Unit = { _, _ -> },
    onOpenVideos: (Int) -> Unit = {},
    onOpenDocs: (Int) -> Unit = {},
    onOpenNotes: (Int) -> Unit = {},
    onOpenNoteDetails: (Int, Int) -> Unit = { _, _ -> },
    onCreateNote: (Int) -> Unit = {},
    onOpenEvents: (Int) -> Unit = {},
    onOpenTransfer: () -> Unit = {},
    currentUserId: Int?,
    musicViewModel: MusicViewModel,
    feedViewModel: FeedViewModel,
    isOfflineMode: Boolean = false,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val safeUserId = currentUserId ?: 0
        key(safeUserId) {
            when (selectedTab) {
                MainTab.Home -> HomeScreen(
                    onOpenProfile = onOpenProfile,
                    onOpenComments = onOpenComments,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenMusic = onOpenMusic,
                    viewModel = feedViewModel
                )
                MainTab.Explore -> {
                    val exploreViewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.factory())
                    
                    ExploreScreen(
                        viewModel = exploreViewModel,
                        onOpenFeature = { feature ->
                            when (feature) {
                                "music" -> onTabChange(MainTab.Music)
                                "messages" -> onTabChange(MainTab.Messages)
                                "friends" -> onOpenFriends(safeUserId, "")
                                "groups" -> onOpenGroups(safeUserId, "")
                                "video" -> onOpenVideos(safeUserId)
                                "docs" -> onOpenDocs(safeUserId)
                                "events" -> onOpenEvents(safeUserId)
                                "notes" -> onOpenNotes(safeUserId)
                                "transfer" -> onOpenTransfer()
                            }
                        },
                        onOpenWebView = onOpenWebView,
                        onOpenProfile = { onOpenProfile(it) }
                    )
                }
                MainTab.Messages -> MessagesScreen(onConversationClick = onOpenChat)
                MainTab.Music -> MusicScreen(
                    onOpenPlaylist = onOpenPlaylist,
                    viewModel = musicViewModel,
                    isOfflineMode = isOfflineMode
                )
                MainTab.Profile -> ProfileScreen(
                    onOpenProfile = onOpenProfile,
                    onOpenComments = onOpenComments,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenFriends = onOpenFriends,
                    onOpenGroups = onOpenGroups,
                    onOpenMusic = onOpenMusic,
                    onOpenGifts = onOpenGifts,
                    onOpenTopics = onOpenTopics,
                    onOpenFollowers = onOpenFollowers,
                    onOpenPhotos = onOpenPhotos,
                    onOpenEditProfile = onOpenEditProfile,
                    onOpenEditGroup = onOpenEditGroup,
                    onOpenCreatePost = onOpenCreatePost
                )
                MainTab.Friends -> FriendsScreen(
                    userId = safeUserId,
                    currentUserId = safeUserId,
                    onOpenProfile = onOpenProfile
                )
                MainTab.Groups -> GroupsScreen(
                    userId = safeUserId,
                    currentUserId = safeUserId,
                    onOpenProfile = onOpenProfile
                )
                MainTab.Notes -> {
                    val notesViewModel: NotesViewModel = viewModel(factory = NotesViewModel.factory(safeUserId))
                    NotesScreen(
                        viewModel = notesViewModel,
                        onBack = {}, // Not applicable here
                        onOpenNote = { note -> onOpenNoteDetails(note.ownerId, note.id) },
                        onCreateNote = { onCreateNote(safeUserId) },
                        route = com.deliriousvoid.openvkmatcha.ui.navigation.Routes.MAIN
                    )
                }
            }
        }
    }
}
