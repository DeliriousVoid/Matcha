package com.deliriousvoid.openvkmatcha.util

import android.util.Log
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
) {
    private val TAG = "LongPollManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false
    private var job: Job? = null
    
    private val client: OkHttpClient = createLongPollClient()

    private var server: String? = null
    private var key: String? = null
    private var ts: Long = 0
    private var fastResponseCount = 0
    private val processedEvents = ConcurrentHashMap<Int, Long>()
    private val MAX_PROCESSED_EVENTS = 1000

    interface OnLongPollEventListener {
        fun onNewMessage(messageId: Int, peerId: Int, timestamp: Long, text: String, fromId: Int, isOut: Boolean)
        fun onMessageRead(peerId: Int, localId: Int)
        fun onMessageEdit(messageId: Int, peerId: Int, newText: String)
        fun onUserTyping(peerId: Int, userId: Int)
        fun onUserOnline(userId: Int, isOnline: Boolean)
        fun onNotificationsCountUpdated()
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

    fun isRunning(): Boolean = isRunning

    fun start() {
        if (isRunning) return
        isRunning = true
        Log.d(TAG, "Starting LongPoll loop")
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
                    Log.e(TAG, "Error in LongPoll loop", e)
                    delay(3000)
                    server = null // Reset to fetch fresh server
                }
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping LongPoll loop")
        isRunning = false
        job?.cancel()
        job = null
        server = null
        key = null
        ts = 0
    }

    private suspend fun fetchServer() {
        notificationsRepository.getLongPollServer().onSuccess { json ->
            val response = json.optJSONObject("response") ?: return@onSuccess
            var srv = response.optString("server")
            if (srv != null && !srv.contains("://")) {
                val scheme = if (api.baseUrl.startsWith("https")) "https://" else "http://"
                srv = scheme + srv
            }
            server = srv
            key = response.optString("key")
            ts = response.optLong("ts")
            fastResponseCount = 0
            Log.d(TAG, "Server obtained: $server, ts: $ts")
        }.onFailure {
            Log.e(TAG, "Failed to get LongPoll server: ${it.message}")
            delay(10000)
        }
    }

    private suspend fun performFirstSync() {
        Log.d(TAG, "Performing first sync request")
        val currentServer = server ?: return
        val currentKey = key ?: return
        val url = "$currentServer?key=$currentKey&act=a_check&wait=1&version=3&ts=$ts"
        val request = Request.Builder().url(url).build()
        
        try {
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            val body = response.body?.string() ?: return
            Log.d(TAG, "First sync response: $body")
            
            if (body.trim() == "[]") return
            
            val json = JSONObject(body)
            if (json.has("failed")) {
                val failCode = json.getInt("failed")
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
            Log.e(TAG, "First sync failed", e)
            server = null
        }
    }

    private suspend fun performCheck() {
        val currentServer = server ?: return
        val currentKey = key ?: return
        
        val currentTime = System.currentTimeMillis() / 1000
        val timeDiff = currentTime - ts
        
        if (Math.abs(timeDiff) > 86400) {
            ts = currentTime
        }

        val url = if (fastResponseCount > 2) {
            "$currentServer?key=$currentKey&act=a_check&wait=45&version=3&ts=$ts"
        } else {
            "$currentServer?key=$currentKey&act=a_check&wait=45&version=3&ts=$ts&mode=2"
        }
        
        val request = Request.Builder().url(url).build()
        val requestStartTime = System.currentTimeMillis()
        
        try {
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            val requestDuration = System.currentTimeMillis() - requestStartTime
            
            val body = response.body?.string() ?: throw Exception("Empty body")
            Log.d(TAG, "Check response (duration=${requestDuration}ms): $body")

            if (requestDuration < 1000) {
                fastResponseCount++
                if (fastResponseCount >= 5) {
                    server = null
                    return
                }
            } else {
                fastResponseCount = 0
            }

            if (body.trim() == "[]") {
                if (requestDuration < 1000) {
                    ts = currentTime
                    delay(1000)
                }
                return
            }

            val json = JSONObject(body)
            if (json.has("failed")) {
                val code = json.getInt("failed")
                when (code) {
                    1 -> ts = json.optLong("ts", ts)
                    2, 3 -> server = null
                }
                return
            }

            ts = json.optLong("ts", ts)
            json.optJSONArray("updates")?.let { processUpdates(it) }

        } catch (e: Exception) {
            if (e !is CancellationException) {
                Log.e(TAG, "Check failed", e)
                delay(3000)
                server = null
            }
        }
    }

    private fun processUpdates(updates: org.json.JSONArray) {
        for (i in 0 until updates.length()) {
            try {
                val update = updates.getJSONArray(i)
                val type = update.getInt(0)
                
                when (type) {
                    4 -> processNewMessage(update)
                    5 -> {
                        val msgId = update.getInt(1)
                        val peerId = update.getInt(3)
                        val text = update.getString(5)
                        notify { it.onMessageEdit(msgId, peerId, text) }
                    }
                    6, 7 -> {
                        val peerId = update.getInt(1)
                        val localId = update.getInt(2)
                        notify { it.onMessageRead(peerId, localId) }
                        scope.launch { AppEvents.emitRefreshNotifications() }
                    }
                    8, 9 -> {
                        val userId = Math.abs(update.getInt(1))
                        notify { it.onUserOnline(userId, type == 8) }
                    }
                    61 -> {
                        val peerId = update.getInt(1)
                        notify { it.onUserTyping(peerId, peerId) }
                    }
                    80 -> {
                        notify { it.onNotificationsCountUpdated() }
                        scope.launch { AppEvents.emitRefreshNotifications() }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing update at $i", e)
            }
        }
    }

    private fun processNewMessage(update: org.json.JSONArray) {
        val msgId = update.getInt(1)
        val flags = update.getInt(2)
        val peerId = update.getInt(3)
        val timestamp = update.getLong(4)
        val text = update.getString(5)
        
        if (processedEvents.containsKey(msgId)) return
        processedEvents[msgId] = timestamp
        if (processedEvents.size > MAX_PROCESSED_EVENTS) processedEvents.clear()

        var fromId = peerId
        if (update.length() > 9) {
            fromId = update.optInt(9, peerId)
        } else if (update.length() > 6) {
            val extra = update.optJSONObject(6)
            fromId = extra?.optInt("from", peerId) ?: peerId
        }

        val isOut = (flags and 2) != 0
        notify { it.onNewMessage(msgId, peerId, timestamp, text, fromId, isOut) }
        
        // Flux behavior: refresh notifications on every new message
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
