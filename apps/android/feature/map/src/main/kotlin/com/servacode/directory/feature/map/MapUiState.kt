package com.servacode.directory.feature.map

import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.model.PublicMapFacility

/**
 * What the map keeps while its view comes and goes (INT-093).
 *
 * The MapView is released whenever another screen opens on top of the map. This is what lets
 * the next one show the same map without asking again: where it was looking, which facility was
 * selected, the markers and the viewport they were loaded for.
 */
data class MapUiState(
    /** False until the start is known; the map is not shown before, so it never opens on the world. */
    val cameraResolved: Boolean = false,
    /** Where a new view is put: the start, then wherever the user last left the map. */
    val camera: MapCamera? = null,
    val facilities: List<PublicMapFacility> = emptyList(),
    val loadedViewport: MapViewport? = null,
    val selectedFacilityId: String? = null,
) {
    /** False when [viewport]'s markers are already here, as after coming back to the map. */
    fun needsLoad(viewport: MapViewport): Boolean = !viewport.sameAs(loadedViewport)
}

/** A camera as a saved-state value, so it also survives the process being killed in the background. */
internal fun MapCamera.toSaved(): DoubleArray = doubleArrayOf(center.latitude, center.longitude, zoom, bearing)

internal fun DoubleArray.toCamera(): MapCamera? =
    if (size == 4) MapCamera(MapPoint(this[0], this[1]), zoom = this[2], bearing = this[3]) else null
