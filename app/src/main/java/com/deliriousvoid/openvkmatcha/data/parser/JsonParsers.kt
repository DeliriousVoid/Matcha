package com.deliriousvoid.openvkmatcha.data.parser

import com.deliriousvoid.openvkmatcha.data.model.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object JsonParsers {

    private fun resolveUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (url.startsWith("//")) return "https:$url"
        
        var baseUrl = try {
            com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance.api.baseUrl.removeSuffix("/")
        } catch (e: Exception) {
            ""
        }
        
        if (baseUrl.isBlank()) return url

        // OpenVK specific: if we are using api. subdomain for API, 
        // images might be on the root domain.
        if (url.startsWith("/") && baseUrl.contains("://api.")) {
            baseUrl = baseUrl.replace("://api.", "://")
        }

        return if (url.startsWith("/")) {
            "$baseUrl$url"
        } else {
            "$baseUrl/$url"
        }
    }

    private fun JSONObject.optStringClean(key: String): String? {
        if (isNull(key)) return null
        val value = optString(key)
        val clean = value.takeIf { it.isNotBlank() && it.lowercase() != "null" } ?: return null
        
        return try {
            val htmlCompatible = clean.replace("\r\n", "<br>").replace("\n", "<br>")
            android.text.Html.fromHtml(htmlCompatible, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (_: Exception) {
            clean
        }
    }

    private fun JSONObject.optUrl(key: String): String? {
        if (isNull(key)) return null
        val value = optString(key)
        val clean = value.takeIf { it.isNotBlank() && it.lowercase() != "null" } ?: return null
        return resolveUrl(clean)
    }

    private fun JSONObject.optBool(key: String, defaultValue: Boolean): Boolean {
        if (isNull(key)) return defaultValue
        val v = opt(key)
        return when (v) {
            is Boolean -> v
            is Int -> v == 1
            is Long -> v == 1L
            is String -> v == "1" || v.lowercase() == "true"
            else -> defaultValue
        }
    }

    fun parseUser(json: JSONObject): UserProfile {
        val counters = json.optJSONObject("counters")
        val cropPhoto = json.optJSONObject("crop_photo")
        val photoFromCrop = cropPhoto?.optJSONObject("photo")?.optJSONArray("sizes")?.let { largestPhotoUrl(it) }

        return UserProfile(
            id = json.optInt("id"),
            firstName = json.optStringClean("first_name") ?: "",
            lastName = json.optStringClean("last_name") ?: "",
            screenName = json.optStringClean("screen_name") ?: "",
            photo50 = json.optUrl("photo_50") ?: "",
            photo200 = json.optUrl("photo_200") ?: json.optUrl("photo_50") ?: "",
            photoMaxOrig = json.optUrl("photo_max") 
                ?: json.optUrl("photo_max_orig") 
                ?: json.optUrl("photo_400_orig") 
                ?: json.optUrl("photo_400")
                ?: photoFromCrop
                ?: json.optUrl("photo_200_orig")
                ?: json.optUrl("photo_200"),
            status = json.optStringClean("status") ?: "",
            online = json.optBool("online", false),
            verified = json.optBool("verified", false),
            friendsCount = counters?.optInt("friends", 0) ?: 0,
            followersCount = counters?.optInt("followers", 0) ?: 0,
            photosCount = counters?.optInt("photos", 0) ?: 0,
            audiosCount = counters?.optInt("audios", 0) ?: 0,
            groupsCount = counters?.optInt("groups", 0) ?: 0,
            giftsCount = counters?.optInt("gifts", 0) ?: 0,
            about = json.optStringClean("about") ?: "",
            isGroup = false,
            friendStatus = json.optInt("friend_status", -1).takeIf { it != -1 },
            blacklistedByMe = json.optBool("blacklisted_by_me", false),
            isIgnored = json.optBool("is_ignored", false),
            canPost = json.optBool("can_post", true),

            // Extended
            bdate = json.optStringClean("bdate"),
            city = json.optJSONObject("city")?.optStringClean("title"),
            country = json.optJSONObject("country")?.optStringClean("title"),
            homeTown = json.optStringClean("home_town"),
            sex = json.optInt("sex").takeIf { it != 0 },
            relation = json.optInt("relation").takeIf { it != 0 },
            site = json.optStringClean("site"),
            mobilePhone = json.optStringClean("mobile_phone"),
            homePhone = json.optStringClean("home_phone"),
            skype = json.optStringClean("skype"),
            facebook = json.optStringClean("facebook"),
            twitter = json.optStringClean("twitter"),
            instagram = json.optStringClean("instagram"),
            universityName = json.optStringClean("university_name"),
            facultyName = json.optStringClean("faculty_name"),
            graduation = json.optInt("graduation").takeIf { it > 0 },
            activities = json.optStringClean("activities"),
            interests = json.optStringClean("interests"),
            music = json.optStringClean("music"),
            movies = json.optStringClean("movies"),
            tv = json.optStringClean("tv"),
            books = json.optStringClean("books"),
            games = json.optStringClean("games"),
            quotes = json.optStringClean("quotes"),
            telegram = json.optStringClean("telegram"),
            nickname = json.optStringClean("nickname"),
            lastSeen = json.optJSONObject("last_seen")?.let { parseLastSeen(it) },
            mobileOnline = json.optBool("online_mobile", false),
        )
    }

    private fun parseLastSeen(json: JSONObject): LastSeen {
        return LastSeen(
            platform = json.optInt("platform"),
            time = json.optLong("time")
        )
    }

    fun parseGroup(json: JSONObject): UserProfile {
        val counters = json.optJSONObject("counters") ?: json.optJSONObject("correct_counters")
        
        val photos = counters?.optInt("photos") ?: json.optInt("photos_count") ?: 0
        val albums = counters?.optInt("albums") ?: json.optInt("albums_count") ?: 0
        val audios = counters?.optInt("audios") ?: json.optInt("audios_count") ?: 0
        val topics = counters?.optInt("topics") ?: json.optInt("topics_count") ?: 0
        val videos = counters?.optInt("videos") ?: json.optInt("videos_count") ?: 0
        val docs = counters?.optInt("docs") ?: json.optInt("docs_count") ?: 0

        return UserProfile(
            id = -json.optInt("id").let { if (it > 0) it else -it }, // Force negative
            firstName = json.optStringClean("name") ?: "",
            lastName = "",
            screenName = json.optStringClean("screen_name") ?: "",
            photo50 = json.optUrl("photo_50") ?: "",
            photo200 = json.optUrl("photo_200") ?: json.optUrl("photo_50") ?: "",
            photoMaxOrig = json.optUrl("photo_max") 
                ?: json.optUrl("photo_400") 
                ?: json.optUrl("photo_max_orig")
                ?: json.optUrl("photo_400_orig")
                ?: json.optUrl("photo_200"),
            status = json.optStringClean("status") ?: "",
            online = false,
            verified = json.optBool("verified", false),
            friendsCount = 0,
            followersCount = json.optInt("members_count", 0),
            photosCount = if (photos > 0) photos else albums,
            audiosCount = audios,
            topicsCount = topics,
            about = json.optStringClean("description") ?: "",
            isGroup = true,
            isMember = json.optBool("is_member", false),
            isAdmin = json.optBool("is_admin", false),
            adminLevel = json.optInt("admin_level", 0),
            isIgnored = json.optBool("is_ignored", false),
            canPost = json.optBool("can_post", false),
            site = json.optStringClean("site"),
            groupType = json.optString("type")
        )
    }

    fun getResponseItems(response: JSONObject): JSONArray {
        val resp = response.opt("response")
        return when (resp) {
            is JSONArray -> resp
            is JSONObject -> resp.optJSONArray("items") ?: JSONArray()
            else -> JSONArray()
        }
    }

    fun getResponseObject(response: JSONObject): JSONObject {
        return response.optJSONObject("response") ?: response
    }

    fun parseUsers(response: JSONObject): List<UserProfile> {
        val array = getResponseItems(response)
        return (0 until array.length()).map { parseUser(array.getJSONObject(it)) }
    }

    fun parseCurrentUser(response: JSONObject): UserProfile {
        val array = getResponseItems(response)
        if (array.length() == 0) throw JSONException("Empty response array")
        return parseUser(array.getJSONObject(0))
    }

    fun parseNewsfeed(response: JSONObject): com.deliriousvoid.openvkmatcha.data.model.NewsfeedResponse {
        val responseObj = getResponseObject(response)
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        val profiles = responseObj.optJSONArray("profiles") ?: JSONArray()
        val groups = responseObj.optJSONArray("groups") ?: JSONArray()
        val nextFrom = responseObj.optString("next_from").takeIf { it.isNotBlank() }
        
        return com.deliriousvoid.openvkmatcha.data.model.NewsfeedResponse(
            posts = parsePosts(items, profiles, groups),
            nextFrom = nextFrom
        )
    }

    fun parseWall(response: JSONObject): List<Post> {
        val responseObj = getResponseObject(response)
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        val profiles = responseObj.optJSONArray("profiles") ?: JSONArray()
        val groups = responseObj.optJSONArray("groups") ?: JSONArray()
        return parsePosts(items, profiles, groups)
    }

    private fun parsePosts(items: JSONArray, profiles: JSONArray, groups: JSONArray): List<Post> {
        val profileMap = parseProfileMap(profiles)
        val groupMap = parseGroupMap(groups)
        val posts = mutableListOf<Post>()

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val postData = if (item.optString("type") == "post" && item.has("post")) {
                item.getJSONObject("post")
            } else {
                item
            }

            val fromId = postData.optInt("from_id", postData.optInt("owner_id"))
            val author = resolveAuthor(fromId, profileMap, groupMap)

            val likes = postData.optJSONObject("likes")
            val comments = postData.optJSONObject("comments")
            val reposts = postData.optJSONObject("reposts")
            val attachmentsArray = postData.optJSONArray("attachments")
            val copyHistoryArray = postData.optJSONArray("copy_history")
            
            val attachments = parseAttachments(attachmentsArray)

            val geo = postData.optJSONObject("geo")?.let { parseGeo(it) }

            val copyHistory = if (copyHistoryArray != null && copyHistoryArray.length() > 0) {
                parsePosts(copyHistoryArray, profiles, groups)
            } else null

            val postSource = postData.optJSONObject("post_source")
            val platform = postSource?.optString("platform")?.takeIf { it.isNotBlank() }
                ?: postSource?.optString("type")?.takeIf { it.isNotBlank() }
            val sourceName = postSource?.optString("name")?.takeIf { it.isNotBlank() }
                ?: postSource?.optString("data")?.takeIf { it.isNotBlank() }

            val copyright = postData.optJSONObject("copyright")
            val copyrightLink = copyright?.optString("link")?.takeIf { it.isNotBlank() }
            val copyrightName = copyright?.optString("name")?.takeIf { it.isNotBlank() }

            posts.add(
                Post(
                    id = postData.optInt("id"),
                    ownerId = postData.optInt("owner_id"),
                    authorId = fromId,
                    authorName = author.name,
                    authorAvatar = author.avatar,
                    authorOnline = author.online,
                    authorMobileOnline = author.mobileOnline,
                    authorVerified = author.verified,
                    text = postData.optStringClean("text") ?: "",
                    date = postData.optLong("date"),
                    likeCount = likes?.optInt("count", 0) ?: 0,
                    commentCount = comments?.optInt("count", 0) ?: 0,
                    repostCount = reposts?.optInt("count", 0) ?: 0,
                    isLiked = likes?.optBool("user_likes", false) ?: false,
                    isPinned = postData.optBool("is_pinned", false),
                    isNsfw = postData.optBool("explicit", false) || 
                             postData.optBool("is_explicit", false),
                    imageUrls = attachments.imageUrls,
                    videos = attachments.videos,
                    audios = attachments.audios,
                    documents = attachments.documents,
                    copyHistory = copyHistory,
                    platform = platform,
                    sourceName = sourceName,
                    copyrightName = copyrightName,
                    copyrightLink = copyrightLink,
                    poll = attachments.poll,
                    geo = geo,
                    canEdit = postData.optBool("can_edit", false),
                    canDelete = postData.optBool("can_delete", false),
                    canPin = postData.optBool("can_pin", false)
                )
            )
        }
        return posts
    }

    fun parseConversations(response: JSONObject): List<Conversation> {
        val responseObj = getResponseObject(response)
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        val profiles = responseObj.optJSONArray("profiles") ?: JSONArray()
        val groups = responseObj.optJSONArray("groups") ?: JSONArray()
        val profileMap = parseProfileMap(profiles)
        val groupMap = parseGroupMap(groups)

        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val conversation = item.getJSONObject("conversation")
            val lastMessageObj = item.optJSONObject("last_message")
            val peer = conversation.getJSONObject("peer")
            val peerId = peer.optInt("id")
            val peerType = peer.optString("type", "user")
            val author = if (peerType == "chat") {
                AuthorInfo(peerId, conversation.optJSONObject("chat_settings")?.optStringClean("title") ?: "Чат", "", false)
            } else {
                resolveAuthor(peerId, profileMap, groupMap)
            }

            Conversation(
                peerId = peerId,
                title = author.name,
                lastMessage = lastMessageObj?.optStringClean("text") ?: "",
                lastMessageDate = lastMessageObj?.optLong("date", 0) ?: 0,
                unreadCount = conversation.optInt("unread_count", 0),
                peerPhoto = author.avatar,
                isOnline = author.online,
                isMobileOnline = author.mobileOnline,
                peerVerified = author.verified,
            )
        }
    }

    fun parseMessages(response: JSONObject, peerId: Int): List<ChatMessage> {
        val responseObj = getResponseObject(response)
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        return (0 until items.length()).map { index ->
            val msg = items.getJSONObject(index)
            ChatMessage(
                id = msg.optInt("id"),
                peerId = peerId,
                fromId = msg.optInt("from_id"),
                text = msg.optStringClean("text") ?: "",
                date = msg.optLong("date"),
                isOutgoing = msg.optInt("out", 0) == 1,
                isRead = msg.optInt("read_state", 1) == 1,
            )
        }.reversed()
    }

    fun parseAudio(response: JSONObject): List<AudioTrack> {
        val items = getResponseItems(response)
        return (0 until items.length()).map { index ->
            parseAudioItem(items.getJSONObject(index))
        }
    }

    private fun parseAudioItem(audio: JSONObject): AudioTrack {
        val url = audio.optUrl("url")
        return AudioTrack(
            id = audio.optInt("id"),
            ownerId = audio.optInt("owner_id"),
            artist = audio.optStringClean("artist") ?: "Неизвестный исполнитель",
            title = audio.optStringClean("title") ?: "Без названия",
            duration = audio.optInt("duration", 0),
            url = url,
            remoteUrl = url,
            artworkUrl = null,
            isAdded = audio.optBool("is_added", false) ||
                      audio.optBool("added", false) ||
                      audio.optBool("user_added", false),
            accessKey = audio.optString("access_key").takeIf { it.isNotBlank() }
        )
    }

    fun parsePlaylists(response: JSONObject): List<Playlist> {
        val responseObj = response.optJSONObject("response") ?: response
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val photoObj = item.optJSONObject("photo")
            val thumbsObj = item.optJSONObject("thumbs")
            val coverObj = item.optJSONObject("cover")
            
            // Try to find track count in various fields
            fun getCount(key: String) = item.opt(key)?.toString()?.toIntOrNull()
            val count = getCount("count")
                ?: getCount("size")
                ?: getCount("track_count")
                ?: getCount("tracks_count")
                ?: getCount("total")
                ?: getCount("items_count")
                ?: 0

            // Try to find cover photo in various fields
            val photoUrl = item.optUrl("cover_url")
                ?: item.optUrl("photo_300")
                ?: item.optUrl("photo_600")
                ?: item.optUrl("photo")
                ?: photoObj?.optUrl("photo_300")
                ?: photoObj?.optUrl("photo_600")
                ?: thumbsObj?.optUrl("photo_300")
                ?: thumbsObj?.optUrl("photo_600")
                ?: coverObj?.optUrl("photo_300")
                ?: coverObj?.optUrl("photo_600")
                ?: item.optUrl("photo_max")

            Playlist(
                id = item.optInt("id"),
                ownerId = item.optInt("owner_id"),
                title = item.optStringClean("title") ?: "Плейлист",
                description = item.optStringClean("description") ?: "",
                trackCount = count,
                photoUrl = photoUrl
            )
        }
    }

    private data class AuthorInfo(
        val id: Int,
        val name: String,
        val avatar: String,
        val verified: Boolean,
        val online: Boolean = false,
        val mobileOnline: Boolean = false,
    )

    private fun resolveAuthor(id: Int, profiles: Map<Int, AuthorInfo>, groups: Map<Int, AuthorInfo>): AuthorInfo {
        if (id > 0) {
            val profile = profiles[id]
            // If profile is missing OR it's a "DELETED" stub, check groups
            if (profile == null || profile.name == "DELETED" || profile.name == "DELETED ") {
                groups[-id]?.let { return it }
            }
            if (profile != null) return profile
            return AuthorInfo(id, "id$id", "", false)
        } else if (id < 0) {
            groups[id]?.let { return it }
            profiles[-id]?.let { return it }
            return AuthorInfo(id, "club${-id}", "", false)
        }
        return AuthorInfo(0, "DELETED", "", false)
    }

    private fun parseProfileMap(profiles: JSONArray): Map<Int, AuthorInfo> {
        val map = mutableMapOf<Int, AuthorInfo>()
        for (i in 0 until profiles.length()) {
            val p = profiles.getJSONObject(i)
            val id = p.optInt("id").let { if (it != 0) it else p.optInt("uid") }
            if (id == 0) continue
            
            map[id] = AuthorInfo(
                id = id,
                name = "${p.optStringClean("first_name") ?: ""} ${p.optStringClean("last_name") ?: ""}".trim(),
                avatar = p.optUrl("photo_50") ?: p.optUrl("photo_100") ?: p.optUrl("photo_200") ?: "",
                verified = p.optBool("verified", false),
                online = p.optBool("online", false),
                mobileOnline = p.optBool("online_mobile", false),
            )
        }
        return map
    }

    private fun parseGroupMap(groups: JSONArray): Map<Int, AuthorInfo> {
        val map = mutableMapOf<Int, AuthorInfo>()
        for (i in 0 until groups.length()) {
            val g = groups.getJSONObject(i)
            val rawId = when {
                g.has("id") -> g.optInt("id")
                g.has("gid") -> g.optInt("gid")
                g.has("uid") -> g.optInt("uid")
                else -> 0
            }
            if (rawId == 0) continue
            
            val id = -kotlin.math.abs(rawId)
            map[id] = AuthorInfo(
                id = id,
                name = g.optStringClean("name") ?: "",
                avatar = g.optUrl("photo_50") ?: g.optUrl("photo_100") ?: g.optUrl("photo_200") ?: "",
                verified = g.optBool("verified", false),
            )
        }
        return map
    }

    fun parseNotifications(response: JSONObject, isArchived: Boolean = false): NotificationsResponse {
        val responseObj = getResponseObject(response)
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        val profiles = responseObj.optJSONArray("profiles") ?: JSONArray()
        val groups = responseObj.optJSONArray("groups") ?: JSONArray()
        val nextFrom = responseObj.optString("next_from").takeIf { it.isNotBlank() }
        val lastViewed = responseObj.optLong("last_viewed", 0)

        val profileMap = parseProfileMap(profiles)
        val groupMap = parseGroupMap(groups)
        
        val parsedItems = mutableListOf<Notification>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val type = item.optString("type")
            val date = item.optLong("date")
            val feedback = item.optJSONObject("feedback")
            val parent = item.optJSONObject("parent")
            
            // Robust fromId extraction with multiple fallbacks (as in Flux)
            var fromId = 0

            // 1. Check feedback items
            val feedbackItems = feedback?.optJSONArray("items")
            if (feedbackItems != null && feedbackItems.length() > 0) {
                val firstItem = feedbackItems.optJSONObject(0)
                fromId = firstItem.optInt("from_id", 0)
                if (fromId == 0) fromId = firstItem.optInt("id", 0)
            }
            
            // 2. Check direct from_id in feedback or item
            if (fromId == 0) fromId = feedback?.optInt("from_id", 0) ?: 0
            if (fromId == 0) fromId = item.optInt("from_id", 0)
            
            // 3. Fallback for comments where author might be in feedback.id
            if (fromId == 0 && (type == "comment_post" || type == "comment_photo")) {
                fromId = feedback?.optInt("id", 0) ?: 0
            }

            // 4. Last resort: parent from_id or parent id (for sent_gift)
            if (fromId == 0) fromId = parent?.optInt("from_id", 0) ?: 0
            if (fromId == 0 && type == "sent_gift") fromId = parent?.optInt("id", 0) ?: 0
            
            // Extract IDs for navigation
            var ownerId = 0
            var itemId = 0
            
            if (type == "mention") {
                itemId = feedback?.optInt("id", 0) ?: 0
                ownerId = feedback?.optInt("to_id", 0) ?: 0
                if (ownerId == 0) ownerId = feedback?.optInt("from_id", 0) ?: 0
            } else if (parent != null) {
                itemId = parent.optInt("id", 0)
                ownerId = parent.optInt("owner_id", 0)
                if (ownerId == 0) ownerId = parent.optInt("to_id", 0)
                if (ownerId == 0) ownerId = parent.optInt("from_id", 0)
                
                if (type == "like_comment" || type == "reply_comment") {
                    val postId = parent.optInt("post_id", 0)
                    if (postId != 0) itemId = postId
                }
            } else if (feedback != null) {
                itemId = feedback.optInt("id", 0)
                ownerId = feedback.optInt("owner_id", 0)
                if (ownerId == 0) ownerId = feedback.optInt("to_id", 0)
            }
            
            val author = resolveAuthor(fromId, profileMap, groupMap)
            
            val actionText = when (type) {
                "like_post" -> "понравился ваш пост"
                "like_comment" -> "оценил ваш комментарий"
                "reply_comment" -> "ответил на ваш комментарий"
                "reply_post" -> "ответил на ваш пост"
                "mention" -> "упомянул вас"
                "comment_post" -> "прокомментировал ваш пост"
                "comment_photo" -> "оставил(а) комментарий под вашей фотографией"
                "copy_post" -> "поделился(-ась) вашим постом"
                "follow" -> "подписался на вас"
                "sent_gift" -> "отправил вам подарок"
                "wall" -> "написал(а) на вашей стене пост"
                else -> "выполнил действие ($type)"
            }

            fun extractCleanText(obj: JSONObject?): String? {
                if (obj == null) return null
                val fields = arrayOf("text", "message", "body")
                for (f in fields) {
                    val v = obj.optStringClean(f)
                    if (v != null) return v
                }
                return null
            }

            fun getAttachmentMarkers(obj: JSONObject?): String {
                if (obj == null) return ""
                val sb = StringBuilder()
                val attachments = obj.optJSONArray("attachments") ?: obj.optJSONArray("attachment")
                if (attachments != null) {
                    val seen = mutableSetOf<String>()
                    for (j in 0 until attachments.length()) {
                        val att = attachments.optJSONObject(j)
                        val attType = att?.optString("type") ?: continue
                        if (seen.contains(attType)) continue
                        seen.add(attType)
                        
                        val label = when (attType) {
                            "photo" -> "[изображение]"
                            "video" -> "[видео]"
                            "audio" -> "[аудио]"
                            "doc" -> "[файл]"
                            "link" -> "[ссылка]"
                            "wall" -> "[Запись]"
                            else -> "[$attType]"
                        }
                        if (sb.isNotEmpty()) sb.append(" ")
                        sb.append(label)
                    }
                }
                
                val nestedItems = obj.optJSONArray("items")
                if (nestedItems != null && nestedItems.length() > 0) {
                    val markers = getAttachmentMarkers(nestedItems.optJSONObject(0))
                    if (markers.isNotBlank()) {
                        if (sb.isNotEmpty()) sb.append(" ")
                        sb.append(markers)
                    }
                }
                
                return sb.toString()
            }

            fun getFullText(obj: JSONObject?): String? {
                if (obj == null) return null
                val text = extractCleanText(obj)
                val markers = getAttachmentMarkers(obj)
                return when {
                    text != null && markers.isNotBlank() -> "$markers $text"
                    text != null -> text
                    markers.isNotBlank() -> markers
                    else -> null
                }
            }

            // Extract main content text (e.g. comment)
            val firstFeedbackItem = feedback?.optJSONArray("items")?.optJSONObject(0)
            val extractedContent = getFullText(firstFeedbackItem) ?: getFullText(feedback) ?: getFullText(item)
            val extractedParent = if (type == "sent_gift") null else getFullText(parent)

            // Ported logic from MatchaOLD: show certain types in the grey context box
            val (mainText, parentContextText) = when (type) {
                "comment_post", "reply_comment", "reply_post", "mention", "comment_photo" -> {
                    // Try to get content text. If missing, fallback to parent text 
                    // so the box doesn't disappear completely.
                    val boxText = extractedContent ?: extractedParent
                    null to boxText
                }
                "sent_gift" -> {
                    val giftText = if (author.name.isNotBlank()) "Подарок от ${author.name}" else "Подарок"
                    giftText to null
                }
                else -> {
                    extractedContent to extractedParent
                }
            }

            val notificationId = "${if (isArchived) "archived_" else ""}${type}_${date}_$i"

            parsedItems.add(
                Notification(
                    id = notificationId,
                    type = type,
                    action = actionText,
                    date = date,
                    authorId = fromId,
                    authorName = author.name,
                    authorAvatar = author.avatar,
                    authorOnline = author.online,
                    authorMobileOnline = author.mobileOnline,
                    authorVerified = author.verified,
                    text = mainText,
                    parentText = parentContextText,
                    ownerId = ownerId,
                    itemId = itemId,
                    isRead = if (isArchived) true else date <= lastViewed,
                    isArchived = isArchived
                )
            )
        }
        
        return NotificationsResponse(
            items = parsedItems,
            nextFrom = nextFrom,
            unreadCount = 0
        )
    }

    fun parseComments(response: JSONObject): CommentsResponse {
        val responseObj = getResponseObject(response)
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        val count = responseObj.optInt("count", 0)
        val profiles = responseObj.optJSONArray("profiles") ?: JSONArray()
        val groups = responseObj.optJSONArray("groups") ?: JSONArray()
        val canPost = responseObj.optBool("can_post", true)

        val profileMap = parseProfileMap(profiles)
        val groupMap = parseGroupMap(groups)

        val parsedComments = (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val fromId = item.optInt("from_id", item.optInt("uid", item.optInt("user_id")))
            val author = resolveAuthor(fromId, profileMap, groupMap)
            val likes = item.optJSONObject("likes")
            val attachments = parseAttachments(item.optJSONArray("attachments"))

            Comment(
                id = item.optInt("id"),
                fromId = author.id,
                ownerId = item.optInt("owner_id"),
                date = item.optLong("date"),
                text = item.optStringClean("text") ?: item.optStringClean("message") ?: "",
                authorName = author.name,
                authorAvatar = author.avatar,
                authorOnline = author.online,
                authorMobileOnline = author.mobileOnline,
                authorVerified = author.verified,
                likeCount = likes?.optInt("count") ?: 0,
                isLiked = likes?.optBool("user_likes", false) ?: false,
                canDelete = item.optBool("can_delete", false),
                imageUrls = attachments.imageUrls,
                videos = attachments.videos,
                audios = attachments.audios,
                documents = attachments.documents,
                poll = attachments.poll,
                replyToComment = item.optInt("reply_to_comment").takeIf { it != 0 },
                replyToUser = item.optInt("reply_to_user").takeIf { it != 0 }
            )
        }

        return CommentsResponse(
            items = parsedComments,
            count = count,
            canPost = canPost
        )
    }

    fun parseGifts(response: JSONObject): GiftsResponse {
        val responseObj = getResponseObject(response)
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        val count = responseObj.optInt("count", 0)
        
        // OpenVK might return profiles/groups inside 'response' or even at the root
        val profiles = responseObj.optJSONArray("profiles") ?: response.optJSONArray("profiles") ?: JSONArray()
        val groups = responseObj.optJSONArray("groups") ?: response.optJSONArray("groups") ?: JSONArray()
        
        val profileMap = parseProfileMap(profiles)
        val groupMap = parseGroupMap(groups)

        val parsedGifts = (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val giftObj = item.optJSONObject("gift") ?: JSONObject()
            val fromId = item.optInt("from_id")

            // Try to find author in maps
            val author = resolveAuthor(fromId, profileMap, groupMap)
            
            // Check if we actually found a real name in the profiles/groups list
            val isAuthorInList = if (fromId > 0) profileMap.containsKey(fromId) else groupMap.containsKey(fromId)
            
            val senderName = if (fromId != 0 && isAuthorInList && author.name.isNotBlank() &&
                author.name != "DELETED" && !author.name.startsWith("id$fromId") && !author.name.startsWith("club")) {
                author.name
            } else {
                // Last fallback: if the API included some basic user info in the item itself (non-standard but possible)
                item.optJSONObject("user")?.let { u ->
                    val fn = u.optString("first_name", "")
                    val ln = u.optString("last_name", "")
                    "$fn $ln".trim().takeIf { it.isNotBlank() }
                }
            }

            Gift(
                id = item.optInt("id"),
                fromId = fromId,
                message = item.optStringClean("message") ?: "",
                date = item.optLong("date"),
                thumb256 = giftObj.optUrl("thumb_256") ?: "",
                senderName = senderName
            )
        }

        return GiftsResponse(
            items = parsedGifts,
            count = count
        )
    }

    fun parseProfileInfo(response: JSONObject): EditableProfileInfo {
        val obj = getResponseObject(response)
        var bdateVisibility = obj.optInt("bdate_visibility", 1)
        if (bdateVisibility == 0) bdateVisibility = 1

        return EditableProfileInfo(
            firstName = obj.optStringClean("first_name") ?: "",
            lastName = obj.optStringClean("last_name") ?: "",
            screenName = obj.optStringClean("screen_name") ?: "",
            sex = obj.optInt("sex", 0),
            relation = obj.optInt("relation", 0),
            bdate = obj.optStringClean("bdate") ?: "",
            bdateVisibility = bdateVisibility,
            homeTown = obj.optStringClean("home_town") ?: "",
            status = obj.optStringClean("status") ?: "",
            photo200 = obj.optStringClean("photo_200"),
            about = obj.optStringClean("about") ?: "",
            activities = obj.optStringClean("activities") ?: "",
            interests = obj.optStringClean("interests") ?: "",
            music = obj.optStringClean("music") ?: "",
            movies = obj.optStringClean("movies") ?: "",
            tv = obj.optStringClean("tv") ?: "",
            books = obj.optStringClean("books") ?: "",
            games = obj.optStringClean("games") ?: "",
            quotes = obj.optStringClean("quotes") ?: ""
        )
    }

    fun parseGroupSettings(response: JSONObject): GroupSettings {
        val obj = getResponseObject(response)
        return GroupSettings(
            title = obj.optStringClean("title") ?: "",
            description = obj.optStringClean("description") ?: "",
            screenName = obj.optStringClean("screen_name") ?: "",
            website = obj.optStringClean("website") ?: ""
        )
    }

    fun parseGiftCategories(response: JSONObject): List<GiftCategory> {
        val items = getResponseItems(response)
        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            GiftCategory(
                id = item.optInt("id"),
                title = item.optStringClean("title") ?: item.optStringClean("name") ?: "",
                photo = item.optUrl("photo") ?: item.optUrl("image") ?: item.optUrl("icon") ?: item.optUrl("thumb") ?: item.optUrl("thumb_256")
            )
        }
    }

    fun parseGiftsInCategory(response: JSONObject): List<SelectableGift> {
        android.util.Log.d("MatchaParser", "FULL RESPONSE gifts.getGiftsInCategory: $response")
        val items = getResponseItems(response)
        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val giftObj = item.optJSONObject("gift") ?: item
            
            var giftId = giftObj.optInt("id").takeIf { it != 0 } 
                ?: item.optInt("gift_id").takeIf { it != 0 }
                ?: item.optInt("id").takeIf { it != 0 }
                ?: giftObj.optInt("gift_id").takeIf { it != 0 }
                ?: item.optInt("tid")
            
            // Fallback: try to extract ID from image URL if it follows /gift(\d+)_ pattern
            if (giftId == 0) {
                val imageUrl = giftObj.optString("image") ?: giftObj.optString("photo") ?: giftObj.optString("thumb") ?: ""
                if (imageUrl.contains("gift")) {
                    val match = Regex("gift(\\d+)_").find(imageUrl)
                    if (match != null) {
                        giftId = match.groupValues[1].toIntOrNull() ?: 0
                    }
                }
            }

            if (giftId == 0) {
                android.util.Log.e("MatchaParser", "Failed to parse gift ID from: $item")
            }

            SelectableGift(
                id = giftId,
                thumb256 = giftObj.optUrl("thumb_256") ?: giftObj.optUrl("thumb") ?: giftObj.optUrl("photo") ?: giftObj.optUrl("image") ?: giftObj.optUrl("photo_256") ?: "",
                price = giftObj.optInt("price").takeIf { giftObj.has("price") } ?: item.optInt("price").takeIf { item.has("price") },
                priceStr = (giftObj.optString("price_str").takeIf { it.isNotBlank() && it != "null" } 
                    ?: item.optString("price_str").takeIf { it.isNotBlank() && it != "null" }),
                left = giftObj.optInt("left").takeIf { giftObj.has("left") } ?: item.optInt("left").takeIf { item.has("left") } ?: item.optInt("usages_left").takeIf { item.has("usages_left") }
            )
        }
    }

    fun parsePhotos(response: JSONObject): PhotosResponse {
        val respObj = getResponseObject(response)
        val itemsArray = respObj.optJSONArray("items") ?: JSONArray()
        val count = respObj.optInt("count", 0)
        
        val photos = (0 until itemsArray.length()).map { 
            parsePhoto(itemsArray.getJSONObject(it))
        }
        return PhotosResponse(photos, count)
    }

    fun parseAlbums(response: JSONObject): List<PhotoAlbum> {
        val items = getResponseItems(response)
        return (0 until items.length()).map { parseAlbum(items.getJSONObject(it)) }
    }

    fun parseTopics(response: JSONObject): TopicsResponse {
        val responseObj = getResponseObject(response)
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        val count = responseObj.optInt("count", 0)
        val topics = (0 until items.length()).map { parseTopic(items.getJSONObject(it)) }
        return TopicsResponse(topics, count)
    }

    private fun parseTopic(json: JSONObject): Topic {
        val id = json.optInt("id").takeIf { it != 0 } ?: json.optInt("tid")
        return Topic(
            id = id,
            ownerId = json.optInt("owner_id", json.optInt("group_id")),
            title = json.optStringClean("title") ?: "",
            created = json.optLong("created"),
            updated = json.optLong("updated"),
            createdBy = json.optInt("created_by"),
            commentsCount = json.optInt("comments", 0),
            isClosed = json.optInt("is_closed", 0) == 1,
            isFixed = json.optInt("is_fixed", 0) == 1
        )
    }

    fun parseTopicComments(response: JSONObject): TopicCommentsResponse {
        val responseObj = getResponseObject(response)
        val items = responseObj.optJSONArray("items") ?: JSONArray()
        val count = responseObj.optInt("count", 0)
        val profiles = parseProfileMap(responseObj.optJSONArray("profiles") ?: JSONArray())
        val groups = parseGroupMap(responseObj.optJSONArray("groups") ?: JSONArray())

        val comments = (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val fromId = item.optInt("from_id").takeIf { it != 0 } ?: item.optInt("uid")
            val author = resolveAuthor(fromId, profiles, groups)
            
            // Discussion topics can have 'attachments' or 'attachment'
            val attachmentsArray = item.optJSONArray("attachments") 
                ?: item.optJSONObject("attachment")?.let { JSONArray().put(it) }
                ?: JSONArray()
                
            val attachments = parseAttachments(attachmentsArray)
            
            // Likes can be in 'likes' object or flat
            val likesObj = item.optJSONObject("likes")
            val likeCount = likesObj?.optInt("count") 
                ?: item.optInt("count") 
                ?: item.optInt("likes") 
                ?: 0
            val isLiked = likesObj?.optInt("user_likes") == 1 
                || item.optInt("user_likes") == 1 
                || item.optBool("user_likes", false)

            TopicComment(
                id = item.optInt("id"),
                fromId = author.id,
                ownerId = item.optInt("owner_id").takeIf { it != 0 } ?: item.optInt("group_id"),
                topicId = item.optInt("topic_id").takeIf { it != 0 } ?: item.optInt("tid"),
                date = item.optLong("date"),
                text = item.optStringClean("text") ?: "",
                authorName = author.name,
                authorAvatar = author.avatar,
                authorOnline = author.online,
                authorMobileOnline = author.mobileOnline,
                authorVerified = author.verified,
                imageUrls = attachments.imageUrls,
                videos = attachments.videos,
                audios = attachments.audios,
                documents = attachments.documents,
                poll = attachments.poll,
                likeCount = likeCount,
                isLiked = isLiked,
                replyToComment = item.optInt("reply_to_comment").takeIf { it != 0 },
                replyToUser = item.optInt("reply_to_user").takeIf { it != 0 }
            )
        }
        
        val profilesList = (0 until (responseObj.optJSONArray("profiles")?.length() ?: 0)).map { 
            parseUser(responseObj.getJSONArray("profiles").getJSONObject(it))
        }
        val groupsList = (0 until (responseObj.optJSONArray("groups")?.length() ?: 0)).map {
            parseGroup(responseObj.getJSONArray("groups").getJSONObject(it))
        }

        return TopicCommentsResponse(comments, count, profilesList, groupsList)
    }

    private data class ParsedAttachments(
        val imageUrls: List<String> = emptyList(),
        val videos: List<Video> = emptyList(),
        val audios: List<AudioTrack> = emptyList(),
        val documents: List<Document> = emptyList(),
        val poll: Poll? = null
    )

    private fun parseAttachments(attachments: JSONArray?): ParsedAttachments {
        val images = mutableListOf<String>()
        val videos = mutableListOf<Video>()
        val audios = mutableListOf<AudioTrack>()
        val documents = mutableListOf<Document>()
        var poll: Poll? = null
        if (attachments != null) {
            for (j in 0 until attachments.length()) {
                val attachment = attachments.getJSONObject(j)
                val type = attachment.optString("type")
                
                // OpenVK Board API special: attachments can be flat or missing 'type' wrapper
                if (type.isBlank()) {
                    if (attachment.has("sizes") || attachment.has("src") || attachment.has("photo_604") || attachment.has("photo_max")) {
                        val sizes = attachment.optJSONArray("sizes")
                        val url = largestPhotoUrl(sizes) 
                            ?: attachment.optUrl("src") 
                            ?: attachment.optUrl("photo_604")
                            ?: attachment.optUrl("photo_max")
                        if (url != null) images.add(url)
                    } else if (attachment.has("artist") && attachment.has("title")) {
                        audios.add(parseAudioItem(attachment))
                    } else if (attachment.has("player")) {
                        videos.add(parseVideo(attachment))
                    } else if (attachment.has("ext") && attachment.has("size")) {
                        documents.add(parseDocument(attachment))
                    }
                    continue
                }

                when (type) {
                    "photo" -> {
                        val photo = attachment.optJSONObject("photo") ?: attachment
                        val sizes = photo.optJSONArray("sizes")
                        val url = largestPhotoUrl(sizes) 
                            ?: photo.optUrl("src") 
                            ?: photo.optUrl("photo_604")
                            ?: photo.optUrl("photo_max")
                        if (url != null) images.add(url)
                    }
                    "video" -> {
                        val video = attachment.optJSONObject("video") ?: attachment
                        videos.add(parseVideo(video))
                    }
                    "audio" -> {
                        val audio = attachment.optJSONObject("audio") ?: attachment
                        audios.add(parseAudioItem(audio))
                    }
                    "doc" -> {
                        val doc = attachment.optJSONObject("doc") ?: attachment
                        documents.add(parseDocument(doc))
                    }
                    "poll" -> {
                        val pollObj = attachment.optJSONObject("poll") ?: attachment
                        poll = parsePoll(pollObj)
                    }
                }
            }
        }
        return ParsedAttachments(images, videos, audios, documents, poll)
    }

    private fun parseAlbum(json: JSONObject): PhotoAlbum {
        val thumbArray = json.optJSONArray("sizes")
        val thumbUrl = largestPhotoUrl(thumbArray) 
            ?: json.optUrl("thumb_src") 
            ?: json.optUrl("thumb_url")
            ?: json.optUrl("photo_2560")
            ?: json.optUrl("photo_1280")
            ?: json.optUrl("photo_604")
            ?: json.optUrl("photo_max")

        return PhotoAlbum(
            id = json.optInt("id"),
            ownerId = json.optInt("owner_id"),
            title = json.optStringClean("title") ?: "",
            description = json.optStringClean("description") ?: "",
            size = json.optInt("size", 0),
            thumbUrl = thumbUrl,
            created = json.optLong("created"),
            updated = json.optLong("updated"),
            canUpload = json.optBool("can_upload", false)
        )
    }

    private fun parsePhoto(json: JSONObject): Photo {
        val sizesArray = json.optJSONArray("sizes") ?: JSONArray()
        val sizes = (0 until sizesArray.length()).map { i ->
            val s = sizesArray.getJSONObject(i)
            PhotoSize(
                type = s.optString("type", ""),
                url = s.optUrl("url") ?: s.optUrl("src") ?: "",
                width = s.optInt("width", 0),
                height = s.optInt("height", 0)
            )
        }
        
        // Comprehensive fallbacks for OpenVK/VK legacy formats
        val largest = largestPhotoUrl(sizesArray) 
            ?: json.optUrl("photo_2560")
            ?: json.optUrl("photo_1280")
            ?: json.optUrl("photo_807")
            ?: json.optUrl("photo_604")
            ?: json.optUrl("photo_max")
            ?: json.optUrl("src_big")
            ?: json.optUrl("url")
            
        val smallest = thumbnailPhotoUrl(sizesArray)
            ?: json.optUrl("photo_130")
            ?: json.optUrl("photo_75")
            ?: json.optUrl("src_small")
            ?: json.optUrl("src")
            ?: largest

        return Photo(
            id = json.optInt("id"),
            ownerId = json.optInt("owner_id"),
            albumId = json.optInt("album_id"),
            userId = json.optInt("user_id"),
            text = json.optStringClean("text") ?: "",
            date = json.optLong("date"),
            sizes = sizes,
            url = largest ?: "",
            thumbUrl = smallest ?: ""
        )
    }

    private fun thumbnailPhotoUrl(sizes: JSONArray?): String? {
        if (sizes == null || sizes.length() == 0) return null
        
        val sizeList = mutableListOf<Pair<Int, String>>()
        for (i in 0 until sizes.length()) {
            val size = sizes.getJSONObject(i)
            val url = size.optUrl("url") ?: size.optUrl("src") ?: size.optUrl("photo") ?: continue
            val w = size.optInt("width", 0)
            sizeList.add(w to url)
        }
        
        if (sizeList.isEmpty()) return null
        
        // Sort by width ascending
        sizeList.sortBy { it.first }
        
        // Find first that is >= 320px (good for grid on modern screens)
        // If none found, the last one (largest available) will be used
        return sizeList.firstOrNull { it.first >= 320 }?.second 
            ?: sizeList.lastOrNull()?.second
    }

    fun parseVideo(json: JSONObject): Video {
        val files = json.optJSONObject("files")
        val videoUrl = files?.let {
            it.optUrl("mp4_1080")
                ?: it.optUrl("mp4_720")
                ?: it.optUrl("mp4_480")
                ?: it.optUrl("mp4_360")
                ?: it.optUrl("mp4_240")
        }

        val sizes = json.optJSONArray("image") ?: json.optJSONArray("sizes")
        val thumb = largestPhotoUrl(sizes) ?: json.optUrl("photo_320") ?: json.optUrl("photo_640")

        return Video(
            id = json.optInt("id"),
            ownerId = json.optInt("owner_id"),
            title = json.optStringClean("title") ?: "",
            duration = json.optInt("duration", 0),
            thumbnailUrl = thumb,
            videoUrl = videoUrl,
            playerUrl = json.optUrl("player"),
            accessKey = json.optString("access_key").takeIf { it.isNotBlank() },
            date = json.optLong("date"),
            views = json.optInt("views")
        )
    }

    fun parsePoll(json: JSONObject): Poll {
        val answersArray = json.optJSONArray("answers") ?: JSONArray()
        val answers = (0 until answersArray.length()).map { i ->
            val a = answersArray.getJSONObject(i)
            Answer(
                id = a.optInt("id"),
                text = a.optStringClean("text") ?: "",
                votes = a.optInt("votes", 0),
                rate = a.optDouble("rate", 0.0)
            )
        }
        
        val answerIdsArray = json.optJSONArray("answer_ids")
        val answerIds = if (answerIdsArray != null) {
            (0 until answerIdsArray.length()).map { answerIdsArray.optInt(it) }
        } else {
            val singleId = json.optInt("answer_id", 0)
            if (singleId != 0) listOf(singleId) else emptyList()
        }

        return Poll(
            id = json.optInt("id"),
            ownerId = json.optInt("owner_id"),
            created = json.optLong("created"),
            question = json.optStringClean("question") ?: "",
            votes = json.optInt("votes", 0),
            answers = answers,
            anonymous = json.optBool("anonymous", false),
            multiple = json.optBool("multiple", false),
            closed = json.optBool("closed", false),
            isBoard = json.optBool("is_board", false),
            canVote = json.optBool("can_vote", false),
            canEdit = json.optBool("can_edit", false),
            canReport = json.optBool("can_report", false),
            canShare = json.optBool("can_share", false),
            answerIds = answerIds,
            endDate = json.optLong("end_date", 0)
        )
    }

    private fun parseGeo(json: JSONObject): Geo {
        val coordinates = json.optStringClean("coordinates")
        val name = json.optStringClean("name") ?: json.optStringClean("title")
        
        var lat = 0.0
        var lon = 0.0
        if (!coordinates.isNullOrBlank()) {
            val parts = coordinates.split(Regex("[, ]+"))
            if (parts.size >= 2) {
                lat = parts[0].toDoubleOrNull() ?: 0.0
                lon = parts[1].toDoubleOrNull() ?: 0.0
            }
        }

        val placeObj = json.opt("place")
        val parsedPlace = when (placeObj) {
            is JSONObject -> parsePlace(placeObj)
            is String -> if (placeObj.isNotBlank() && placeObj.lowercase() != "null") {
                Place(0, placeObj, lat, lon, 0, null, null, null, null)
            } else null
            else -> null
        }

        // Final name resolution: geo.name -> place.title -> geo.title -> default "Место"
        val hasCoords = lat != 0.0 || lon != 0.0
        val resolvedName = name ?: parsedPlace?.title?.takeIf { it.isNotBlank() } ?: if (hasCoords) "Место" else null
        
        var finalPlace = if (parsedPlace != null) {
            if (resolvedName != null) parsedPlace.copy(title = resolvedName) else parsedPlace
        } else if (resolvedName != null) {
            Place(0, resolvedName, lat, lon, 0, null, null, null, null)
        } else null

        // Ensure coordinates are injected into place if it has none but geo has them
        if (finalPlace != null && finalPlace.latitude == 0.0 && finalPlace.longitude == 0.0 && hasCoords) {
            finalPlace = finalPlace.copy(latitude = lat, longitude = lon)
        }

        return Geo(
            type = json.optString("type", ""),
            coordinates = coordinates,
            place = finalPlace
        )
    }

    private fun parsePlace(json: JSONObject): Place {
        val cityObj = json.opt("city")
        val city = when (cityObj) {
            is JSONObject -> cityObj.optStringClean("title") ?: cityObj.optStringClean("name")
            is String -> cityObj.takeIf { it.isNotBlank() && it.lowercase() != "null" }
            else -> null
        }

        val countryObj = json.opt("country")
        val country = when (countryObj) {
            is JSONObject -> countryObj.optStringClean("title") ?: countryObj.optStringClean("name")
            is String -> countryObj.takeIf { it.isNotBlank() && it.lowercase() != "null" }
            else -> null
        }

        return Place(
            id = json.optInt("id"),
            title = json.optStringClean("title") 
                ?: json.optStringClean("name") 
                ?: json.optStringClean("place_name")
                ?: "",
            latitude = json.optDouble("latitude", 0.0),
            longitude = json.optDouble("longitude", 0.0),
            created = json.optLong("created"),
            icon = json.optStringClean("icon"),
            country = country,
            city = city,
            address = json.optStringClean("address")
        )
    }

    fun parseDocument(json: JSONObject): Document {
        val preview = json.optJSONObject("preview")
        val photo = preview?.optJSONObject("photo")
        val video = preview?.optJSONObject("video")
        
        val previewUrl = largestPhotoUrl(photo?.optJSONArray("sizes"))
        val previewGifUrl = video?.optUrl("src")

        // Try 'url' first, then 'link'
        val docUrl = json.optUrl("url") ?: json.optUrl("link")

        return Document(
            id = json.optInt("id"),
            ownerId = json.optInt("owner_id"),
            title = json.optStringClean("title") ?: "Файл",
            size = json.optInt("size"),
            ext = json.optString("ext", ""),
            url = docUrl,
            date = json.optLong("date"),
            type = json.optInt("type"),
            previewUrl = previewUrl,
            previewGifUrl = previewGifUrl,
            accessKey = json.optString("access_key").takeIf { it.isNotBlank() }
        )
    }

    fun parseNote(json: JSONObject): Note {
        return Note(
            id = json.optInt("id", json.optInt("nid")),
            ownerId = json.optInt("owner_id", json.optInt("user_id")),
            title = json.optStringClean("title") ?: "",
            text = json.optStringClean("text") ?: json.optStringClean("message") ?: json.optStringClean("body") ?: "",
            date = json.optLong("date"),
            commentsCount = json.optInt("comments"),
            viewUrl = json.optUrl("view_url")
        )
    }

    fun parseNotes(response: JSONObject): NotesResponse {
        val respObj = getResponseObject(response)
        val items = respObj.optJSONArray("items") ?: JSONArray()
        val count = respObj.optInt("count", items.length())
        
        val list = mutableListOf<Note>()
        for (i in 0 until items.length()) {
            list.add(parseNote(items.getJSONObject(i)))
        }
        return NotesResponse(list, count)
    }

    private fun largestPhotoUrl(sizes: JSONArray?): String? {
        if (sizes == null) return null
        var bestUrl: String? = null
        var bestScore = -1
        for (i in 0 until sizes.length()) {
            val size = sizes.getJSONObject(i)
            val url = size.optUrl("url") 
                ?: size.optUrl("src") 
                ?: size.optUrl("photo")
                ?: size.optUrl("link")
            
            if (url != null) {
                val w = size.optInt("width", 0)
                val h = size.optInt("height", 0)
                val type = size.optString("type")
                
                val typePriority = when(type) {
                    "w" -> 10000
                    "z" -> 9000
                    "y" -> 8000
                    "x" -> 7000
                    "m" -> 1000
                    "s" -> 100
                    else -> 0
                }
                
                val currentScore = (if (w > 0 && h > 0) w * h else w) + typePriority
                if (currentScore >= bestScore) {
                    bestScore = currentScore
                    bestUrl = url
                }
            }
        }
        return bestUrl
    }
}
