package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import com.deliriousvoid.openvkmatcha.data.model.Post
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import org.json.JSONArray
import kotlin.math.absoluteValue

class FeedRepository(private val api: OpenVKApi) {

    suspend fun loadGlobalFeed(startFrom: String? = null, count: Int = 20) = api.callMethod(
        "newsfeed.getGlobal",
        mapOf(
            "start_from" to (startFrom ?: ""),
            "count" to count.toString(),
            "fields" to "photo_50,verified,online",
            "extended" to "1",
        ),
    ).map { JsonParsers.parseNewsfeed(it) }

    suspend fun loadSubscriptionsFeed(startFrom: String? = null, count: Int = 20) = api.callMethod(
        "newsfeed.get",
        mapOf(
            "start_from" to (startFrom ?: ""),
            "count" to count.toString(),
            "fields" to "photo_50,verified,online",
            "extended" to "1",
        ),
    ).map { JsonParsers.parseNewsfeed(it) }

    suspend fun toggleLike(post: Post) = api.callMethod(
        if (post.isLiked) "likes.delete" else "likes.add",
        mapOf(
            "type" to "post",
            "owner_id" to post.ownerId.toString(),
            "item_id" to post.id.toString(),
        ),
    )

    suspend fun getPostById(ownerId: Int, postId: Int) = api.callMethod(
        "wall.getById",
        mapOf(
            "posts" to "${ownerId}_$postId",
            "extended" to "1",
            "fields" to "photo_50,verified,online"
        )
    ).map { JsonParsers.parseWall(it).firstOrNull() }

    suspend fun repost(ownerId: Int, postId: Int, message: String, groupId: Int? = null, attachments: String? = null) = api.callMethod(
        "wall.repost",
        mutableMapOf(
            "object" to "wall${ownerId}_$postId",
            "message" to message,
        ).apply {
            if (groupId != null) put("group_id", groupId.toString())
            if (!attachments.isNullOrBlank()) put("attachments", attachments)
        }
    )

    suspend fun deletePost(ownerId: Int, postId: Int) = api.callMethod(
        "wall.delete",
        mapOf(
            "owner_id" to ownerId.toString(),
            "post_id" to postId.toString()
        )
    )

    suspend fun editPost(ownerId: Int, postId: Int, text: String, fromGroup: Boolean? = null, isNsfw: Boolean? = null) = api.callMethod(
        "wall.edit",
        mutableMapOf(
            "owner_id" to ownerId.toString(),
            "post_id" to postId.toString(),
            "message" to text
        ).apply {
            if (fromGroup != null && ownerId < 0) put("from_group", if (fromGroup) "1" else "0")
            if (isNsfw != null) put("explicit", if (isNsfw) "1" else "0")
        }
    )

    suspend fun pinPost(ownerId: Int, postId: Int) = api.callMethod(
        "wall.pin",
        mapOf(
            "owner_id" to ownerId.toString(),
            "post_id" to postId.toString()
        )
    )

    suspend fun createPost(
        ownerId: Int,
        message: String,
        attachments: String? = null,
        fromGroup: Boolean = false,
        signed: Boolean = false,
        lat: Double? = null,
        long: Double? = null,
        placeName: String? = null,
        copyright: String? = null,
        isNsfw: Boolean = false
    ) = api.callMethod(
        "wall.post",
        mutableMapOf(
            "owner_id" to ownerId.toString(),
            "message" to message
        ).apply {
            if (!attachments.isNullOrBlank()) put("attachments", attachments)
            if (fromGroup) put("from_group", "1")
            if (signed) put("signed", "1")
            if (lat != null) put("lat", lat.toString())
            if (long != null) put("long", long.toString())
            if (!placeName.isNullOrBlank()) put("place_name", placeName)
            if (!copyright.isNullOrBlank()) put("copyright", copyright)
            if (isNsfw) put("explicit", "1")
        },
        isPost = true
    )

    suspend fun unpinPost(ownerId: Int, postId: Int) = api.callMethod(
        "wall.unpin",
        mapOf(
            "owner_id" to ownerId.toString(),
            "post_id" to postId.toString()
        )
    )

    suspend fun addPollVote(ownerId: Int, pollId: Int, answerIds: List<Int>) = api.callMethod(
        "polls.addVote",
        mapOf(
            "owner_id" to ownerId.toString(),
            "poll_id" to pollId.toString(),
            "answer_ids" to answerIds.joinToString(","),
        )
    )

    suspend fun createPoll(
        question: String,
        answers: List<String>,
        anonymous: Boolean = false,
        multiple: Boolean = false,
        disableUnvote: Boolean = false,
        endDate: Long? = null
    ) = api.callMethod(
        "polls.create",
        mutableMapOf(
            "question" to question,
            "add_answers" to JSONArray(answers).toString(),
            "is_anonymous" to if (anonymous) "1" else "0",
            "is_multiple" to if (multiple) "1" else "0",
            "disable_unvote" to if (disableUnvote) "1" else "0"
        ).apply {
            if (endDate != null) put("end_date", endDate.toString())
        },
        isPost = true
    ).map { JsonParsers.parsePoll(it) }

    suspend fun deletePollVote(ownerId: Int, pollId: Int) = api.callMethod(
        "polls.deleteVote",
        mapOf(
            "owner_id" to ownerId.toString(),
            "poll_id" to pollId.toString(),
        )
    )

    suspend fun checkCopyrightLink(link: String) = api.callMethod(
        "wall.checkCopyrightLink",
        mapOf("link" to link)
    )

    suspend fun ignoreSource(id: Int) = api.callMethod(
        "newsfeed.addBan",
        if (id > 0) mapOf("user_ids" to id.toString()) else mapOf("group_ids" to id.absoluteValue.toString())
    )

    suspend fun unignoreSource(id: Int) = api.callMethod(
        "newsfeed.deleteBan",
        if (id > 0) mapOf("user_ids" to id.toString()) else mapOf("group_ids" to id.absoluteValue.toString())
    )

    suspend fun getIgnoredSources(offset: Int = 0, count: Int = 50) = api.callMethod(
        "newsfeed.getBanned",
        mapOf(
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "fields" to "photo_50,verified,online"
        )
    ).map { json ->
        val response = JsonParsers.getResponseObject(json)
        val profiles = response.optJSONArray("profiles") ?: JSONArray()
        val groups = response.optJSONArray("groups") ?: JSONArray()
        
        val list = mutableListOf<UserProfile>()
        for (i in 0 until profiles.length()) {
            list.add(JsonParsers.parseUser(profiles.getJSONObject(i)))
        }
        for (i in 0 until groups.length()) {
            list.add(JsonParsers.parseGroup(groups.getJSONObject(i)))
        }
        list
    }
}
