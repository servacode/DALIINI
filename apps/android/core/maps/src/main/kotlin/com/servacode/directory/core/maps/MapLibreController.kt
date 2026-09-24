package com.servacode.directory.core.maps

import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

/** One per map: it remembers which marker is which facility. */
class MapLibreController(
    private val map: MapLibreMap,
) : MapController {
    private var navigationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var destinationMarker: Marker? = null

    /** Marker id to facility id. The facility id is not put in the marker, which would show it. */
    private val facilityMarkers = mutableMapOf<Long, String>()

    override val camera: MapCamera?
        get() {
            val position = map.cameraPosition
            val target = position.target ?: return null
            return MapCamera(MapPoint(target.latitude, target.longitude), position.zoom, position.bearing)
        }

    /** A step of scale, about where the map is already looking. */
    fun zoomBy(steps: Double) {
        val current = camera ?: return
        moveCamera(current.copy(zoom = current.zoom + steps), animated = true)
    }

    override fun moveCamera(camera: MapCamera, animated: Boolean) {
        val position = CameraPosition.Builder()
            .target(LatLng(camera.center.latitude, camera.center.longitude))
            .zoom(camera.zoom)
            .bearing(camera.bearing)
            .build()
        val update = CameraUpdateFactory.newCameraPosition(position)
        if (animated) map.animateCamera(update) else map.moveCamera(update)
    }

    override fun showFacilities(pins: List<FacilityMapPin>, selectedFacilityId: String?) {
        clearFacilities()
        pins.forEach { pin ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(pin.point.latitude, pin.point.longitude))
                    .title(pin.label),
            )
            facilityMarkers[marker.id] = pin.facilityId
            // Selecting opens the marker's name above it.
            if (pin.facilityId == selectedFacilityId) map.selectMarker(marker)
        }
    }

    override fun clearFacilities() {
        map.clear()
        facilityMarkers.clear()
        navigationMarker = null
        routePolyline = null
        destinationMarker = null
    }

    override fun setOnFacilitySelected(listener: (String) -> Unit) {
        map.setOnMarkerClickListener { marker ->
            val facilityId = facilityMarkers[marker.id] ?: return@setOnMarkerClickListener false
            map.selectMarker(marker)
            listener(facilityId)
            true
        }
        map.setOnInfoWindowClickListener { marker ->
            val facilityId = facilityMarkers[marker.id] ?: return@setOnInfoWindowClickListener false
            listener(facilityId)
            true
        }
    }

    override fun showSelectionPoint(point: MapPoint) {
        map.clear()
        facilityMarkers.clear()
        navigationMarker = null
        routePolyline = null
        destinationMarker = null
        // No title: the pin itself is the answer, and its tip is the exact point.
        map.addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)))
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

    /**
     * The whole way on screen at once, with room left at the bottom for the card over the map.
     *
     * A route preview centred on where the person is standing shows them the first street and
     * hides the rest; what they came to see is how far it is and which way it goes.
     */
    fun frameRoute(points: List<MapPoint>, sidePaddingPx: Int, bottomPaddingPx: Int) {
        if (points.size < 2) return
        val bounds = LatLngBounds.Builder()
            .includes(points.map { LatLng(it.latitude, it.longitude) })
            .build()
        // The map has to be measured before bounds can be turned into a camera. If it is not
        // yet, the midpoint keeps both ends roughly in view until the next frame asks again.
        runCatching {
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    sidePaddingPx,
                    sidePaddingPx,
                    sidePaddingPx,
                    bottomPaddingPx,
                ),
            )
        }.onFailure {
            moveCamera(
                MapCamera(MapPoint(bounds.center.latitude, bounds.center.longitude), zoom = 13.0),
                animated = false,
            )
        }
    }

    /** Where they are going, marked, so the end of the line is a place and not a line's end. */
    fun showDestination(point: MapPoint, label: String?) {
        destinationMarker?.let(map::removeMarker)
        val options = MarkerOptions().position(LatLng(point.latitude, point.longitude))
        if (!label.isNullOrBlank()) options.title(label)
        destinationMarker = map.addMarker(options)
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
