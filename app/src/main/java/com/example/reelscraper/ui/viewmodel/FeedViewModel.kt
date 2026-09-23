package com.example.reelscraper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.repository.MediaRepository
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

data class FeedUiState(
    val currentItemIndex: Int = 0,
    val isPlaying: Boolean = true,
    val isMuted: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isControlsVisible: Boolean = true,
    val isBuffering: Boolean = false,
    val isFilterSheetVisible: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword.asStateFlow()

    private val _selectedDomains = MutableStateFlow<Set<String>>(emptySet())
    val selectedDomains: StateFlow<Set<String>> = _selectedDomains.asStateFlow()

    val availableDomains: StateFlow<List<String>> = repository.distinctSourceDomains
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hasActiveFilters: StateFlow<Boolean> = combine(_keyword, _selectedDomains) { kw, domains ->
        kw.isNotBlank() || domains.isNotEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // Debounce keyword to avoid lag during fast typing, combine with domain selection
    val mediaList: StateFlow<List<ScrapedMedia>> = combine(
        _keyword.debounce(150).distinctUntilChanged(),
        _selectedDomains
    ) { kw, domains ->
        Pair(kw, domains)
    }.flatMapLatest { (kw, domains) ->
        repository.getFilteredMedia(keyword = kw, selectedDomains = domains)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _feedState = MutableStateFlow(FeedUiState())
    val feedState: StateFlow<FeedUiState> = _feedState.asStateFlow()

    fun onKeywordChanged(newKeyword: String) {
        _keyword.value = newKeyword
    }

    fun toggleDomainSelection(domain: String) {
        _selectedDomains.update { current ->
            if (current.contains(domain)) {
                current - domain
            } else {
                current + domain
            }
        }
    }

    fun selectAllDomains() {
        _selectedDomains.value = emptySet()
    }

    fun clearAllFilters() {
        _keyword.value = ""
        _selectedDomains.value = emptySet()
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
    }

    fun toggleFavorite(media: ScrapedMedia) {
        viewModelScope.launch {
            repository.toggleFavorite(media.id, !media.isFavorite)
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
