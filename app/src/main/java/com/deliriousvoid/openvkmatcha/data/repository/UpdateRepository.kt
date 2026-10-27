package com.deliriousvoid.openvkmatcha.data.repository

import android.content.Context
import com.deliriousvoid.openvkmatcha.BuildConfig
import com.deliriousvoid.openvkmatcha.data.model.GithubAsset
import com.deliriousvoid.openvkmatcha.data.model.GithubRelease
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val repoUrl = "https://api.github.com/repos/DeliriousVoid/Matcha/releases"

    suspend fun getLatestRelease(ignorePrereleases: Boolean): Result<GithubRelease?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(repoUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Matcha-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("GitHub API error: ${response.code}"))

            val bodyText = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val array = JSONArray(bodyText)
            
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isPrerelease = obj.getBoolean("prerelease")
                if (ignorePrereleases && isPrerelease) continue

                val tagName = obj.getString("tag_name")
                val name = obj.getString("name")
                val releaseBody = obj.getString("body")
                val htmlUrl = obj.getString("html_url")
                
                val assetsArray = obj.getJSONArray("assets")
                val assets = mutableListOf<GithubAsset>()
                for (j in 0 until assetsArray.length()) {
                    val assetObj = assetsArray.getJSONObject(j)
                    assets.add(GithubAsset(
                        name = assetObj.getString("name"),
                        browserDownloadUrl = assetObj.getString("browser_download_url"),
                        contentType = assetObj.getString("content_type"),
                        size = assetObj.getLong("size")
                    ))
                }

                return@withContext Result.success(GithubRelease(
                    tagName = tagName,
                    name = name,
                    body = releaseBody,
                    isPrerelease = isPrerelease,
                    htmlUrl = htmlUrl,
                    assets = assets
                ))
            }
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isUpdateAvailable(latestTagName: String): Boolean {
        val current = BuildConfig.VERSION_NAME

        val regex = Regex("[0-9]+(?:\\.[0-9]+)*")
        
        val currentMatch = regex.find(current)?.value ?: return false
        val latestMatch = regex.find(latestTagName)?.value ?: return false
        
        val currentParts = currentMatch.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latestMatch.split(".").mapNotNull { it.toIntOrNull() }
        
        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        
        // If versions are numerically equal (e.g. "pre-1.4" and "1.4")
        // we check if the current one is a pre-release and the latest is not.
        val currentIsPre = current.contains("pre", ignoreCase = true) || current.contains("dev", ignoreCase = true)
        val latestIsPre = latestTagName.contains("pre", ignoreCase = true) || latestTagName.contains("dev", ignoreCase = true)
        
        if (currentIsPre && !latestIsPre) {
            return true
        }

        return false
    }
}
