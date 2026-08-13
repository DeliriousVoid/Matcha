package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.ArtworkApi

class ArtworkRepository(private val api: ArtworkApi) {
    private val cache = mutableMapOf<String, String?>()

    suspend fun getArtworkUrl(artist: String, title: String): Result<String?> {
        val cacheKey = "$artist - $title"
        getCachedArtworkUrl(artist, title)?.let { return Result.success(it) }

        return api.getArtworkUrl(artist, title).onSuccess {
            cache[cacheKey] = it
        }
    }

    fun getCachedArtworkUrl(artist: String, title: String): String? {
        return cache["$artist - $title"]
    }
}
