package com.deliriousvoid.openvkmatcha.util

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException

class NewPipeDownloader(private val client: OkHttpClient) : Downloader() {

    @Throws(IOException::class)
    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val body = if (dataToSend != null && dataToSend.isNotEmpty()) {
            RequestBody.create(null, dataToSend)
        } else if (httpMethod == "POST") {
            RequestBody.create(null, ByteArray(0))
        } else {
            null
        }

        val builder = Request.Builder()
            .url(url)
            .method(httpMethod, body)

        headers.forEach { (name, values) ->
            values.forEach { value ->
                builder.addHeader(name, value)
            }
        }

        val okRequest = builder.build()
        client.newCall(okRequest).execute().use { response ->
            val responseBody = response.body?.string()
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBody,
                response.request.url.toString()
            )
        }
    }
}
