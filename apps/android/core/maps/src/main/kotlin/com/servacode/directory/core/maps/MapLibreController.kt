package com.servacode.directory.core.maps

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class MapLibreController(
    private val map: MapLibreMap,
) : MapController {
    override fun moveCamera(camera: MapCamera, animated: Boolean) {
        val position = CameraPosition.Builder()
            .target(LatLng(camera.center.latitude, camera.center.longitude))
            .zoom(camera.zoom)
            .build()
        val update = CameraUpdateFactory.newCameraPosition(position)
        if (animated) map.animateCamera(update) else map.moveCamera(update)
    }

    override fun showFacilities(pins: List<FacilityMapPin>) {
        // P15 owns visual marker rendering. Foundation keeps MapLibre behind this boundary.
    }

    override fun clearFacilities() = Unit
}
