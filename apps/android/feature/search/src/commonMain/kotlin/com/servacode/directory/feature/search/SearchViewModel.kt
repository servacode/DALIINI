package com.servacode.directory.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.analytics.AnalyticsEvent
import com.servacode.directory.core.analytics.AnalyticsTracker
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.toAppError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(
        val values: List<FacilitySummary>,
        val hasMore: Boolean = false,
        val loadingMore: Boolean = false,
        val moreError: AppError? = null,
    ) : SearchUiState
    data class Error(val error: AppError) : SearchUiState
}

/**
 * The search's state, on both platforms. Android's navigation asks Hilt for the subclass in
 * androidMain; the iPhone makes this one with its graph's use case (DECISION-095).
 */
open class SearchViewModel(
    private val search: SearchUseCase,
    private val analytics: AnalyticsTracker,
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
                        // How long what they typed was, never what it was. A search that found
                        // nothing is its own event: it is the one number that says what the
                        // directory is missing, and it is invisible in the first one.
                        val length = SearchQuery.normalize(value).length
                        val provinceId = result.first.provinceId
                        analytics.track(AnalyticsEvent.SearchSubmitted(length, provinceId))
                        if (result.second.items.isEmpty()) {
                            analytics.track(AnalyticsEvent.SearchZeroResults(length, provinceId))
                        }
                        SearchUiState.Results(result.second.items, hasMore = result.second.hasMore)
                    }
                },
                onFailure = { SearchUiState.Error(it.toAppError()) },
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
                    _state.value = current.copy(loadingMore = false, moreError = it.toAppError())
                }
        }
    }
}
