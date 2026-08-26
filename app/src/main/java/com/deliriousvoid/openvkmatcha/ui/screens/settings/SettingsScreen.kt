package com.deliriousvoid.openvkmatcha.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.BuildConfig
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.theme.AccentColor
import com.deliriousvoid.openvkmatcha.ui.theme.AppTheme
import com.deliriousvoid.openvkmatcha.ui.theme.CustomAccent
import com.deliriousvoid.openvkmatcha.ui.theme.CustomTheme
import com.deliriousvoid.openvkmatcha.ui.theme.LocalOpaqueColorScheme
import com.deliriousvoid.openvkmatcha.ui.util.matchaStringResource
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    onNavigateToGeneral: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToDeveloper: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val app = OpenVKMatchaApp.instance
    val isDeveloperMode by viewModel.developerModeActive.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SettingsCategoryButton(
            title = matchaStringResource(R.string.title_settings_general),
            subtitle = matchaStringResource(R.string.settings_general_desc),
            icon = Icons.Default.Settings,
            onClick = onNavigateToGeneral
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCategoryButton(
            title = matchaStringResource(R.string.title_settings_appearance),
            subtitle = matchaStringResource(R.string.settings_appearance_desc),
            icon = Icons.Default.Brush,
            onClick = onNavigateToAppearance
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCategoryButton(
            title = matchaStringResource(R.string.title_settings_music),
            subtitle = matchaStringResource(R.string.settings_music_desc),
            icon = Icons.Default.MusicNote,
            onClick = onNavigateToMusic
        )

        if (isDeveloperMode) {
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCategoryButton(
                title = matchaStringResource(R.string.title_settings_developer),
                subtitle = matchaStringResource(R.string.settings_developer_desc),
                icon = Icons.Default.Code,
                onClick = onNavigateToDeveloper
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                app.authRepository.logout()
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(matchaStringResource(R.string.logout))
        }
    }
}

@Composable
fun GeneralSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToIgnored: () -> Unit,
    onNavigateToAboutInstance: () -> Unit,
    onBack: () -> Unit
) {
    val app = OpenVKMatchaApp.instance
    val cacheSize by viewModel.cacheSize.collectAsState()
    val pauseVideoOnScroll by viewModel.pauseVideoOnScroll.collectAsState()
    val doubleTapToLike by viewModel.doubleTapToLike.collectAsState()
    val doubleTapTimeout by viewModel.doubleTapTimeout.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.developerHint.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToAboutInstance.collect {
            onNavigateToAboutInstance()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsActionRow(
            label = matchaStringResource(R.string.title_settings_ignored),
            icon = Icons.Default.Block,
            onClick = onNavigateToIgnored
        )
        SettingsActionRow(
            label = matchaStringResource(R.string.account_management),
            icon = Icons.Default.People,
            onClick = onNavigateToAccounts
        )
        SettingsActionRow(
            label = matchaStringResource(R.string.clear_cache),
            value = cacheSize,
            icon = Icons.Default.Storage,
        ) { viewModel.clearCache() }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = matchaStringResource(R.string.video_category),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = matchaStringResource(R.string.pause_on_scroll),
            checked = pauseVideoOnScroll,
            onCheckedChange = { viewModel.setPauseVideoOnScroll(it) }
        )
        Text(
            text = matchaStringResource(R.string.pause_on_scroll_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = matchaStringResource(com.deliriousvoid.openvkmatcha.R.string.settings_category_feed),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = matchaStringResource(com.deliriousvoid.openvkmatcha.R.string.settings_double_tap_like),
            checked = doubleTapToLike,
            onCheckedChange = { viewModel.setDoubleTapToLike(it) }
        )
        Text(
            text = matchaStringResource(com.deliriousvoid.openvkmatcha.R.string.settings_double_tap_like_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (doubleTapToLike) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = matchaStringResource(
                    com.deliriousvoid.openvkmatcha.R.string.settings_double_tap_timeout,
                    doubleTapTimeout
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = doubleTapTimeout.toFloat(),
                onValueChange = { viewModel.setDoubleTapTimeout(it.toLong()) },
                valueRange = 75f..150f,
                steps = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = matchaStringResource(R.string.info),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsRow(
            label = matchaStringResource(R.string.instance),
            value = app.api.baseUrl,
            onClick = { viewModel.onInstanceClick() }
        )
        SettingsRow(
            label = matchaStringResource(R.string.version),
            value = BuildConfig.VERSION_NAME,
            onClick = { viewModel.onVersionClick() }
        )
    }
}

@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToNavigationSettings: () -> Unit,
    onNavigateToAppIconSettings: () -> Unit,
    onNavigateToLanguageSettings: () -> Unit,
    onNavigateToThemeEditor: (String?) -> Unit,
    onNavigateToAccentEditor: (String?) -> Unit,
    onBack: () -> Unit
) {
    val currentTheme by viewModel.theme.collectAsState()
    val customThemes by viewModel.customThemes.collectAsState()
    val selectedCustomThemeId by viewModel.selectedCustomThemeId.collectAsState()
    
    val currentAccent by viewModel.accent.collectAsState()
    val customAccents by viewModel.customAccents.collectAsState()
    val selectedCustomAccentId by viewModel.selectedCustomAccentId.collectAsState()
    
    val navBarLabelsVisible by viewModel.navBarLabelsVisible.collectAsState()
    val autoHidePlayer by viewModel.autoHidePlayer.collectAsState()
    
    val context = LocalContext.current
    
    val themeImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.importTheme(it)
        }
    }
    
    val accentImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.importAccent(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = matchaStringResource(R.string.theme),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = matchaStringResource(R.string.beta),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            IconButton(onClick = { themeImportLauncher.launch("*/*") }) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeButton(
                label = matchaStringResource(R.string.theme_amoled),
                selected = currentTheme == AppTheme.AMOLED,
                onClick = { viewModel.setTheme(AppTheme.AMOLED) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            ThemeButton(
                label = matchaStringResource(R.string.theme_light),
                selected = currentTheme == AppTheme.LIGHT,
                onClick = { viewModel.setTheme(AppTheme.LIGHT) }
            )
            
            customThemes.forEach { theme ->
                Spacer(modifier = Modifier.width(8.dp))
                CustomThemeItem(
                    theme = theme,
                    selected = currentTheme == AppTheme.CUSTOM && selectedCustomThemeId == theme.id,
                    onClick = { viewModel.setTheme(AppTheme.CUSTOM, theme.id) },
                    onEdit = { onNavigateToThemeEditor(theme.id) },
                    onDelete = {
                        viewModel.deleteTheme(theme.id)
                    },
                    onShare = {
                        viewModel.exportTheme(theme.id) { file ->
                            file?.let { shareFile(context, it) }
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onNavigateToThemeEditor(null) },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = matchaStringResource(R.string.accent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { accentImportLauncher.launch("*/*") }) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentButton(
                label = matchaStringResource(R.string.blue),
                color = Color(0xFF0077FF),
                selected = currentAccent == AccentColor.BLUE,
                onClick = { viewModel.setAccent(AccentColor.BLUE) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            AccentButton(
                label = matchaStringResource(R.string.green),
                color = Color(0xFF4BB34B),
                selected = currentAccent == AccentColor.GREEN,
                onClick = { viewModel.setAccent(AccentColor.GREEN) }
            )
            
            customAccents.forEach { accent ->
                Spacer(modifier = Modifier.width(8.dp))
                CustomAccentItem(
                    accent = accent,
                    selected = currentAccent == AccentColor.CUSTOM && selectedCustomAccentId == accent.id,
                    onClick = { viewModel.setAccent(AccentColor.CUSTOM, accent.id) },
                    onEdit = { onNavigateToAccentEditor(accent.id) },
                    onDelete = {
                        viewModel.deleteAccent(accent.id)
                    },
                    onShare = {
                        viewModel.exportAccent(accent.id) { file ->
                            file?.let { shareFile(context, it) }
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onNavigateToAccentEditor(null) },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = matchaStringResource(R.string.navigation),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        SettingsActionRow(
            label = matchaStringResource(R.string.nav_buttons_setup),
            icon = Icons.Default.Settings,
            onClick = onNavigateToNavigationSettings
        )
        SettingsActionRow(
            label = matchaStringResource(R.string.app_icon),
            icon = Icons.Default.Brush,
            onClick = onNavigateToAppIconSettings
        )
        SettingsActionRow(
            label = matchaStringResource(R.string.language),
            icon = Icons.Default.Translate,
            onClick = onNavigateToLanguageSettings
        )
        SettingsSwitchRow(
            label = matchaStringResource(R.string.show_nav_labels),
            checked = navBarLabelsVisible,
            onCheckedChange = { viewModel.setNavBarLabelsVisible(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = matchaStringResource(R.string.player),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        SettingsSwitchRow(
            label = matchaStringResource(com.deliriousvoid.openvkmatcha.R.string.settings_auto_hide_player),
            checked = autoHidePlayer,
            onCheckedChange = { viewModel.setAutoHidePlayer(it) }
        )
        Text(
            text = matchaStringResource(com.deliriousvoid.openvkmatcha.R.string.settings_auto_hide_player_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomThemeItem(
    theme: CustomTheme,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isCompact = theme.author.isBlank()
    
    Box {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = if (isCompact) 8.dp else 4.dp,
                    bottom = if (isCompact) 8.dp else 4.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = theme.name,
                        style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge
                    )
                    if (!isCompact) {
                        Text(text = theme.author, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = (if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(matchaStringResource(R.string.action_edit)) },
                onClick = { showMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(matchaStringResource(R.string.action_share)) },
                onClick = { showMenu = false; onShare() },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(matchaStringResource(R.string.action_delete)) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

@Composable
private fun CustomAccentItem(
    accent: CustomAccent,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isCompact = accent.author.isBlank()
    
    Box {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = if (isCompact) 8.dp else 4.dp,
                    bottom = if (isCompact) 8.dp else 4.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(12.dp).background(Color(accent.primary), CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = accent.name,
                        style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge
                    )
                    if (!isCompact) {
                        Text(text = accent.author, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = (if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(matchaStringResource(R.string.action_edit)) },
                onClick = { showMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(matchaStringResource(R.string.action_share)) },
                onClick = { showMenu = false; onShare() },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(matchaStringResource(R.string.action_delete)) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share theme"))
}

@Composable
fun MusicSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val autoDownload by viewModel.autoDownload.collectAsState()
    val offlineMode by viewModel.offlineMode.collectAsState()
    val lbEnabled by viewModel.listenBrainzEnabled.collectAsState()
    val lbToken by viewModel.listenBrainzToken.collectAsState()
    val tracksPerPage by viewModel.tracksPerPage.collectAsState()
    val hidePlayedTracks by viewModel.hidePlayedTracks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = matchaStringResource(R.string.mode),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = matchaStringResource(R.string.offline_mode),
            checked = offlineMode,
            onCheckedChange = { viewModel.setOfflineMode(it) }
        )
        Text(
            text = matchaStringResource(R.string.offline_mode_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = matchaStringResource(R.string.queue),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = matchaStringResource(R.string.hide_played_tracks),
            checked = hidePlayedTracks,
            onCheckedChange = { viewModel.setHidePlayedTracks(it) }
        )
        Text(
            text = matchaStringResource(R.string.hide_played_tracks_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = matchaStringResource(R.string.download_category),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = matchaStringResource(R.string.auto_download),
            checked = autoDownload,
            onCheckedChange = { viewModel.setAutoDownload(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = matchaStringResource(R.string.tracks_per_page_format, tracksPerPage),
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = tracksPerPage.toFloat(),
            onValueChange = { viewModel.setTracksPerPage(it.toInt()) },
            valueRange = 50f..500f,
            steps = 8, // (500-50)/50 - 1 = 8 steps for increments of 50
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = matchaStringResource(R.string.tracks_per_page_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        
        Text(
            text = "ListenBrainz",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = matchaStringResource(R.string.listenbrainz_scrobbling),
            checked = lbEnabled,
            onCheckedChange = { viewModel.setListenBrainzEnabled(it) }
        )
        if (lbEnabled) {
            OutlinedTextField(
                value = lbToken,
                onValueChange = { viewModel.setListenBrainzToken(it) },
                label = { Text("User Token") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            Text(
                text = matchaStringResource(R.string.listenbrainz_token_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun DeveloperSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val currentClientName by viewModel.clientName.collectAsState()
    val invisibility by viewModel.invisibility.collectAsState()
    val experimentalFeatures by viewModel.experimentalFeatures.collectAsState()
    val opaqueColorScheme = LocalOpaqueColorScheme.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showDialog by remember { mutableStateOf(value = false) }
    var customName by remember { mutableStateOf("") }
    var showLoginHint by remember { mutableStateOf(value = false) }

    val predefinedNames = listOf(
        "openvk_native",
        "openvk_flux_android",
        "openvk_ios",
        "vk4me",
        "windows_phone",
        "Matcha"
    )

    LaunchedEffect(Unit) {
        viewModel.showLoginHint.collect {
            showLoginHint = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = matchaStringResource(R.string.online_mode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = matchaStringResource(R.string.invisibility),
            checked = invisibility,
            onCheckedChange = { viewModel.setInvisibility(it) }
        )
        Text(
            text = matchaStringResource(R.string.invisibility_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SettingsSwitchRow(
            label = matchaStringResource(R.string.experimental_features),
            checked = experimentalFeatures,
            onCheckedChange = { viewModel.setExperimentalFeatures(it) }
        )
        Text(
            text = matchaStringResource(R.string.experimental_features_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = matchaStringResource(R.string.client_name_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = matchaStringResource(R.string.client_name_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        predefinedNames.forEach { name ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setClientName(name) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentClientName == name,
                    onClick = { viewModel.setClientName(name) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
            }
        }

        val isCustom = !predefinedNames.contains(currentClientName)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isCustom,
                onClick = { showDialog = true }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = matchaStringResource(R.string.custom_name), style = MaterialTheme.typography.bodyLarge)
                if (isCustom) {
                    Text(
                        text = currentClientName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = matchaStringResource(R.string.settings_trigger_crash),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = matchaStringResource(R.string.settings_trigger_crash_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = { viewModel.triggerCrash() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(Icons.Default.BugReport, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(matchaStringResource(R.string.settings_trigger_crash))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = matchaStringResource(R.string.token_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = {
                viewModel.accessToken?.let {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(matchaStringResource(R.string.copy_access_token))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = opaqueColorScheme.surface,
            title = { Text(matchaStringResource(R.string.custom_client_name)) },
            text = {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text(matchaStringResource(R.string.first_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customName.isNotBlank()) {
                        viewModel.setClientName(customName)
                        showDialog = false
                    }
                }) {
                    Text(matchaStringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(matchaStringResource(R.string.cancel))
                }
            }
        )
    }

    if (showLoginHint) {
        AlertDialog(
            onDismissRequest = { showLoginHint = false },
            containerColor = opaqueColorScheme.surface,
            title = { Text(matchaStringResource(R.string.dialog_attention)) },
            text = { Text(matchaStringResource(R.string.login_hint_text)) },
            confirmButton = {
                TextButton(onClick = { showLoginHint = false }) {
                    Text(matchaStringResource(R.string.action_ok))
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(label)
    }
}

@Composable
private fun AccentButton(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label)
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    icon: ImageVector,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
