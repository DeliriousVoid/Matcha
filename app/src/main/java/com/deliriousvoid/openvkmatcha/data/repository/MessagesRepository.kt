package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers

class MessagesRepository(private val api: OpenVKApi) {

    suspend fun loadConversations(offset: Int = 0, count: Int = 20) = api.callMethod(
        "messages.getConversations",
        mapOf(
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "fields" to "photo_50,online,verified,last_seen,sex",
        ),
    ).map { JsonParsers.parseConversations(it) }

    suspend fun getConversationsById(peerIds: List<Int>) = api.callMethod(
        "messages.getConversationsById",
        mapOf(
            "peer_ids" to peerIds.joinToString(","),
            "extended" to "1",
            "fields" to "photo_50,online,verified,last_seen,sex",
        )
    ).map { JsonParsers.parseConversations(it) }

    suspend fun loadHistory(
        peerId: Int,
        offset: Int = 0,
        count: Int = 30,
        startMessageId: Int? = null
    ): Result<com.deliriousvoid.openvkmatcha.data.model.ParsedHistory> = api.callMethod(
        "messages.getHistory",
        mutableMapOf(
            "peer_id" to peerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "fields" to "photo_50,online,verified",
        ).apply {
            if (startMessageId != null) put("start_message_id", startMessageId.toString())
        },
    ).map { JsonParsers.parseMessages(it, peerId) }

    suspend fun sendMessage(
        peerId: Int, 
        text: String, 
        replyTo: Int? = null, 
        stickerId: Int? = null,
        attachments: String? = null
    ) = api.callMethod(
        "messages.send",
        mutableMapOf(
            "peer_id" to peerId.toString(),
            "random_id" to (System.currentTimeMillis() + (0..1000).random()).toString(),
            "message" to text,
        ).apply {
            if (replyTo != null) put("reply_to", replyTo.toString())
            if (stickerId != null) put("sticker_id", stickerId.toString())
            if (attachments != null) put("attachment", attachments)
        },
        isPost = true
    )

    suspend fun editMessage(peerId: Int, messageId: Int, text: String) = api.callMethod(
        "messages.edit",
        mapOf(
            "peer_id" to peerId.toString(),
            "message_id" to messageId.toString(),
            "message" to text
        ),
        isPost = true
    )

    suspend fun deleteMessages(peerId: Int, messageIds: List<Int>, deleteForAll: Boolean = true) = api.callMethod(
        "messages.delete",
        mapOf(
            "peer_id" to peerId.toString(),
            "message_ids" to messageIds.joinToString(","),
            "delete_for_all" to if (deleteForAll) "1" else "0"
        ),
        isPost = true
    )

    suspend fun restoreMessage(peerId: Int, messageId: Int) = api.callMethod(
        "messages.restore",
        mapOf("peer_id" to peerId.toString(), "message_id" to messageId.toString()),
        isPost = true
    )

    suspend fun pinMessage(peerId: Int, messageId: Int) = api.callMethod(
        "messages.pin",
        mapOf("peer_id" to peerId.toString(), "message_id" to messageId.toString()),
        isPost = true
    )

    suspend fun unpinMessage(peerId: Int) = api.callMethod(
        "messages.unpin",
        mapOf("peer_id" to peerId.toString()),
        isPost = true
    )

    suspend fun markAsRead(peerId: Int) = api.callMethod(
        "messages.markAsRead",
        mapOf("peer_id" to peerId.toString()),
        isPost = true
    )

    suspend fun setActivity(peerId: Int, type: String = "typing") = api.callMethod(
        "messages.setActivity",
        mapOf("peer_id" to peerId.toString(), "type" to type),
        isPost = true
    )

    suspend fun getChatMembers(peerId: Int) = api.callMethod(
        "messages.getConversationMembers",
        mapOf("peer_id" to peerId.toString(), "extended" to "1"),
    )

    suspend fun createChat(title: String, userIds: List<Int>) = api.callMethod(
        "messages.createChat",
        mapOf("title" to title, "user_ids" to userIds.joinToString(",")),
        isPost = true
    )
}
