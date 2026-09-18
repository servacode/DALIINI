package com.servacode.directory.feature.map

import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.PublicMapFacility
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Markers for the visible viewport, as the backend's geo query returns them. */
class MapRepository @Inject constructor(
    private val api: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
) {
    suspend fun load(viewport: MapViewport): Result<List<PublicMapFacility>> {
        val provinceId = preferences.values.first().selectedProvinceId
            ?: return Result.failure(AppException(AppError(AppError.Kind.VALIDATION, code = "PROVINCE_REQUIRED")))
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
