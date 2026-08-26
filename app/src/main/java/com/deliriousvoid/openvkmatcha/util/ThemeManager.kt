package com.deliriousvoid.openvkmatcha.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.deliriousvoid.openvkmatcha.ui.theme.CustomAccent
import com.deliriousvoid.openvkmatcha.ui.theme.CustomColorScheme
import com.deliriousvoid.openvkmatcha.ui.theme.CustomTheme
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ThemeManager(private val context: Context) {
    private val themesDir = File(context.filesDir, "themes").apply { mkdirs() }
    private val accentsDir = File(context.filesDir, "accents").apply { mkdirs() }

    private var _cachedThemes: List<CustomTheme>? = null
    private var _cachedAccents: List<CustomAccent>? = null

    fun getThemes(): List<CustomTheme> {
        _cachedThemes?.let { return it }
        val list = mutableListOf<CustomTheme>()
        themesDir.listFiles { file -> file.isDirectory }?.forEach { dir ->
            val themeFile = File(dir, "theme.json")
            if (themeFile.exists()) {
                try {
                    val json = JSONObject(themeFile.readText())
                    list.add(parseTheme(dir.name, json, dir))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        _cachedThemes = list
        return list
    }

    fun getAccents(): List<CustomAccent> {
        _cachedAccents?.let { return it }
        val list = mutableListOf<CustomAccent>()
        accentsDir.listFiles { file -> file.extension == "mcolor" }?.forEach { file ->
            try {
                val lines = file.readLines()
                if (lines.isNotEmpty()) {
                    val name = lines[0].trim()
                    val author = if (lines.size > 1) lines[1].trim() else "Unknown"
                    val xml = lines.drop(2).joinToString("\n")
                    
                    val primary = xml.substringAfter("<primary>").substringBefore("</primary>").toLong(16)
                    val container = if (xml.contains("<container>")) {
                        xml.substringAfter("<container>").substringBefore("</container>").toLong(16)
                    } else null
                    
                    list.add(CustomAccent(file.nameWithoutExtension, name, author, primary, container))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _cachedAccents = list
        return list
    }

    fun saveTheme(theme: CustomTheme, bitmap: Bitmap? = null) {
        val dir = File(themesDir, theme.id)
        dir.mkdirs()
        
        val json = JSONObject().apply {
            put("name", theme.name)
            put("author", theme.author)
            put("isDark", theme.isDark)
            put("blur", theme.backgroundBlur)
            put("dim", theme.backgroundDim)
            put("alpha", theme.backgroundAlpha)
            
            val colorsJson = JSONObject()
            // Using reflection to iterate over CustomColorScheme properties would be better but for now manual
            // Or just put all fields
            val colors = theme.colors
            colorsJson.put("primary", colors.primary)
            colorsJson.put("onPrimary", colors.onPrimary)
            colorsJson.put("primaryContainer", colors.primaryContainer)
            colorsJson.put("onPrimaryContainer", colors.onPrimaryContainer)
            colorsJson.put("secondary", colors.secondary)
            colorsJson.put("onSecondary", colors.onSecondary)
            colorsJson.put("secondaryContainer", colors.secondaryContainer)
            colorsJson.put("onSecondaryContainer", colors.onSecondaryContainer)
            colorsJson.put("tertiary", colors.tertiary)
            colorsJson.put("onTertiary", colors.onTertiary)
            colorsJson.put("tertiaryContainer", colors.tertiaryContainer)
            colorsJson.put("onTertiaryContainer", colors.onTertiaryContainer)
            colorsJson.put("error", colors.error)
            colorsJson.put("onError", colors.onError)
            colorsJson.put("errorContainer", colors.errorContainer)
            colorsJson.put("onErrorContainer", colors.onErrorContainer)
            colorsJson.put("background", colors.background)
            colorsJson.put("onBackground", colors.onBackground)
            colorsJson.put("surface", colors.surface)
            colorsJson.put("onSurface", colors.onSurface)
            colorsJson.put("surfaceVariant", colors.surfaceVariant)
            colorsJson.put("onSurfaceVariant", colors.onSurfaceVariant)
            colorsJson.put("outline", colors.outline)
            colorsJson.put("outlineVariant", colors.outlineVariant)
            colorsJson.put("scrim", colors.scrim)
            colorsJson.put("inverseSurface", colors.inverseSurface)
            colorsJson.put("inverseOnSurface", colors.inverseOnSurface)
            colorsJson.put("inversePrimary", colors.inversePrimary)
            colorsJson.put("surfaceDim", colors.surfaceDim)
            colorsJson.put("surfaceBright", colors.surfaceBright)
            colorsJson.put("surfaceContainerLowest", colors.surfaceContainerLowest)
            colorsJson.put("surfaceContainerLow", colors.surfaceContainerLow)
            colorsJson.put("surfaceContainer", colors.surfaceContainer)
            colorsJson.put("surfaceContainerHigh", colors.surfaceContainerHigh)
            colorsJson.put("surfaceContainerHighest", colors.surfaceContainerHighest)
            put("colors", colorsJson)
            
            theme.internalAccent?.let { accent ->
                val accentJson = JSONObject()
                accentJson.put("name", accent.name)
                accentJson.put("author", accent.author)
                accentJson.put("primary", accent.primary)
                accentJson.put("container", accent.primaryContainer)
                put("internalAccent", accentJson)
            }
        }
        
        File(dir, "theme.json").writeText(json.toString())
        _cachedThemes = null
        
        bitmap?.let {
            val wallpaperFile = File(dir, "wallpaper.jpg")
            FileOutputStream(wallpaperFile).use { out ->
                it.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        }
    }

    fun saveAccent(accent: CustomAccent) {
        val file = File(accentsDir, "${accent.id}.mcolor")
        val content = StringBuilder()
        content.append(accent.name).append("\n")
        content.append(accent.author).append("\n")
        content.append("<resources>\n")
        content.append("    <primary>${accent.primary.toString(16)}</primary>\n")
        accent.primaryContainer?.let { 
            content.append("    <container>${it.toString(16)}</container>\n")
        }
        content.append("</resources>")
        file.writeText(content.toString())
        _cachedAccents = null
    }

    fun deleteTheme(id: String) {
        val dir = File(themesDir, id)
        if (dir.exists()) dir.deleteRecursively()
        _cachedThemes = null
    }

    fun deleteAccent(id: String) {
        val file = File(accentsDir, "$id.mcolor")
        if (file.exists()) file.delete()
        _cachedAccents = null
    }

    fun exportTheme(id: String): File? {
        val dir = File(themesDir, id)
        if (!dir.exists()) return null
        
        val tempZip = File(context.cacheDir, "${id}.mtheme")
        ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
            dir.listFiles()?.forEach { file ->
                zos.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return tempZip
    }

    fun exportAccent(id: String): File? {
        val file = File(accentsDir, "$id.mcolor")
        return if (file.exists()) file else null
    }

    fun importTheme(uri: Uri): Result<Unit> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return Result.failure(Exception("Failed to open stream"))
            val id = "theme_${System.currentTimeMillis()}"
            val dir = File(themesDir, id)
            dir.mkdirs()
            
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(dir, entry.name)
                    FileOutputStream(file).use { zis.copyTo(it) }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            _cachedThemes = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importAccent(uri: Uri): Result<Unit> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return Result.failure(Exception("Failed to open stream"))
            val content = inputStream.bufferedReader().readText()
            val name = content.lines().firstOrNull()?.trim() ?: "Imported Accent"
            val id = "accent_${System.currentTimeMillis()}"
            val file = File(accentsDir, "$id.mcolor")
            file.writeText(content)
            _cachedAccents = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTheme(id: String, json: JSONObject, dir: File): CustomTheme {
        val colorsJson = json.getJSONObject("colors")
        val colors = CustomColorScheme(
            primary = colorsJson.getLong("primary"),
            onPrimary = colorsJson.getLong("onPrimary"),
            primaryContainer = colorsJson.getLong("primaryContainer"),
            onPrimaryContainer = colorsJson.getLong("onPrimaryContainer"),
            secondary = colorsJson.getLong("secondary"),
            onSecondary = colorsJson.getLong("onSecondary"),
            secondaryContainer = colorsJson.getLong("secondaryContainer"),
            onSecondaryContainer = colorsJson.getLong("onSecondaryContainer"),
            tertiary = colorsJson.getLong("tertiary"),
            onTertiary = colorsJson.getLong("onTertiary"),
            tertiaryContainer = colorsJson.getLong("tertiaryContainer"),
            onTertiaryContainer = colorsJson.getLong("onTertiaryContainer"),
            error = colorsJson.getLong("error"),
            onError = colorsJson.getLong("onError"),
            errorContainer = colorsJson.getLong("errorContainer"),
            onErrorContainer = colorsJson.getLong("onErrorContainer"),
            background = colorsJson.getLong("background"),
            onBackground = colorsJson.getLong("onBackground"),
            surface = colorsJson.getLong("surface"),
            onSurface = colorsJson.getLong("onSurface"),
            surfaceVariant = colorsJson.getLong("surfaceVariant"),
            onSurfaceVariant = colorsJson.getLong("onSurfaceVariant"),
            outline = colorsJson.getLong("outline"),
            outlineVariant = colorsJson.getLong("outlineVariant"),
            scrim = colorsJson.getLong("scrim"),
            inverseSurface = colorsJson.getLong("inverseSurface"),
            inverseOnSurface = colorsJson.getLong("inverseOnSurface"),
            inversePrimary = colorsJson.getLong("inversePrimary"),
            surfaceDim = colorsJson.getLong("surfaceDim"),
            surfaceBright = colorsJson.getLong("surfaceBright"),
            surfaceContainerLowest = colorsJson.getLong("surfaceContainerLowest"),
            surfaceContainerLow = colorsJson.getLong("surfaceContainerLow"),
            surfaceContainer = colorsJson.getLong("surfaceContainer"),
            surfaceContainerHigh = colorsJson.getLong("surfaceContainerHigh"),
            surfaceContainerHighest = colorsJson.getLong("surfaceContainerHighest")
        )
        
        val internalAccent = if (json.has("internalAccent")) {
            val aj = json.getJSONObject("internalAccent")
            CustomAccent(
                id = "internal",
                name = aj.getString("name"),
                author = aj.getString("author"),
                primary = aj.getLong("primary"),
                primaryContainer = if (aj.has("container")) aj.getLong("container") else null
            )
        } else null
        
        val wallpaper = File(dir, "wallpaper.jpg")
        
        return CustomTheme(
            id = id,
            name = json.getString("name"),
            author = json.getString("author"),
            colors = colors,
            internalAccent = internalAccent,
            backgroundImagePath = if (wallpaper.exists()) wallpaper.absolutePath else null,
            backgroundBlur = json.optDouble("blur", 0.0).toFloat(),
            backgroundDim = json.optDouble("dim", 0.0).toFloat(),
            backgroundAlpha = json.optDouble("alpha", 1.0).toFloat(),
            isDark = json.optBoolean("isDark", true)
        )
    }
}
