package com.deliriousvoid.openvkmatcha.data.repository

import android.util.Log
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ListenBrainzRepository {
    private val client = OkHttpClient()
    private val baseUrl = "https://api.listenbrainz.org/1"
    private val resolvedTracksCache = mutableMapOf<String, List<AudioTrack>>()

    fun getCachedTracks(mbid: String): List<AudioTrack>? {
        return resolvedTracksCache[mbid]
    }

    fun cacheTracks(mbid: String, tracks: List<AudioTrack>) {
        resolvedTracksCache[mbid] = tracks
    }

    fun clearCache(mbid: String) {
        resolvedTracksCache.remove(mbid)
    }

    suspend fun validateToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedToken = token.trim()
        if (trimmedToken.isBlank()) return@withContext Result.failure(Exception("Token is empty"))

        try {
            val request = Request.Builder()
                .url("$baseUrl/validate-token")
                .addHeader("Authorization", "Token $trimmedToken")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                if (json.optBoolean("valid")) {
                    Result.success(json.getString("user_name"))
                } else {
                    Result.failure(Exception(json.optString("message", "Invalid token")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecommendations(username: String, token: String?): Result<List<com.deliriousvoid.openvkmatcha.data.model.Playlist>> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("$baseUrl/user/$username/playlists/createdfor")
            
            token?.trim()?.takeIf { it.isNotBlank() }?.let {
                requestBuilder.addHeader("Authorization", "Token $it")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                
                val json = JSONObject(body)
                val playlists = json.getJSONArray("playlists")
                val result = mutableListOf<com.deliriousvoid.openvkmatcha.data.model.Playlist>()
                
                for (i in 0 until playlists.length()) {
                    val item = playlists.getJSONObject(i)
                    val playlistObj = item.getJSONObject("playlist")
                    val identifier = playlistObj.optString("identifier", "")
                    val mbid = identifier.substringAfterLast("/")

                    result.add(
                        com.deliriousvoid.openvkmatcha.data.model.Playlist(
                            id = 0,
                            ownerId = 0,
                            title = playlistObj.getString("title"),
                            description = playlistObj.optString("description", ""),
                            trackCount = 0, // Will be fetched later
                            photoUrl = com.deliriousvoid.openvkmatcha.Constants.RECOMMENDATION_COVER_URL,
                            mbid = mbid,
                        )
                    )
                }
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylistTracks(mbid: String, token: String?): Result<List<AudioTrack>> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("$baseUrl/playlist/$mbid")
            
            token?.trim()?.takeIf { it.isNotBlank() }?.let {
                requestBuilder.addHeader("Authorization", "Token $it")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                
                val json = JSONObject(body)
                val playlist = json.getJSONObject("playlist")
                val tracks = playlist.getJSONArray("track")
                val result = mutableListOf<AudioTrack>()
                
                for (i in 0 until tracks.length()) {
                    val item = tracks.getJSONObject(i)
                    val creator = item.optString("creator", "Unknown Artist")
                    val title = item.optString("title", "Unknown Track")
                    val duration = item.optInt("duration", 0) / 1000
                    
                    result.add(
                        AudioTrack(
                            id = i + 1, // Artificial ID
                            ownerId = -1, // Mark as ListenBrainz
                            artist = creator,
                            title = title,
                            duration = duration,
                            url = null,
                            isReady = true
                        )
                    )
                }
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitListen(
        token: String, 
        track: AudioTrack, 
        type: String, 
        listenedAt: Long? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedToken = token.trim()
        if (trimmedToken.isBlank()) return@withContext Result.failure(Exception("Token is empty"))

        try {
            val trackMetadata = JSONObject().apply {
                put("artist_name", track.artist.takeIf { it.isNotBlank() } ?: "Unknown Artist")
                put("track_name", track.title.takeIf { it.isNotBlank() } ?: "Unknown Track")
                put("additional_info", JSONObject().apply {
                    put("duration", track.duration)
                    put("submission_client", "matcha")
                    put("submission_client_version", "1.0")
                })
            }

            val payloadItem = JSONObject().apply {
                put("track_metadata", trackMetadata)
                if (listenedAt != null) {
                    put("listened_at", listenedAt)
                }
            }

            val requestBody = JSONObject().apply {
                put("listen_type", type)
                put("payload", JSONArray().put(payloadItem))
            }.toString()

            val authHeader = "Token $trimmedToken"
            
            val submitUrl = "$baseUrl/submit-listens"
            Log.d("ListenBrainz", "SUBMIT ($type) to $submitUrl")
            Log.d("ListenBrainz", "Auth: ${authHeader.take(15)}...")

            val request = Request.Builder()
                .url(submitUrl)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Authorization", authHeader)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                Log.d("ListenBrainz", "RESPONSE (${response.code}): $body")
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e("ListenBrainz", "ERROR: ${e.message}", e)
            Result.failure(e)
        }
    }
}
