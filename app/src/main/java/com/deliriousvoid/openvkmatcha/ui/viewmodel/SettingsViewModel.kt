package com.deliriousvoid.openvkmatcha.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R
import com.deliriousvoid.openvkmatcha.ui.navigation.MainTab
import com.deliriousvoid.openvkmatcha.ui.theme.AccentColor
import com.deliriousvoid.openvkmatcha.ui.theme.AppTheme
import com.deliriousvoid.openvkmatcha.ui.theme.CustomAccent
import com.deliriousvoid.openvkmatcha.ui.theme.CustomTheme
import com.deliriousvoid.openvkmatcha.util.LanguageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AppIconVariant(val title: String, val componentName: String, val iconRes: Int) {
    DEFAULT("Matcha", "com.deliriousvoid.openvkmatcha.MainActivityDefault", R.mipmap.matcha),
    OLD("Matcha Old", "com.deliriousvoid.openvkmatcha.MainActivityOld", R.mipmap.matcha_old),
    VK_LEGACY("VK Legacy", "com.deliriousvoid.openvkmatcha.MainActivityVkLegacy", R.mipmap.ic_launcher_vk_legacy),
    VK_CLASSIC("VK Classic", "com.deliriousvoid.openvkmatcha.MainActivityVkClassic", R.mipmap.ic_launcher_vk_classic),
    MATCHA_CLASSIC("Matcha Classic", "com.deliriousvoid.openvkmatcha.MainActivityMatchaClassic", R.mipmap.ic_launcher_matcha_classic),
    MATCHA_AGENT("Matcha @ Агент", "com.deliriousvoid.openvkmatcha.MainActivityMatchaAgent", R.mipmap.ic_launcher_matcha_agent),
    MAXCHA("MAXcha", "com.deliriousvoid.openvkmatcha.MainActivityMaxcha", R.mipmap.ic_launcher_maxcha),
    MATCHA_RETRO("Matcha Retro", "com.deliriousvoid.openvkmatcha.MainActivityMatchaRetro", R.mipmap.ic_launcher_matcha_retro),
    MATCHA_LEGACY("Matcha Legacy", "com.deliriousvoid.openvkmatcha.MainActivityMatchaLegacy", R.mipmap.ic_launcher_matcha_legacy)
}

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    private val _autoDownload = MutableStateFlow(prefs.getBoolean("auto_download", false))
    val autoDownload = _autoDownload.asStateFlow()

    private val _autoHidePlayer = MutableStateFlow(prefs.getBoolean("auto_hide_player", false))
    val autoHidePlayer = _autoHidePlayer.asStateFlow()

    private val _openPlayerOnStart = MutableStateFlow(prefs.getBoolean("open_player_on_start", false))
    val openPlayerOnStart = _openPlayerOnStart.asStateFlow()

    private val _currentIcon = MutableStateFlow(
        try {
            AppIconVariant.valueOf(prefs.getString("app_icon_variant", AppIconVariant.DEFAULT.name) ?: AppIconVariant.DEFAULT.name)
        } catch (e: Exception) {
            AppIconVariant.DEFAULT
        }
    )
    val currentIcon = _currentIcon.asStateFlow()

    private val _pauseVideoOnScroll = MutableStateFlow(prefs.getBoolean("pause_video_on_scroll", true))
    val pauseVideoOnScroll = _pauseVideoOnScroll.asStateFlow()

    private val _doubleTapToLike = MutableStateFlow(prefs.getBoolean("double_tap_to_like", false))
    val doubleTapToLike = _doubleTapToLike.asStateFlow()

    private val _doubleTapTimeout = MutableStateFlow(prefs.getLong("double_tap_timeout", 100L))
    val doubleTapTimeout = _doubleTapTimeout.asStateFlow()

    private val _offlineMode = MutableStateFlow(prefs.getBoolean("offline_mode", false))
    val offlineMode = _offlineMode.asStateFlow()

    private val _backgroundNotifications = MutableStateFlow(prefs.getBoolean("background_notifications", false))
    val backgroundNotifications = _backgroundNotifications.asStateFlow()

    private val _listenBrainzEnabled = MutableStateFlow(prefs.getBoolean("lb_enabled", false))
    val listenBrainzEnabled = _listenBrainzEnabled.asStateFlow()

    private val _listenBrainzToken = MutableStateFlow(prefs.getString("lb_token", "")?.trim() ?: "")
    val listenBrainzToken = _listenBrainzToken.asStateFlow()

    private val _theme = MutableStateFlow(
        try {
            AppTheme.valueOf(prefs.getString("theme_mode", AppTheme.AMOLED.name) ?: AppTheme.AMOLED.name)
        } catch (e: Exception) {
            AppTheme.AMOLED
        }
    )
    val theme = _theme.asStateFlow()

    private val _customThemes = MutableStateFlow<List<CustomTheme>>(emptyList())
    val customThemes = _customThemes.asStateFlow()

    private val _selectedCustomThemeId = MutableStateFlow(prefs.getString("custom_theme_id", null))
    val selectedCustomThemeId = _selectedCustomThemeId.asStateFlow()

    val currentCustomTheme = combine(customThemes, selectedCustomThemeId) { themes, id ->
        themes.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _accent = MutableStateFlow(
        try {
            AccentColor.valueOf(prefs.getString("accent_color", AccentColor.GREEN.name) ?: AccentColor.GREEN.name)
        } catch (e: Exception) {
            AccentColor.GREEN
        }
    )
    val accent = _accent.asStateFlow()

    private val _customAccents = MutableStateFlow<List<CustomAccent>>(emptyList())
    val customAccents = _customAccents.asStateFlow()

    private val _selectedCustomAccentId = MutableStateFlow(prefs.getString("custom_accent_id", null))
    val selectedCustomAccentId = _selectedCustomAccentId.asStateFlow()

    val currentCustomAccent = combine(customAccents, selectedCustomAccentId) { accents, id ->
        accents.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _feedType = MutableStateFlow(
        try {
            FeedType.valueOf(prefs.getString("feed_type", FeedType.GLOBAL.name) ?: FeedType.GLOBAL.name)
        } catch (e: Exception) {
            FeedType.GLOBAL
        }
    )
    val feedType = _feedType.asStateFlow()

    private val _tracksPerPage = MutableStateFlow(prefs.getInt("tracks_per_page", 50))
    val tracksPerPage = _tracksPerPage.asStateFlow()

    private val _cacheSize = MutableStateFlow("0 B")
    val cacheSize = _cacheSize.asStateFlow()

    private val _clientName = MutableStateFlow(com.deliriousvoid.openvkmatcha.Constants.CLIENT_NAME)
    val clientName = _clientName.asStateFlow()

    private val _invisibility = MutableStateFlow(prefs.getBoolean("invisibility", false))
    val invisibility = _invisibility.asStateFlow()

    private val _showLoginHint = MutableSharedFlow<Unit>()
    val showLoginHint = _showLoginHint.asSharedFlow()

    private val _developerModeActive = MutableStateFlow(prefs.getBoolean("developer_mode", false))
    val developerModeActive = _developerModeActive.asStateFlow()

    private val _experimentalFeatures = MutableStateFlow(prefs.getBoolean("experimental_features", false))
    val experimentalFeatures = _experimentalFeatures.asStateFlow()

    private val _navigationTabs = MutableStateFlow<List<MainTab>>(
        prefs.getString("navigation_tabs", null)?.split(",")?.mapNotNull {
            try { MainTab.valueOf(it) } catch (e: Exception) { null }
        } ?: listOf(MainTab.Home, MainTab.Explore, MainTab.Messages, MainTab.Music, MainTab.Profile)
    )
    val navigationTabs = _navigationTabs.asStateFlow()

    private val _navBarLabelsVisible = MutableStateFlow(prefs.getBoolean("nav_bar_labels_visible", true))
    val navBarLabelsVisible = _navBarLabelsVisible.asStateFlow()

    private val _hidePlayedTracks = MutableStateFlow(prefs.getBoolean("hide_played_tracks", true))
    val hidePlayedTracks = _hidePlayedTracks.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<LanguageInfo>>(emptyList())
    val availableLanguages = _availableLanguages.asStateFlow()

    private val _currentLanguageId = MutableStateFlow(OpenVKMatchaApp.instance.languageManager.currentLanguageId)
    val currentLanguageId = _currentLanguageId.asStateFlow()

    private val _developerHint = MutableSharedFlow<String>()
    val developerHint = _developerHint.asSharedFlow()

    private val _navigateToAboutInstance = MutableSharedFlow<Unit>()
    val navigateToAboutInstance = _navigateToAboutInstance.asSharedFlow()

    private var versionClicks = 0
    private var lastClickTime = 0L

    val accessToken: String? get() = OpenVKMatchaApp.instance.tokenManager.getToken()

    init {
        updateCacheSize()
        viewModelScope.launch(Dispatchers.IO) {
            val name = OpenVKMatchaApp.instance.tokenManager.getClientName()
            _clientName.value = name
            loadLanguages()
            loadThemes()
            loadAccents()
        }
    }

    fun loadLanguages() {
        _availableLanguages.value = OpenVKMatchaApp.instance.languageManager.getAvailableLanguages()
        _currentLanguageId.value = OpenVKMatchaApp.instance.languageManager.currentLanguageId
    }

    fun loadThemes() {
        viewModelScope.launch(Dispatchers.IO) {
            _customThemes.value = OpenVKMatchaApp.instance.themeManager.getThemes()
        }
    }

    fun loadAccents() {
        viewModelScope.launch(Dispatchers.IO) {
            _customAccents.value = OpenVKMatchaApp.instance.themeManager.getAccents()
        }
    }

    fun saveAccent(accent: CustomAccent, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenVKMatchaApp.instance.themeManager.saveAccent(accent)
            loadAccents()
            viewModelScope.launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun deleteAccent(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenVKMatchaApp.instance.themeManager.deleteAccent(id)
            loadAccents()
        }
    }

    fun saveTheme(theme: CustomTheme, bitmap: android.graphics.Bitmap? = null, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenVKMatchaApp.instance.themeManager.saveTheme(theme, bitmap)
            loadThemes()
            viewModelScope.launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun deleteTheme(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenVKMatchaApp.instance.themeManager.deleteTheme(id)
            loadThemes()
        }
    }

    fun importTheme(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = OpenVKMatchaApp.instance.themeManager.importTheme(uri)
            if (result.isSuccess) {
                loadThemes()
            }
        }
    }

    fun importAccent(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = OpenVKMatchaApp.instance.themeManager.importAccent(uri)
            if (result.isSuccess) {
                loadAccents()
            }
        }
    }

    fun exportTheme(id: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = OpenVKMatchaApp.instance.themeManager.exportTheme(id)
            viewModelScope.launch(Dispatchers.Main) {
                onComplete(file)
            }
        }
    }

    fun exportAccent(id: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = OpenVKMatchaApp.instance.themeManager.exportAccent(id)
            viewModelScope.launch(Dispatchers.Main) {
                onComplete(file)
            }
        }
    }

    fun setLanguage(id: String) {
        OpenVKMatchaApp.instance.languageManager.loadLanguage(id)
        _currentLanguageId.value = id
    }

    fun importLanguage(uri: android.net.Uri) {
        viewModelScope.launch {
            val result = OpenVKMatchaApp.instance.languageManager.importLanguage(uri)
            if (result.isSuccess) {
                loadLanguages()
                _developerHint.emit(OpenVKMatchaApp.instance.languageManager.getStringOrDefault(R.string.language_import_success))
            } else {
                _developerHint.emit(OpenVKMatchaApp.instance.languageManager.getStringOrDefault(R.string.language_import_error, result.exceptionOrNull()?.message ?: ""))
            }
        }
    }

    fun deleteLanguage(id: String) {
        OpenVKMatchaApp.instance.languageManager.deleteLanguage(id)
        loadLanguages()
    }

    fun getLanguageFile(id: String): File? {
        return OpenVKMatchaApp.instance.languageManager.getLanguageFile(id)
    }

    fun onVersionClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 2000) {
            versionClicks = 0
        }
        lastClickTime = now
        versionClicks++

        viewModelScope.launch {
            if (_developerModeActive.value) {
                _developerHint.emit(OpenVKMatchaApp.instance.languageManager.getStringOrDefault(R.string.settings_developer_active))
                return@launch
            }

            val lm = OpenVKMatchaApp.instance.languageManager
            when (versionClicks) {
                2 -> _developerHint.emit(lm.getStringOrDefault(R.string.settings_developer_clicks_left, 3))
                3 -> _developerHint.emit(lm.getStringOrDefault(R.string.settings_developer_clicks_left, 2))
                4 -> _developerHint.emit(lm.getStringOrDefault(R.string.settings_developer_clicks_last))
                5 -> {
                    _developerModeActive.value = true
                    prefs.edit().putBoolean("developer_mode", true).apply()
                    _developerHint.emit(lm.getStringOrDefault(R.string.settings_developer_success))
                }
            }
        }
    }

    fun onInstanceClick() {
        viewModelScope.launch {
            _navigateToAboutInstance.emit(Unit)
        }
    }

    fun setExperimentalFeatures(enabled: Boolean) {
        _experimentalFeatures.value = enabled
        prefs.edit().putBoolean("experimental_features", enabled).apply()
    }

    fun triggerCrash() {
        throw RuntimeException("Debug crash triggered from developer settings")
    }

    fun setClientName(name: String) {
        _clientName.value = name
        OpenVKMatchaApp.instance.tokenManager.saveClientName(name)
        viewModelScope.launch {
            _showLoginHint.emit(Unit)
        }
    }

    fun setInvisibility(enabled: Boolean) {
        _invisibility.value = enabled
        prefs.edit().putBoolean("invisibility", enabled).apply()
    }

    fun setAutoDownload(enabled: Boolean) {
        _autoDownload.value = enabled
        prefs.edit().putBoolean("auto_download", enabled).apply()
    }

    fun setAutoHidePlayer(enabled: Boolean) {
        _autoHidePlayer.value = enabled
        prefs.edit().putBoolean("auto_hide_player", enabled).apply()
    }

    fun setOpenPlayerOnStart(enabled: Boolean) {
        _openPlayerOnStart.value = enabled
        prefs.edit().putBoolean("open_player_on_start", enabled).apply()
    }

    fun setPauseVideoOnScroll(enabled: Boolean) {
        _pauseVideoOnScroll.value = enabled
        prefs.edit().putBoolean("pause_video_on_scroll", enabled).apply()
    }

    fun setDoubleTapToLike(enabled: Boolean) {
        _doubleTapToLike.value = enabled
        prefs.edit().putBoolean("double_tap_to_like", enabled).apply()
    }

    fun setDoubleTapTimeout(timeout: Long) {
        _doubleTapTimeout.value = timeout
        prefs.edit().putLong("double_tap_timeout", timeout).apply()
    }

    fun setOfflineMode(enabled: Boolean) {
        _offlineMode.value = enabled
        prefs.edit().putBoolean("offline_mode", enabled).apply()
    }

    fun setBackgroundNotifications(enabled: Boolean) {
        _backgroundNotifications.value = enabled
        prefs.edit().putBoolean("background_notifications", enabled).apply()
        if (enabled) {
            com.deliriousvoid.openvkmatcha.services.LongPollService.start(context)
        } else {
            com.deliriousvoid.openvkmatcha.services.LongPollService.stop(context)
        }
    }

    fun setListenBrainzEnabled(enabled: Boolean) {
        _listenBrainzEnabled.value = enabled
        prefs.edit().putBoolean("lb_enabled", enabled).apply()
    }

    fun setListenBrainzToken(token: String) {
        val trimmed = token.trim()
        _listenBrainzToken.value = trimmed
        prefs.edit().putString("lb_token", trimmed).apply()
    }

    fun setTheme(theme: AppTheme, customThemeId: String? = null) {
        _theme.value = theme
        _selectedCustomThemeId.value = customThemeId
        prefs.edit().putString("theme_mode", theme.name)
            .putString("custom_theme_id", customThemeId)
            .apply()
    }

    fun setAccent(accent: AccentColor, customAccentId: String? = null) {
        _accent.value = accent
        _selectedCustomAccentId.value = customAccentId
        prefs.edit().putString("accent_color", accent.name)
            .putString("custom_accent_id", customAccentId)
            .apply()
    }

    fun setAppIcon(variant: AppIconVariant) {
        if (_currentIcon.value == variant) return
        
        val pm = context.packageManager
        
        // Disable current
        pm.setComponentEnabledSetting(
            ComponentName(context, _currentIcon.value.componentName),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        
        // Enable new
        pm.setComponentEnabledSetting(
            ComponentName(context, variant.componentName),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        
        _currentIcon.value = variant
        prefs.edit().putString("app_icon_variant", variant.name).apply()
    }

    fun setFeedType(type: FeedType) {
        _feedType.value = type
        prefs.edit().putString("feed_type", type.name).apply()
    }

    fun setTracksPerPage(count: Int) {
        _tracksPerPage.value = count
        prefs.edit().putInt("tracks_per_page", count).apply()
    }

    fun setNavigationTabs(tabs: List<MainTab>) {
        if (tabs.size in 3..5) {
            _navigationTabs.value = tabs
            prefs.edit().putString("navigation_tabs", tabs.joinToString(",") { it.name }).apply()
        }
    }

    fun moveNavigationTab(from: Int, to: Int) {
        val current = _navigationTabs.value.toMutableList()
        if (from in current.indices && to in current.indices) {
            val item = current.removeAt(from)
            current.add(to, item)
            setNavigationTabs(current)
        }
    }

    fun setNavBarLabelsVisible(visible: Boolean) {
        _navBarLabelsVisible.value = visible
        prefs.edit().putBoolean("nav_bar_labels_visible", visible).apply()
    }

    fun setHidePlayedTracks(enabled: Boolean) {
        _hidePlayedTracks.value = enabled
        prefs.edit().putBoolean("hide_played_tracks", enabled).apply()
    }

    fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = getDirSize(context.cacheDir) + getDirSize(context.externalCacheDir)
            _cacheSize.value = formatSize(size)
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            deleteDir(context.cacheDir)
            deleteDir(context.externalCacheDir)
            updateCacheSize()
        }
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return true
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) deleteDir(file) else file.delete()
        }
        return true
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(OpenVKMatchaApp.instance) as T
            }
        }
    }
}
