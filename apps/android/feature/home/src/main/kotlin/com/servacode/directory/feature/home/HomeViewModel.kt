package com.servacode.directory.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.RealtimeInvalidation
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object ProvinceRequired : HomeUiState
    data class Content(val snapshot: HomeSnapshot, val stale: Boolean) : HomeUiState
    data class Error(val error: AppError) : HomeUiState
}

/**
 * The list under the chips.
 *
 * One list, always — not a snapshot that a filter sometimes replaces. "All" is simply the
 * query with nothing narrowed, which keeps one code path for paging, for errors and for the
 * empty state instead of two that drift apart.
 */
data class HomeListState(
    val items: List<FacilitySummary> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: AppError? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadHome: HomeUseCase,
    private val loadAds: HomeAdsUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** The place the header shows. Null until the first answer; the header shows nothing then. */
    private val _place = MutableStateFlow<HomePlace?>(null)
    val place: StateFlow<HomePlace?> = _place.asStateFlow()

    private val _filters = MutableStateFlow(HomeFilters())
    val filters: StateFlow<HomeFilters> = _filters.asStateFlow()

    /** The chosen category. Null only until the snapshot names the province's categories. */
    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    /**
     * The specialties and services the chosen category lets its list be narrowed by, for the rows
     * under the chips. Empty draws no rows: a category that does not filter by them, one that has
     * none, and every failure to read them.
     */
    private val _tags = MutableStateFlow(CategoryTags())
    val tags: StateFlow<CategoryTags> = _tags.asStateFlow()
    private var loadingTags: Job? = null

    private val _list = MutableStateFlow(HomeListState())
    val list: StateFlow<HomeListState> = _list.asStateFlow()

    /** Whether "nearest" can be offered at all; the chip is not drawn without it. */
    private val _hasLocation = MutableStateFlow(false)
    val hasLocation: StateFlow<Boolean> = _hasLocation.asStateFlow()

    /** Unread messages behind the bell. Zero draws no badge at all. */
    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread.asStateFlow()

    /**
     * The slider's ads, from `public/ads` for the province on screen. Empty hides the slider,
     * which is also what every failure shows: an advertisement is never worth an error.
     */
    private val _ads = MutableStateFlow<List<HomeAd>>(emptyList())
    val ads: StateFlow<List<HomeAd>> = _ads.asStateFlow()
    private var adsProvince: String? = null
    private var loadingAds: Job? = null

    private var loading: Job? = null
    private var listing: Job? = null
    private var nextCursor: String? = null

    init {
        resolvePlace()
        // And then keeps answering: the label is where the reader is, not where they were when
        // the app started. A move also re-orders a list sorted by distance.
        viewModelScope.launch {
            loadHome.placeUpdates().collect { here ->
                val moved = here.provinceId != _place.value?.provinceId
                _place.value = here
                if (moved) refresh() else reload()
            }
        }
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
            _unread.value = loadHome.unreadMessages()
            // A position arriving after the first list would otherwise leave it ordered by
            // name with no distances, so the list is asked again once — not on every fix.
            if (_hasLocation.value && _list.value.items.none { it.distanceMeters != null }) {
                reload()
            }
        }
    }

    /** Pick a category. Its own capabilities decide which chips remain offered. */
    fun select(category: Category?) {
        if (category?.id == _category.value?.id) return
        _category.value = category
        // A specialty or a service was chosen among one category's own; it goes with it.
        _filters.value = _filters.value.clearTags().withinReach(_hasLocation.value, category)
        refreshTags()
        reload()
    }

    /** Turn one chip on or off. "All" clears the rest. */
    fun toggle(chip: HomeChip) {
        _filters.value = _filters.value.toggle(chip).withinReach(_hasLocation.value, _category.value)
        reload()
    }

    /** One specialty, or every one again with null or with the one already chosen. */
    fun chooseSpecialty(id: String?) = narrow { it.chooseSpecialty(id) }

    /** One service, the same way. */
    fun chooseService(id: String?) = narrow { it.chooseService(id) }

    /** «امسح التصفية»: every specialty and service again. */
    fun clearTags() = narrow { it.clearTags() }

    /** Change the filters, and ask for the list again only when the question actually changed. */
    private fun narrow(change: (HomeFilters) -> HomeFilters) {
        val next = change(_filters.value)
        if (next == _filters.value) return
        _filters.value = next
        reload()
    }

    /**
     * The newly chosen category's specialties and services, cached first.
     *
     * The previous category's rows go at once rather than showing under the wrong one. Nothing
     * is asked for a category whose capabilities filter by neither. Each answer also drops a
     * choice the category no longer offers, so the list is never narrowed by a missing chip.
     */
    private fun refreshTags() {
        loadingTags?.cancel()
        _tags.value = CategoryTags()
        val category = _category.value
        val provinceId = province() ?: return
        if (category == null || !filtersByTags(category)) return
        loadingTags = viewModelScope.launch {
            loadHome.tags(provinceId, category.id).collect { tags ->
                val offered = offeredTags(category, tags)
                _tags.value = offered
                narrow { it.withinTags(offered) }
            }
        }
    }

    /**
     * Start the list again from the backend's first page.
     *
     * Every filter or category change comes through here, so a cursor from the previous
     * question can never be used against the new one, and results never mix.
     */
    private fun reload() {
        listing?.cancel()
        nextCursor = null
        val provinceId = province() ?: return
        _list.value = HomeListState(loading = true)
        listing = viewModelScope.launch {
            runCatching { loadHome.filtered(provinceId, _category.value?.id, _filters.value) }
                .onSuccess { page ->
                    nextCursor = page.nextCursor
                    _list.value = HomeListState(
                        items = page.items,
                        loading = false,
                        hasMore = page.hasMore,
                    )
                }
                .onFailure {
                    _list.value = HomeListState(
                        loading = false,
                        error = it.toAppError(),
                    )
                }
        }
    }

    /** The next page, with the cursor the backend handed back for this exact question. */
    fun loadMore() {
        val current = _list.value
        val cursor = nextCursor ?: return
        val provinceId = province() ?: return
        if (current.loadingMore || current.loading) return
        _list.value = current.copy(loadingMore = true, error = null)
        listing = viewModelScope.launch {
            runCatching {
                loadHome.filtered(provinceId, _category.value?.id, _filters.value, cursor)
            }
                .onSuccess { page ->
                    nextCursor = page.nextCursor
                    _list.value = current.copy(
                        items = current.items + page.items,
                        loadingMore = false,
                        hasMore = page.hasMore,
                    )
                }
                .onFailure {
                    _list.value = current.copy(
                        loadingMore = false,
                        error = it.toAppError(),
                    )
                }
        }
    }

    private fun province(): String? = (_state.value as? HomeUiState.Content)?.snapshot?.province?.id

    fun refresh() {
        // A refresh asks for the ads again too; the snapshot decides for which province.
        adsProvince = null
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
                        is Loaded.Failed -> HomeUiState.Error(loaded.error)
                    }
                }
                adoptSnapshot()
            }
        }
    }

    /**
     * Take the province's own taxonomy from the snapshot the first time it arrives.
     *
     * The category is chosen here rather than in the interface so that the list has one before
     * anything is drawn; the first category the backend lists is the province's own order, not
     * a name this app decided to look for.
     */
    private fun adoptSnapshot() {
        val snapshot = (_state.value as? HomeUiState.Content)?.snapshot ?: return
        if (_category.value == null && snapshot.categories.isNotEmpty()) {
            _category.value = snapshot.categories.first()
            _filters.value = _filters.value.withinReach(_hasLocation.value, _category.value)
            refreshTags()
        }
        if (_list.value.items.isEmpty() && _list.value.error == null) reload()
        if (snapshot.province.id != adsProvince) refreshAds(snapshot.province.id)
    }

    private fun refreshAds(provinceId: String) {
        adsProvince = provinceId
        loadingAds?.cancel()
        loadingAds = viewModelScope.launch {
            loadAds(provinceId)
                .catch { emit(emptyList()) }
                .collect { _ads.value = it }
        }
    }
}
