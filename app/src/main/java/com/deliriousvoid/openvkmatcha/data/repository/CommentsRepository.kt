package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import com.deliriousvoid.openvkmatcha.data.model.Comment

class CommentsRepository(private val api: OpenVKApi) {

    suspend fun getComments(ownerId: Int, postId: Int, offset: Int = 0, count: Int = 30) = api.callMethod(
        "wall.getComments",
        mapOf(
            "owner_id" to ownerId.toString(),
            "post_id" to postId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "sort" to "asc",
            "fields" to "photo_50,photo_200,verified"
        )
    ).map { JsonParsers.parseComments(it) }

    suspend fun createComment(ownerId: Int, postId: Int, text: String, attachments: String? = null, fromGroup: Boolean = false, replyToComment: Int? = null) = api.callMethod(
        "wall.createComment",
        mutableMapOf(
            "owner_id" to ownerId.toString(),
            "post_id" to postId.toString(),
            "message" to text
        ).apply {
            if (!attachments.isNullOrBlank()) put("attachments", attachments)
            put("from_group", if (fromGroup) "1" else "0")
            if (replyToComment != null) put("reply_to_comment", replyToComment.toString())
        },
        isPost = true
    )

    suspend fun toggleLike(comment: Comment) = api.callMethod(
        if (comment.isLiked) "likes.delete" else "likes.add",
        mapOf(
            "type" to "comment",
            "owner_id" to comment.ownerId.toString(),
            "item_id" to comment.id.toString(),
        )
    )

    suspend fun deleteComment(commentId: Int) = api.callMethod(
        "wall.deleteComment",
        mapOf(
            "comment_id" to commentId.toString()
        ),
        isPost = true
    )

    suspend fun editComment(commentId: Int, text: String, attachments: String? = null) = api.callMethod(
        "wall.editComment",
        mutableMapOf(
            "comment_id" to commentId.toString(),
            "message" to text
        ).apply {
            if (!attachments.isNullOrBlank()) put("attachments", attachments)
        },
        isPost = true
    )
}
