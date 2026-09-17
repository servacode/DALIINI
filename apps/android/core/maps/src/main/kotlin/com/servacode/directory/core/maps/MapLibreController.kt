package com.servacode.directory.core.maps

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
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
        pins.forEach { pin ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(pin.point.latitude, pin.point.longitude))
                    .title(pin.label)
                    .snippet(pin.facilityId),
            )
        }
    }

    override fun clearFacilities() {
        map.clear()
    }

    override fun showSelectionPoint(point: MapPoint) {
        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(LatLng(point.latitude, point.longitude))
                .title("selected"),
        )
    }

    override fun setOnPointSelected(listener: (MapPoint) -> Unit) {
        map.addOnMapClickListener { point ->
            listener(MapPoint(point.latitude, point.longitude))
            true
        }
    }
}
