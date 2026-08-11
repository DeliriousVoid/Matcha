package com.deliriousvoid.openvkmatcha.data.api

import android.util.Log
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
    private val tag = "LyricsApi"

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

            val url = urlBuilder.toString()
            Log.d(tag, "Fetching lyrics from: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OpenVKMatcha/1.0.1 (https://github.com/deliriousvoid/OpenVKMatcha)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    Log.d(tag, "Lyrics not found (404)")
                    return@withContext Result.success(null)
                }
                if (!response.isSuccessful) {
                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                    Log.e(tag, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

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
            Log.e(tag, "Error fetching lyrics", e)
            Result.failure(e)
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
