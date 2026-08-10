package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.LyricsApi
import com.deliriousvoid.openvkmatcha.data.model.LyricsRecord

class LyricsRepository(private val api: LyricsApi) {
    private val cache = mutableMapOf<String, LyricsRecord?>()

    suspend fun getLyrics(trackName: String, artistName: String, duration: Int): Result<LyricsRecord?> {
        val cacheKey = "$trackName - $artistName"
        if (cache.containsKey(cacheKey)) {
            return Result.success(cache[cacheKey])
        }

        return api.getLyrics(trackName, artistName, duration).onSuccess {
            cache[cacheKey] = it
        }
    }
}
