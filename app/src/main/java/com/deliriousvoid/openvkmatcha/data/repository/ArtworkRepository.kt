package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.ArtworkApi

class ArtworkRepository(private val api: ArtworkApi) {
    private val cache = mutableMapOf<String, String?>()

    suspend fun getArtworkUrl(artist: String, title: String): Result<String?> {
        val cacheKey = "$artist - $title"
        if (cache.containsKey(cacheKey)) {
            return Result.success(cache[cacheKey])
        }

        return api.getArtworkUrl(artist, title).onSuccess {
            cache[cacheKey] = it
        }
    }
}
