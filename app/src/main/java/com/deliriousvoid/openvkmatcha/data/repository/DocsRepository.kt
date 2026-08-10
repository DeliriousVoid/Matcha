package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers

class DocsRepository(private val api: OpenVKApi) {
    suspend fun getDocs(ownerId: Int, offset: Int = 0, count: Int = 30) = api.callMethod(
        "docs.get",
        mapOf(
            "owner_id" to ownerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString()
        )
    ).map { json ->
        val items = JsonParsers.getResponseItems(json)
        (0 until items.length()).map { JsonParsers.parseDocument(items.getJSONObject(it)) }
    }

    suspend fun searchDocs(query: String, offset: Int = 0, count: Int = 30) = api.callMethod(
        "docs.search",
        mapOf(
            "q" to query,
            "offset" to offset.toString(),
            "count" to count.toString(),
            "search_own" to "1"
        )
    ).map { json ->
        val items = JsonParsers.getResponseItems(json)
        (0 until items.length()).map { JsonParsers.parseDocument(items.getJSONObject(it)) }
    }
}
