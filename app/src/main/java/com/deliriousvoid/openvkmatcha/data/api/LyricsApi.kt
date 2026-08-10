package com.deliriousvoid.openvkmatcha.data.api

import com.deliriousvoid.openvkmatcha.data.model.LyricsRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class LyricsApi {
    private val client = OkHttpClient()
    private val baseUrl = "https://lrclib.net/api"

    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        duration: Int? = null
    ): Result<LyricsRecord?> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder("$baseUrl/get")
            urlBuilder.append("?track_name=${encode(trackName)}")
            urlBuilder.append("&artist_name=${encode(artistName)}")
            duration?.let { urlBuilder.append("&duration=$it") }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext Result.success(null)
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))

                val bodyText = response.body?.string() ?: return@withContext Result.success(null)
                val json = JSONObject(bodyText)
                
                Result.success(
                    LyricsRecord(
                        id = json.getInt("id"),
                        name = json.optString("name"),
                        trackName = json.optString("trackName"),
                        artistName = json.optString("artistName"),
                        albumName = json.optString("albumName"),
                        duration = json.optInt("duration"),
                        instrumental = json.optBoolean("instrumental"),
                        plainLyrics = json.optString("plainLyrics"),
                        syncedLyrics = json.optString("syncedLyrics")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
