package com.servacode.directory.feature.map

import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.fixWithoutPrompt
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.GeoPoint
import com.servacode.directory.core.model.PublicMapFacility
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first

/** Markers for the visible viewport, as the backend's geo query returns them, and where the map starts. */
class MapRepository @Inject constructor(
    private val api: PublicApiBoundary,
    private val cache: PublicCache,
    private val preferences: DirectoryPreferencesStore,
    private val location: LocationProvider,
) {
    suspend fun load(viewport: MapViewport, filters: MapFilters): Result<List<PublicMapFacility>> {
        val provinceId = preferences.values.first().selectedProvinceId
            ?: return Result.failure(AppException(AppError(AppError.Kind.VALIDATION, code = "PROVINCE_REQUIRED")))
        return runCatching {
            api.mapFacilities(
                provinceId = provinceId,
                west = viewport.west,
                south = viewport.south,
                east = viewport.east,
                north = viewport.north,
                categoryId = filters.categoryId,
                openNow = filters.openNow,
                dutyNow = filters.dutyNow,
            )
        }
    }

    /**
     * The categories this province actually serves, for the rail beside the map.
     *
     * The backend's own taxonomy, never a list written into the app: when a province starts
     * serving clinics or laboratories they appear here without a release.
     */
    suspend fun categories(): List<Category> {
        val provinceId = preferences.values.first().selectedProvinceId ?: return emptyList()
        return runCatching { api.categories(provinceId) }
            // Offline, the home snapshot already cached for this province carries the same list.
            .getOrElse { cache.home(provinceId)?.categories.orEmpty() }
    }

    /** Where a facility is: the copy its detail screen cached, else the backend's. */
    suspend fun facilityPoint(id: String): GeoPoint? {
        val detail = cache.facility(id) ?: runCatching { api.facility(id) }.getOrNull() ?: return null
        val latitude = detail.latitude ?: return null
        val longitude = detail.longitude ?: return null
        return GeoPoint(latitude, longitude)
    }

    /** The user's position when the app may already read it. Never asks for the permission. */
    suspend fun userPoint(): GeoPoint? =
        location.fixWithoutPrompt()?.let { GeoPoint(it.latitude, it.longitude) }

    /**
     * The selected province's map centre. The backend's list first, because a cached one from
     * an older build carries no centre; the cached list when the backend cannot be reached.
     */
    suspend fun provinceCenter(): GeoPoint? {
        val id = preferences.values.first().selectedProvinceId ?: return null
        val provinces = runCatching { api.provinces() }.getOrNull() ?: cache.provinces()
        return provinces.firstOrNull { it.id == id }?.mapCenter
    }
}
