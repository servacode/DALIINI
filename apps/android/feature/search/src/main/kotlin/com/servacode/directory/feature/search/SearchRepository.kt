package com.servacode.directory.feature.search

import com.servacode.directory.core.datastore.PreferencesRepository
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SearchRepository @Inject constructor(
    private val api: PublicApiBoundary,
    private val preferences: PreferencesRepository,
    private val locationProvider: LocationProvider,
) {
    suspend fun search(rawQuery: String): Result<List<FacilitySummary>> {
        val query = SearchQuery.normalize(rawQuery)
        if (query.isEmpty()) return Result.success(emptyList())
        val provinceId = preferences.values.first().selectedProvinceId
            ?: return Result.failure(IllegalStateException("PROVINCE_REQUIRED"))
        val fix = locationProvider.lastKnown()
        return runCatching { api.search(provinceId, query, fix?.latitude, fix?.longitude) }
    }
}

object SearchQuery {
    fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")
}
