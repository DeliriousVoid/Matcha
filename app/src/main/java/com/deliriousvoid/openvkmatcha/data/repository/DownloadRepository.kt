package com.deliriousvoid.openvkmatcha.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DownloadRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "OpenVK").apply { if (!exists()) mkdirs() }
    private val manifestFile = File(context.getExternalFilesDir(null), "downloaded.json")

    private val _downloadedTracksFlow = MutableStateFlow<Set<String>>(emptySet())
    val downloadedTracksFlow = _downloadedTracksFlow.asStateFlow()

    private var metadataCache = mapOf<String, AudioTrack>()

    init {
        updateDownloadedFlow()
    }

    fun updateDownloadedFlow() {
        if (hasStoragePermission()) {
            metadataCache = getDownloadedTracksFromFiles().associateBy { it.stableId }
            _downloadedTracksFlow.value = metadataCache.keys
        } else {
            metadataCache = emptyMap()
            _downloadedTracksFlow.value = emptySet()
        }
    }

    fun hasStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun enrichTrack(track: AudioTrack): AudioTrack {
        val localMatch = metadataCache[track.stableId] ?: metadataCache.values.find {
            it.artist.equals(track.artist, ignoreCase = true) && 
            it.title.equals(track.title, ignoreCase = true)
        }

        return localMatch?.let {
            track.copy(
                artist = it.artist,
                title = it.title,
                duration = if (it.duration > 0) it.duration else track.duration,
                url = it.url,
                artworkUrl = track.artworkUrl ?: it.artworkUrl,
                isAdded = true
            )
        } ?: track
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    suspend fun downloadTrack(track: AudioTrack): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = track.url ?: return@withContext Result.failure(Exception("URL трека отсутствует"))
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Ошибка загрузки: ${response.code}"))
            
            val fileName = sanitizeFileName("${track.artist} - ${track.title}.mp3")
            val file = File(musicDir, fileName)
            response.body?.byteStream()?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            writeMetadata(file, track)

            saveToManifest(track.copy(url = file.absolutePath))
            updateDownloadedFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeMetadata(file: File, track: AudioTrack) {
        try {
            val mp3File = Mp3File(file.absolutePath)
            val id3v2Tag = if (mp3File.hasId3v2Tag()) mp3File.id3v2Tag else ID3v24Tag()
            
            id3v2Tag.artist = track.artist
            id3v2Tag.title = track.title
            id3v2Tag.comment = "openvk_id:${track.ownerId}_${track.id}"

            track.artworkUrl?.let { url ->
                try {
                    val artworkRequest = Request.Builder().url(url).build()
                    client.newCall(artworkRequest).execute().use { artworkResponse ->
                        if (artworkResponse.isSuccessful) {
                            val imageData = artworkResponse.body?.bytes()
                            if (imageData != null) {
                                val mimeType = if (url.lowercase().endsWith(".png")) "image/png" else "image/jpeg"
                                id3v2Tag.setAlbumImage(imageData, mimeType)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DownloadRepository", "Error downloading artwork: ${e.message}")
                }
            }

            mp3File.id3v2Tag = id3v2Tag
            val tempFile = File(file.parent, "${file.name}.tmp")
            mp3File.save(tempFile.absolutePath)
            if (tempFile.exists()) {
                file.delete()
                tempFile.renameTo(file)
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "Error writing metadata: ${e.message}")
        }
    }

    private fun saveToManifest(track: AudioTrack) {
        val tracks = getDownloadedTracksInternal().toMutableList()
        if (tracks.none { it.id == track.id }) {
            tracks.add(track)
            val array = JSONArray()
            tracks.forEach { t ->
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("ownerId", t.ownerId)
                    put("artist", t.artist)
                    put("title", t.title)
                    put("duration", t.duration)
                    put("url", t.url)
                    put("remoteUrl", t.remoteUrl)
                    put("artworkUrl", t.artworkUrl)
                }
                array.put(obj)
            }
            manifestFile.writeText(array.toString())
        }
    }

    fun getDownloadedTracks(): List<AudioTrack> = getDownloadedTracksFromFiles()

    fun getDownloadedTracksFromFiles(): List<AudioTrack> {
        val fileSystemTracks = getDownloadedTracksFromFileSystem()
        val mediaStoreTracks = getDownloadedTracksFromMediaStore()
        return (fileSystemTracks + mediaStoreTracks).distinctBy { it.url }
    }

    private fun getDownloadedTracksFromFileSystem(): List<AudioTrack> = try {
        val files = musicDir.listFiles { file -> file.extension.lowercase() == "mp3" } ?: emptyArray()
        files.mapNotNull { file ->
            try {
                val mp3File = Mp3File(file.absolutePath)
                val id3v2 = if (mp3File.hasId3v2Tag()) mp3File.id3v2Tag else null
                val id3v1 = if (mp3File.hasId3v1Tag()) mp3File.id3v1Tag else null
                
                val artist = id3v2?.artist ?: id3v1?.artist ?: "Неизвестный исполнитель"
                val title = id3v2?.title ?: id3v1?.title ?: file.nameWithoutExtension
                
                val comment = id3v2?.comment ?: ""
                var ownerId = 0
                var id = 0
                
                if (comment.startsWith("openvk_id:")) {
                    val parts = comment.substringAfter("openvk_id:").split("_")
                    if (parts.size >= 2) {
                        ownerId = parts[0].toIntOrNull() ?: 0
                        id = parts[1].toIntOrNull() ?: 0
                    }
                }
                
                if (id == 0) {
                    val parts = file.nameWithoutExtension.split("_")
                    if (parts.size >= 2) {
                        ownerId = parts[0].toIntOrNull() ?: 0
                        id = parts[1].toIntOrNull() ?: 0
                    }
                }
                
                if (id == 0) {
                    val synthetic = (artist.trim() + title.trim()).lowercase().hashCode()
                    id = synthetic
                    ownerId = -1
                }
                
                AudioTrack(
                    id = id,
                    ownerId = ownerId,
                    artist = artist.trim(),
                    title = title.trim(),
                    duration = mp3File.lengthInSeconds.toInt(),
                    url = file.absolutePath,
                    isAdded = true
                )
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun getDownloadedTracksFromMediaStore(): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()
        try {
            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media.ARTIST,
                android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.DURATION,
                android.provider.MediaStore.Audio.Media.DATA
            )
            val selection = "${android.provider.MediaStore.Audio.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("%/Music/OpenVK/%")
            
            context.contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val artistIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                val titleIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                val durationIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                val dataIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataIndex)
                    if (!path.lowercase().endsWith(".mp3")) continue
                    
                    val artist = cursor.getString(artistIndex) ?: "Неизвестный исполнитель"
                    val title = cursor.getString(titleIndex) ?: File(path).nameWithoutExtension
                    val duration = cursor.getInt(durationIndex) / 1000
                    val synthetic = (artist.trim() + title.trim()).lowercase().hashCode()
                    
                    tracks.add(AudioTrack(
                        id = synthetic,
                        ownerId = -1,
                        artist = artist.trim(),
                        title = title.trim(),
                        duration = duration,
                        url = path,
                        isAdded = true
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "MediaStore error: ${e.message}")
        }
        return tracks
    }

    fun refresh() {
        updateDownloadedFlow()
    }

    private fun getDownloadedTracksInternal(): List<AudioTrack> {
        if (!manifestFile.exists()) return emptyList()
        return try {
            val content = manifestFile.readText()
            val array = JSONArray(content)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                AudioTrack(
                    id = obj.getInt("id"),
                    ownerId = obj.getInt("ownerId"),
                    artist = obj.getString("artist"),
                    title = obj.getString("title"),
                    duration = obj.getInt("duration"),
                    url = obj.getString("url"),
                    remoteUrl = obj.optString("remoteUrl").takeIf { it.isNotBlank() },
                    artworkUrl = obj.optString("artworkUrl").takeIf { it.isNotBlank() },
                    isAdded = true
                )
            }.filter { File(it.url!!).exists() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isDownloaded(trackId: Int, ownerId: Int): Boolean {
        return _downloadedTracksFlow.value.contains("${ownerId}_$trackId")
    }

    fun deleteTrack(trackId: Int, ownerId: Int) {
        val tracks = getDownloadedTracksInternal().toMutableList()
        val track = tracks.find { it.id == trackId && it.ownerId == ownerId }
        if (track != null) {
            val file = File(track.url!!)
            if (file.exists()) file.delete()
            tracks.remove(track)
            val array = JSONArray()
            tracks.forEach { t ->
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("ownerId", t.ownerId)
                    put("artist", t.artist)
                    put("title", t.title)
                    put("duration", t.duration)
                    put("url", t.url)
                    put("remoteUrl", t.remoteUrl)
                    put("artworkUrl", t.artworkUrl)
                }
                array.put(obj)
            }
            manifestFile.writeText(array.toString())
            updateDownloadedFlow()
        }
    }

    fun downloadFile(url: String?, fileName: String, fallbackExt: String = "bin") {
        if (url.isNullOrBlank()) return
        var cleanUrl = url.trim()
        if (cleanUrl.startsWith("//")) cleanUrl = "https:$cleanUrl"
        if (!cleanUrl.startsWith("http")) return
        val finalFileName = if (fileName.contains(".")) fileName else "$fileName.$fallbackExt"
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadDir, finalFileName)
        if (file.exists() && file.length() > 0) {
            openFile(file)
            return
        }
        try {
            val downloadUri = Uri.parse(cleanUrl)
            val request = DownloadManager.Request(downloadUri)
                .setTitle(finalFileName)
                .setDescription("Загрузка из OpenVK")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, finalFileName)
                .addRequestHeader("User-Agent", "Mozilla/5.0")
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        } catch (e: Exception) {}
    }

    private fun openFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "com.deliriousvoid.openvkmatcha.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {}
    }
}
