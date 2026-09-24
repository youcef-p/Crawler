package com.example.reelscraper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscraper.data.model.CrawlJobStatus
import com.example.reelscraper.data.repository.MediaRepository
import com.example.reelscraper.data.scraper.CrawlProgressState
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SampleSite(
    val title: String,
    val description: String,
    val url: String,
    val tag: String
)

sealed interface ScrapeUiState {
    object Idle : ScrapeUiState
    data class Scanning(val progress: CrawlProgressState) : ScrapeUiState
    data class Success(
        val itemsFound: Int,
        val pagesScanned: Int,
        val duplicatesSkipped: Int,
        val summaryMessage: String
    ) : ScrapeUiState
    data class Stopped(val itemsKept: Int, val message: String) : ScrapeUiState
    data class Error(val message: String, val itemsKept: Int = 0) : ScrapeUiState
}

data class ScraperScreenState(
    val targetUrl: String = "",
    val scanLevels: Int = 2,
    val showDepthDialog: Boolean = false,
    val uiState: ScrapeUiState = ScrapeUiState.Idle,
    val indexedCount: Int = 0
)

class ScraperViewModel(
    private val repository: MediaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScraperScreenState())
    val uiState: StateFlow<ScraperScreenState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    val sampleSites = listOf(
        SampleSite(
            title = "Peach Blender Project",
            description = "Open-source Big Buck Bunny video source page",
            url = "https://peach.blender.org/",
            tag = "Videos"
        ),
        SampleSite(
            title = "W3C HTML5 Video Testbed",
            description = "Official W3C video media showcase with .mp4 & .webm",
            url = "https://www.w3.org/2010/05/video/mediaevents.html",
            tag = "HTML5 Video"
        ),
        SampleSite(
            title = "Mux Live & VOD Streams",
            description = "Reference HLS (.m3u8) video streaming links",
            url = "https://mux.com/test-streams",
            tag = "HLS Stream"
        ),
        SampleSite(
            title = "Wikimedia Media Showcase",
            description = "Open educational video media repository",
            url = "https://commons.wikimedia.org/wiki/Category:Featured_videos",
            tag = "WebM / MP4"
        )
    )

    init {
        viewModelScope.launch {
            repository.mediaCount.collect { count ->
                _uiState.update { it.copy(indexedCount = count) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { current ->
                    if (current.uiState is ScrapeUiState.Idle && !current.showDepthDialog) {
                        current.copy(scanLevels = settings.defaultScanLevels.coerceIn(1, 10))
                    } else {
                        current
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.seedStarterSamplesIfEmpty()
            repository.cleanupOrphanJobs()
        }
        // Collect live crawl progress
        viewModelScope.launch {
            repository.crawlProgress.collect { progress ->
                if (progress.jobStatus == CrawlJobStatus.RUNNING && !progress.isFinished) {
                    _uiState.update {
                        if (it.uiState is ScrapeUiState.Scanning) {
                            it.copy(uiState = ScrapeUiState.Scanning(progress))
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(targetUrl = newUrl) }
    }

    fun onScanLevelsChanged(levels: Int) {
        _uiState.update { it.copy(scanLevels = levels.coerceIn(1, 10)) }
    }

    fun selectSample(sample: SampleSite) {
        _uiState.update { it.copy(targetUrl = sample.url) }
    }

    /**
     * User clicks "Scan": opens the Scan Depth Dialog before crawling starts.
     */
    fun onRequestScan() {
        val url = _uiState.value.targetUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(uiState = ScrapeUiState.Error("Please enter a valid webpage URL to scan")) }
            return
        }
        _uiState.update { it.copy(showDepthDialog = true) }
    }

    fun onDismissDepthDialog() {
        _uiState.update { it.copy(showDepthDialog = false) }
    }

    fun onConfirmScanWithLevels(levels: Int) {
        val cleanLevels = levels.coerceIn(1, 10)
        _uiState.update {
            it.copy(
                scanLevels = cleanLevels,
                showDepthDialog = false
            )
        }
        executeCrawl(cleanLevels)
    }

    private fun executeCrawl(levels: Int) {
        val url = _uiState.value.targetUrl.trim()
        if (url.isBlank()) return

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            _uiState.update {
                it.copy(
                    uiState = ScrapeUiState.Scanning(
                        CrawlProgressState(
                            pagesScanned = 0,
                            linksDiscovered = 0,
                            mediaFound = 0,
                            duplicatesSkipped = 0,
                            rowsInserted = 0,
                            currentDepth = 1,
                            maxDepth = levels,
                            currentUrl = url,
                            statusMessage = "Starting Level 1 scan on $url...",
                            jobStatus = CrawlJobStatus.RUNNING,
                            isFinished = false
                        )
                    )
                )
            }

            val result = repository.scrapeAndIndex(
                url = url,
                depth = levels,
                settings = settings
            )

            result.onSuccess { finalProgress ->
                when (finalProgress.jobStatus) {
                    CrawlJobStatus.COMPLETED -> {
                        if (finalProgress.rowsInserted > 0) {
                            _uiState.update {
                                it.copy(
                                    uiState = ScrapeUiState.Success(
                                        itemsFound = finalProgress.rowsInserted,
                                        pagesScanned = finalProgress.pagesScanned,
                                        duplicatesSkipped = finalProgress.duplicatesSkipped,
                                        summaryMessage = "Scan complete! Added ${finalProgress.rowsInserted} new items (${finalProgress.duplicatesSkipped} duplicates skipped)."
                                    )
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    uiState = ScrapeUiState.Error(
                                        message = "No new playable media found across $levels level(s) (${finalProgress.duplicatesSkipped} duplicate items skipped).",
                                        itemsKept = 0
                                    )
                                )
                            }
                        }
                    }
                    CrawlJobStatus.CANCELLED -> {
                        _uiState.update {
                            it.copy(
                                uiState = ScrapeUiState.Stopped(
                                    itemsKept = finalProgress.rowsInserted,
                                    message = "Scan stopped. ${finalProgress.rowsInserted} items kept in Library."
                                )
                            )
                        }
                    }
                    CrawlJobStatus.FAILED -> {
                        _uiState.update {
                            it.copy(
                                uiState = ScrapeUiState.Error(
                                    message = finalProgress.statusMessage.ifBlank { "Scan failed." },
                                    itemsKept = finalProgress.rowsInserted
                                )
                            )
                        }
                    }
                    CrawlJobStatus.RUNNING -> Unit
                }
            }.onFailure { error ->
                val currentProgress = repository.crawlProgress.value
                _uiState.update {
                    it.copy(
                        uiState = ScrapeUiState.Error(
                            message = error.localizedMessage ?: "Failed to scan URL. Please verify internet connection.",
                            itemsKept = currentProgress.rowsInserted
                        )
                    )
                }
            }
        }
    }

    fun cancelScan() {
        val currentRows = repository.crawlProgress.value.rowsInserted
        scanJob?.cancel()
        scanJob = null
        _uiState.update {
            it.copy(
                uiState = ScrapeUiState.Stopped(
                    itemsKept = currentRows,
                    message = "Scan stopped. $currentRows items kept in Library."
                )
            )
        }
    }

    fun resetState() {
        _uiState.update { it.copy(uiState = ScrapeUiState.Idle) }
    }

    fun clearAllIndexedMedia() {
        viewModelScope.launch {
            repository.clearAll()
            _uiState.update { it.copy(uiState = ScrapeUiState.Idle) }
        }
    }
}
