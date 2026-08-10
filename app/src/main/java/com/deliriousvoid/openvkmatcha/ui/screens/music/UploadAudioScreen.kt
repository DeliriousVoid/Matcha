package com.deliriousvoid.openvkmatcha.ui.screens.music

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun UploadAudioScreen() {
    val baseUrl = OpenVKMatchaApp.instance.api.baseUrl
    val uploadUrl = "$baseUrl/player/upload"
    
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        callback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        filePathCallback = callback
                        filePickerLauncher.launch("audio/*")
                        return true
                    }
                }
                
                val headers = mapOf("Accept-Language" to java.util.Locale.getDefault().language)
                loadUrl(uploadUrl, headers)
            }
        },
        update = { webView ->
            // Update logic if needed
        }
    )
}
