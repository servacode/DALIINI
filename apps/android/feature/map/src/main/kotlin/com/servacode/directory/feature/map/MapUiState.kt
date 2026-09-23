package com.servacode.directory.feature.map

import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.PublicMapFacility

/**
 * What the map keeps while its view comes and goes (INT-093).
 *
 * The MapView is released whenever another screen opens on top of the map. This is what lets
 * the next one show the same map without asking again: where it was looking, which facility was
 * selected, the markers and the viewport they were loaded for.
 */
/**
 * What the map is being asked to show.
 *
 * The same three questions Home asks, and one category at a time from the province's own
 * taxonomy. Both are sent to the backend: a map that filtered its own markers would answer
 * differently from a list asked the same thing.
 */
data class MapFilters(
    val categoryId: String? = null,
    val openNow: Boolean = false,
    val dutyNow: Boolean = false,
) {
    fun withCategory(id: String?) = copy(categoryId = if (categoryId == id) null else id)
    fun toggleOpenNow() = copy(openNow = !openNow, dutyNow = false)
    fun toggleDutyNow() = copy(dutyNow = !dutyNow, openNow = false)
}

data class MapUiState(
    /** False until the start is known; the map is not shown before, so it never opens on the world. */
    val cameraResolved: Boolean = false,
    /** Where a new view is put: the start, then wherever the user last left the map. */
    val camera: MapCamera? = null,
    val facilities: List<PublicMapFacility> = emptyList(),
    val loadedViewport: MapViewport? = null,
    val selectedFacilityId: String? = null,
    val filters: MapFilters = MapFilters(),
    /** The province's own categories, for the rail; empty until they are known. */
    val categories: List<Category> = emptyList(),
) {
    /**
     * False when [viewport]'s markers are already here, as after coming back to the map.
     *
     * A change of filter always needs a load, whatever the viewport: the markers on screen
     * answer a question the user has just changed.
     */
    fun needsLoad(viewport: MapViewport): Boolean = !viewport.sameAs(loadedViewport)
}

/** A camera as a saved-state value, so it also survives the process being killed in the background. */
internal fun MapCamera.toSaved(): DoubleArray = doubleArrayOf(center.latitude, center.longitude, zoom, bearing)

internal fun DoubleArray.toCamera(): MapCamera? =
    if (size == 4) MapCamera(MapPoint(this[0], this[1]), zoom = this[2], bearing = this[3]) else null
