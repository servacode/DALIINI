package com.servacode.directory.core.maps

import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class MapLibreController(
    private val map: MapLibreMap,
) : MapController {
    private var navigationMarker: Marker? = null
    private var routePolyline: Polyline? = null

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
        navigationMarker = null
        routePolyline = null
    }

    override fun showSelectionPoint(point: MapPoint) {
        map.clear()
        navigationMarker = null
        routePolyline = null
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

    override fun showRoute(points: List<MapPoint>, colorArgb: Int) {
        routePolyline?.let(map::removePolyline)
        if (points.size < 2) return
        routePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(points.map { LatLng(it.latitude, it.longitude) })
                .color(colorArgb)
                .width(6f),
        )
    }

    override fun showNavigationLocation(point: MapPoint) {
        val latLng = LatLng(point.latitude, point.longitude)
        val existing = navigationMarker
        if (existing == null) {
            navigationMarker = map.addMarker(
                MarkerOptions().position(latLng).title("current-location"),
            )
        } else {
            existing.position = latLng
        }
    }
}
