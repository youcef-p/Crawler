package com.example.reelscraper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscraper.data.local.ChapterDao
import com.example.reelscraper.data.local.PlaybackStateDao
import com.example.reelscraper.data.local.SubtitleTrackDao
import com.example.reelscraper.data.model.Chapter
import com.example.reelscraper.data.model.PlaybackState
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.model.SubtitleTrack
import com.example.reelscraper.data.repository.MediaRepository
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.settings.SettingsRepository
import com.example.reelscraper.intelligence.chapter.LocalChapterGenerator
import com.example.reelscraper.intelligence.subtitle.LocalSubtitleGenerator
import com.example.reelscraper.player.DynamicStreamResolver
import com.example.reelscraper.player.HeatmapTracker
import com.example.reelscraper.player.PredictivePreloader
import com.example.reelscraper.player.TrickPlayManager
import com.example.reelscraper.ui.components.PlayerAspectRatio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FeedUiState(
    val currentItemIndex: Int = 0,
    val isPlaying: Boolean = true,
    val isMuted: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isControlsVisible: Boolean = true,
    val isBuffering: Boolean = false,
    val isFilterSheetVisible: Boolean = false,
    val aspectRatio: PlayerAspectRatio = PlayerAspectRatio.FIT,
    val playbackSpeed: Float = 1.0f,
    val isRefreshingStream: Boolean = false,
    val statusMessage: String? = null,
    val activeChapters: List<Chapter> = emptyList(),
    val activeSubtitles: List<SubtitleTrack> = emptyList()
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val repository: MediaRepository,
    private val streamResolver: DynamicStreamResolver? = null,
    private val settingsRepository: SettingsRepository? = null,
    val trickPlayManager: TrickPlayManager? = null,
    val heatmapTracker: HeatmapTracker? = null,
    val predictivePreloader: PredictivePreloader? = null,
    val chapterGenerator: LocalChapterGenerator? = null,
    val subtitleGenerator: LocalSubtitleGenerator? = null,
    private val playbackStateDao: PlaybackStateDao? = null,
    private val chapterDao: ChapterDao? = null,
    private val subtitleDao: SubtitleTrackDao? = null
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = settingsRepository?.settingsFlow
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
        ?: MutableStateFlow(AppSettings())

    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword.asStateFlow()

    private val _selectedDomains = MutableStateFlow<Set<String>>(emptySet())
    val selectedDomains: StateFlow<Set<String>> = _selectedDomains.asStateFlow()

    private val _selectedFormat = MutableStateFlow("all")
    val selectedFormat: StateFlow<String> = _selectedFormat.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()

    private val _onlyDynamic = MutableStateFlow(false)
    val onlyDynamic: StateFlow<Boolean> = _onlyDynamic.asStateFlow()

    private val _hideBroken = MutableStateFlow(true)
    val hideBroken: StateFlow<Boolean> = _hideBroken.asStateFlow()

    val availableDomains: StateFlow<List<String>> = repository.distinctSourceDomains
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hasActiveFilters: StateFlow<Boolean> = combine(
        _keyword, _selectedDomains, _selectedFormat, _onlyFavorites, _onlyDynamic
    ) { kw, domains, fmt, fav, dyn ->
        kw.isNotBlank() || domains.isNotEmpty() || fmt != "all" || fav || dyn
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private data class Criteria(
        val format: String,
        val onlyFavorites: Boolean,
        val onlyDynamic: Boolean,
        val hideBroken: Boolean
    )

    private val criteriaFlow = combine(
        _selectedFormat,
        _onlyFavorites,
        _onlyDynamic,
        _hideBroken
    ) { fmt, fav, dyn, hideBrk ->
        Criteria(fmt, fav, dyn, hideBrk)
    }

    val mediaList: StateFlow<List<ScrapedMedia>> = combine(
        _keyword.debounce(150).distinctUntilChanged(),
        _selectedDomains,
        criteriaFlow
    ) { kw, domains, crit ->
        FilterParams(kw, domains, crit.format, crit.onlyFavorites, crit.onlyDynamic, crit.hideBroken)
    }.flatMapLatest { p ->
        repository.getFilteredMediaAdvanced(
            keyword = p.keyword,
            selectedDomains = p.domains,
            selectedFormat = p.format,
            onlyFavorites = p.onlyFavorites,
            onlyDynamic = p.onlyDynamic,
            hideBroken = p.hideBroken
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private data class FilterParams(
        val keyword: String,
        val domains: Set<String>,
        val format: String,
        val onlyFavorites: Boolean,
        val onlyDynamic: Boolean,
        val hideBroken: Boolean
    )

    private val _feedState = MutableStateFlow(FeedUiState())
    private var lastPlaybackPersistAt: Long = 0L
    private var lastPersistedMediaId: Long = -1L
    val feedState: StateFlow<FeedUiState> = _feedState.asStateFlow()

    fun onKeywordChanged(newKeyword: String) {
        _keyword.value = newKeyword
    }

    fun toggleDomainSelection(domain: String) {
        _selectedDomains.update { current ->
            if (current.contains(domain)) current - domain else current + domain
        }
    }

    fun selectAllDomains() {
        _selectedDomains.value = emptySet()
    }

    fun setFormat(format: String) {
        _selectedFormat.value = format
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.update { !it }
    }

    fun toggleDynamicFilter() {
        _onlyDynamic.update { !it }
    }

    fun toggleHideBroken() {
        _hideBroken.update { !it }
    }

    fun clearAllFilters() {
        _keyword.value = ""
        _selectedDomains.value = emptySet()
        _selectedFormat.value = "all"
        _onlyFavorites.value = false
        _onlyDynamic.value = false
        _hideBroken.value = true
    }

    fun openFilterSheet() {
        _feedState.update { it.copy(isFilterSheetVisible = true) }
    }

    fun closeFilterSheet() {
        _feedState.update { it.copy(isFilterSheetVisible = false) }
    }

    fun onPageChanged(newIndex: Int) {
        _feedState.update {
            it.copy(
                currentItemIndex = newIndex,
                isPlaying = true,
                currentPositionMs = 0L,
                totalDurationMs = 0L,
                isBuffering = true
            )
        }

        val currentList = mediaList.value
        if (newIndex in currentList.indices) {
            val media = currentList[newIndex]

            // Trigger Predictive Preloading
            predictivePreloader?.onPageChanged(
                currentIndex = newIndex,
                mediaList = currentList,
                preloadCount = appSettings.value.preloadAdjacentCount,
                isDataSaver = appSettings.value.dataSaverMode
            )

            // Load or generate chapters & subtitles
            loadIntelligenceForMedia(media)
        }
    }

    private fun loadIntelligenceForMedia(media: ScrapedMedia) {
        viewModelScope.launch {
            // Load Chapters
            val existingChapters = withContext(Dispatchers.IO) {
                chapterDao?.getChaptersForMediaDirect(media.id) ?: emptyList()
            }
            if (existingChapters.isNotEmpty()) {
                if (_feedState.value.currentItemIndex < mediaList.value.size && mediaList.value[_feedState.value.currentItemIndex].id == media.id) {
                    _feedState.update { it.copy(activeChapters = existingChapters) }
                }
            } else if (appSettings.value.enableAutoChapters && chapterGenerator != null) {
                val generated = chapterGenerator.generateChaptersIfNeeded(media)
                _feedState.update { it.copy(activeChapters = generated) }
            }

            // Load Subtitles
            val existingSubs = withContext(Dispatchers.IO) {
                subtitleDao?.getSubtitlesForMediaDirect(media.id) ?: emptyList()
            }
            if (_feedState.value.currentItemIndex < mediaList.value.size && mediaList.value[_feedState.value.currentItemIndex].id == media.id) {
                _feedState.update { it.copy(activeSubtitles = existingSubs) }
            }

            // Trigger background speech recognition if enabled and none exists
            if (existingSubs.isEmpty() && appSettings.value.enableLocalTranscription && subtitleGenerator != null) {
                launch(Dispatchers.IO) {
                    val generatedSub = subtitleGenerator.generateSubtitles(
                        media = media,
                        language = appSettings.value.transcriptionLanguage,
                        profile = appSettings.value.transcriptionQuality
                    )
                    if (generatedSub != null) {
                        if (_feedState.value.currentItemIndex < mediaList.value.size && mediaList.value[_feedState.value.currentItemIndex].id == media.id) {
                            _feedState.update { it.copy(activeSubtitles = listOf(generatedSub)) }
                        }
                    }
                }
            }
        }
    }

    fun addManualChapter(title: String, positionMs: Long) {
        val currentIdx = _feedState.value.currentItemIndex
        val media = mediaList.value.getOrNull(currentIdx) ?: return
        viewModelScope.launch {
            val created = chapterGenerator?.addManualChapter(media.id, title, positionMs)
            if (created != null) {
                _feedState.update { it.copy(activeChapters = it.activeChapters + created) }
            }
        }
    }

    fun togglePlayPause() {
        _feedState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun setPlaying(playing: Boolean) {
        _feedState.update { it.copy(isPlaying = playing) }
    }

    fun toggleMute() {
        _feedState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun cycleAspectRatio() {
        _feedState.update {
            val next = when (it.aspectRatio) {
                PlayerAspectRatio.FIT -> PlayerAspectRatio.FILL
                PlayerAspectRatio.FILL -> PlayerAspectRatio.STRETCH
                PlayerAspectRatio.STRETCH -> PlayerAspectRatio.FIT
            }
            it.copy(aspectRatio = next)
        }
    }

    fun cycleSpeed() {
        _feedState.update {
            val nextSpeed = when (it.playbackSpeed) {
                1.0f -> 1.25f
                1.25f -> 1.5f
                1.5f -> 2.0f
                2.0f -> 0.75f
                else -> 1.0f
            }
            it.copy(playbackSpeed = nextSpeed)
        }
    }

    fun refreshStream(media: ScrapedMedia) {
        if (streamResolver == null) return
        viewModelScope.launch {
            _feedState.update { it.copy(isRefreshingStream = true, statusMessage = "Refreshing stream from source...") }
            try {
                streamResolver.refreshStream(media, appSettings.value)
                _feedState.update { it.copy(isRefreshingStream = false, statusMessage = "Stream refreshed!") }
            } catch (_: Exception) {
                _feedState.update { it.copy(isRefreshingStream = false, statusMessage = "Failed to refresh stream") }
            }
        }
    }

    fun clearStatusMessage() {
        _feedState.update { it.copy(statusMessage = null) }
    }

    fun toggleControlsVisibility() {
        _feedState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }

    fun updateProgress(positionMs: Long, durationMs: Long, buffering: Boolean) {
        _feedState.update {
            it.copy(
                currentPositionMs = positionMs,
                totalDurationMs = durationMs,
                isBuffering = buffering
            )
        }

        // Persist playback state (resume position, watched threshold)
        val currentIdx = _feedState.value.currentItemIndex
        val media = mediaList.value.getOrNull(currentIdx)
        if (media != null && durationMs > 0 && playbackStateDao != null) {
            val now = System.currentTimeMillis()
            val isCompleted = = (positionMs.toFloat() / durationMs.toFloat()) >= (appSettings.value.markWatchedThreshold / 100f)
            if (!isCompleted && media.id == lastPersistedMediaId && now - lastPlaybackPersistAt < 1000L) return
            lastPlaybackPersistAt = now
            lastPersistedMediaId = media.id
            viewModelScope.launch(Dispatchers.IO) {
                playbackStateDao.upsertPlaybackState(
                    PlaybackState(
                        mediaId = media.id,
                        positionMillis = positionMs,
                        durationMillis = durationMs,
                        watchedPercent = positionMs.toFloat() / durationMs.toFloat(),
                        completed = isCompleted,
                        lastPlayedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun toggleFavorite(media: ScrapedMedia) {
        viewModelScope.launch {
            repository.toggleFavorite(media.id, !media.isFavorite)
        }
    }

    fun toggleBroken(media: ScrapedMedia) {
        viewModelScope.launch {
            repository.markBroken(media.id, !media.isBroken)
        }
    }

    fun deleteMedia(mediaId: Long) {
        viewModelScope.launch {
            repository.deleteMedia(mediaId)
        }
    }

    fun jumpToIndex(index: Int) {
        _feedState.update { it.copy(currentItemIndex = index) }
    }
}
