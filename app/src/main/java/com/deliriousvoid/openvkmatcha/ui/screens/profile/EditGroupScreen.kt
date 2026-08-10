package com.deliriousvoid.openvkmatcha.ui.screens.profile

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.ui.components.ErrorText
import com.deliriousvoid.openvkmatcha.ui.components.LoadingBox
import com.deliriousvoid.openvkmatcha.ui.viewmodel.EditGroupTab
import com.deliriousvoid.openvkmatcha.ui.viewmodel.EditGroupViewModel

import androidx.compose.runtime.DisposableEffect
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupScreen(
    groupId: Int,
    onBack: () -> Unit,
    viewModel: EditGroupViewModel = viewModel(factory = EditGroupViewModel.factory(groupId))
) {
    val state by viewModel.uiState.collectAsState()
    val baseUrl = OpenVKMatchaApp.instance.api.baseUrl

    DisposableEffect(state.currentTab) {
        AppEvents.setTopBarState(TopBarState(
            title = "Редактирование группы",
            navigationIcon = {
                IconButton(onClick = {
                    if (state.currentTab == EditGroupTab.ADVANCED) {
                        viewModel.setTab(EditGroupTab.MAIN)
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            }
        ))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onBack()
        }
    }

    Scaffold(
    ) { padding ->
        when (state.currentTab) {
            EditGroupTab.MAIN -> MainTabContent(padding, state, viewModel)
            EditGroupTab.ADVANCED -> WebViewTabContent(padding, "$baseUrl/club$groupId/edit")
        }
    }
}

@Composable
private fun MainTabContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    state: com.deliriousvoid.openvkmatcha.ui.viewmodel.EditGroupUiState,
    viewModel: EditGroupViewModel
) {
    if (state.isLoading) {
        LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (state.error != null) {
                ErrorText(message = state.error, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = state.settings.title,
                onValueChange = { newValue -> viewModel.updateSettings { it.copy(title = newValue) } },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.settings.description,
                onValueChange = { newValue -> viewModel.updateSettings { it.copy(description = newValue) } },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.settings.screenName,
                onValueChange = { newValue -> viewModel.updateSettings { it.copy(screenName = newValue) } },
                label = { Text("Короткое имя (shortcode)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.settings.website,
                onValueChange = { newValue -> viewModel.updateSettings { it.copy(website = newValue) } },
                label = { Text("Сайт") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Сохранить изменения")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.setTab(EditGroupTab.ADVANCED) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Расширенные настройки")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewTabContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    url: String
) {
    val token = OpenVKMatchaApp.instance.api.token
    
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }
                }
                
                val headers = mutableMapOf<String, String>()
                token?.let { headers["Authorization"] = "Bearer $it" }
                
                loadUrl(url, headers)
            }
        }
    )
}
