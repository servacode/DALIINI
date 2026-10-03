package com.servacode.directory.feature.map

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapCameraPolicy
import com.servacode.directory.core.maps.toMapPoint
import com.servacode.directory.core.model.PublicMapFacility

class MapUseCase @Inject constructor(
    private val repository: MapRepository,
) {
    suspend operator fun invoke(
        viewport: MapViewport,
        filters: MapFilters = MapFilters(),
    ): Result<List<PublicMapFacility>> = repository.load(viewport, filters)

    /** The province's own categories, for the rail beside the map. */
    suspend fun categories() = repository.categories()

    /** Where the user is, when the app may already read it. Never asks for the permission. */
    suspend fun userPoint() = repository.userPoint()
}

/** Where the map opens, by [MapCameraPolicy]: each source is read only if the ones before it have nothing. */
class MapStartUseCase @Inject constructor(
    private val repository: MapRepository,
) {
    suspend operator fun invoke(focusFacilityId: String?, restored: MapCamera?): MapCamera? = MapCameraPolicy.forMap(
        restored = restored,
        facility = { focusFacilityId?.let { repository.facilityPoint(it) }?.toMapPoint() },
        user = { repository.userPoint()?.toMapPoint() },
        province = { repository.provinceCenter()?.toMapPoint() },
    )
}
