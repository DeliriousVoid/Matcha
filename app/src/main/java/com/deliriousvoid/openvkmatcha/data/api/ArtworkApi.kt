package com.deliriousvoid.openvkmatcha.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class ArtworkApi {
    private val client = OkHttpClient()
    private val baseUrl = "https://itunes.apple.com/search"

    suspend fun getArtworkUrl(artist: String, title: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val query = "$artist $title"
            val url = "$baseUrl?term=${URLEncoder.encode(query, "UTF-8")}&media=music&limit=1"

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))

                val bodyText = response.body?.string() ?: return@withContext Result.success(null)
                val json = JSONObject(bodyText)
                val results = json.optJSONArray("results")
                
                if (results != null && results.length() > 0) {
                    val track = results.getJSONObject(0)
                    // Use a more robust way to get higher resolution
                    val artworkUrl100 = track.optString("artworkUrl100")
                    val artworkUrl = artworkUrl100.replace(Regex("/\\d+x\\d+bb"), "/600x600bb")
                    Result.success(artworkUrl.ifBlank { artworkUrl100 })
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
