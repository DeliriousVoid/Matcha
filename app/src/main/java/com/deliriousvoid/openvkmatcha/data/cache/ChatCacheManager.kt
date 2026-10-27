package com.deliriousvoid.openvkmatcha.data.cache

import android.content.Context
import com.deliriousvoid.openvkmatcha.data.model.ParsedHistory
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import com.deliriousvoid.openvkmatcha.util.MLog
import org.json.JSONObject
import java.io.File

class ChatCacheManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("chat_positions", Context.MODE_PRIVATE)

    init {
        cleanupLegacyCache()
    }

    private fun getAccountKey(): String {
        try {
            val app = com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance
            val currentAccountId = app.accountManager.getCurrentAccountId()
            if (!currentAccountId.isNullOrBlank()) {
                return currentAccountId
            }
            val currentAccount = app.accountManager.getCurrentAccount()
            if (currentAccount != null) {
                return currentAccount.id
            }
            val token = app.tokenManager.getToken()
            if (!token.isNullOrBlank()) {
                return token.hashCode().toString()
            }
        } catch (e: Exception) {
            MLog.e("ChatCacheManager", "Error resolving account key", e)
        }
        return "default"
    }

    private fun cleanupLegacyCache() {
        try {
            val dir = getCacheDir()
            val files = dir.listFiles() ?: return
            for (file in files) {
                val name = file.name
                if ((name.startsWith("history_") || name.startsWith("peer_")) && name.endsWith(".json")) {
                    val middle = name.substringAfter("_").substringBeforeLast(".json")
                    if (middle.all { it.isDigit() || it == '-' }) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            MLog.e("ChatCacheManager", "Failed to cleanup legacy cache", e)
        }
    }

    private fun getCacheDir(): File {
        val dir = File(context.cacheDir, "chat_history")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getCacheFile(peerId: Int): File {
        val key = getAccountKey()
        return File(getCacheDir(), "history_${key}_$peerId.json")
    }

    private fun getPeerFile(peerId: Int): File {
        val key = getAccountKey()
        return File(getCacheDir(), "peer_${key}_$peerId.json")
    }

    fun saveRawHistory(peerId: Int, jsonResponse: JSONObject) {
        try {
            val file = getCacheFile(peerId)
            file.writeText(jsonResponse.toString())
        } catch (e: Exception) {
            MLog.e("ChatCacheManager", "Failed to save chat history cache for peer $peerId: ${e.message}")
        }
    }

    fun getCachedHistory(peerId: Int): ParsedHistory? {
        return try {
            val file = getCacheFile(peerId)
            if (!file.exists()) return null
            val content = file.readText()
            if (content.isBlank()) return null
            val json = JSONObject(content)
            JsonParsers.parseMessages(json, peerId)
        } catch (e: Exception) {
            MLog.e("ChatCacheManager", "Failed to read chat history cache for peer $peerId: ${e.message}")
            null
        }
    }

    fun updateCachedMessage(peerId: Int, messageId: Int, newText: String) {
        try {
            val file = getCacheFile(peerId)
            if (!file.exists()) return
            val content = file.readText()
            if (content.isBlank()) return
            val json = JSONObject(content)
            val responseObj = if (json.has("response")) json.optJSONObject("response") else json
            val items = responseObj?.optJSONArray("items") ?: return
            var updated = false
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                if (item.optInt("id") == messageId) {
                    item.put("text", newText)
                    item.put("update_time", System.currentTimeMillis() / 1000)
                    item.put("is_edited", 1)
                    updated = true
                    break
                }
            }
            if (updated) {
                file.writeText(json.toString())
            }
        } catch (e: Exception) {
            MLog.e("ChatCacheManager", "Failed to update cached message $messageId for peer $peerId: ${e.message}")
        }
    }

    fun markCachedMessageDeleted(peerId: Int, messageId: Int) {
        try {
            val file = getCacheFile(peerId)
            if (!file.exists()) return
            val content = file.readText()
            if (content.isBlank()) return
            val json = JSONObject(content)
            val responseObj = if (json.has("response")) json.optJSONObject("response") else json
            val items = responseObj?.optJSONArray("items") ?: return
            var updated = false
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                if (item.optInt("id") == messageId) {
                    item.put("deleted", 1)
                    item.put("is_deleted", true)
                    updated = true
                    break
                }
            }
            if (updated) {
                file.writeText(json.toString())
            }
        } catch (e: Exception) {
            MLog.e("ChatCacheManager", "Failed to mark cached message $messageId deleted for peer $peerId: ${e.message}")
        }
    }

    fun saveVisiblePosition(peerId: Int, messageId: Int) {
        if (messageId <= 0) return
        val key = getAccountKey()
        prefs.edit().putInt("pos_${key}_$peerId", messageId).apply()
    }

    fun getVisiblePosition(peerId: Int): Int? {
        val key = getAccountKey()
        val id = prefs.getInt("pos_${key}_$peerId", -1)
        return if (id > 0) id else null
    }

    fun clearPosition(peerId: Int) {
        val key = getAccountKey()
        prefs.edit().remove("pos_${key}_$peerId").apply()
    }

    fun savePeerInfo(peer: com.deliriousvoid.openvkmatcha.data.model.Peer) {
        try {
            val file = getPeerFile(peer.id)
            val json = JSONObject().apply {
                put("id", peer.id)
                put("type", peer.type.name)
                put("title", peer.title)
                put("photo", peer.photo)
                put("online", peer.online)
                put("mobileOnline", peer.mobileOnline)
                put("verified", peer.verified)
                if (peer.membersCount != null) {
                    put("membersCount", peer.membersCount)
                }
            }
            file.writeText(json.toString())
        } catch (e: Exception) {
            MLog.e("ChatCacheManager", "Failed to save peer cache for ${peer.id}: ${e.message}")
        }
    }

    fun getCachedPeerInfo(peerId: Int): com.deliriousvoid.openvkmatcha.data.model.Peer? {
        return try {
            val file = getPeerFile(peerId)
            if (!file.exists()) return null
            val content = file.readText()
            if (content.isBlank()) return null
            val json = JSONObject(content)
            val membersCount = if (json.has("membersCount")) json.getInt("membersCount") else null
            com.deliriousvoid.openvkmatcha.data.model.Peer(
                id = json.getInt("id"),
                type = com.deliriousvoid.openvkmatcha.data.model.PeerType.valueOf(json.optString("type", "USER")),
                title = json.optString("title", ""),
                photo = json.optString("photo", ""),
                online = json.optBoolean("online", false),
                mobileOnline = json.optBoolean("mobileOnline", false),
                verified = json.optBoolean("verified", false),
                membersCount = membersCount
            )
        } catch (e: Exception) {
            null
        }
    }
}