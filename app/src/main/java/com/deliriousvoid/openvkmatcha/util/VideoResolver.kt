package com.deliriousvoid.openvkmatcha.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamExtractor

object VideoResolver {
    private var isInitialized = false

    private fun ensureInitialized() {
        if (!isInitialized) {
            NewPipe.init(NewPipeDownloader(okhttp3.OkHttpClient()), Localization.DEFAULT)
            isInitialized = true
        }
    }

    suspend fun resolveDirectUrl(playerUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val service = try {
                NewPipe.getServiceByUrl(playerUrl)
            } catch (e: Exception) {
                ServiceList.YouTube
            }
            
            val extractor: StreamExtractor = service.getStreamExtractor(playerUrl)
            extractor.fetchPage()

            // Try to find a progressive stream (video + audio)
            val progressiveStream = extractor.videoStreams.firstOrNull { it.url != null }
            if (progressiveStream != null) {
                return@withContext progressiveStream.url
            }

            // Fallback to highest quality video-only stream (ExoPlayer can handle this if we had audio too, but let's keep it simple)
            extractor.videoOnlyStreams.firstOrNull { it.url != null }?.url
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun isYouTube(url: String?): Boolean {
        if (url == null) return false
        return url.contains("youtube.com") || url.contains("youtu.be")
    }
}
