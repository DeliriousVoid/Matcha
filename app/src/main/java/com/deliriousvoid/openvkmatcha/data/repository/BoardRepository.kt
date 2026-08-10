package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import kotlin.math.absoluteValue

class BoardRepository(private val api: OpenVKApi) {

    suspend fun getTopics(groupId: Int, offset: Int = 0, count: Int = 30) = api.callMethod(
        "board.getTopics",
        mapOf(
            "group_id" to groupId.absoluteValue.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "preview" to "1"
        )
    ).map { JsonParsers.parseTopics(it) }

    suspend fun getTopicById(groupId: Int, virtualId: Int) = api.callMethod(
        "board.getTopics",
        mapOf(
            "group_id" to groupId.absoluteValue.toString(),
            "topic_ids" to virtualId.toString(),
            "count" to "1"
        )
    ).map { JsonParsers.parseTopics(it).items.firstOrNull() }

    suspend fun getComments(groupId: Int, topicId: Int, offset: Int = 0, count: Int = 30) = api.callMethod(
        "board.getComments",
        mapOf(
            "group_id" to groupId.absoluteValue.toString(),
            "topic_id" to topicId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "need_likes" to "1",
            "fields" to "photo_50,photo_200,verified"
        )
    ).map { JsonParsers.parseTopicComments(it) }

    suspend fun createComment(groupId: Int, topicId: Int, message: String, attachments: String? = null, fromGroup: Boolean = false) = api.callMethod(
        "board.createComment",
        mutableMapOf(
            "group_id" to groupId.absoluteValue.toString(),
            "topic_id" to topicId.toString(),
            "message" to message
        ).apply {
            if (!attachments.isNullOrBlank()) put("attachments", attachments)
            put("from_group", if (fromGroup) "1" else "0")
        },
        isPost = true
    )

    suspend fun toggleLike(groupId: Int, commentId: Int, isLiked: Boolean) = api.callMethod(
        if (isLiked) "likes.delete" else "likes.add",
        mapOf(
            "type" to "comment",
            "owner_id" to (-groupId.absoluteValue).toString(),
            "item_id" to commentId.toString()
        )
    )
}
