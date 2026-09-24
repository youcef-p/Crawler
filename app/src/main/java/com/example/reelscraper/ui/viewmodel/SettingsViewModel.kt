package com.example.reelscraper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscraper.data.repository.MediaRepository
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
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

    fun updateAudioNormalization(enabled: Boolean, strength: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(audioNormalization = enabled, normalizationStrength = strength)
            }
        }
    }

    fun update60FpsConverter(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(enable60FpsConverter = enabled)
            }
        }
    }

    fun updateAmbientMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(ambientMode = mode)
            }
        }
    }

    fun updateSmartReframe(mode: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(smartReframeMode = mode)
            }
        }
    }

    fun updateQualityAndDataSaver(quality: String, dataSaver: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(qualityPreference = quality, dataSaverMode = dataSaver)
            }
        }
    }

    fun updateChaptersAndTranscription(autoChapters: Boolean, localTranscription: Boolean, language: String, quality: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    enableAutoChapters = autoChapters,
                    enableLocalTranscription = localTranscription,
                    transcriptionLanguage = language,
                    transcriptionQuality = quality
                )
            }
        }
    }

    fun updatePhashMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(pHashMode = mode)
            }
        }
    }

    fun updateSubtitleStyling(fontSize: Int, textColor: String, bgColor: String, verticalOffset: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    subtitleFontSizeSp = fontSize,
                    subtitleTextColorHex = textColor,
                    subtitleBgColorHex = bgColor,
                    subtitleVerticalOffsetDp = verticalOffset
                )
            }
        }
    }

    fun updatePlaybackSettings(
        autoplayNext: Boolean,
        muteByDefault: Boolean,
        preloadCount: Int,
        pauseOnBackground: Boolean,
        resume: Boolean = true,
        debugStats: Boolean = false
    ) {
        viewModelScope.launch {
            settingsRepository.updatePlaybackSettings(
                autoplayNext,
                muteByDefault,
                preloadCount,
                pauseOnBackground,
                resume,
                debugStats
            )
        }
    }

    fun updateDiscoverySettings(
        sameDomainOnly: Boolean,
        includeSubdomains: Boolean,
        discoverSitemaps: Boolean,
        discoverMediaFromLinkPreloads: Boolean,
        followPaginationLinks: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.updateDiscoverySettings(
                sameDomainOnly, includeSubdomains, discoverSitemaps,
                discoverMediaFromLinkPreloads, followPaginationLinks
            )
        }
    }

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

    fun updateDynamicStreamSettings(
        enableDynamicStreaming: Boolean,
        enableLocalProxy: Boolean = false,
        refreshExpiredStreams: Boolean = true
    ) {
        viewModelScope.launch {
            settingsRepository.updateDynamicStreamSettings(
                enableDynamicStreaming,
                enableLocalProxy,
                refreshExpiredStreams
            )
        }
    }

    fun updateAppearance(
        themeMode: String,
        showBadges: Boolean,
        showDomainLabels: Boolean,
        showDynamicBadges: Boolean = true
    ) {
        viewModelScope.launch {
            settingsRepository.updateAppearance(themeMode, showBadges, showDomainLabels, showDynamicBadges)
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

    fun clearBrokenMedia() {
        viewModelScope.launch {
            val removed = mediaRepository.clearBrokenMedia()
            _message.emit("Cleared $removed broken media entries")
        }
    }

    fun exportDatabaseJson(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = mediaRepository.exportJson()
            onExportReady(json)
        }
    }

    fun importDatabaseJson(json: String) {
        viewModelScope.launch {
            try {
                val count = mediaRepository.importJson(json)
                _message.emit("Successfully imported $count media items")
            } catch (e: Exception) {
                _message.emit("Import failed: ${e.message}")
            }
        }
    }
}
