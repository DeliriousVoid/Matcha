package com.deliriousvoid.openvkmatcha.ui.screens.settings

import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val languages by viewModel.availableLanguages.collectAsState()
    val currentLanguageId by viewModel.currentLanguageId.collectAsState()
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importLanguage(it) }
    }

    val titleLabel = matchaStringResource(R.string.language)
    val backDesc = matchaStringResource(R.string.desc_back)
    val addDesc = matchaStringResource(R.string.action_add)

    DisposableEffect(Unit) {
        com.deliriousvoid.openvkmatcha.util.AppEvents.setTopBarState(com.deliriousvoid.openvkmatcha.util.TopBarState(
            title = titleLabel,
            route = com.deliriousvoid.openvkmatcha.ui.navigation.Routes.SETTINGS_LANGUAGE,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, backDesc)
                }
            },
            actions = {
                IconButton(onClick = { launcher.launch("*/*") }) {
                    Icon(Icons.Default.Add, addDesc)
                }
            }
        ))
        onDispose {
            com.deliriousvoid.openvkmatcha.util.AppEvents.setTopBarState(null)
        }
    }

    val shareLanguageLabel = matchaStringResource(R.string.share_language_action)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(languages) { lang ->
            LanguageItem(
                info = lang,
                isSelected = lang.id == currentLanguageId,
                onSelect = { viewModel.setLanguage(lang.id) },
                onDelete = if (!lang.isBuiltin) { { viewModel.deleteLanguage(lang.id) } } else null,
                onDownload = {
                    val file = viewModel.getLanguageFile(lang.id)
                    if (file != null) {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/octet-stream"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, shareLanguageLabel))
                    }
                }
            )
        }
    }
}

@Composable
fun LanguageItem(
    info: com.deliriousvoid.openvkmatcha.util.LanguageInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
    onDownload: (() -> Unit)?
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = matchaStringResource(R.string.author_format, info.author),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (onDownload != null) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, matchaStringResource(R.string.action_download))
                }
            }
            
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, matchaStringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
            
            RadioButton(selected = isSelected, onClick = onSelect)
        }
    }
}
