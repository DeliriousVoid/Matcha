package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers

class NotificationsRepository(private val api: OpenVKApi) {

    suspend fun loadNotifications(startFrom: String? = null, count: Int = 30, archived: Boolean = false) = api.callMethod(
        "notifications.get",
        mapOf(
            "start_from" to (startFrom ?: ""),
            "offset" to (if (startFrom == null) "0" else ""), // Fallback if startFrom is offset
            "count" to count.toString(),
            "filters" to "wall,mentions,comments,likes,reposts,followers",
            "archived" to if (archived) "1" else "0"
        )
    ).map { JsonParsers.parseNotifications(it, archived) }

    suspend fun getUnreadCount() = api.callMethod(
        "account.getCounters",
        emptyMap() // Request all counters to be safe
    ).map {
        val response = JsonParsers.getResponseObject(it)
        response.optInt("notifications", 0)
    }

    suspend fun getLongPollServer() = api.callMethod(
        "messages.getLongPollServer",
        mapOf("lp_version" to "3", "need_pts" to "1", "group_id" to "0")
    )

    suspend fun markAsViewed() = api.callMethod("notifications.markAsViewed")
}
