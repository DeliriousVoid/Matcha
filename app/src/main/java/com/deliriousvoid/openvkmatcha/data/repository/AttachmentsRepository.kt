package com.deliriousvoid.openvkmatcha.data.repository

import android.content.Context
import android.net.Uri
import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.model.AttachmentType
import com.deliriousvoid.openvkmatcha.data.model.PendingAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AttachmentsRepository(private val context: Context, private val api: OpenVKApi) {

    suspend fun uploadAttachment(pending: PendingAttachment): Result<String> = withContext(Dispatchers.IO) {
        if (pending.isExisting && pending.attachmentString != null) {
            return@withContext Result.success(pending.attachmentString)
        }

        val uri = pending.uri ?: return@withContext Result.failure(Exception("URI is null for non-existing attachment"))

        try {
            val bytes = readBytes(uri) ?: return@withContext Result.failure(Exception("Failed to read file"))
            val fileName = pending.name.ifEmpty { "file_${System.currentTimeMillis()}" }

            when (pending.type) {
                AttachmentType.PHOTO, AttachmentType.GRAFFITI -> uploadPhoto(bytes, fileName)
                AttachmentType.DOCUMENT -> uploadDocument(bytes, fileName)
                AttachmentType.AUDIO -> uploadAudio(bytes, fileName)
                AttachmentType.VIDEO -> uploadVideo(bytes, fileName)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadPhoto(bytes: ByteArray, fileName: String): Result<String> {
        android.util.Log.d("Attachments", "Getting wall upload server...")
        return api.callMethod("photos.getWallUploadServer").fold(
            onSuccess = { json ->
                val uploadUrl = json.getJSONObject("response").getString("upload_url")
                android.util.Log.d("Attachments", "Uploading to: $uploadUrl")
                api.uploadFile(uploadUrl, bytes, fileName, "photo").fold(
                    onSuccess = { uploadResult ->
                        val server = uploadResult.optString("server")
                        val photo = uploadResult.optString("photo")
                        val hash = uploadResult.optString("hash")
                        android.util.Log.d("Attachments", "Saving wall photo: server=$server, hash=$hash")
                        api.callMethod("photos.saveWallPhoto", mapOf(
                            "server" to server,
                            "photo" to photo,
                            "hash" to hash
                        )).map { saveResult ->
                            val photoObj = saveResult.getJSONArray("response").getJSONObject(0)
                            val result = "photo${photoObj.getInt("owner_id")}_${photoObj.getInt("id")}"
                            android.util.Log.d("Attachments", "Photo saved: $result")
                            result
                        }
                    },
                    onFailure = { 
                        android.util.Log.e("Attachments", "Upload failed: ${it.message}")
                        Result.failure(it) 
                    }
                )
            },
            onFailure = { 
                android.util.Log.e("Attachments", "Failed to get upload server: ${it.message}")
                Result.failure(it) 
            }
        )
    }

    private suspend fun uploadDocument(bytes: ByteArray, fileName: String): Result<String> {
        return api.callMethod("docs.getUploadServer").fold(
            onSuccess = { json ->
                val uploadUrl = json.getJSONObject("response").getString("upload_url")
                api.uploadFile(uploadUrl, bytes, fileName, "file").fold(
                    onSuccess = { uploadResult ->
                        val file = uploadResult.optString("file")
                        api.callMethod("docs.save", mapOf("file" to file)).map { saveResult ->
                            val response = saveResult.optJSONObject("response")
                            val docObj = if (response?.has("doc") == true) {
                                response.getJSONObject("doc")
                            } else if (saveResult.optJSONArray("response") != null) {
                                saveResult.getJSONArray("response").getJSONObject(0)
                            } else {
                                response ?: saveResult
                            }
                            
                            val ownerId = docObj.optInt("owner_id")
                            val id = docObj.optInt("id")
                            val result = "document${ownerId}_$id"
                            android.util.Log.d("Attachments", "Document saved: $result")
                            result
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun uploadAudio(bytes: ByteArray, fileName: String): Result<String> {
        return api.callMethod("audio.getUploadServer").fold(
            onSuccess = { json ->
                val uploadUrl = json.getJSONObject("response").getString("upload_url")
                api.uploadFile(uploadUrl, bytes, fileName, "file").fold(
                    onSuccess = { uploadResult ->
                        val server = uploadResult.optString("server")
                        val audio = uploadResult.optString("audio")
                        val hash = uploadResult.optString("hash")
                        api.callMethod("audio.save", mapOf(
                            "server" to server,
                            "audio" to audio,
                            "hash" to hash
                        )).map { saveResult ->
                            val response = saveResult.optJSONObject("response")
                            val audioObj = if (saveResult.optJSONArray("response") != null) {
                                saveResult.getJSONArray("response").getJSONObject(0)
                            } else {
                                response ?: saveResult
                            }
                            
                            val ownerId = audioObj.optInt("owner_id")
                            val id = audioObj.optInt("id")
                            "audio${ownerId}_$id"
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun uploadVideo(bytes: ByteArray, fileName: String): Result<String> {
        // Video upload usually requires first calling video.save to get upload_url
        return api.callMethod("video.save", mapOf(
            "name" to fileName,
            "description" to "Uploaded via Matcha"
        )).fold(
            onSuccess = { json ->
                val response = json.getJSONObject("response")
                val uploadUrl = response.getString("upload_url")
                val videoId = response.getInt("video_id")
                val ownerId = response.getInt("owner_id")
                
                api.uploadFile(uploadUrl, bytes, fileName, "video_file").fold(
                    onSuccess = { Result.success("video${ownerId}_$videoId") },
                    onFailure = { Result.failure(it) }
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun readBytes(uri: Uri): ByteArray? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val byteBuffer = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            byteBuffer.toByteArray()
        }
    }
}
