package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import com.deliriousvoid.openvkmatcha.data.model.EditableProfileInfo
import com.deliriousvoid.openvkmatcha.data.model.GroupSettings
import com.deliriousvoid.openvkmatcha.data.model.PhotosResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

class ProfileRepository(private val api: OpenVKApi) {

    private val userFields = "photo_200,photo_max_orig,photo_400_orig,photo_200_orig,photo_max,crop_photo,status,online,verified,counters,about,screen_name,bdate,city,country,home_town,sex,relation,site,contacts,connections,education,activities,interests,music,movies,tv,books,games,quotes,friend_status,telegram,nickname,last_seen,online_mobile,can_post"

    suspend fun loadCurrentUser() = api.callMethod(
        "users.get",
        mapOf("fields" to userFields),
    ).map { JsonParsers.parseCurrentUser(it) }

    suspend fun getProfileInfo() = api.callMethod("account.getProfileInfo")
        .map { JsonParsers.parseProfileInfo(it) }

    suspend fun saveProfileInfo(info: EditableProfileInfo) = api.callMethod(
        "account.saveProfileInfo",
        mapOf(
            "first_name" to info.firstName,
            "last_name" to info.lastName,
            "screen_name" to info.screenName,
            "sex" to info.sex.toString(),
            "relation" to info.relation.toString(),
            "bdate" to info.bdate,
            "bdate_visibility" to info.bdateVisibility.toString(),
            "home_town" to info.homeTown,
            "status" to info.status
        ),
        isPost = true
    )

    suspend fun saveInterestsInfo(info: EditableProfileInfo) = api.callMethod(
        "account.saveInterestsInfo",
        mapOf(
            "about" to info.about,
            "activities" to info.activities,
            "interests" to info.interests,
            "fav_music" to info.music,
            "fav_films" to info.movies,
            "fav_shows" to info.tv,
            "fav_books" to info.books,
            "fav_games" to info.games,
            "fav_quote" to info.quotes
        ),
        isPost = true
    )

    suspend fun getGroupSettings(groupId: Int) = api.callMethod(
        "groups.getSettings",
        mapOf("group_id" to groupId.toString())
    ).map { JsonParsers.parseGroupSettings(it) }

    suspend fun editGroup(groupId: Int, settings: GroupSettings) = api.callMethod(
        "groups.edit",
        mapOf(
            "group_id" to groupId.toString(),
            "title" to settings.title,
            "description" to settings.description,
            "screen_name" to settings.screenName,
            "website" to settings.website
        ),
        isPost = true
    )

    suspend fun getOwnerPhotoUploadServer() = api.callMethod("photos.getOwnerPhotoUploadServer")

    suspend fun uploadFile(url: String, bytes: ByteArray, fileName: String) = api.uploadFile(url, bytes, fileName)

    suspend fun saveOwnerPhoto(server: String, photo: String, hash: String, mid: String) = api.callMethod(
        "photos.saveOwnerPhoto",
        mapOf(
            "server" to server,
            "photo" to photo,
            "hash" to hash,
            "mid" to mid
        ),
        isPost = true
    )

    suspend fun loadProfile(idOrName: String) = withContext(Dispatchers.IO) {
        val id = idOrName.toIntOrNull()
        if (id != null) {
            if (id == 0) {
                loadCurrentUser()
            } else {
                loadProfileById(id)
            }
        } else {
            api.resolveScreenName(idOrName).mapCatching { json ->
                val response = JsonParsers.getResponseObject(json)
                val type = response.optString("type")
                val objectId = response.optInt("object_id")
                if (type == "user") {
                    loadProfileById(objectId).getOrThrow()
                } else if (type == "group") {
                    loadProfileById(-objectId).getOrThrow()
                } else {
                    throw Exception("Не удалось разрешить имя")
                }
            }
        }
    }

    private suspend fun loadProfileById(id: Int) = if (id > 0) {
        api.callMethod(
            "users.get",
            mapOf(
                "user_ids" to id.toString(),
                "fields" to userFields
            )
        ).map { JsonParsers.parseCurrentUser(it) }
    } else {
        api.callMethod(
            "groups.getById",
            mapOf(
                "group_id" to (-id).toString(),
                "fields" to "status,verified,members_count,description,counters,topics,screen_name,site,photo_max,photo_400,photo_200,is_member,is_admin,admin_level,can_post,audios"
            )
        ).map { 
            val array = JsonParsers.getResponseItems(it)
            JsonParsers.parseGroup(array.getJSONObject(0))
        }
    }

    suspend fun loadUserWall(userId: Int, offset: Int = 0, count: Int = 20) = api.callMethod(
        "wall.get",
        mapOf(
            "owner_id" to userId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "fields" to "photo_50,verified",
        ),
    ).map { JsonParsers.parseWall(it) }

    suspend fun getAdminGroups(offset: Int = 0, count: Int = 50) = api.callMethod(
        "groups.get",
        mapOf(
            "filter" to "admin",
            "extended" to "1",
            "offset" to offset.toString(),
            "count" to count.toString(),
            "fields" to "status,verified,members_count,can_post,counters,audios"
        )
    ).map {
        val items = JsonParsers.getResponseItems(it)
        (0 until items.length()).map { i -> JsonParsers.parseGroup(items.getJSONObject(i)) }
    }

    suspend fun getFriends(userId: Int, offset: Int = 0, count: Int = 1000, onlyOnline: Boolean = false) = api.callMethod(
        "friends.get",
        mutableMapOf(
            "user_id" to userId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "fields" to "photo_50,photo_200,online,verified,status",
            "order" to "hints"
        ).apply {
            if (onlyOnline) put("only_online", "1")
        }
    ).map { JsonParsers.parseUsers(it) }

    suspend fun getFollowers(userId: Int, offset: Int = 0, count: Int = 50) = api.callMethod(
        "users.getFollowers",
        mapOf(
            "user_id" to userId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "fields" to "photo_200,online,verified,status"
        )
    ).map { JsonParsers.parseUsers(it) }

    suspend fun getMembers(groupId: Int, offset: Int = 0, count: Int = 50) = api.callMethod(
        "groups.getMembers",
        mapOf(
            "group_id" to groupId.absoluteValue.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "fields" to "photo_200,online,verified,status"
        )
    ).map { JsonParsers.parseUsers(it) }

    suspend fun getAlbums(userId: Int, offset: Int = 0, count: Int = 50) = api.callMethod(
        "photos.getAlbums",
        mapOf(
            "owner_id" to userId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "need_covers" to "1",
            "photo_sizes" to "1"
        )
    ).map { JsonParsers.parseAlbums(it) }

    suspend fun getAlbumsCount(userId: Int) = api.callMethod(
        "photos.getAlbumsCount",
        mapOf("owner_id" to userId.toString())
    ).map { JsonParsers.getResponseObject(it).optInt("response", 0) }

    suspend fun createAlbum(title: String, description: String = "", privacy: Int = 0, commentPrivacy: Int = 0) = api.callMethod(
        "photos.createAlbum",
        mapOf(
            "title" to title,
            "description" to description,
            "privacy" to privacy.toString(),
            "comment_privacy" to commentPrivacy.toString()
        )
    )

    suspend fun deleteAlbum(albumId: Int) = api.callMethod(
        "photos.deleteAlbum",
        mapOf("album_id" to albumId.toString())
    )

    suspend fun editAlbum(albumId: Int, title: String, description: String = "") = api.callMethod(
        "photos.editAlbum",
        mapOf(
            "album_id" to albumId.toString(),
            "title" to title,
            "description" to description
        )
    )

    suspend fun deletePhoto(ownerId: Int, photoId: Int) = api.callMethod(
        "photos.delete",
        mapOf(
            "owner_id" to ownerId.toString(),
            "photo_id" to photoId.toString()
        )
    )

    suspend fun editPhoto(ownerId: Int, photoId: Int, caption: String) = api.callMethod(
        "photos.edit",
        mapOf(
            "owner_id" to ownerId.toString(),
            "photo_id" to photoId.toString(),
            "caption" to caption
        )
    )

    suspend fun savePhotos(
        albumId: Int,
        groupId: Int? = null,
        server: String,
        photosList: String,
        hash: String,
        caption: String = ""
    ) = api.callMethod(
        "photos.save",
        mutableMapOf(
            "album_id" to albumId.toString(),
            "server" to server,
            "photos_list" to photosList,
            "hash" to hash,
            "caption" to caption
        ).apply {
            if (groupId != null) put("group_id", groupId.toString())
        }
    )

    suspend fun getUserPhotos(
        userId: Int,
        albumId: Int? = null,
        offset: Int = 0,
        count: Int = 200,
        reversed: Boolean = false
    ): Result<PhotosResponse> = if (albumId == null) {
        api.callMethod(
            "photos.getAll",
            mapOf(
                "owner_id" to userId.toString(),
                "offset" to offset.toString(),
                "count" to count.toString(),
                "extended" to "1",
                "photo_sizes" to "1",
                "no_service_albums" to "0",
                "rev" to if (reversed) "0" else "1"
            )
        ).map { JsonParsers.parsePhotos(it) }
    } else {
        // rev: 1 — newest first, 0 — oldest first
        val revValue = if (reversed) "0" else "1"
        api.callMethod(
            "photos.get",
            mapOf(
                "owner_id" to userId.toString(),
                "album_id" to albumId.toString(),
                "offset" to offset.toString(),
                "count" to count.toString(),
                "extended" to "1",
                "photo_sizes" to "1",
                "rev" to revValue
            )
        ).map { JsonParsers.parsePhotos(it) }
    }

    suspend fun getFriendRequests(offset: Int = 0, count: Int = 50) = api.callMethod(
        "friends.getRequests",
        mapOf(
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "need_viewed" to "1",
            "fields" to "photo_200,online,verified,status"
        )
    ).map { JsonParsers.parseUsers(it) }

    suspend fun addFriend(userId: Int) = api.callMethod(
        "friends.add",
        mapOf("user_id" to userId.toString())
    )

    suspend fun deleteFriend(userId: Int) = api.callMethod(
        "friends.delete",
        mapOf("user_id" to userId.toString())
    )

    suspend fun joinGroup(groupId: Int) = api.callMethod(
        "groups.join",
        mapOf("group_id" to groupId.absoluteValue.toString())
    )

    suspend fun leaveGroup(groupId: Int) = api.callMethod(
        "groups.leave",
        mapOf("group_id" to groupId.absoluteValue.toString())
    )

    suspend fun getUserGroups(userId: Int, offset: Int = 0, count: Int = 50) = api.callMethod(
        "groups.get",
        mapOf(
            "user_id" to userId.toString(),
            "extended" to "1",
            "offset" to offset.toString(),
            "count" to count.toString(),
            "fields" to "status,verified,members_count,can_post,counters,audios,group_type"
        )
    ).map {
        val items = JsonParsers.getResponseItems(it)
        (0 until items.length()).map { i -> JsonParsers.parseGroup(items.getJSONObject(i)) }
    }

    suspend fun searchGroups(query: String, offset: Int = 0, count: Int = 50) = api.callMethod(
        "groups.search",
        mapOf(
            "q" to query,
            "offset" to offset.toString(),
            "count" to count.toString(),
            "fields" to "status,verified,members_count,can_post,counters,audios"
        )
    ).map {
        val items = JsonParsers.getResponseItems(it)
        (0 until items.length()).map { i -> JsonParsers.parseGroup(items.getJSONObject(i)) }
    }

    suspend fun getUserEvents(userId: Int, offset: Int = 0, count: Int = 50) = api.callMethod(
        "groups.get",
        mapOf(
            "user_id" to userId.toString(),
            "extended" to "1",
            "count" to "1000", // Fetch more to filter locally
            "fields" to "status,verified,members_count,can_post,counters,audios,start_date"
        )
    ).map { 
        val items = JsonParsers.getResponseItems(it)
        val events = mutableListOf<UserProfile>()
        for (i in 0 until items.length()) {
            val group = JsonParsers.parseGroup(items.getJSONObject(i))
            if (group.groupType == "event") {
                events.add(group)
            }
        }
        events
    }

    suspend fun getUserGifts(userId: Int, offset: Int = 0, count: Int = 200) = api.callMethod(
        "gifts.get",
        mapOf(
            "user_id" to userId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1"
        )
    ).map { JsonParsers.parseGifts(it) }

    suspend fun getGiftCategories() = api.callMethod(
        "gifts.getCategories",
        mapOf("extended" to "1")
    ).map { JsonParsers.parseGiftCategories(it) }

    suspend fun getGiftsInCategory(categoryId: Int) = api.callMethod(
        "gifts.getGiftsInCategory",
        mapOf("id" to categoryId.toString())
    ).map { JsonParsers.parseGiftsInCategory(it) }

    suspend fun sendGift(userId: Int, giftId: Int, message: String, privacy: Int) = api.callMethod(
        "gifts.send",
        mapOf(
            "user_ids" to userId.toString(),
            "gift_id" to giftId.toString(),
            "message" to message,
            "privacy" to privacy.toString()
        )
    )

    suspend fun loadUsers(ids: List<Int>) = api.callMethod(
        "users.get",
        mapOf(
            "user_ids" to ids.joinToString(","),
            "fields" to "photo_50,photo_200,verified"
        )
    ).map { JsonParsers.parseUsers(it) }

    suspend fun loadGroupsByIds(ids: List<Int>) = api.callMethod(
        "groups.getById",
        mapOf(
            "group_ids" to ids.joinToString(",") { it.absoluteValue.toString() },
            "fields" to "status,verified,can_post,photo_50,photo_100,photo_200"
        )
    ).map { 
        val array = JsonParsers.getResponseItems(it)
        (0 until array.length()).map { i -> JsonParsers.parseGroup(array.getJSONObject(i)) }
    }

    suspend fun banUser(userId: Int) = api.callMethod(
        "account.ban",
        mapOf("owner_id" to userId.toString())
    )

    suspend fun unbanUser(userId: Int) = api.callMethod(
        "account.unban",
        mapOf("owner_id" to userId.toString())
    )

    suspend fun searchUsers(query: String, offset: Int = 0, count: Int = 100, sort: Int = 0) = api.callMethod(
        "users.search",
        mapOf(
            "q" to query,
            "offset" to offset.toString(),
            "count" to count.toString(),
            "sort" to sort.toString(),
            "fields" to "photo_50,photo_200,online,verified,status,friend_status"
        )
    ).map { JsonParsers.parseUsers(it) }

    suspend fun sendVotes(recipientId: Int, amount: Int, message: String? = null) = api.callMethod(
        "account.sendVotes",
        mutableMapOf(
            "receiver" to recipientId.toString(),
            "reciever" to recipientId.toString(), // Support both variants if any
            "value" to amount.toString()
        ).apply {
            if (!message.isNullOrBlank()) put("message", message)
        },
        isPost = true
    )
}
