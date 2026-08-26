package com.deliriousvoid.openvkmatcha.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.deliriousvoid.openvkmatcha.R
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.launch

data class LanguageInfo(
    val id: String, // filename or "builtin_ru"
    val name: String,
    val author: String,
    val isBuiltin: Boolean = false
)

class LanguageManager(private val context: Context) {
    private val languagesDir = File(context.filesDir, "languages").apply { mkdirs() }
    
    private var _currentLanguageId = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        .getString("language_id", "builtin_ru") ?: "builtin_ru"
    
    val currentLanguageId: String get() = _currentLanguageId
        
    private val customStrings = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val resourceIdToKey = java.util.concurrent.ConcurrentHashMap<Int, String>()
    private var _cachedAvailableLanguages: List<LanguageInfo>? = null
    
    init {
        if (_currentLanguageId == "builtin_ru") {
            loadLanguage("builtin_ru")
        } else {
            // Load custom language in background to avoid blocking startup
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                loadLanguage(_currentLanguageId)
            }
        }
    }

    fun getAvailableLanguages(): List<LanguageInfo> {
        _cachedAvailableLanguages?.let { return it }
        val list = mutableListOf(LanguageInfo("builtin_ru", context.getString(R.string.builtin_russian), "OpenVK Matcha", true))
        languagesDir.listFiles { file -> file.extension == "mlang" }?.forEach { file ->
            try {
                file.inputStream().use { input ->
                    val reader = input.bufferedReader()
                    val name = reader.readLine()?.trim() ?: ""
                    val author = reader.readLine()?.trim() ?: context.getString(R.string.author_unknown)
                    if (name.isNotEmpty()) {
                        list.add(LanguageInfo(file.nameWithoutExtension, name, author))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _cachedAvailableLanguages = list
        return list
    }

    fun loadLanguage(id: String) {
        _currentLanguageId = id
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putString("language_id", id).apply()
        
        customStrings.clear()
        if (id == "builtin_ru") return

        val file = File(languagesDir, "$id.mlang")
        if (!file.exists()) return

        try {
            file.inputStream().use { inputStream ->
                val reader = inputStream.bufferedReader()
                reader.readLine() // Skip Name
                reader.readLine() // Skip Author
                
                // Read the rest as XML
                val xml = reader.readText()
                parseXml(xml.byteInputStream())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseXml(inputStream: InputStream) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "string") {
                val key = parser.getAttributeValue(null, "name")
                val value = try {
                    parser.nextText()
                } catch (e: Exception) {
                    ""
                }
                if (key != null) {
                    customStrings[key] = value
                }
            }
            eventType = parser.next()
        }
    }

    fun getString(resourceId: Int): String? {
        return try {
            val key = resourceIdToKey.getOrPut(resourceId) {
                context.resources.getResourceEntryName(resourceId)
            }
            customStrings[key]
        } catch (e: Exception) {
            null
        }
    }

    fun getStringOrDefault(resourceId: Int): String {
        return getString(resourceId) ?: context.getString(resourceId)
    }

    fun getStringOrDefault(resourceId: Int, vararg formatArgs: Any): String {
        val custom = getString(resourceId)
        return if (custom != null) {
            try {
                String.format(custom, *formatArgs)
            } catch (e: Exception) {
                context.getString(resourceId, *formatArgs)
            }
        } else {
            context.getString(resourceId, *formatArgs)
        }
    }

    fun importLanguage(uri: Uri): Result<Unit> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return Result.failure(Exception(context.getString(R.string.error_file_open_failed)))
            val content = inputStream.bufferedReader().use { it.readText() }
            val lines = content.lines()
            if (lines.size < 3) return Result.failure(Exception(context.getString(R.string.error_invalid_mlang_format)))
            
            val name = lines[0].trim()
            if (name.isEmpty()) return Result.failure(Exception(context.getString(R.string.error_no_language_name)))
            
            // Basic validation of XML part
            if (!content.contains("<resources>") || !content.contains("</resources>")) {
                return Result.failure(Exception(context.getString(R.string.error_invalid_xml)))
            }

            val fileName = "${name.filter { it.isLetterOrDigit() }}_${System.currentTimeMillis()}.mlang"
            val file = File(languagesDir, fileName)
            file.writeText(content)
            _cachedAvailableLanguages = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteLanguage(id: String) {
        if (id == "builtin_ru") return
        val file = File(languagesDir, "$id.mlang")
        if (file.exists()) {
            file.delete()
        }
        _cachedAvailableLanguages = null
        if (_currentLanguageId == id) {
            loadLanguage("builtin_ru")
        }
    }
    
    fun getLanguageFile(id: String): File? {
        if (id == "builtin_ru") return generateBuiltinLanguageFile()
        val file = File(languagesDir, "$id.mlang")
        return if (file.exists()) file else null
    }

    private fun generateBuiltinLanguageFile(): File? {
        val tempFile = File(context.cacheDir, "Russian_template.mlang")
        try {
            val sb = StringBuilder()
            sb.append(context.getString(R.string.builtin_russian)).append("\n")
            sb.append("OpenVK Matcha\n")
            sb.append("<resources>\n")
            
            val stringClass = com.deliriousvoid.openvkmatcha.R.string::class.java
            val fields = stringClass.fields
            for (field in fields) {
                try {
                    val id = field.getInt(null)
                    val key = field.name
                    // Skip internal month fields that might be renamed or missing in R8
                    if (key.startsWith("month_") && key.substring(6).toIntOrNull() == null) continue
                    
                    val value = context.getString(id)
                    val escapedValue = value.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    sb.append("    <string name=\"$key\">$escapedValue</string>\n")
                } catch (e: Exception) {
                    // Skip fields that can't be accessed (e.g. R8 renamed ones)
                }
            }
            sb.append("</resources>")
            tempFile.writeText(sb.toString())
            return tempFile
        } catch (e: Exception) {
            return null
        }
    }
}
