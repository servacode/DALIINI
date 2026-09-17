package com.servacode.directory.feature.map

import com.servacode.directory.core.datastore.PreferencesRepository
import com.servacode.directory.core.model.PublicMapFacility
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MapRepository @Inject constructor(
    private val api: PublicApiBoundary,
    private val preferences: PreferencesRepository,
) {
    suspend fun load(viewport: MapViewport): Result<List<PublicMapFacility>> {
        val provinceId = preferences.values.first().selectedProvinceId
            ?: return Result.failure(IllegalStateException("PROVINCE_REQUIRED"))
        return runCatching {
            api.mapFacilities(
                provinceId = provinceId,
                west = viewport.west,
                south = viewport.south,
                east = viewport.east,
                north = viewport.north,
            )
        }
    }
}
