package com.deliriousvoid.openvkmatcha.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.deliriousvoid.openvkmatcha.BuildConfig
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.components.ParsedText
import com.deliriousvoid.openvkmatcha.ui.navigation.Routes
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel
import com.deliriousvoid.openvkmatcha.util.AppEvents
import com.deliriousvoid.openvkmatcha.util.TopBarState

@Composable
fun MatchaSettingsScreen(
    viewModel: SettingsViewModel,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit
) {
    val latestRelease by viewModel.latestRelease.collectAsState()
    val updateChecking by viewModel.updateChecking.collectAsState()
    val updateAvailable by viewModel.updateAvailable.collectAsState()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState()
    val ignorePrereleases by viewModel.ignorePrereleases.collectAsState()

    val titleLabel = matchaStringResource(R.string.settings_category_matcha)
    DisposableEffect(Unit) {
        AppEvents.setTopBarState(TopBarState(
            title = titleLabel,
            route = Routes.SETTINGS_MATCHA
        ))
        onDispose {
            AppEvents.setTopBarState(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.mipmap.matcha)
                    .build()
            ),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Matcha",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        val versionText = if (updateAvailable && latestRelease != null) {
            "v${BuildConfig.VERSION_NAME} -> ${latestRelease!!.tagName}"
        } else {
            "v${BuildConfig.VERSION_NAME}"
        }
        
        Text(
            text = versionText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (updateAvailable && latestRelease != null) {
            Button(
                onClick = { viewModel.downloadUpdate(latestRelease!!) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(matchaStringResource(R.string.settings_matcha_install))
            }
        } else {
            Button(
                onClick = { viewModel.checkUpdates() },
                enabled = !updateChecking,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (updateChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(matchaStringResource(R.string.settings_matcha_check_updates))
                }
            }
        }
        
        latestRelease?.body?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Что нового:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ParsedText(
                        text = notes,
                        onMentionClick = {},
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SettingsSwitchRow(
            label = matchaStringResource(R.string.settings_matcha_auto_check),
            checked = autoCheckUpdates,
            onCheckedChange = { viewModel.setAutoCheckUpdates(it) }
        )
        
        SettingsSwitchRow(
            label = matchaStringResource(R.string.settings_matcha_ignore_prereleases),
            checked = ignorePrereleases,
            onCheckedChange = { viewModel.setIgnorePrereleases(it) }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
