package com.servacode.directory.feature.directory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.RealtimeInvalidation
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DirectoryUiState {
    data object Loading : DirectoryUiState
    data class Content(
        val values: List<FacilitySummary>,
        val filter: DirectoryFilter,
        val stale: Boolean,
        val hasMore: Boolean = false,
        val loadingMore: Boolean = false,
        val moreError: String? = null,
    ) : DirectoryUiState
    data object ProvinceRequired : DirectoryUiState
    data class Error(val message: String) : DirectoryUiState
}

@HiltViewModel
class DirectoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val directory: DirectoryUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val categoryId = savedStateHandle.toRoute<DirectoryRoute.Directory>().categoryId
    private val _state = MutableStateFlow<DirectoryUiState>(DirectoryUiState.Loading)
    val state: StateFlow<DirectoryUiState> = _state.asStateFlow()
    private var filter = DirectoryFilter()
    private var query: DirectoryQuery? = null
    private var nextCursor: String? = null
    private var loading: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                if (RealtimeInvalidation.refreshesProvinceLists(event, query?.provinceId)) refresh()
            }
        }
    }

    fun setOpenNow(value: Boolean) { filter = filter.copy(openNow = value); refresh() }
    fun setDutyNow(value: Boolean) { filter = filter.copy(dutyNow = value); refresh() }
    fun setSearch(value: String) { filter = filter.copy(search = value); refresh() }

    fun refresh() {
        loading?.cancel()
        loading = viewModelScope.launch {
            _state.value = DirectoryUiState.Loading
            directory.firstPage(categoryId, filter).collect { load ->
                when (load) {
                    DirectoryLoad.ProvinceRequired -> _state.value = DirectoryUiState.ProvinceRequired
                    is DirectoryLoad.FirstPage -> {
                        query = load.query
                        _state.value = when (val loaded = load.loaded) {
                            is Loaded.Cached -> content(loaded.value.items, stale = false, cursor = null)
                            is Loaded.Fresh -> content(loaded.value.items, stale = false, cursor = loaded.value.nextCursor)
                            is Loaded.Stale -> content(loaded.value.items, stale = true, cursor = null)
                            is Loaded.Failed -> DirectoryUiState.Error(AppErrorText.of(loaded.error))
                        }
                    }
                }
            }
        }
    }

    /** The next page, asked for with the cursor and the query the first page was fetched with. */
    fun loadMore() {
        val current = _state.value as? DirectoryUiState.Content ?: return
        val cursor = nextCursor ?: return
        val query = query ?: return
        if (current.loadingMore) return
        _state.value = current.copy(loadingMore = true, moreError = null)
        viewModelScope.launch {
            runCatching { directory.nextPage(query, cursor, current.values.size) }
                .onSuccess { page ->
                    nextCursor = page.nextCursor
                    _state.value = current.copy(
                        values = current.values + page.items,
                        hasMore = page.hasMore,
                        loadingMore = false,
                    )
                }
                .onFailure { failure ->
                    _state.value = current.copy(loadingMore = false, moreError = AppErrorText.of(failure.toAppError()))
                }
        }
    }

    private fun content(values: List<FacilitySummary>, stale: Boolean, cursor: String?): DirectoryUiState {
        nextCursor = cursor
        return DirectoryUiState.Content(values, filter, stale, hasMore = cursor != null)
    }
}
