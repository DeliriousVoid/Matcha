package com.deliriousvoid.openvkmatcha.util

import android.content.Context
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.StickerItem
import org.json.JSONArray
import org.json.JSONObject

class RecentStickersManager(private val context: Context = OpenVKMatchaApp.instance) {
    private val prefs = context.getSharedPreferences("recent_stickers", Context.MODE_PRIVATE)
    private val limit = 30

    fun load(userId: Int): List<StickerItem> {
        if (userId <= 0) return emptyList()
        val jsonStr = prefs.getString("stickers_$userId", null) ?: return emptyList()
        val result = mutableListOf<StickerItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    StickerItem(
                        id = obj.getInt("id"),
                        packId = obj.optInt("packId"),
                        url = obj.getString("url"),
                        animationUrl = obj.optString("animationUrl").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (e: Exception) {
            MLog.e("RecentStickersManager", "Failed to parse recent stickers", e)
        }
        return result
    }

    fun record(sticker: StickerItem, userId: Int): List<StickerItem> {
        if (userId <= 0) return emptyList()
        val current = load(userId).filter { it.id != sticker.id }
        val updated = (listOf(sticker) + current).take(limit)
        try {
            val array = JSONArray()
            for (item in updated) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("packId", item.packId)
                    put("url", item.url)
                    put("animationUrl", item.animationUrl ?: "")
                }
                array.put(obj)
            }
            prefs.edit().putString("stickers_$userId", array.toString()).apply()
        } catch (e: Exception) {
            MLog.e("RecentStickersManager", "Failed to save recent stickers", e)
        }
        return updated
    }
}
