package com.deliriousvoid.openvkmatcha.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.ui.navigation.MainTab
import com.deliriousvoid.openvkmatcha.ui.theme.AccentColor
import com.deliriousvoid.openvkmatcha.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    private val _autoDownload = MutableStateFlow(prefs.getBoolean("auto_download", false))
    val autoDownload = _autoDownload.asStateFlow()

    private val _pauseVideoOnScroll = MutableStateFlow(prefs.getBoolean("pause_video_on_scroll", true))
    val pauseVideoOnScroll = _pauseVideoOnScroll.asStateFlow()

    private val _doubleTapToLike = MutableStateFlow(prefs.getBoolean("double_tap_to_like", false))
    val doubleTapToLike = _doubleTapToLike.asStateFlow()

    private val _doubleTapTimeout = MutableStateFlow(prefs.getLong("double_tap_timeout", 100L))
    val doubleTapTimeout = _doubleTapTimeout.asStateFlow()

    private val _offlineMode = MutableStateFlow(prefs.getBoolean("offline_mode", false))
    val offlineMode = _offlineMode.asStateFlow()

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

    private val _accent = MutableStateFlow(
        try {
            AccentColor.valueOf(prefs.getString("accent_color", AccentColor.GREEN.name) ?: AccentColor.GREEN.name)
        } catch (e: Exception) {
            AccentColor.GREEN
        }
    )
    val accent = _accent.asStateFlow()

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

    private val _clientName = MutableStateFlow(OpenVKMatchaApp.instance.tokenManager.getClientName())
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

    private val _developerHint = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val developerHint = _developerHint.asSharedFlow()

    private val _navigateToAboutInstance = MutableSharedFlow<Unit>()
    val navigateToAboutInstance = _navigateToAboutInstance.asSharedFlow()

    private var versionClicks = 0
    private var lastClickTime = 0L

    val accessToken: String? get() = OpenVKMatchaApp.instance.tokenManager.getToken()

    init {
        updateCacheSize()
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
                _developerHint.emit("Вы уже разработчик!")
                return@launch
            }

            when (versionClicks) {
                2 -> _developerHint.emit("Осталось кликнуть 3 раза...")
                3 -> _developerHint.emit("Осталось кликнуть 2 раза...")
                4 -> _developerHint.emit("Осталось кликнуть 1 раз!")
                5 -> {
                    _developerModeActive.value = true
                    prefs.edit().putBoolean("developer_mode", true).apply()
                    _developerHint.emit("Вы стали разработчиком!")
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

    fun setListenBrainzEnabled(enabled: Boolean) {
        _listenBrainzEnabled.value = enabled
        prefs.edit().putBoolean("lb_enabled", enabled).apply()
    }

    fun setListenBrainzToken(token: String) {
        val trimmed = token.trim()
        _listenBrainzToken.value = trimmed
        prefs.edit().putString("lb_token", trimmed).apply()
    }

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
        prefs.edit().putString("theme_mode", theme.name).apply()
    }

    fun setAccent(accent: AccentColor) {
        _accent.value = accent
        prefs.edit().putString("accent_color", accent.name).apply()
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
