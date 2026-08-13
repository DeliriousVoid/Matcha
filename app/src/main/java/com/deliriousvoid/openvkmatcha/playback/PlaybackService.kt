package com.deliriousvoid.openvkmatcha.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.util.BitmapLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future
import android.graphics.Bitmap
import android.net.Uri

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var videoMediaSession: MediaSession? = null

    companion object {
        const val ACTION_UPDATE_VIDEO_SESSION = "com.deliriousvoid.openvkmatcha.playback.ACTION_UPDATE_VIDEO_SESSION"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // Release existing session if any (should not happen in normal Lifecycle)
        mediaSession?.release()
        
        val preloadManager = OpenVKMatchaApp.instance.preloadManager
        
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30000, 60000, 2500, 5000)
            .build()
            
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(preloadManager.createCacheDataSourceFactory()))
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(audioAttributes, true)
        
        val intent = Intent(this, Class.forName("com.deliriousvoid.openvkmatcha.MainActivity")).apply {
            action = "com.deliriousvoid.openvkmatcha.ACTION_OPEN_PLAYER"
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setId("MusicPlaybackService_${System.currentTimeMillis()}")
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(CoilBitmapLoader())
            .build()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @UnstableApi
    private inner class CoilBitmapLoader : BitmapLoader {
        override fun supportsMimeType(mimeType: String): Boolean = true

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
            return Futures.immediateFailedFuture(UnsupportedOperationException())
        }

        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
            return serviceScope.future {
                val request = ImageRequest.Builder(this@PlaybackService)
                    .data(uri)
                    .size(600, 600)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap
                } else {
                    throw (result as coil.request.ErrorResult).throwable
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val vSession = videoMediaSession
        if (vSession != null && (vSession.player.isPlaying || vSession.player.playbackState == Player.STATE_BUFFERING)) {
            return vSession
        }
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_VIDEO_SESSION) {
            updateVideoSession()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateVideoSession() {
        val activeVideo = AppEvents.activeVideo.value
        val player = AppEvents.activeExoPlayer.value

        // Release old video session
        videoMediaSession?.let {
            removeSession(it)
            it.release()
        }
        videoMediaSession = null
        AppEvents.setVideoSession(null)

        if (activeVideo != null && player != null) {
            val intent = Intent(this, Class.forName("com.deliriousvoid.openvkmatcha.MainActivity")).apply {
                action = "com.deliriousvoid.openvkmatcha.ACTION_OPEN_VIDEO_PLAYER"
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            val session = MediaSession.Builder(this, player)
                .setId("VideoPlaybackSession_${activeVideo.id}_${System.currentTimeMillis()}")
                .setSessionActivity(pendingIntent)
                .build()
            
            videoMediaSession = session
            addSession(session)
            AppEvents.setVideoSession(session)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = videoMediaSession?.player ?: mediaSession?.player
        if (player != null && (!player.playWhenReady || player.playbackState == Player.STATE_IDLE)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        videoMediaSession?.run {
            player.release()
            release()
            videoMediaSession = null
        }
        super.onDestroy()
    }
}
