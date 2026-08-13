package com.deliriousvoid.openvkmatcha.data.api

import com.deliriousvoid.openvkmatcha.Constants
import com.deliriousvoid.openvkmatcha.data.security.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class OpenVKApi(private val tokenManager: TokenManager) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(Constants.READ_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(Constants.WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()
    private var lastRequestTime = 0L

    val baseUrl: String get() = tokenManager.getInstance()
    val token: String? get() = tokenManager.getToken()

    fun saveInstance(url: String) = tokenManager.saveInstance(url)
    fun logout() = tokenManager.clear()
    fun hasToken(): Boolean = tokenManager.hasToken()

    suspend fun login(username: String, password: String, code: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            enforceRateLimit()
            try {
                val body = FormBody.Builder()
                    .add("grant_type", "password")
                    .add("username", username)
                    .add("password", password)
                    .add("code", code.orEmpty())
                    .add("client_name", tokenManager.getClientName())
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/token")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(ApiException(parseError(bodyText)))
                    }
                    val json = JSONObject(bodyText)
                    if (json.has("access_token")) {
                        val accessToken = json.getString("access_token")
                        tokenManager.saveToken(accessToken)
                        Result.success(accessToken)
                    } else {
                        Result.failure(ApiException(parseError(bodyText)))
                    }
                }
            } catch (e: Exception) {
                Result.failure(ApiException("Ошибка сети: ${e.message}", cause = e))
            }
        }

    suspend fun loginWithToken(token: String): Result<Unit> {
        tokenManager.saveToken(token)
        return validateToken()
    }

    suspend fun validateToken(): Result<Unit> = withContext(Dispatchers.IO) {
        callMethod("users.get", mapOf("fields" to "photo_200")).map { }
    }

    suspend fun resolveScreenName(screenName: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        callMethod("utils.resolveScreenName", mapOf("screen_name" to screenName))
    }

    suspend fun callMethod(
        method: String,
        params: Map<String, String> = emptyMap(),
        isPost: Boolean = false,
    ): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            val accessToken = tokenManager.getToken()
                ?: return@withContext Result.failure(ApiException("Нет access_token"))

            enforceRateLimit()
            try {
                val urlBuilder = StringBuilder("$baseUrl/method/$method")
                
                val request = if (isPost) {
                    val bodyBuilder = FormBody.Builder()
                    bodyBuilder.add("access_token", accessToken)
                    params.forEach { (key, value) ->
                        bodyBuilder.add(key, value)
                    }
                    Request.Builder()
                        .url(urlBuilder.toString())
                        .post(bodyBuilder.build())
                        .build()
                } else {
                    urlBuilder.append("?access_token=${encode(accessToken)}")
                    params.forEach { (key, value) ->
                        urlBuilder.append("&${encode(key)}=${encode(value)}")
                    }
                    Request.Builder()
                        .url(urlBuilder.toString())
                        .get()
                        .build()
                }

                client.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            ApiException("HTTP ${response.code}: $bodyText")
                        )
                    }
                    val json = JSONObject(bodyText)
                    when {
                        json.has("response") -> Result.success(json)
                        json.has("error") -> {
                            val error = json.getJSONObject("error")
                            Result.failure(
                                ApiException(
                                    message = error.optString("error_msg", "Неизвестная ошибка"),
                                    errorCode = error.optInt("error_code", -1).takeIf { it != -1 }
                                )
                            )
                        }
                        else -> Result.success(json) // Some methods return Boolean or Int directly
                    }
                }
            } catch (e: Exception) {
                Result.failure(ApiException("Ошибка: ${e.message}", cause = e))
            }
        }

    suspend fun uploadFile(url: String, bytes: ByteArray, fileName: String, fieldName: String = "photo"): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val body = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart(
                        fieldName,
                        fileName,
                        bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(ApiException("Upload failed: $bodyText"))
                    }
                    Result.success(JSONObject(bodyText))
                }
            } catch (e: Exception) {
                Result.failure(ApiException("Upload error: ${e.message}", cause = e))
            }
        }


    private suspend fun enforceRateLimit() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < Constants.MIN_REQUEST_INTERVAL_MS) {
            delay(Constants.MIN_REQUEST_INTERVAL_MS - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun parseError(body: String): String {
        if (body.isBlank()) return "Пустой ответ сервера"
        return try {
            val json = JSONObject(body)
            when {
                json.has("error_description") -> json.getString("error_description")
                json.has("error_msg") -> json.getString("error_msg")
                json.has("error") -> json.optString("error")
                else -> body
            }
        } catch (_: Exception) {
            body
        }
    }
}

class ApiException(message: String, val errorCode: Int? = null, cause: Throwable? = null) : Exception(message, cause)
