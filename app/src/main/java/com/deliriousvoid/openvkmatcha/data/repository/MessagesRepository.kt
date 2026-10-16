package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.model.Conversation
import com.deliriousvoid.openvkmatcha.data.model.ParsedHistory
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import org.json.JSONObject
import kotlin.random.Random

class MessagesRepository(private val api: OpenVKApi) {

    suspend fun loadConversations(offset: Int = 0, count: Int = 20): Result<List<Conversation>> {
        val params = mapOf(
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "fields" to "id,first_name,last_name,screen_name,photo_100,photo_200,online,last_seen,verified"
        )
        return api.callMethod("messages.getConversations", params).map { response ->
            JsonParsers.parseConversations(response)
        }
    }

    suspend fun getConversationsById(peerIds: List<Int>): Result<List<Conversation>> {
        if (peerIds.isEmpty()) return Result.success(emptyList())
        val params = mapOf(
            "peer_ids" to peerIds.joinToString(","),
            "extended" to "1",
            "fields" to "id,first_name,last_name,screen_name,photo_100,photo_200,online,last_seen,verified"
        )
        return api.callMethod("messages.getConversationsById", params).map { response ->
            JsonParsers.parseConversations(response)
        }
    }

    suspend fun loadHistory(
        peerId: Int,
        offset: Int = 0,
        count: Int = 30,
        startMessageId: Int? = null,
        reverse: Boolean = false
    ): Result<ParsedHistory> {
        val params = mutableMapOf(
            "peer_id" to peerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1"
        )
        if (startMessageId != null) {
            params["start_message_id"] = startMessageId.toString()
        }
        if (reverse) {
            params["rev"] = "1"
        }
        return api.callMethod("messages.getHistory", params).map { response ->
            JsonParsers.parseMessages(response, peerId)
        }
    }

    suspend fun sendMessage(
        peerId: Int,
        text: String,
        replyTo: Int? = null,
        stickerId: Int? = null,
        attachments: String? = null
    ): Result<JSONObject> {
        val randomId = (System.currentTimeMillis() xor Random.nextLong(1, 1000000)).toString()
        val params = mutableMapOf(
            "peer_id" to peerId.toString(),
            "random_id" to randomId
        )
        if (text.isNotBlank()) {
            params["message"] = text
        }
        if (replyTo != null) {
            params["reply_to"] = replyTo.toString()
        }
        if (stickerId != null) {
            params["sticker_id"] = stickerId.toString()
        }
        if (!attachments.isNullOrBlank()) {
            params["attachment"] = attachments
        }
        return api.callMethod("messages.send", params, isPost = true)
    }

    suspend fun sendPhotos(
        peerId: Int,
        text: String,
        replyTo: Int?,
        photos: List<Pair<ByteArray, String>> // (bytes, fileName)
    ): Result<JSONObject> {
        if (photos.isEmpty()) {
            return Result.failure(Exception("No photos provided"))
        }
        return try {
            val serverResult = api.callMethod("photos.getMessagesUploadServer", mapOf("peer_id" to peerId.toString()))
            if (serverResult.isFailure) return serverResult

            val serverObj = serverResult.getOrNull()?.optJSONObject("response")
                ?: serverResult.getOrNull()
            val uploadUrl = serverObj?.optString("upload_url")
                ?: return Result.failure(Exception("Failed to get upload server URL"))

            val attachmentKeys = mutableListOf<String>()
            for ((bytes, name) in photos) {
                val uploadResult = api.uploadFile(uploadUrl, bytes, name, "photo")
                if (uploadResult.isFailure) return uploadResult
                val uploadedJson = uploadResult.getOrThrow()

                val saveParams = mapOf(
                    "photo" to uploadedJson.optString("photo", ""),
                    "hash" to uploadedJson.optString("hash", ""),
                    "server" to uploadedJson.optInt("server", 0).toString()
                )
                val saveResult = api.callMethod("photos.saveMessagesPhoto", saveParams, isPost = true)
                if (saveResult.isFailure) return saveResult

                val saveResponse = saveResult.getOrNull()?.optJSONArray("response")
                    ?: saveResult.getOrNull()?.optJSONObject("response")?.optJSONArray("items")
                if (saveResponse != null && saveResponse.length() > 0) {
                    val item = saveResponse.getJSONObject(0)
                    val ownerId = item.optInt("owner_id")
                    val id = item.optInt("id")
                    val accessKey = item.optString("access_key")
                    val keyStr = if (accessKey.isNotBlank()) "photo${ownerId}_${id}_$accessKey" else "photo${ownerId}_$id"
                    attachmentKeys.add(keyStr)
                }
            }

            sendMessage(
                peerId = peerId,
                text = text,
                replyTo = replyTo,
                attachments = attachmentKeys.joinToString(",")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(peerId: Int, messageId: Int, text: String): Result<JSONObject> =
        api.callMethod(
            "messages.edit",
            mapOf(
                "peer_id" to peerId.toString(),
                "message_id" to messageId.toString(),
                "message" to text
            ),
            isPost = true
        )

    suspend fun deleteMessages(peerId: Int, messageIds: List<Int>, deleteForAll: Boolean = true): Result<JSONObject> =
        api.callMethod(
            "messages.delete",
            mapOf(
                "peer_id" to peerId.toString(),
                "message_ids" to messageIds.joinToString(","),
                "delete_for_all" to if (deleteForAll) "1" else "0"
            ),
            isPost = true
        )

    suspend fun restoreMessage(peerId: Int, messageId: Int): Result<JSONObject> =
        api.callMethod(
            "messages.restore",
            mapOf("message_id" to messageId.toString()),
            isPost = true
        )

    suspend fun pinMessage(peerId: Int, messageId: Int): Result<JSONObject> =
        api.callMethod(
            "messages.pin",
            mapOf("peer_id" to peerId.toString(), "message_id" to messageId.toString()),
            isPost = true
        )

    suspend fun unpinMessage(peerId: Int): Result<JSONObject> =
        api.callMethod(
            "messages.unpin",
            mapOf("peer_id" to peerId.toString()),
            isPost = true
        )

    suspend fun markAsRead(peerId: Int): Result<JSONObject> =
        api.callMethod(
            "messages.markAsRead",
            mapOf("peer_id" to peerId.toString()),
            isPost = true
        )

    suspend fun setActivity(peerId: Int, type: String = "typing"): Result<JSONObject> =
        api.callMethod(
            "messages.setActivity",
            mapOf("peer_id" to peerId.toString(), "type" to type),
            isPost = true
        )

    suspend fun getChatMembers(peerId: Int): Result<JSONObject> =
        api.callMethod(
            "messages.getConversationMembers",
            mapOf("peer_id" to peerId.toString(), "extended" to "1")
        )

    suspend fun getHistoryAttachments(
        peerId: Int,
        mediaType: String,
        startFrom: String? = null,
        count: Int = 40
    ): Result<JSONObject> {
        val params = mutableMapOf(
            "peer_id" to peerId.toString(),
            "media_type" to mediaType,
            "count" to count.toString(),
            "photo_sizes" to "1"
        )
        if (!startFrom.isNullOrBlank()) {
            params["start_from"] = startFrom
        }
        return api.callMethod("messages.getHistoryAttachments", params)
    }

    suspend fun addChatUser(peerId: Int, userIds: List<Int>): Result<JSONObject> =
        api.callMethod(
            "messages.addChatUser",
            mapOf("peer_id" to peerId.toString(), "user_id" to userIds.joinToString(",")),
            isPost = true
        )

    suspend fun setMemberRole(peerId: Int, userId: Int, role: String): Result<JSONObject> =
        api.callMethod(
            "messages.setMemberRole",
            mapOf("peer_id" to peerId.toString(), "user_id" to userId.toString(), "role" to role),
            isPost = true
        )

    suspend fun removeChatUser(peerId: Int, userId: Int): Result<JSONObject> =
        api.callMethod(
            "messages.removeChatUser",
            mapOf("peer_id" to peerId.toString(), "user_id" to userId.toString()),
            isPost = true
        )

    suspend fun createChat(title: String, userIds: List<Int>): Result<JSONObject> =
        api.callMethod(
            "messages.createChat",
            mapOf("title" to title, "user_ids" to userIds.joinToString(",")),
            isPost = true
        )

    suspend fun setChatPhoto(chatId: Int, bytes: ByteArray, fileName: String): Result<JSONObject> {
        return try {
            val serverResult = api.callMethod("photos.getChatUploadServer", mapOf("chat_id" to chatId.toString()))
            if (serverResult.isFailure) return serverResult

            val serverObj = serverResult.getOrNull()?.optJSONObject("response") ?: serverResult.getOrNull()
            val uploadUrl = serverObj?.optString("upload_url")
                ?: return Result.failure(Exception("Failed to get chat upload server URL"))

            val uploadResult = api.uploadFile(uploadUrl, bytes, fileName, "file")
            if (uploadResult.isFailure) return uploadResult

            val uploadedJson = uploadResult.getOrThrow()
            val params = mapOf(
                "chat_id" to chatId.toString(),
                "file" to uploadedJson.optString("file", uploadedJson.optString("photo", "")),
                "hash" to uploadedJson.optString("hash", "")
            )
            api.callMethod("messages.setChatPhoto", params, isPost = true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStickerPacks(): Result<JSONObject> =
        api.callMethod("stickers.get", mapOf("count" to "100"))

    suspend fun getPushSettings(peerId: Int): Result<JSONObject> =
        api.callMethod("account.getPushSettings", mapOf("peer_id" to peerId.toString()))

    suspend fun setSilenceMode(peerId: Int, time: Int = -1, sound: Int = 0): Result<JSONObject> =
        api.callMethod(
            "account.setSilenceMode",
            mapOf("peer_id" to peerId.toString(), "time" to time.toString(), "sound" to sound.toString()),
            isPost = true
        )

    suspend fun deleteConversation(peerId: Int): Result<JSONObject> =
        api.callMethod(
            "messages.deleteConversation",
            mapOf("peer_id" to peerId.toString()),
            isPost = true
        )

    suspend fun leaveChat(peerId: Int): Result<JSONObject> =
        api.callMethod(
            "messages.removeChatUser",
            mapOf("peer_id" to peerId.toString()),
            isPost = true
        )

    suspend fun getInviteLink(peerId: Int): Result<String> {
        return api.callMethod("messages.getInviteLink", mapOf("peer_id" to peerId.toString())).map { json ->
            val resp = json.optJSONObject("response") ?: json
            resp.optString("link", "")
        }
    }
}
