package com.deliriousvoid.openvkmatcha

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.deliriousvoid.openvkmatcha.data.api.ArtworkApi
import com.deliriousvoid.openvkmatcha.data.api.LyricsApi
import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.repository.*
import com.deliriousvoid.openvkmatcha.data.security.AccountManager
import com.deliriousvoid.openvkmatcha.data.security.TokenManager
import com.deliriousvoid.openvkmatcha.playback.MusicPlayerManager
import com.deliriousvoid.openvkmatcha.playback.PreloadManager
import com.deliriousvoid.openvkmatcha.util.LongPollManager
import com.deliriousvoid.openvkmatcha.util.OnlineManager
import androidx.lifecycle.ProcessLifecycleOwner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class OpenVKMatchaApp : Application(), ImageLoaderFactory {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var accountManager: AccountManager
        private set

    lateinit var api: OpenVKApi
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var feedRepository: FeedRepository
        private set

    lateinit var messagesRepository: MessagesRepository
        private set

    lateinit var musicRepository: MusicRepository
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var lyricsRepository: LyricsRepository
        private set

    lateinit var artworkRepository: ArtworkRepository
        private set

    lateinit var downloadRepository: DownloadRepository
        private set

    lateinit var notificationsRepository: NotificationsRepository
        private set

    lateinit var commentsRepository: CommentsRepository
        private set

    lateinit var listenBrainzRepository: ListenBrainzRepository
        private set

    lateinit var reportsRepository: ReportsRepository
        private set

    lateinit var boardRepository: BoardRepository
        private set

    lateinit var attachmentsRepository: AttachmentsRepository
        private set

    lateinit var videoRepository: VideoRepository
        private set

    lateinit var docsRepository: DocsRepository
        private set

    lateinit var notesRepository: NotesRepository
        private set

    lateinit var longPollManager: LongPollManager
        private set

    lateinit var onlineManager: OnlineManager
        private set

    lateinit var playerManager: MusicPlayerManager
        private set

    lateinit var preloadManager: PreloadManager
        private set

    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(enable = true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize osmdroid configuration
        @Suppress("DEPRECATION")
        org.osmdroid.config.Configuration.getInstance().load(this, android.preference.PreferenceManager.getDefaultSharedPreferences(this))
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName

        tokenManager = TokenManager(this)
        accountManager = AccountManager(this)
        api = OpenVKApi(tokenManager)
        authRepository = AuthRepository(api)
        feedRepository = FeedRepository(api)
        messagesRepository = MessagesRepository(api)
        musicRepository = MusicRepository(api)
        profileRepository = ProfileRepository(api)
        lyricsRepository = LyricsRepository(LyricsApi())
        artworkRepository = ArtworkRepository(ArtworkApi())
        downloadRepository = DownloadRepository(this)
        notificationsRepository = NotificationsRepository(api)
        commentsRepository = CommentsRepository(api)
        listenBrainzRepository = ListenBrainzRepository()
        reportsRepository = ReportsRepository(api)
        boardRepository = BoardRepository(api)
        attachmentsRepository = AttachmentsRepository(this, api)
        videoRepository = VideoRepository(api)
        docsRepository = DocsRepository(api)
        notesRepository = NotesRepository(api)
        longPollManager = LongPollManager(api, notificationsRepository)
        onlineManager = OnlineManager(this, authRepository)
        ProcessLifecycleOwner.get().lifecycle.addObserver(onlineManager)
        preloadManager = PreloadManager(this)
        playerManager = MusicPlayerManager(this, musicRepository, artworkRepository)

        if (tokenManager.hasToken()) {
            com.deliriousvoid.openvkmatcha.services.LongPollService.start(this)
            onlineManager.start()
        }
    }

    companion object {
        lateinit var instance: OpenVKMatchaApp
            private set
    }
}
