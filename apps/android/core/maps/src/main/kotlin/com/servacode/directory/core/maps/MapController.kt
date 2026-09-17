package com.servacode.directory.core.maps

data class MapPoint(val latitude: Double, val longitude: Double)
data class MapCamera(val center: MapPoint, val zoom: Double)

data class FacilityMapPin(
    val facilityId: String,
    val point: MapPoint,
    val label: String,
)

interface MapController {
    fun moveCamera(camera: MapCamera, animated: Boolean = true)
    fun showFacilities(pins: List<FacilityMapPin>)
    fun clearFacilities()
    fun showSelectionPoint(point: MapPoint)
    fun setOnPointSelected(listener: (MapPoint) -> Unit)
}
