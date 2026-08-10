package com.deliriousvoid.openvkmatcha.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.deliriousvoid.openvkmatcha.data.model.Document
import com.deliriousvoid.openvkmatcha.data.model.Video
import com.deliriousvoid.openvkmatcha.ui.util.formatDuration
import com.deliriousvoid.openvkmatcha.ui.util.formatFileSize
import com.deliriousvoid.openvkmatcha.ui.viewmodel.DocsPickerViewModel
import com.deliriousvoid.openvkmatcha.ui.viewmodel.VideoPickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPickerBottomSheet(
    onDismiss: () -> Unit,
    onVideoSelect: (Video) -> Unit,
    viewModel: VideoPickerViewModel = viewModel(factory = VideoPickerViewModel.factory())
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showUpload by remember { mutableStateOf(false) }
    
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex > totalItems - 5 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.8f).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Выбор видео",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = { showUpload = true }) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Загрузить")
                }
            }
            
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                placeholder = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading && state.videos.isEmpty()) {
                    LoadingBox(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.videos) { video ->
                            VideoPickerItem(
                                video = video,
                                onClick = { onVideoSelect(video) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 120.dp))
                        }
                        if (state.isLoadingMore) {
                            item { LoadingBox(modifier = Modifier.padding(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showUpload) {
        WebViewBottomSheet(
            url = "https://openvk.org/videos/upload",
            onDismiss = { 
                showUpload = false 
                viewModel.loadVideos(isRefresh = true)
            }
        )
    }
}

@Composable
fun VideoPickerItem(
    video: Video,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(100.dp, 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(video.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsPickerBottomSheet(
    onDismiss: () -> Unit,
    onDocSelect: (Document) -> Unit,
    viewModel: DocsPickerViewModel = viewModel(factory = DocsPickerViewModel.factory())
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showUpload by remember { mutableStateOf(false) }
    
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex > totalItems - 5 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.8f).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Выбор документа",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = { showUpload = true }) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Загрузить")
                }
            }
            
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                placeholder = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading && state.docs.isEmpty()) {
                    LoadingBox(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.docs) { doc ->
                            DocumentItem(
                                document = doc,
                                onClick = { onDocSelect(doc) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (state.isLoadingMore) {
                            item { LoadingBox(modifier = Modifier.padding(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showUpload) {
        WebViewBottomSheet(
            url = "https://openvk.org/docs/upload",
            onDismiss = { 
                showUpload = false 
                viewModel.loadDocs(isRefresh = true)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewBottomSheet(
    url: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
    }
}
