package com.servacode.directory.core.maps

import com.servacode.directory.core.model.GeoPoint

data class MapPoint(val latitude: Double, val longitude: Double)

fun GeoPoint.toMapPoint() = MapPoint(latitude, longitude)

data class MapCamera(val center: MapPoint, val zoom: Double, val bearing: Double = 0.0)

data class FacilityMapPin(
    val facilityId: String,
    val point: MapPoint,
    val label: String,
)

interface MapController {
    /** Where the map is looking now, or null before it has a position. */
    val camera: MapCamera?
    fun moveCamera(camera: MapCamera, animated: Boolean = true)

    /** Replaces the facility markers; the one with [selectedFacilityId] is shown selected. */
    fun showFacilities(pins: List<FacilityMapPin>, selectedFacilityId: String? = null)
    fun clearFacilities()

    /** A tap on a facility marker, or on the name shown above a selected one. */
    fun setOnFacilitySelected(listener: (String) -> Unit)
    fun showSelectionPoint(point: MapPoint)
    fun setOnPointSelected(listener: (MapPoint) -> Unit)
    fun showRoute(points: List<MapPoint>, colorArgb: Int)
    fun showNavigationLocation(point: MapPoint)
}

object MapStyle {
    /** A production build ships a placeholder until a map provider is configured. */
    fun isConfigured(styleUrl: String): Boolean =
        styleUrl.startsWith("https://") && !styleUrl.contains("<ROOT_DOMAIN>")
}
