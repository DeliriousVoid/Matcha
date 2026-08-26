package com.deliriousvoid.openvkmatcha.ui.screens.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.Document
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.ui.viewmodel.DocsViewModel

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: DocsViewModel,
    onBack: () -> Unit,
    onOpenWebView: (String, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()

    val title = matchaStringResource(R.string.title_documents)
    val loadingLabel = matchaStringResource(R.string.loading_document)
    val uploadLabel = matchaStringResource(R.string.action_upload)
    
    DisposableEffect(Unit) {
        AppEvents.setTopBarState(TopBarState(tag = "documents", title = title, route = Routes.DOCUMENTS))
        onDispose {
            if (AppEvents.topBarState.value?.tag == "documents") {
                AppEvents.setTopBarState(null)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (state.error != null) {
            Text(state.error!!, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.docs) { doc ->
                    DocumentItem(doc)
                }
            }
        }

        FloatingActionButton(
            onClick = {
                val baseUrl = OpenVKMatchaApp.instance.api.baseUrl
                onOpenWebView("$baseUrl/docs?act=add", loadingLabel)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Upload, contentDescription = uploadLabel)
        }
    }
}

@Composable
fun DocumentItem(doc: Document) {
    val downloadLabel = matchaStringResource(R.string.action_download)
    ListItem(
        headlineContent = {
            Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text("${formatSize(doc.size)} • ${doc.ext.uppercase()}")
        },
        leadingContent = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = {
                OpenVKMatchaApp.instance.downloadRepository.downloadFile(doc.url, doc.title, doc.ext)
            }) {
                Icon(Icons.Default.Download, contentDescription = downloadLabel)
            }
        },
        modifier = Modifier.clickable {
             OpenVKMatchaApp.instance.downloadRepository.downloadFile(doc.url, doc.title, doc.ext)
        }
    )
}

@Composable
private fun formatSize(size: Int): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> matchaStringResource(R.string.size_mb, mb)
        kb >= 1.0 -> matchaStringResource(R.string.size_kb, kb)
        else -> matchaStringResource(R.string.size_b, size)
    }
}
