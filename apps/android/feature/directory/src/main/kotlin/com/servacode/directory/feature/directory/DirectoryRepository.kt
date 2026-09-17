package com.servacode.directory.feature.directory

import com.servacode.directory.core.database.PublicCacheDataSource
import com.servacode.directory.core.datastore.PreferencesRepository
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class DirectoryFilter(
    val openNow: Boolean = false,
    val dutyNow: Boolean = false,
)

sealed interface DirectoryLoadResult {
    data class Content(val values: List<FacilitySummary>, val stale: Boolean) : DirectoryLoadResult
    data object ProvinceRequired : DirectoryLoadResult
    data object Unavailable : DirectoryLoadResult
}

class DirectoryRepository @Inject constructor(
    private val cache: PublicCacheDataSource,
    private val api: PublicApiBoundary,
    private val preferences: PreferencesRepository,
    private val locationProvider: LocationProvider,
) {
    suspend fun load(categoryId: String, filter: DirectoryFilter): DirectoryLoadResult {
        val provinceId = preferences.values.first().selectedProvinceId
            ?: return DirectoryLoadResult.ProvinceRequired
        val cached = cache.directory(provinceId, categoryId)
        val fix = locationProvider.lastKnown()
        return runCatching {
            api.directory(
                provinceId = provinceId,
                categoryId = categoryId,
                openNow = filter.openNow,
                dutyNow = filter.dutyNow,
                latitude = fix?.latitude,
                longitude = fix?.longitude,
            ).also { cache.putDirectory(it, provinceId, categoryId) }
        }.fold(
            onSuccess = { DirectoryLoadResult.Content(it, stale = false) },
            onFailure = {
                if (cached.isNotEmpty()) DirectoryLoadResult.Content(cached, stale = true)
                else DirectoryLoadResult.Unavailable
            },
        )
    }
}
