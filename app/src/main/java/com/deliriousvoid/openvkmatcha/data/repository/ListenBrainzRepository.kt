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
    private val apiUrl = "https://api.listenbrainz.org/1/submit-listens"

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
            
            Log.d("ListenBrainz", "SUBMIT ($type) to $apiUrl")
            Log.d("ListenBrainz", "Auth: ${authHeader.take(15)}...")

            val request = Request.Builder()
                .url(apiUrl)
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
