package com.elza.pulse.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elza.pulse.providers.innertube.Innertube
import com.elza.pulse.providers.innertube.models.bodies.SearchBody
import com.elza.pulse.providers.innertube.requests.searchPage
import com.elza.pulse.providers.innertube.utils.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val items: List<Innertube.SongItem>) : SearchState()
    data class Error(val message: String) : SearchState()
}

class SearchViewModel : ViewModel() {
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            Innertube.searchPage(
                body = SearchBody(
                    query = query,
                    params = Innertube.SearchFilter.Song.value
                ),
                fromMusicShelfRendererContent = Innertube.SongItem::from
            )?.onSuccess { itemsPage ->
                val items = itemsPage?.items ?: emptyList()
                _searchState.value = SearchState.Success(items)
            }?.onFailure { e ->
                Log.e("SearchViewModel", "Search error", e)
                _searchState.value = SearchState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
