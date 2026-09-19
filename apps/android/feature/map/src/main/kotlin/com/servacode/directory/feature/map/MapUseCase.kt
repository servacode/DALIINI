package com.servacode.directory.feature.map

import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapCameraPolicy
import com.servacode.directory.core.maps.toMapPoint
import com.servacode.directory.core.model.PublicMapFacility
import javax.inject.Inject

class MapUseCase @Inject constructor(
    private val repository: MapRepository,
) {
    suspend operator fun invoke(viewport: MapViewport): Result<List<PublicMapFacility>> = repository.load(viewport)
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
