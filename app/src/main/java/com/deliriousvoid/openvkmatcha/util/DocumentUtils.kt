package com.deliriousvoid.openvkmatcha.util

import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.Document

fun downloadDocument(doc: Document) {
    val docUrl = doc.url
    if (!docUrl.isNullOrBlank()) {
        val safeTitle = doc.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(100)
        val fileName = if (safeTitle.endsWith("." + doc.ext, ignoreCase = true)) {
            safeTitle
        } else {
            "$safeTitle.${doc.ext}"
        }
        
        OpenVKMatchaApp.instance.downloadRepository.downloadFile(
            url = docUrl,
            fileName = fileName,
            fallbackExt = doc.ext
        )
    } else {
        android.widget.Toast.makeText(
            OpenVKMatchaApp.instance,
            "Ошибка: прямая ссылка на документ отсутствует",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
