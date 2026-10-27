package com.deliriousvoid.openvkmatcha.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.repository.NotificationsRepository
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class LongPollManager(
    private val api: OpenVKApi,
    private val notificationsRepository: NotificationsRepository
) : DefaultLifecycleObserver {
    private val TAG = "LongPollManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false
    private var job: Job? = null
    
    private val client: OkHttpClient = createLongPollClient()

    private var server: String? = null
    private var key: String? = null
    private var ts: Long = 0
    private var pts: Int? = null
    private val processedEvents = ConcurrentHashMap<Int, Long>()
    private val MAX_PROCESSED_EVENTS = 1000

    interface OnLongPollEventListener {
        fun onNewMessage(messageId: Int, peerId: Int, timestamp: Long, text: String, fromId: Int, isOut: Boolean)
        fun onMessageRead(peerId: Int, localId: Int) {}
        fun onIncomingMessagesRead(peerId: Int, localId: Int) {}
        fun onOutgoingMessagesRead(peerId: Int, localId: Int) {}
        fun onMessageEdit(messageId: Int, peerId: Int, newText: String) {}
        fun onMessageDelete(messageId: Int, peerId: Int) {}
        fun onUserTyping(peerId: Int, userId: Int) {}
        fun onUserOnline(userId: Int, isOnline: Boolean) {}
        fun onChatChanged(peerId: Int) {}
        fun onNotificationsCountUpdated() {}
    }

    private val listeners = mutableListOf<OnLongPollEventListener>()

    fun addListener(listener: OnLongPollEventListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) listeners.add(listener)
        }
    }

    fun removeListener(listener: OnLongPollEventListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        MLog.d(TAG, "App in foreground, starting LongPoll if not running")
        start()
    }

    override fun onStop(owner: LifecycleOwner) {
        MLog.d(TAG, "App in background, stopping LongPoll")
        val prefs = OpenVKMatchaApp.instance.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("background_notifications", false)) {
            stop()
        }
    }

    fun isRunning(): Boolean = isRunning

    fun start() {
        if (isRunning) return
        isRunning = true
        MLog.d(TAG, "Starting LongPoll loop")
        job = scope.launch {
            while (isRunning) {
                try {
                    if (server == null || key == null) {
                        fetchServer()
                        if (isRunning && server != null) {
                            performFirstSync()
                        }
                    } else {
                        performCheck()
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    MLog.e(TAG, "Error in LongPoll loop", e)
                    delay(3000)
                    server = null
                }
            }
        }
    }

    fun stop() {
        MLog.d(TAG, "Stopping LongPoll loop")
        isRunning = false
        job?.cancel()
        job = null
        server = null
        key = null
        ts = 0
        pts = null
        processedEvents.clear()
    }

    private suspend fun fetchServer() {
        notificationsRepository.getLongPollServer().onSuccess { json ->
            val response = json.optJSONObject("response") ?: json
            var srv = response.optString("server")
            if (srv.isNotBlank() && !srv.contains("://")) {
                val scheme = if (api.baseUrl.startsWith("https")) "https://" else "http://"
                srv = scheme + srv
            }
            if (srv.isNotBlank()) {
                server = srv
                key = response.optString("key")
                ts = response.optLong("ts")
                pts = if (response.has("pts")) response.optInt("pts") else null
                MLog.d(TAG, "Server obtained: $server, ts: $ts")
            } else {
                MLog.e(TAG, "Empty server in response")
                delay(5000)
            }
        }.onFailure {
            MLog.e(TAG, "Failed to get LongPoll server: ${it.message}")
            delay(5000)
        }
    }

    private suspend fun performFirstSync() {
        MLog.d(TAG, "Performing first sync request")
        val currentServer = server ?: return
        val currentKey = key ?: return
        val url = "$currentServer?key=$currentKey&act=a_check&wait=1&version=3&ts=$ts"
        val request = Request.Builder().url(url).build()
        
        try {
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            val body = response.body?.string() ?: return
            MLog.data(TAG, "First sync response", body)
            
            if (body.isBlank() || body.trim() == "[]") return
            
            val json = JSONObject(body)
            if (json.has("failed")) {
                val failCode = json.optInt("failed")
                if (failCode == 1 && json.has("ts")) {
                    ts = json.getLong("ts")
                } else {
                    server = null
                }
            } else if (json.has("ts")) {
                ts = json.getLong("ts")
                json.optJSONArray("updates")?.let { processUpdates(it) }
            }
        } catch (e: Exception) {
            MLog.e(TAG, "First sync failed", e)
            server = null
        }
    }

    private suspend fun performCheck() {
        val currentServer = server ?: return
        val currentKey = key ?: return

        // Mode: 2 (attachments) + 8 (pts) + 32 (pts) + 64 (extra) + 128 (random_id) = 234
        val urlBuilder = StringBuilder(if (currentServer.startsWith("http")) currentServer else "https://$currentServer")
            .append("?key=").append(currentKey)
            .append("&act=a_check&wait=25&version=3&ts=").append(ts)
            .append("&mode=234")
        
        pts?.let { urlBuilder.append("&pts=").append(it) }
        
        val request = Request.Builder().url(urlBuilder.toString()).build()
        
        try {
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            val body = response.body?.string() ?: throw Exception("Empty body")
            MLog.data(TAG, "Check response", body)

            if (body.isBlank() || body.trim() == "[]") {
                delay(1000)
                return
            }

            val json = JSONObject(body)
            if (json.has("failed")) {
                val code = json.optInt("failed")
                MLog.w(TAG, "LongPoll returned failed code: $code")
                when (code) {
                    1 -> {
                        if (json.has("ts")) {
                            ts = json.getLong("ts")
                        }
                    }
                    2, 3, 4 -> {
                        server = null
                        key = null
                    }
                }
                return
            }

            if (json.has("ts")) {
                ts = json.getLong("ts")
            }
            if (json.has("pts")) {
                pts = json.optInt("pts")
            }
            json.optJSONArray("updates")?.let { processUpdates(it) }

        } catch (e: Exception) {
            if (e !is CancellationException) {
                MLog.e(TAG, "Check failed", e)
                delay(3000)
                server = null
            }
        }
    }

    private fun normalizePeerId(rawPeerId: Int, flags: Int = 0, attachments: JSONObject? = null): Int {
        if (rawPeerId >= 2000000000 || rawPeerId < 0) return rawPeerId
        val isChat = (flags and 16) != 0 || (attachments != null && attachments.has("from") && attachments.optString("from").toIntOrNull() != rawPeerId)
        return if (isChat) 2000000000 + rawPeerId else rawPeerId
    }

    private fun processUpdates(updates: org.json.JSONArray) {
        for (i in 0 until updates.length()) {
            try {
                val update = updates.getJSONArray(i)
                val type = update.getInt(0)
                
                when (type) {
                    3 -> {
                        // [3, message_id, flags, peer_id]
                        val msgId = update.optInt(1)
                        val flags = update.optInt(2)
                        val rawPeerId = update.optInt(3)
                        val peerId = normalizePeerId(rawPeerId, flags)
                        notify { it.onMessageRead(peerId, msgId) }
                    }
                    4 -> processNewMessage(update)
                    5 -> {
                        val msgId = update.getInt(1)
                        val flags = update.optInt(2)
                        val rawPeerId = update.optInt(3)
                        val peerId = normalizePeerId(rawPeerId, flags)
                        val text = update.optString(5)
                        notify { it.onMessageEdit(msgId, peerId, text) }
                    }
                    6, 7 -> {
                        val rawPeerId = update.getInt(1)
                        val peerId = normalizePeerId(rawPeerId)
                        val localId = update.getInt(2)
                        notify { it.onMessageRead(peerId, localId) }
                        scope.launch { AppEvents.emitRefreshNotifications() }
                    }
                    8, 9 -> {
                        val userId = Math.abs(update.getInt(1))
                        notify { it.onUserOnline(userId, type == 8) }
                    }
                    13 -> {
                        // [13, message_id, flags, peer_id]
                        val msgId = update.optInt(1)
                        val rawPeerId = update.optInt(3)
                        val peerId = normalizePeerId(rawPeerId)
                        notify { it.onMessageDelete(msgId, peerId) }
                    }
                    51, 52 -> {
                        // [51/52, chat_id, self]
                        val chatId = update.optInt(1)
                        val peerId = if (chatId < 2000000000) 2000000000 + chatId else chatId
                        notify { it.onChatChanged(peerId) }
                    }
                    61, 62 -> {
                        val userID = update.optInt(1)
                        val rawPeerID = if (type == 62) update.optInt(2) else userID
                        val peerID = if (type == 62 && rawPeerID < 2000000000) 2000000000 + rawPeerID else rawPeerID
                        notify { it.onUserTyping(peerID, userID) }
                    }
                    63, 64 -> {
                        val rawPeerID = update.optInt(1)
                        val peerID = if (rawPeerID < 2000000000) 2000000000 + rawPeerID else rawPeerID
                        val userIDs = update.optJSONArray(2)
                        if (userIDs != null) {
                            for (u in 0 until userIDs.length()) {
                                notify { it.onUserTyping(peerID, userIDs.optInt(u)) }
                            }
                        }
                    }
                    80 -> {
                        notify { it.onNotificationsCountUpdated() }
                        scope.launch { AppEvents.emitRefreshNotifications() }
                    }
                }
            } catch (e: Exception) {
                MLog.e(TAG, "Error processing update at $i", e)
            }
        }
    }

    private fun processNewMessage(update: org.json.JSONArray) {
        // [4, message_id, flags, peer_id, timestamp, subject, text, attachments, random_id]
        if (update.length() < 5) return
        
        val msgId = update.optInt(1)
        val flags = update.optInt(2)
        val rawPeerId = update.optInt(3)
        val timestamp = update.optLong(4)
        val text = update.optString(6).ifEmpty { update.optString(5) }
        
        if (processedEvents.containsKey(msgId)) return
        processedEvents[msgId] = timestamp
        if (processedEvents.size > MAX_PROCESSED_EVENTS) processedEvents.clear()

        val attachments = if (update.length() > 7) update.optJSONObject(7) else null
        val peerId = normalizePeerId(rawPeerId, flags, attachments)

        var fromId = peerId
        attachments?.let { 
            if (it.has("from")) {
                fromId = it.optString("from").toIntOrNull() ?: peerId
            }
        }

        val isOut = (flags and 2) != 0
        
        notify { it.onNewMessage(msgId, peerId, timestamp, text, fromId, isOut) }
        scope.launch { AppEvents.emitRefreshNotifications() }
    }

    private fun notify(block: (OnLongPollEventListener) -> Unit) {
        synchronized(listeners) {
            listeners.forEach { block(it) }
        }
    }

    private fun createLongPollClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
        )
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .readTimeout(90, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}
