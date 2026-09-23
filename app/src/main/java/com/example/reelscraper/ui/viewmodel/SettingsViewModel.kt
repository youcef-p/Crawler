package com.example.reelscraper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscraper.data.repository.MediaRepository
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun updateScanLevels(levels: Int) {
        viewModelScope.launch {
            settingsRepository.updateScanLevels(levels)
        }
    }

    fun updateCrawlLimits(maxLinks: Int, concurrentRequests: Int, timeoutSec: Int, delayMs: Long) {
        viewModelScope.launch {
            settingsRepository.updateCrawlLimits(maxLinks, concurrentRequests, timeoutSec, delayMs)
        }
    }

    fun updateFormatToggles(mp4: Boolean, webm: Boolean, hls: Boolean, dash: Boolean, gif: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFormatToggles(mp4, webm, hls, dash, gif)
        }
    }

    fun updateAdvancedExtraction(
        html: Boolean, meta: Boolean, jsonLd: Boolean, inlineScript: Boolean,
        regex: Boolean, attribute: Boolean, iframe: Boolean, feedSitemap: Boolean,
        jsonApi: Boolean, webView: Boolean, webViewOnlyWhenNoMedia: Boolean,
        contentTypeSniffing: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.updateAdvancedExtraction(
                html, meta, jsonLd, inlineScript, regex, attribute, iframe, feedSitemap,
                jsonApi, webView, webViewOnlyWhenNoMedia, contentTypeSniffing
            )
        }
    }

    fun updatePlaybackSettings(autoplayNext: Boolean, muteByDefault: Boolean, preloadCount: Int, pauseOnBackground: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePlaybackSettings(autoplayNext, muteByDefault, preloadCount, pauseOnBackground)
        }
    }

    fun updateAppearance(themeMode: String, showBadges: Boolean, showDomainLabels: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAppearance(themeMode, showBadges, showDomainLabels)
        }
    }

    fun clearAllMedia() {
        viewModelScope.launch {
            mediaRepository.clearAll()
            _message.emit("All media cleared from local database")
        }
    }

    fun clearDuplicateMedia() {
        viewModelScope.launch {
            val removed = mediaRepository.clearDuplicates()
            _message.emit("Cleared $removed duplicate entries")
        }
    }

    fun exportDatabaseJson(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = mediaRepository.exportJson()
            onExportReady(json)
            _message.emit("Database exported as JSON")
        }
    }

    fun importDatabaseJson(json: String) {
        viewModelScope.launch {
            val count = mediaRepository.importJson(json)
            _message.emit("Imported $count items into library")
        }
    }
}
