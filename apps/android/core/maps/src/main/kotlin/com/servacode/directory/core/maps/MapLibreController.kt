package com.servacode.directory.core.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
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
    /**
     * Needed to draw a section's mark into a pin. Null keeps the plain teardrop, which is what
     * a map of one kind of thing wants anyway.
     */
    private val context: Context? = null,
) : MapController {
    private var navigationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var destinationMarker: Marker? = null

    /** The route and the person, drawn as style layers; see [NavigationLayers]. */
    private val layers = NavigationLayers(map)

    /** Marker id to facility id. The facility id is not put in the marker, which would show it. */
    private val facilityMarkers = mutableMapOf<Long, String>()

    override val camera: MapCamera?
        get() {
            val position = map.cameraPosition
            val target = position.target ?: return null
            return MapCamera(MapPoint(target.latitude, target.longitude), position.zoom, position.bearing)
        }

    /**
     * The pin a section wears, drawn once per section and kept.
     *
     * A hundred markers of one kind would otherwise build a hundred identical bitmaps, and the
     * map stutters while they are made.
     */
    private val icons = mutableMapOf<Int, Icon>()

    private fun markerIcon(@DrawableRes iconRes: Int?): Icon? {
        val resource = iconRes ?: return null
        val host = context ?: return null
        return icons.getOrPut(resource) { IconFactory.getInstance(host).fromBitmap(pinBitmap(host, resource)) }
    }

    /**
     * A round white pin with the section's mark inside it.
     *
     * The mark is what the reader recognises; the disc is what makes it legible against a map
     * of pale streets and green parks, and the point below it is what says "here, exactly".
     */
    private fun pinBitmap(host: Context, @DrawableRes iconRes: Int): Bitmap {
        val size = PIN_SIZE
        val bitmap = Bitmap.createBitmap(size, size + PIN_TAIL, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centre = size / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        canvas.drawCircle(centre, centre, centre - 2f, paint)
        // The tip, so the pin points at its own coordinate rather than hovering over it.
        val tip = Path().apply {
            moveTo(centre - 14f, centre + 24f)
            lineTo(centre, (size + PIN_TAIL).toFloat() - 2f)
            lineTo(centre + 14f, centre + 24f)
            close()
        }
        canvas.drawPath(tip, paint)
        paint.color = PIN_RING
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(centre, centre, centre - 4f, paint)

        val drawable = ContextCompat.getDrawable(host, iconRes)
        if (drawable != null) {
            val inset = size / 4
            drawable.setBounds(inset, inset, size - inset, size - inset)
            drawable.draw(canvas)
        }
        return bitmap
    }

    /** A step of scale, about where the map is already looking. */
    fun zoomBy(steps: Double) {
        val current = camera ?: return
        moveCamera(current.copy(zoom = current.zoom + steps), animated = true)
    }

    override fun moveCamera(camera: MapCamera, animated: Boolean, durationMillis: Int) {
        val position = CameraPosition.Builder()
            .target(LatLng(camera.center.latitude, camera.center.longitude))
            .zoom(camera.zoom)
            .bearing(camera.bearing)
            .tilt(camera.tilt)
            .build()
        val update = CameraUpdateFactory.newCameraPosition(position)
        if (animated) map.animateCamera(update, durationMillis) else map.moveCamera(update)
    }

    /**
     * Told when a hand moves the map, and not when the app does.
     *
     * A navigator that keeps dragging the view back to the driver while they are looking ahead
     * is a navigator people stop trusting. This is how the screen knows to stop following until
     * it is asked to resume.
     */
    fun onUserMovedMap(listener: () -> Unit) {
        map.addOnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) listener()
        }
    }

    override fun showFacilities(pins: List<FacilityMapPin>, selectedFacilityId: String?) {
        clearFacilities()
        pins.forEach { pin ->
            val options = MarkerOptions()
                .position(LatLng(pin.point.latitude, pin.point.longitude))
                .title(pin.label)
            markerIcon(pin.iconRes)?.let(options::icon)
            val marker = map.addMarker(options)
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

    /**
     * The way there, drawn the way this mode is drawn, and the person on it.
     *
     * Replaces [showRoute] for guidance: it can break the line for a walk and turn the mark to a
     * heading, and it changes two sources in place rather than rebuilding the whole overlay every
     * second, which is what made the map stutter under real readings.
     */
    fun showGuidance(
        points: List<MapPoint>,
        colorArgb: Int,
        stroke: RouteStroke,
        user: MapPoint?,
        bearingDegrees: Float,
        mark: UserMark,
    ) {
        layers.showRoute(points, colorArgb, stroke)
        user?.let { layers.showUser(it, bearingDegrees, mark) }
    }

    /**
     * The ways there being offered and not followed, drawn under the one that is.
     *
     * Kept apart from [showGuidance] because they change on a different clock: the route
     * ahead is redrawn on every reading, while what else was on offer changes only when a
     * new set of routes arrives.
     */
    fun showAlternatives(lines: List<LabelledLine>, colorArgb: Int) {
        layers.showAlternatives(lines, colorArgb)
    }

    /**
     * Press one of the other ways there to take it.
     *
     * The press is answered only when it lands on a way; otherwise it is passed on, so panning
     * and everything else the map does with a touch still works.
     */
    fun setOnAlternativeSelected(listener: (Int) -> Unit) {
        map.addOnMapClickListener { point ->
            val chosen = layers.alternativeAt(map.projection.toScreenLocation(point))
            if (chosen == null) {
                false
            } else {
                listener(chosen)
                true
            }
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

    private companion object {
        const val PIN_SIZE = 96
        const val PIN_TAIL = 26
        val PIN_RING = 0xFF042623.toInt()
    }
}
