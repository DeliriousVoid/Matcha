package com.deliriousvoid.openvkmatcha.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes

object LinkHandler {
    
    private fun isMirroredHost(host: String): Boolean {
        return host.contains("openvk.org") || 
               host.contains("openvk.uk") || 
               host.contains("openvk.xyz") || 
               host.contains("openvk.su") ||
               host.contains("ovk.to")
    }

    fun getRouteForUrl(url: String): String? {
        val normalizedUrl = if (!url.startsWith("http")) "https://$url" else url
        val uri = Uri.parse(normalizedUrl)
        val host = uri.host ?: ""
        val path = uri.path?.trim('/') ?: ""

        if (!isMirroredHost(host) && !isMirroredHost(url)) {
            return null
        }

        return when {
            path.startsWith("id") && path.substring(2).all { it.isDigit() } -> {
                val id = if (path == "id0") "0" else path.substring(2)
                Routes.profileRoute(id)
            }
            path == "id0" -> Routes.profileRoute("0")
            path.startsWith("event") -> {
                val idStr = path.removePrefix("event").trimStart('/')
                if (idStr.all { it.isDigit() } && idStr.isNotEmpty()) {
                    Routes.profileRoute("-$idStr")
                } else null
            }
            path.startsWith("playlist") -> {
                val idStr = path.removePrefix("playlist").trimStart('/')
                val parts = idStr.split("_")
                if (parts.size == 2) {
                    val ownerId = parts[0].toIntOrNull()
                    val playlistId = parts[1].toIntOrNull()
                    if (ownerId != null && playlistId != null) {
                        Routes.playlistRoute(ownerId, playlistId, "")
                    } else null
                } else null
            }
            (path.startsWith("club") || path.startsWith("public")) -> {
                val prefix = if (path.startsWith("club")) 4 else 6
                val idStr = path.substring(prefix)
                if (idStr.all { it.isDigit() }) {
                    Routes.profileRoute("-$idStr")
                } else {
                    Routes.profileRoute(path) // screen name
                }
            }
            path.startsWith("wall") -> {
                val parts = path.substring(4).split("_")
                if (parts.size == 2) {
                    val ownerId = parts[0].toIntOrNull()
                    val postId = parts[1].toIntOrNull()
                    if (ownerId != null && postId != null) {
                        Routes.commentsRoute(ownerId, postId)
                    } else null
                } else null
            }
            path.startsWith("audios") && path.substring(6).all { it.isDigit() || (it == '-' && path.length > 7) } -> {
                val id = path.substring(6).toIntOrNull() ?: 0
                Routes.userMusicRoute(id, "")
            }
            path == "im" -> {
                val sel = uri.getQueryParameter("sel")
                if (sel != null && sel.all { it.isDigit() || (it == '-' && sel.length > 1) }) {
                    val id = sel.toIntOrNull() ?: 0
                    Routes.chatRoute(id, "")
                } else null
            }
            path.isNotBlank() && !path.contains("/") && !listOf("token", "method", "api", "docs", "apps").contains(path) -> {
                Routes.profileRoute(path)
            }
            else -> null
        }
    }

    fun handleLink(
        context: Context,
        url: String,
        onProfileClick: (Any) -> Unit,
        onWallClick: (Int, Int) -> Unit,
        onMusicClick: (Int) -> Unit,
        onPlaylistClick: (Int, Int) -> Unit = { _, _ -> },
        onChatClick: (Int) -> Unit = {},
        onExternalClick: (String) -> Unit = {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    ) {
        val normalizedUrl = if (!url.startsWith("http")) "https://$url" else url
        val uri = Uri.parse(normalizedUrl)
        val host = uri.host ?: ""
        val path = uri.path?.trim('/') ?: ""

        if (!isMirroredHost(host) && !isMirroredHost(url)) {
            onExternalClick(normalizedUrl)
            return
        }

        when {
            path.startsWith("id") && path.substring(2).all { it.isDigit() } -> {
                val id = path.substring(2).toIntOrNull()
                if (id != null) onProfileClick(id) else onExternalClick(normalizedUrl)
            }
            path == "id0" -> onProfileClick(0)
            path.startsWith("event") -> {
                val idStr = path.removePrefix("event").trimStart('/')
                val id = idStr.toIntOrNull()
                if (id != null) onProfileClick(-id) else onExternalClick(normalizedUrl)
            }
            path.startsWith("playlist") -> {
                val idStr = path.removePrefix("playlist").trimStart('/')
                val parts = idStr.split("_")
                if (parts.size == 2) {
                    val ownerId = parts[0].toIntOrNull()
                    val playlistId = parts[1].toIntOrNull()
                    if (ownerId != null && playlistId != null) {
                        onPlaylistClick(ownerId, playlistId)
                    } else {
                        onExternalClick(normalizedUrl)
                    }
                } else {
                    onExternalClick(normalizedUrl)
                }
            }
            (path.startsWith("club") || path.startsWith("public")) -> {
                val prefix = if (path.startsWith("club")) 4 else 6
                val idStr = path.substring(prefix)
                if (idStr.all { it.isDigit() }) {
                    val id = idStr.toIntOrNull()
                    if (id != null) onProfileClick(-id) else onExternalClick(normalizedUrl)
                } else {
                    onProfileClick(path) // screen name
                }
            }
            path.startsWith("wall") -> {
                val parts = path.substring(4).split("_")
                if (parts.size == 2) {
                    val ownerId = parts[0].toIntOrNull()
                    val postId = parts[1].toIntOrNull()
                    if (ownerId != null && postId != null) {
                        onWallClick(ownerId, postId)
                    } else {
                        onExternalClick(normalizedUrl)
                    }
                } else {
                    onExternalClick(normalizedUrl)
                }
            }
            path.startsWith("audios") && path.substring(6).all { it.isDigit() || (it == '-' && path.length > 7) } -> {
                val id = path.substring(6).toIntOrNull()
                if (id != null) onMusicClick(id) else onExternalClick(normalizedUrl)
            }
            path == "im" -> {
                val sel = uri.getQueryParameter("sel")
                if (sel != null && sel.all { it.isDigit() || (it == '-' && sel.length > 1) }) {
                    val id = sel.toIntOrNull() ?: 0
                    onChatClick(id)
                } else {
                    onExternalClick(normalizedUrl)
                }
            }
            path.isNotBlank() && !path.contains("/") && !listOf("token", "method", "api", "docs", "apps").contains(path) -> {
                onProfileClick(path)
            }
            else -> {
                onExternalClick(normalizedUrl)
            }
        }
    }

    fun handleGeo(context: Context, geo: com.deliriousvoid.openvkmatcha.data.model.Geo) {
        var lat = 0.0
        var lon = 0.0
        var found = false
        val title: String? = geo.place?.title

        // 1. Try to parse from coordinates string (most reliable)
        if (!geo.coordinates.isNullOrBlank()) {
            val parts = geo.coordinates.split(Regex("[, ]+"))
            if (parts.size >= 2) {
                val pLat = parts[0].toDoubleOrNull()
                val pLon = parts[1].toDoubleOrNull()
                if (pLat != null && pLon != null) {
                    lat = pLat
                    lon = pLon
                    found = true
                }
            }
        }

        // 2. Fallback to place coordinates if string parsing failed or was 0,0
        if (!found || (lat == 0.0 && lon == 0.0)) {
            if (geo.place != null && (geo.place.latitude != 0.0 || geo.place.longitude != 0.0)) {
                lat = geo.place.latitude
                lon = geo.place.longitude
                found = true
            }
        }

        if (!found) return

        // Robust URI format: geo:0,0?q=lat,lon(label)
        val label = if (!title.isNullOrBlank()) "($title)" else ""
        val uriString = "geo:0,0?q=$lat,$lon$label"
        val uri = Uri.parse(uriString)
        
        android.util.Log.d("LinkHandler", "Opening geo URI: $uriString")
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("LinkHandler", "Failed to open geo intent: ${e.message}")
            // Fallback to browser/google maps URL
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }
    }
}
