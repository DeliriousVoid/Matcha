package com.deliriousvoid.openvkmatcha.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deliriousvoid.openvkmatcha.BuildConfig
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.ui.theme.AccentColor
import com.deliriousvoid.openvkmatcha.ui.theme.AppTheme
import com.deliriousvoid.openvkmatcha.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateToGeneral: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToDeveloper: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel
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
            title = "Основные",
            subtitle = "Аккаунты, кеш, о приложении",
            icon = Icons.Default.Settings,
            onClick = onNavigateToGeneral
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCategoryButton(
            title = "Внешний вид",
            subtitle = "Тема, акцентный цвет",
            icon = Icons.Default.Brush,
            onClick = onNavigateToAppearance
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCategoryButton(
            title = "Музыка",
            subtitle = "Загрузки, ListenBrainz",
            icon = Icons.Default.MusicNote,
            onClick = onNavigateToMusic
        )

        if (isDeveloperMode) {
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCategoryButton(
                title = "Для разработчика",
                subtitle = "Идентификация клиента, API",
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
            Text("Выйти из аккаунта")
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
            label = "Игнорируемые источники",
            icon = Icons.Default.Block,
            onClick = onNavigateToIgnored
        )
        SettingsActionRow(
            label = "Управление аккаунтами",
            icon = Icons.Default.SupervisorAccount,
            onClick = onNavigateToAccounts
        )
        SettingsActionRow(
            label = "Очистить кеш",
            value = cacheSize,
            icon = Icons.Default.Storage,
            onClick = { viewModel.clearCache() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Видео",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = "Пауза при промотке",
            checked = pauseVideoOnScroll,
            onCheckedChange = { viewModel.setPauseVideoOnScroll(it) }
        )
        Text(
            text = "Если выключено, видео при промотке будет открываться в мини-плеере. Изменения вступят в силу после перезапуска.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(com.deliriousvoid.openvkmatcha.R.string.settings_category_feed),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = androidx.compose.ui.res.stringResource(com.deliriousvoid.openvkmatcha.R.string.settings_double_tap_like),
            checked = doubleTapToLike,
            onCheckedChange = { viewModel.setDoubleTapToLike(it) }
        )
        Text(
            text = androidx.compose.ui.res.stringResource(com.deliriousvoid.openvkmatcha.R.string.settings_double_tap_like_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (doubleTapToLike) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(
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
            text = "Информация",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsRow(
            label = "Инстанс",
            value = app.api.baseUrl,
            onClick = { viewModel.onInstanceClick() }
        )
        SettingsRow(
            label = "Версия",
            value = BuildConfig.VERSION_NAME,
            onClick = { viewModel.onVersionClick() }
        )
    }
}

@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToNavigationSettings: () -> Unit,
    onBack: () -> Unit
) {
    val currentTheme by viewModel.theme.collectAsState()
    val currentAccent by viewModel.accent.collectAsState()
    val navBarLabelsVisible by viewModel.navBarLabelsVisible.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Тема",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            ThemeButton(
                label = "Amoled",
                selected = currentTheme == AppTheme.AMOLED,
                onClick = { viewModel.setTheme(AppTheme.AMOLED) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            ThemeButton(
                label = "Светлая",
                selected = currentTheme == AppTheme.LIGHT,
                onClick = { viewModel.setTheme(AppTheme.LIGHT) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Акцент",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            AccentButton(
                label = "Синий",
                color = Color(0xFF0077FF),
                selected = currentAccent == AccentColor.BLUE,
                onClick = { viewModel.setAccent(AccentColor.BLUE) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AccentButton(
                label = "Зелёный",
                color = Color(0xFF4BB34B),
                selected = currentAccent == AccentColor.GREEN,
                onClick = { viewModel.setAccent(AccentColor.GREEN) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Навигация",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        SettingsActionRow(
            label = "Настройка кнопок навигации",
            icon = Icons.Default.Settings,
            onClick = onNavigateToNavigationSettings
        )
        SettingsSwitchRow(
            label = "Показывать подписи кнопок",
            checked = navBarLabelsVisible,
            onCheckedChange = { viewModel.setNavBarLabelsVisible(it) }
        )
    }
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
            text = "Режим",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = "Оффлайн режим",
            checked = offlineMode,
            onCheckedChange = { viewModel.setOfflineMode(it) }
        )
        Text(
            text = "Скрывает все онлайн-разделы и отключает сетевые запросы.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Очередь",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = "Скрывать проигранные треки",
            checked = hidePlayedTracks,
            onCheckedChange = { viewModel.setHidePlayedTracks(it) }
        )
        Text(
            text = "В очереди воспроизведения будут видны только текущий и последующие треки.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Загрузка",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = "Автоматически скачивать треки",
            checked = autoDownload,
            onCheckedChange = { viewModel.setAutoDownload(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Количество треков на странице: $tracksPerPage",
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
            text = "Влияет на то, сколько треков подгружается за один раз в списках и плейлистах.",
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
            label = "Скробблинг ListenBrainz",
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
                text = "Токен можно найти в настройках профиля на listenbrainz.org",
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
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var showLoginHint by remember { mutableStateOf(false) }

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
            text = "Режим онлайн",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsSwitchRow(
            label = "Невидимка",
            checked = invisibility,
            onCheckedChange = { viewModel.setInvisibility(it) }
        )
        Text(
            text = "Отключает автоматическое обновление статуса «онлайн». Статус всё равно может обновиться при совершении действий (отправка сообщений и т.д.).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SettingsSwitchRow(
            label = "Экспериментальные функции",
            checked = experimentalFeatures,
            onCheckedChange = { viewModel.setExperimentalFeatures(it) }
        )
        Text(
            text = "Включает доступ к экспериментальным и не полностью готовым функциям (опросы, источники, вложения видео и документов).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Имя клиента (client_name)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Влияет на отображаемое имя и иконку около ваших постов",
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
                Text(text = "Собственное имя", style = MaterialTheme.typography.bodyLarge)
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
            text = "Никому не передавайте ваш access_token! Это может привести к потере доступа к аккаунту, вы действуете на свой страх и риск",
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
            Text("Скопировать access_token")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Собственное имя клиента") },
            text = {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Имя") },
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
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showLoginHint) {
        AlertDialog(
            onDismissRequest = { showLoginHint = false },
            title = { Text("Внимание") },
            text = { Text("Чтобы смена прошла успешно — перезайдите в аккаунт.") },
            confirmButton = {
                TextButton(onClick = { showLoginHint = false }) {
                    Text("ОК")
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
