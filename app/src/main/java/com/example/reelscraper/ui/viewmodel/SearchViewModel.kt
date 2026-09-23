package com.example.reelscraper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.repository.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchScreenState(
    val searchQuery: String = "",
    val selectedFilter: MediaType? = null,
    val showOnlyFavorites: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _screenState = MutableStateFlow(SearchScreenState())
    val screenState: StateFlow<SearchScreenState> = _screenState.asStateFlow()

    val searchResults: StateFlow<List<ScrapedMedia>> = _screenState
        .flatMapLatest { state ->
            if (state.showOnlyFavorites) {
                repository.getFavorites()
            } else {
                repository.searchMedia(state.searchQuery, state.selectedFilter)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalCount: StateFlow<Int> = repository.mediaCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun onQueryChanged(query: String) {
        _screenState.update { it.copy(searchQuery = query) }
    }

    fun onFilterSelected(filter: MediaType?) {
        _screenState.update { it.copy(selectedFilter = filter, showOnlyFavorites = false) }
    }

    fun onFavoritesToggled(showOnlyFavorites: Boolean) {
        _screenState.update { it.copy(showOnlyFavorites = showOnlyFavorites, selectedFilter = null) }
    }

    fun clearQuery() {
        _screenState.update { it.copy(searchQuery = "") }
    }

    fun deleteMedia(id: Long) {
        viewModelScope.launch {
            repository.deleteMedia(id)
        }
    }

    fun toggleFavorite(media: ScrapedMedia) {
        viewModelScope.launch {
            repository.toggleFavorite(media.id, !media.isFavorite)
        }
    }
}
