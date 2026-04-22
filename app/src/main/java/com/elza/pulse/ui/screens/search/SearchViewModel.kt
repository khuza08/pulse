package com.elza.pulse.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elza.pulse.innertube.Innertube
import com.elza.pulse.innertube.InnertubeMapper
import com.elza.pulse.innertube.models.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val items: List<SongItem>) : SearchState()
    data class Error(val message: String) : SearchState()
}

class SearchViewModel : ViewModel() {
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    fun search(query: String) {
        println("SearchViewModel: Searching for '$query'")
        if (query.isBlank()) {
            println("SearchViewModel: Query is blank, ignoring")
            return
        }
        
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                println("SearchViewModel: Calling Innertube.search('$query')")
                val response = Innertube.search(query)
                val items = InnertubeMapper.fromSearchResponse(response)
                println("SearchViewModel: Search success: ${items.size} items found")
                _searchState.value = SearchState.Success(items)
            } catch (e: Exception) {
                println("SearchViewModel: Search error: ${e.message}")
                e.printStackTrace()
                _searchState.value = SearchState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
