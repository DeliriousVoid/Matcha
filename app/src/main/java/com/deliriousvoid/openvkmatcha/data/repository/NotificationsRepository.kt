package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers

class NotificationsRepository(private val api: OpenVKApi) {

    suspend fun loadNotifications(startFrom: String? = null, archived: Boolean = false) = api.callMethod(
        "notifications.get",
        mapOf(
            "start_from" to (startFrom ?: ""),
            "count" to "30",
            "filters" to "wall,mentions,comments,likes,reposts,followers",
            "archived" to if (archived) "1" else "0"
        )
    ).map { JsonParsers.parseNotifications(it) }

    suspend fun getUnreadCount() = api.callMethod(
        "account.getCounters",
        mapOf("filter" to "notifications")
    ).map {
        val response = JsonParsers.getResponseObject(it)
        response.optInt("notifications", 0)
    }

    suspend fun getLongPollServer() = api.callMethod(
        "messages.getLongPollServer",
        mapOf("lp_version" to "3", "need_pts" to "1")
    )

    suspend fun markAsViewed() = api.callMethod("notifications.markAsViewed")
}
