package com.servacode.directory.feature.search

import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first

/** One search, fixed at its first page so that later pages continue the same ordering. */
data class SearchContext(
    val provinceId: String,
    val query: String,
    val latitude: Double?,
    val longitude: Double?,
)

/**
 * The backend is the search: matching, ranking and paging happen there. The app only
 * normalises the text it sends and never filters results locally.
 */
class SearchRepository @Inject constructor(
    private val api: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
    private val locationProvider: LocationProvider,
) {
    /** Null context for an empty query: nothing is sent. Throws AppException on failure. */
    suspend fun first(rawQuery: String): Pair<SearchContext, Page<FacilitySummary>>? {
        val query = SearchQuery.normalize(rawQuery)
        if (query.isEmpty()) return null
        val provinceId = preferences.values.first().selectedProvinceId
            ?: throw AppException(AppError(AppError.Kind.VALIDATION, code = "PROVINCE_REQUIRED"))
        val fix = locationProvider.lastKnown()
        val context = SearchContext(provinceId, query, fix?.latitude, fix?.longitude)
        return context to next(context, cursor = null)
    }

    suspend fun next(context: SearchContext, cursor: String?): Page<FacilitySummary> =
        api.search(context.provinceId, context.query, context.latitude, context.longitude, cursor)
}

object SearchQuery {
    fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")
}
