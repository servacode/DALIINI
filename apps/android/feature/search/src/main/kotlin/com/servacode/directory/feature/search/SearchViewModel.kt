package com.servacode.directory.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(
        val values: List<FacilitySummary>,
        val hasMore: Boolean = false,
        val loadingMore: Boolean = false,
        val moreError: String? = null,
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val search: SearchUseCase,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var job: Job? = null
    private var context: SearchContext? = null
    private var nextCursor: String? = null

    fun updateQuery(value: String) {
        _query.value = value
        job?.cancel()
        if (SearchQuery.normalize(value).isEmpty()) {
            _state.value = SearchUiState.Idle
            return
        }
        job = viewModelScope.launch {
            delay(300)
            _state.value = SearchUiState.Loading
            _state.value = runCatching { search.first(value) }.fold(
                onSuccess = { result ->
                    if (result == null) {
                        SearchUiState.Idle
                    } else {
                        context = result.first
                        nextCursor = result.second.nextCursor
                        SearchUiState.Results(result.second.items, hasMore = result.second.hasMore)
                    }
                },
                onFailure = { SearchUiState.Error(AppErrorText.of(it.toAppError())) },
            )
        }
    }

    fun loadMore() {
        val current = _state.value as? SearchUiState.Results ?: return
        val context = context ?: return
        val cursor = nextCursor ?: return
        if (current.loadingMore) return
        _state.value = current.copy(loadingMore = true, moreError = null)
        job = viewModelScope.launch {
            runCatching { search.next(context, cursor) }
                .onSuccess { page ->
                    nextCursor = page.nextCursor
                    _state.value = current.copy(values = current.values + page.items, hasMore = page.hasMore)
                }
                .onFailure {
                    _state.value = current.copy(loadingMore = false, moreError = AppErrorText.of(it.toAppError()))
                }
        }
    }
}
