package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi

class ReportsRepository(private val api: OpenVKApi) {
    suspend fun report(type: String, ownerId: Int, itemId: Int? = null, comment: String? = null) = api.callMethod(
        "reports.add",
        mutableMapOf(
            "type" to type,
            "owner_id" to ownerId.toString()
        ).apply {
            if (itemId != null) put("item_id", itemId.toString())
            if (comment != null) put("comment", comment)
        }
    )
}
