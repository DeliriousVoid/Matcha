package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers

class VideoRepository(private val api: OpenVKApi) {
    suspend fun getVideos(ownerId: Int, offset: Int = 0, count: Int = 30) = api.callMethod(
        "video.get",
        mapOf(
            "owner_id" to ownerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString()
        )
    ).map { json ->
        val items = JsonParsers.getResponseItems(json)
        (0 until items.length()).map { JsonParsers.parseVideo(items.getJSONObject(it)) }
    }

    suspend fun searchVideos(query: String, offset: Int = 0, count: Int = 30) = api.callMethod(
        "video.search",
        mapOf(
            "q" to query,
            "offset" to offset.toString(),
            "count" to count.toString()
        )
    ).map { json ->
        val items = JsonParsers.getResponseItems(json)
        (0 until items.length()).map { JsonParsers.parseVideo(items.getJSONObject(it)) }
    }
}
