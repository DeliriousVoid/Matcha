package com.deliriousvoid.openvkmatcha.util

import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.Document

fun downloadDocument(doc: Document) {
    val docUrl = doc.url
    if (!docUrl.isNullOrBlank()) {
        val imageExtensions = listOf("jpg", "jpeg", "png", "webp", "gif")
        val isImage = imageExtensions.contains(doc.ext.lowercase())
        
        val targetExt = if (isImage) "png" else doc.ext
        val safeTitle = doc.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(100)
        
        // Remove old extension from title if it exists to avoid something.jpg.png
        val titleWithoutExt = if (safeTitle.contains(".")) {
            val lastDot = safeTitle.lastIndexOf(".")
            val extInTitle = safeTitle.substring(lastDot + 1).lowercase()
            if (imageExtensions.contains(extInTitle)) {
                safeTitle.substring(0, lastDot)
            } else {
                safeTitle
            }
        } else {
            safeTitle
        }

        val fileName = "$titleWithoutExt.$targetExt"
        
        OpenVKMatchaApp.instance.downloadRepository.downloadFile(
            url = docUrl,
            fileName = fileName,
            fallbackExt = targetExt
        )
    } else {
        android.widget.Toast.makeText(
            OpenVKMatchaApp.instance,
            "Ошибка: прямая ссылка на документ отсутствует",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
