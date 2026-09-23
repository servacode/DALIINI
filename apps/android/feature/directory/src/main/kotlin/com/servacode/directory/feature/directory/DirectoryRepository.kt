package com.servacode.directory.feature.directory

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.database.cacheFirst
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class DirectoryFilter(
    val openNow: Boolean = false,
    val dutyNow: Boolean = false,
    val search: String = "",
) {
    /** Only the unfiltered list is cached; a filtered one is a view of the moment. */
    val cacheable: Boolean get() = !openNow && !dutyNow && search.isBlank()
}

sealed interface DirectoryLoad {
    data object ProvinceRequired : DirectoryLoad

    /**
     * The first page. [query] is what the next pages must be asked with: the cursor encodes a
     * position in an ordering that depends on the location, so the location is fixed at the
     * first page and never re-resolved mid-list.
     */
    data class FirstPage(val query: DirectoryQuery, val loaded: Loaded<Page<FacilitySummary>>) : DirectoryLoad
}

class DirectoryRepository @Inject constructor(
    private val cache: PublicCache,
    private val api: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
    private val locationProvider: LocationProvider,
) {
    fun firstPage(categoryId: String, filter: DirectoryFilter): Flow<DirectoryLoad> = flow {
        val provinceId = preferences.values.first().selectedProvinceId
        if (provinceId == null) {
            emit(DirectoryLoad.ProvinceRequired)
            return@flow
        }
        val fix = locationProvider.lastKnown()
        val query = DirectoryQuery(
            provinceId = provinceId,
            categoryId = categoryId,
            openNow = filter.openNow,
            dutyNow = filter.dutyNow,
            search = filter.search.trim().ifBlank { null },
            latitude = fix?.latitude,
            longitude = fix?.longitude,
        )
        emitAll(
            cacheFirst(
                read = {
                    if (!filter.cacheable) null
                    else cache.directory(provinceId, categoryId).takeIf { it.isNotEmpty() }
                        // Offline there is no cursor to continue from.
                        ?.let { Page(items = it, nextCursor = null, hasMore = false) }
                },
                fetch = { api.directory(query) },
                write = { page -> if (filter.cacheable) cache.putDirectoryPage(page.items, provinceId, categoryId, 0) },
            ).map { DirectoryLoad.FirstPage(query, it) },
        )
    }

    /**
     * The page after [cursor]. `loadedCount` is how many rows are already shown, so a cached
     * copy keeps the backend's order across pages. Throws AppException on failure.
     */
    suspend fun nextPage(query: DirectoryQuery, cursor: String, loadedCount: Int): Page<FacilitySummary> {
        val page = api.directory(query, cursor)
        // Only a category's own unfiltered list is cached: the cache is keyed by category, and
        // a province-wide or filtered view is a view of the moment.
        val categoryId = query.categoryId
        val cacheable = categoryId != null && !query.openNow && !query.dutyNow && query.search == null
        if (cacheable) {
            runCatching { cache.putDirectoryPage(page.items, query.provinceId, categoryId, loadedCount) }
        }
        return page
    }
}
