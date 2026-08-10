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
            "fields" to "photo_50,online,verified",
        ),
    ).map { JsonParsers.parseConversations(it) }

    suspend fun loadHistory(peerId: Int, offset: Int = 0, count: Int = 30) = api.callMethod(
        "messages.getHistory",
        mapOf(
            "peer_id" to peerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
        ),
    ).map { JsonParsers.parseMessages(it, peerId) }

    suspend fun sendMessage(peerId: Int, text: String) = api.callMethod(
        "messages.send",
        mapOf(
            "peer_id" to peerId.toString(),
            "random_id" to System.currentTimeMillis().toString(),
            "message" to text,
        ),
    )
}
