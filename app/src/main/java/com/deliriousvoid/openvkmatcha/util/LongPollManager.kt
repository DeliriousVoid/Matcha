package com.deliriousvoid.openvkmatcha.util

import android.util.Log
import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.repository.NotificationsRepository
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class LongPollManager(
    private val api: OpenVKApi,
    private val notificationsRepository: NotificationsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false
    private var job: Job? = null
    
    private val client: OkHttpClient = createLongPollClient()

    private var server: String? = null
    private var key: String? = null
    private var ts: Long = 0
    private var isFirstRequest = true

    fun start() {
        if (isRunning) return
        isRunning = true
        isFirstRequest = true
        Log.d("LongPollManager", "Starting LongPoll loop")
        job = scope.launch {
            while (isRunning) {
                try {
                    if (server == null || key == null) {
                        fetchServer()
                        isFirstRequest = true
                    } else {
                        performCheck()
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: java.io.IOException) {
                    Log.w("LongPollManager", "Network error in loop, retrying: ${e.message}")
                    delay(5000)
                } catch (e: Exception) {
                    Log.e("LongPollManager", "Fatal error in loop, resetting server", e)
                    delay(5000)
                    server = null
                }
            }
        }
    }

    fun stop() {
        Log.d("LongPollManager", "Stopping LongPoll loop")
        isRunning = false
        isFirstRequest = true
        job?.cancel()
        job = null
    }

    private suspend fun fetchServer() {
        notificationsRepository.getLongPollServer().onSuccess { json ->
            val response = json.optJSONObject("response") ?: return@onSuccess
            server = response.optString("server")
            if (server != null && !server!!.contains("://")) {
                val scheme = if (api.baseUrl.startsWith("https")) "https://" else "http://"
                server = scheme + server
            }
            key = response.optString("key")
            ts = response.optLong("ts")
            Log.d("LongPollManager", "Server fetched: $server, ts: $ts")
        }.onFailure {
            Log.e("LongPollManager", "Failed to fetch server: ${it.message}")
            delay(10000)
        }
    }

    private suspend fun performCheck() {
        val currentServer = server ?: return
        val currentKey = key ?: return
        
        val waitTime = if (isFirstRequest) 1 else 45
        val url = "$currentServer?act=a_check&key=$currentKey&ts=$ts&wait=$waitTime&mode=66&version=3"
        val request = Request.Builder()
            .url(url)
            .build()
        
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty body")
            val json = JSONObject(body)
            
            if (json.has("failed")) {
                val code = json.getInt("failed")
                Log.w("LongPollManager", "LongPoll failed with code $code")
                when (code) {
                    1 -> ts = json.optLong("ts", ts)
                    2, 3 -> {
                        server = null
                        key = null
                    }
                }
                isFirstRequest = true
                return
            }
            
            ts = json.optLong("ts", ts)
            
            if (isFirstRequest) {
                Log.d("LongPollManager", "Sync request completed, ts: $ts")
                isFirstRequest = false
                return
            }

            val updates = json.optJSONArray("updates")
            if (updates != null && updates.length() > 0) {
                var shouldRefresh = false
                for (i in 0 until updates.length()) {
                    val update = updates.optJSONArray(i) ?: continue
                    val type = update.optInt(0)
                    // 80: Notifications count changed
                    // 4: New message (might contain mentions)
                    if (type == 80 || type == 4) {
                        shouldRefresh = true
                        break
                    }
                }

                if (shouldRefresh) {
                    Log.d("LongPollManager", "Relevant updates found, triggering refresh")
                    AppEvents.emitRefreshNotifications()
                }
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                if (e !is java.io.IOException) {
                    Log.e("LongPollManager", "Check failed with unexpected error", e)
                }
                throw e
            }
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
