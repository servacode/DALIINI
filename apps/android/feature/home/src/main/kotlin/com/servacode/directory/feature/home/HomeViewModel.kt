package com.servacode.directory.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.RealtimeInvalidation
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object ProvinceRequired : HomeUiState
    data class Content(val snapshot: HomeSnapshot, val stale: Boolean) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

/**
 * What a chosen quick filter is showing.
 *
 * It sits beside the snapshot rather than replacing it, so leaving a filter puts the user back
 * where they were without another round trip.
 */
data class QuickFilterState(
    val filter: HomeQuickFilter,
    val items: List<FacilitySummary> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadHome: HomeUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** The place the header shows. Null until the first answer; the header shows nothing then. */
    private val _place = MutableStateFlow<HomePlace?>(null)
    val place: StateFlow<HomePlace?> = _place.asStateFlow()

    /** Null when the user is looking at Home itself rather than at one of the quick filters. */
    private val _quickFilter = MutableStateFlow<QuickFilterState?>(null)
    val quickFilter: StateFlow<QuickFilterState?> = _quickFilter.asStateFlow()

    /** Whether "nearest" can be offered at all; the chip is not shown as choosable without it. */
    private val _hasLocation = MutableStateFlow(false)
    val hasLocation: StateFlow<Boolean> = _hasLocation.asStateFlow()

    private var loading: Job? = null
    private var filtering: Job? = null
    private var nextCursor: String? = null

    init {
        resolvePlace()
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                val province = (_state.value as? HomeUiState.Content)?.snapshot?.province?.id
                if (RealtimeInvalidation.refreshesProvinceLists(event, province)) refresh()
            }
        }
    }

    /**
     * Where the user is, asked once per start and after a permission changes. A failure leaves
     * the header as it was: the app never tells the user it does not know where they are when
     * it knew a moment ago.
     */
    fun resolvePlace() {
        viewModelScope.launch {
            _hasLocation.value = loadHome.hasLocation()
            _place.value = runCatching { loadHome.place() }.getOrNull() ?: _place.value
        }
    }

    /** Choose a quick filter, or the same one again to leave it. */
    fun select(filter: HomeQuickFilter?) {
        filtering?.cancel()
        nextCursor = null
        if (filter == null) {
            _quickFilter.value = null
            return
        }
        _quickFilter.value = QuickFilterState(filter = filter)
        filtering = viewModelScope.launch {
            val page = runCatching { loadHome.filtered(filter) }
            _quickFilter.value = page.fold(
                onSuccess = { result ->
                    nextCursor = result?.nextCursor
                    QuickFilterState(
                        filter = filter,
                        items = result?.items.orEmpty(),
                        loading = false,
                        hasMore = result?.hasMore ?: false,
                    )
                },
                onFailure = {
                    QuickFilterState(
                        filter = filter,
                        loading = false,
                        error = AppErrorText.of(it.toAppError()),
                    )
                },
            )
        }
    }

    /** The next page of the chosen filter, with the cursor the backend handed back. */
    fun loadMore() {
        val current = _quickFilter.value ?: return
        val cursor = nextCursor ?: return
        if (current.loadingMore || current.loading) return
        _quickFilter.value = current.copy(loadingMore = true, error = null)
        filtering = viewModelScope.launch {
            runCatching { loadHome.filtered(current.filter, cursor) }
                .onSuccess { page ->
                    nextCursor = page?.nextCursor
                    _quickFilter.value = current.copy(
                        items = current.items + page?.items.orEmpty(),
                        loadingMore = false,
                        hasMore = page?.hasMore ?: false,
                    )
                }
                .onFailure {
                    _quickFilter.value = current.copy(
                        loadingMore = false,
                        error = AppErrorText.of(it.toAppError()),
                    )
                }
        }
    }

    fun refresh() {
        loading?.cancel()
        loading = viewModelScope.launch {
            if (_state.value !is HomeUiState.Content) _state.value = HomeUiState.Loading
            loadHome().collect { load ->
                _state.value = when (load) {
                    HomeLoad.ProvinceRequired -> HomeUiState.ProvinceRequired
                    is HomeLoad.Snapshot -> when (val loaded = load.loaded) {
                        is Loaded.Cached -> HomeUiState.Content(loaded.value, stale = false)
                        is Loaded.Fresh -> HomeUiState.Content(loaded.value, stale = false)
                        is Loaded.Stale -> HomeUiState.Content(loaded.value, stale = true)
                        is Loaded.Failed -> HomeUiState.Error(AppErrorText.of(loaded.error))
                    }
                }
            }
        }
    }
}
