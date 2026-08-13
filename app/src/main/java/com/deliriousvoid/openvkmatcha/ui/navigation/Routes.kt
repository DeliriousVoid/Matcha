package com.deliriousvoid.openvkmatcha.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val NOTIFICATIONS = "notifications"
    const val COMMENTS = "comments/{ownerId}/{postId}"
    const val SETTINGS = "settings"
    const val SETTINGS_ACCOUNTS = "settings/accounts"
    const val SETTINGS_GENERAL = "settings/general"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_MUSIC = "settings/music"
    const val SETTINGS_IGNORED = "settings/ignored"
    const val SETTINGS_DEVELOPER = "settings/developer"
    const val SETTINGS_NAVIGATION = "settings/navigation"
    const val SETTINGS_ABOUT_INSTANCE = "settings/about_instance"
    const val CHAT = "chat/{peerId}?title={title}"
    const val PLAYLIST_DETAILS = "playlist/{ownerId}/{playlistId}?title={title}"
    const val PROFILE = "profile/{id}"
    const val EXPLORE = "explore"
    const val SEARCH = "search"
    const val TWO_FACTOR = "two_factor/{username}/{password}/{instance}"
    const val TRANSFER = "transfer"
    const val VIDEOS = "videos/{userId}"
    const val DOCUMENTS = "documents/{userId}"
    const val NOTES = "notes/{userId}"
    const val NOTE_DETAILS = "note/{ownerId}/{noteId}"
    const val CREATE_EDIT_NOTE = "note/edit?ownerId={ownerId}&noteId={noteId}"
    const val EVENTS = "events/{userId}"
    const val UPLOAD_AUDIO = "upload_audio"
    const val WEBVIEW = "webview?url={url}&title={title}"
    const val FRIENDS = "friends/{id}/{name}?initialTab={initialTab}"
    const val GROUPS = "groups/{id}/{name}"
    const val USER_MUSIC = "user_music/{id}/{name}"
    const val GIFTS = "gifts/{id}/{name}"
    const val FOLLOWERS = "followers/{id}/{isGroup}/{name}"
    const val PHOTO_ALBUMS = "photo_albums/{id}/{name}"
    const val PHOTOS = "photos/{id}/{title}?albumId={albumId}"
    const val TOPICS = "topics/{id}/{name}"
    const val TOPIC_COMMENTS = "topic_comments/{groupId}/{topicId}/{title}?vid={vid}"
    const val SEND_GIFT = "send_gift?userId={userId}"
    const val EDIT_PROFILE = "edit_profile"
    const val EDIT_GROUP = "edit_group/{groupId}"
    const val CREATE_POST = "create_post/{ownerId}"
    const val MAP_PICKER = "map_picker?lat={lat}&lon={lon}"
    const val QR_DISPLAY = "qr_display/{url}/{title}/{avatarUrl}"
    const val QR_SCANNER = "qr_scanner"
    const val GRAFFITI = "graffiti"

    fun friendsRoute(id: Int, name: String, initialTab: Int = 0): String = "friends/$id/${Uri.encode(name)}?initialTab=$initialTab"
    fun groupsRoute(id: Int, name: String): String = "groups/$id/${Uri.encode(name)}"
    fun userMusicRoute(id: Int, name: String): String = "user_music/$id/${Uri.encode(name)}"
    fun giftsRoute(id: Int, name: String): String = "gifts/$id/${Uri.encode(name)}"
    fun followersRoute(id: Int, isGroup: Boolean, name: String): String = "followers/$id/$isGroup/${Uri.encode(name)}"
    fun photoAlbumsRoute(id: Int, name: String): String = "photo_albums/$id/${Uri.encode(name)}"
    fun topicsRoute(id: Int, name: String): String = "topics/$id/${Uri.encode(name)}"
    fun topicCommentsRoute(groupId: Int, topicId: Int, title: String, vid: Int? = null): String {
        val encodedTitle = Uri.encode(title)
        return if (vid != null) "topic_comments/$groupId/$topicId/$encodedTitle?vid=$vid" else "topic_comments/$groupId/$topicId/$encodedTitle"
    }
    fun editGroupRoute(groupId: Int): String = "edit_group/$groupId"
    fun photosRoute(id: Int, title: String, albumId: Int? = null): String {
        val encodedTitle = Uri.encode(title)
        return if (albumId != null) "photos/$id/$encodedTitle?albumId=$albumId" else "photos/$id/$encodedTitle"
    }

    fun createPostRoute(ownerId: Int): String = "create_post/$ownerId"

    fun qrDisplayRoute(url: String, title: String, avatarUrl: String): String {
        return "qr_display/${Uri.encode(url)}/${Uri.encode(title)}/${Uri.encode(avatarUrl)}"
    }

    fun qrScannerRoute(): String = "qr_scanner"

    fun chatRoute(peerId: Int, title: String): String {
        val encodedTitle = Uri.encode(title)
        return "chat/$peerId?title=$encodedTitle"
    }

    fun playlistRoute(ownerId: Int, playlistId: Int, title: String): String {
        val encodedTitle = Uri.encode(title)
        return "playlist/$ownerId/$playlistId?title=$encodedTitle"
    }

    fun profileRoute(id: Any): String {
        return "profile/$id"
    }

    fun webviewRoute(url: String, title: String): String {
        return "webview?url=${Uri.encode(url)}&title=${Uri.encode(title)}"
    }

    fun twoFactorRoute(username: String, password: String, instance: String): String {
        return "two_factor/${Uri.encode(username)}/${Uri.encode(password)}/${Uri.encode(instance)}"
    }

    fun videosRoute(userId: Int): String = "videos/$userId"
    fun documentsRoute(userId: Int): String = "documents/$userId"
    fun notesRoute(userId: Int): String = "notes/$userId"
    fun noteDetailsRoute(ownerId: Int, noteId: Int): String = "note/$ownerId/$noteId"
    fun createEditNoteRoute(ownerId: Int? = null, noteId: Int? = null): String {
        return "note/edit?ownerId=${ownerId ?: ""}&noteId=${noteId ?: ""}"
    }
    fun eventsRoute(userId: Int): String = "events/$userId"

    fun sendGiftRoute(userId: Int? = null): String = 
        if (userId != null) "send_gift?userId=$userId" else "send_gift"

    fun commentsRoute(ownerId: Int, postId: Int): String {
        return "comments/$ownerId/$postId"
    }
}

enum class MainTab(val title: String) {
    Home("Новости"),
    Explore("Обзор"),
    Messages("Сообщения"),
    Music("Музыка"),
    Profile("Профиль"),
    Friends("Друзья"),
    Groups("Сообщества"),
    Notes("Заметки");

    fun icon(selected: Boolean): ImageVector = when (this) {
        Home -> if (selected) Icons.Filled.Newspaper else Icons.Outlined.Newspaper
        Explore -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
        Messages -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
        Music -> if (selected) Icons.Filled.MusicNote else Icons.Outlined.MusicNote
        Profile -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
        Friends -> if (selected) Icons.Filled.People else Icons.Outlined.People
        Groups -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
        Notes -> if (selected) Icons.AutoMirrored.Filled.StickyNote2 else Icons.AutoMirrored.Outlined.StickyNote2
    }
}
