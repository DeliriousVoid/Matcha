package com.deliriousvoid.openvkmatcha.data.model

data class UserProfile(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val screenName: String,
    val photo50: String,
    val photo200: String,
    val photoMaxOrig: String? = null,
    val status: String,
    val online: Boolean,
    val verified: Boolean,
    val friendsCount: Int = 0,
    val followersCount: Int = 0,
    val photosCount: Int = 0,
    val audiosCount: Int = 0,
    val groupsCount: Int = 0,
    val giftsCount: Int = 0,
    val topicsCount: Int = 0,
    val about: String = "",
    val isGroup: Boolean = false,
    val isMember: Boolean = false,
    val friendStatus: Int? = null,
    
    // Extended info
    val bdate: String? = null,
    val city: String? = null,
    val country: String? = null,
    val homeTown: String? = null,
    val sex: Int? = null,
    val relation: Int? = null,
    val site: String? = null,
    val mobilePhone: String? = null,
    val homePhone: String? = null,
    val skype: String? = null,
    val facebook: String? = null,
    val twitter: String? = null,
    val instagram: String? = null,
    val universityName: String? = null,
    val facultyName: String? = null,
    val graduation: Int? = null,
    val activities: String? = null,
    val interests: String? = null,
    val music: String? = null,
    val movies: String? = null,
    val tv: String? = null,
    val books: String? = null,
    val games: String? = null,
    val quotes: String? = null,
    val telegram: String? = null,
    val nickname: String? = null,
    val lastSeen: LastSeen? = null,
    val mobileOnline: Boolean = false,
    val isAdmin: Boolean = false,
    val adminLevel: Int = 0,
    val blacklistedByMe: Boolean = false,
    val isIgnored: Boolean = false,
    val canPost: Boolean = true,
    val groupType: String? = null // "group", "page", or "event"
) {
    val fullName: String get() = if (nickname.isNullOrBlank()) "$firstName $lastName".trim() else "$firstName ($nickname) $lastName".trim()

    val birthdayDisplay: String? get() {
        val date = bdate ?: return null
        val parts = date.split(".").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size < 2) return null
        val day = parts[0]
        val month = parts[1]
        if (day !in 1..31 || month !in 1..12) return null

        val monthNames = listOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        var result = "$day ${monthNames[month - 1]}"
        if (parts.size >= 3) {
            val year = parts[2]
            result += " $year"
            val age = calculateAge(year, month, day)
            if (age >= 0) {
                result += " ($age ${getAgeWord(age)})"
            }
        }
        return result
    }

    val compactAge: String? get() {
        val date = bdate ?: return null
        val parts = date.split(".").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size < 3) return null
        val day = parts[0]
        val month = parts[1]
        val year = parts[2]
        val age = calculateAge(year, month, day)
        return if (age >= 0) "$age ${getAgeWord(age)}" else null
    }

    private fun calculateAge(year: Int, month: Int, day: Int): Int {
        val dob = java.util.Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        val today = java.util.Calendar.getInstance()
        var age = today.get(java.util.Calendar.YEAR) - dob.get(java.util.Calendar.YEAR)
        if (today.get(java.util.Calendar.DAY_OF_YEAR) < dob.get(java.util.Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    private fun getAgeWord(age: Int): String {
        val mod10 = age % 10
        val mod100 = age % 100
        return when {
            mod10 == 1 && mod100 != 11 -> "год"
            mod10 in 2..4 && mod100 !in 12..14 -> "года"
            else -> "лет"
        }
    }
}

data class EditableProfileInfo(
    val firstName: String = "",
    val lastName: String = "",
    val screenName: String = "",
    val sex: Int = 0,
    val relation: Int = 0,
    val bdate: String = "",
    val bdateVisibility: Int = 1,
    val homeTown: String = "",
    val status: String = "",
    val photo200: String? = null,
    val about: String = "",
    val activities: String = "",
    val interests: String = "",
    val music: String = "",
    val movies: String = "",
    val tv: String = "",
    val books: String = "",
    val games: String = "",
    val quotes: String = ""
)

data class GroupSettings(
    val title: String = "",
    val description: String = "",
    val screenName: String = "",
    val website: String = ""
)

data class LastSeen(
    val platform: Int,
    val time: Long
)

data class Post(
    val id: Int,
    val ownerId: Int,
    val authorId: Int,
    val authorName: String,
    val authorAvatar: String,
    val authorOnline: Boolean = false,
    val authorMobileOnline: Boolean = false,
    val authorVerified: Boolean,
    val text: String,
    val date: Long,
    val likeCount: Int,
    val commentCount: Int,
    val repostCount: Int,
    val isLiked: Boolean,
    val isPinned: Boolean = false,
    val isNsfw: Boolean = false,
    var isNsfwRevealed: Boolean = false,
    val imageUrls: List<String> = emptyList(),
    val videos: List<Video> = emptyList(),
    val audios: List<AudioTrack> = emptyList(),
    val documents: List<Document> = emptyList(),
    val copyHistory: List<Post>? = null,
    val platform: String? = null,
    val sourceName: String? = null,
    val copyrightName: String? = null,
    val copyrightLink: String? = null,
    val poll: Poll? = null,
    val geo: Geo? = null,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val canPin: Boolean = false,
)

data class Poll(
    val id: Int,
    val ownerId: Int,
    val created: Long,
    val question: String,
    val votes: Int,
    val answers: List<Answer>,
    val anonymous: Boolean,
    val multiple: Boolean,
    val closed: Boolean,
    val isBoard: Boolean,
    val canVote: Boolean,
    val canEdit: Boolean,
    val canReport: Boolean,
    val canShare: Boolean,
    val answerIds: List<Int> = emptyList(),
    val endDate: Long = 0,
)

data class Answer(
    val id: Int,
    val text: String,
    val votes: Int,
    val rate: Double,
)

data class Geo(
    val type: String,
    val coordinates: String?,
    val place: Place?,
)

data class Place(
    val id: Int,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val created: Long,
    val icon: String?,
    val country: String?,
    val city: String?,
    val address: String?,
)

data class Document(
    val id: Int,
    val ownerId: Int,
    val title: String,
    val size: Int,
    val ext: String,
    val url: String?,
    val date: Long,
    val type: Int,
    val previewUrl: String? = null,
    val previewGifUrl: String? = null,
    val accessKey: String? = null,
    val ownerName: String? = null
)

data class Video(
    val id: Int,
    val ownerId: Int,
    val title: String,
    val duration: Int,
    val thumbnailUrl: String?,
    val videoUrl: String?,
    val playerUrl: String?,
    val accessKey: String? = null,
    val date: Long = 0,
    val views: Int = 0,
    val ownerName: String? = null
)

data class NewsfeedResponse(
    val posts: List<Post>,
    val nextFrom: String?
)

data class Conversation(
    val peerId: Int,
    val title: String,
    val lastMessage: String,
    val lastMessageDate: Long,
    val unreadCount: Int,
    val peerPhoto: String,
    val isOnline: Boolean,
    val isMobileOnline: Boolean = false,
    val peerVerified: Boolean,
)

data class ChatMessage(
    val id: Int,
    val peerId: Int,
    val fromId: Int,
    val text: String,
    val date: Long,
    val isOutgoing: Boolean,
    val isRead: Boolean,
)

data class AudioTrack(
    val id: Int,
    val ownerId: Int,
    val artist: String,
    val title: String,
    val duration: Int,
    val url: String?,
    val remoteUrl: String? = null,
    val artworkUrl: String? = null,
    val isAdded: Boolean = false,
    val accessKey: String? = null
) {
    val stableId: String get() = "${ownerId}_$id"
}

data class Playlist(
    val id: Int,
    val ownerId: Int,
    val title: String,
    val description: String,
    val trackCount: Int,
    val photoUrl: String?,
)

sealed class PlaylistSource {
    data class UserAudio(val userId: Int) : PlaylistSource()
    data class PlaylistAudio(val ownerId: Int, val playlistId: Int) : PlaylistSource()
    data class SearchAudio(val query: String) : PlaylistSource()
    data class LocalAudio(val tracks: List<AudioTrack>) : PlaylistSource()
    data object Unknown : PlaylistSource()

    fun toJsonString(): String {
        return when (this) {
            is UserAudio -> "user_audio:$userId"
            is PlaylistAudio -> "playlist_audio:$ownerId:$playlistId"
            is SearchAudio -> "search_audio:$query"
            is LocalAudio -> "local_audio"
            is Unknown -> "unknown"
        }
    }

    companion object {
        fun fromJsonString(str: String?): PlaylistSource {
            if (str == null) return Unknown
            val parts = str.split(":")
            return when (parts[0]) {
                "user_audio" -> UserAudio(parts.getOrNull(1)?.toIntOrNull() ?: 0)
                "playlist_audio" -> PlaylistAudio(parts.getOrNull(1)?.toIntOrNull() ?: 0, parts.getOrNull(2)?.toIntOrNull() ?: 0)
                "search_audio" -> SearchAudio(parts.drop(1).joinToString(":"))
                "local_audio" -> LocalAudio(emptyList()) // Tracks should be restored separately
                else -> Unknown
            }
        }
    }
}

data class Notification(
    val id: String,
    val type: String, // like_post, reply_comment, follow, etc.
    val action: String,
    val date: Long,
    val authorId: Int,
    val authorName: String,
    val authorAvatar: String,
    val authorOnline: Boolean = false,
    val authorMobileOnline: Boolean = false,
    val authorVerified: Boolean,
    val text: String?,
    val parentText: String? = null,
    val ownerId: Int = 0,
    val itemId: Int = 0,
    val isRead: Boolean = false,
    val isDetailsLoaded: Boolean = false,
)

data class NotificationsResponse(
    val items: List<Notification>,
    val nextFrom: String?,
    val unreadCount: Int = 0
)

enum class AttachmentType {
    PHOTO, VIDEO, AUDIO, DOCUMENT, GRAFFITI
}

data class PendingAttachment(
    val uri: android.net.Uri? = null,
    val type: AttachmentType,
    val name: String = "",
    val size: Long = 0,
    val isExisting: Boolean = false,
    val attachmentString: String? = null // e.g. "photo123_456" after upload or for existing
)

data class Comment(
    val id: Int,
    val fromId: Int,
    val ownerId: Int,
    val date: Long,
    val text: String,
    val authorName: String,
    val authorAvatar: String,
    val authorOnline: Boolean = false,
    val authorMobileOnline: Boolean = false,
    val authorVerified: Boolean,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val canDelete: Boolean = false,
    val imageUrls: List<String> = emptyList(),
    val videos: List<Video> = emptyList(),
    val audios: List<AudioTrack> = emptyList(),
    val documents: List<Document> = emptyList(),
    val poll: Poll? = null,
)

data class CommentsResponse(
    val items: List<Comment>,
    val count: Int,
    val canPost: Boolean = true,
)

data class Gift(
    val id: Int,
    val fromId: Int,
    val message: String,
    val date: Long,
    val thumb256: String,
    val senderName: String? = null,
)

data class GiftsResponse(
    val items: List<Gift>,
    val count: Int
)

data class GiftCategory(
    val id: Int,
    val title: String,
    val photo: String? = null
)

data class SelectableGift(
    val id: Int,
    val thumb256: String,
    val price: Int? = null,
    val priceStr: String? = null,
    val left: Int? = null
)

data class Photo(
    val id: Int,
    val ownerId: Int,
    val albumId: Int,
    val userId: Int,
    val text: String,
    val date: Long,
    val sizes: List<PhotoSize>,
    val url: String,
    val thumbUrl: String
)

data class PhotoSize(
    val type: String,
    val url: String,
    val width: Int,
    val height: Int
)

data class PhotosResponse(
    val items: List<Photo>,
    val count: Int
)

data class PhotoAlbum(
    val id: Int,
    val ownerId: Int,
    val title: String,
    val description: String,
    val size: Int,
    val thumbUrl: String? = null,
    val created: Long = 0,
    val updated: Long = 0,
    val canUpload: Boolean = false,
)

data class Topic(
    val id: Int,
    val ownerId: Int,
    val title: String,
    val created: Long,
    val updated: Long,
    val createdBy: Int,
    val commentsCount: Int,
    val isClosed: Boolean,
    val isFixed: Boolean,
)

data class TopicComment(
    val id: Int,
    val fromId: Int,
    val ownerId: Int,
    val topicId: Int,
    val date: Long,
    val text: String,
    val authorName: String,
    val authorAvatar: String,
    val authorOnline: Boolean = false,
    val authorMobileOnline: Boolean = false,
    val authorVerified: Boolean,
    val imageUrls: List<String> = emptyList(),
    val videos: List<Video> = emptyList(),
    val audios: List<AudioTrack> = emptyList(),
    val documents: List<Document> = emptyList(),
    val poll: Poll? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
)

data class TopicsResponse(
    val items: List<Topic>,
    val count: Int
)

data class TopicCommentsResponse(
    val items: List<TopicComment>,
    val count: Int,
    val profiles: List<UserProfile> = emptyList(),
    val groups: List<UserProfile> = emptyList()
)

data class Note(
    val id: Int,
    val ownerId: Int,
    val title: String,
    val text: String,
    val date: Long,
    val commentsCount: Int,
    val viewUrl: String? = null,
    val authorName: String? = null,
    val authorAvatar: String? = null
)

data class NotesResponse(
    val items: List<Note>,
    val count: Int
)

data class InstanceInfo(
    val name: String,
    val description: String,
    val openvkVersion: String,
    val statistics: InstanceStatistics? = null,
    val administrators: InstanceAdmins? = null,
    val links: InstanceLinks? = null
)

data class InstanceStatistics(
    val usersCount: Int = 0,
    val onlineUsersCount: Int = 0,
    val activeUsersCount: Int = 0,
    val groupsCount: Int = 0
)

data class InstanceAdmins(
    val items: List<UserProfile> = emptyList()
)

data class InstanceLinks(
    val items: List<InstanceLink> = emptyList()
)

data class InstanceLink(
    val name: String,
    val url: String
)
