package com.servacode.directory.feature.facility

import com.servacode.directory.core.database.PublicCacheDataSource
import com.servacode.directory.core.datastore.PreferencesRepository
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface FacilityLoadResult {
    data class Content(val value: FacilityDetail, val stale: Boolean) : FacilityLoadResult
    data object Unavailable : FacilityLoadResult
}

class FacilityRepository @Inject constructor(
    private val cache: PublicCacheDataSource,
    private val api: PublicApiBoundary,
    private val preferences: PreferencesRepository,
) {
    suspend fun load(id: String): FacilityLoadResult {
        val cached = cache.facility(id)
        val provinceId = preferences.values.first().selectedProvinceId
        return runCatching { api.facility(id) }.fold(
            onSuccess = { detail ->
                if (provinceId != null) cache.putFacility(detail, provinceId)
                FacilityLoadResult.Content(detail, stale = false)
            },
            onFailure = {
                cached?.let { FacilityLoadResult.Content(it, stale = true) }
                    ?: FacilityLoadResult.Unavailable
            },
        )
    }
}
