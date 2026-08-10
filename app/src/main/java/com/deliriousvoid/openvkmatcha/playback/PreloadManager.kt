package com.deliriousvoid.openvkmatcha.playback

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.core.net.toUri
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
class PreloadManager(private val context: Context) {
    private val cacheDir = File(context.cacheDir, "media_cache")
    private val cacheEvictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024) // 500MB
    private val cache = SimpleCache(cacheDir, cacheEvictor, androidx.media3.database.StandaloneDatabaseProvider(context))

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var prefetchJobs = mutableMapOf<String, Job>()

    fun createCacheDataSourceFactory(): CacheDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("OpenVKMatcha/1.0")
            .setAllowCrossProtocolRedirects(true)
        
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun preload(tracks: List<AudioTrack>) {
        // Cancel old jobs that are not in the new list
        val trackIds = tracks.asSequence().map { it.stableId }.toSet()
        prefetchJobs.keys.toList().forEach { id ->
            if (id !in trackIds) {
                prefetchJobs[id]?.cancel()
                prefetchJobs.remove(id)
            }
        }

        tracks.forEach { track ->
            if ((track.stableId !in prefetchJobs) && (track.url != null)) {
                val job = scope.launch {
                    try {
                        val uri = track.url.toUri()
                        val dataSpec = DataSpec(uri, 0, 1024 * 1024 * 2) // Cache first 2MB
                        val dataSource = createCacheDataSourceFactory().createDataSource()
                        
                        val buffer = ByteArray(8192)
                        dataSource.open(dataSpec)
                        var totalRead = 0
                        while (totalRead < dataSpec.length) {
                            val read = dataSource.read(buffer, 0, buffer.size)
                            if (read == -1) break
                            totalRead += read
                        }
                        dataSource.close()
                    } catch (e: Exception) {
                        Log.e("PreloadManager", "Preload failed for ${track.stableId}", e)
                    }
                }
                prefetchJobs[track.stableId] = job
            }
        }
    }
}
