package com.servacode.directory.core.maps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/** How the way there is drawn, which is not the same for someone walking it. */
enum class RouteStroke {
    /** One unbroken line: a vehicle is bound to the road it is on. */
    SOLID,

    /**
     * A line of dots.
     *
     * Walking is not bound to the carriageway — the way runs along pavements, across squares and
     * through gaps no car takes — so a solid line claims a precision the route does not have.
     * Every map that shows walking shows it broken, and people read it that way already.
     */
    DOTTED,
}

/** The mark that stands where the person is. */
enum class UserMark {
    /** A triangle that points the way they are facing. */
    ARROW,

    /**
     * A walking figure, for someone on foot.
     *
     * It does not turn. A heading on foot is the shoulders, not the way: a person crossing a
     * square reads as facing four directions in ten seconds, and an arrow that spins is worse
     * than a mark that simply stands where they are.
     */
    WALKER,
}

/**
 * The route and the person, drawn as style layers rather than as annotations.
 *
 * The annotation API cannot draw a broken line and cannot turn a marker to a heading, and it
 * redraws the whole overlay on every change, which is what made the map stutter once a reading
 * arrived every second. These are two sources and three layers, updated in place.
 */
internal class NavigationLayers(private val map: MapLibreMap) {
    private var installedStyle: Style? = null

    /**
     * Put the layers on the style, once per style.
     *
     * Returns false when the style is not ready, so the caller can try again on the next frame
     * rather than dropping the route.
     */
    private fun style(): Style? {
        val style = map.style?.takeIf { it.isFullyLoaded } ?: return null
        if (installedStyle === style) return style
        installedStyle = style
        runCatching {
            style.addSource(GeoJsonSource(ROUTE_SOURCE))
            style.addSource(GeoJsonSource(USER_SOURCE))
            style.addImage(ARROW_IMAGE, arrowBitmap())
            style.addImage(WALKER_IMAGE, walkerBitmap())
            // A casing under the line, so the route reads against both a pale street and a park.
            style.addLayer(
                LineLayer(ROUTE_CASING_LAYER, ROUTE_SOURCE).withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineWidth(CASING_WIDTH),
                    PropertyFactory.lineColor(Color.WHITE),
                    PropertyFactory.lineOpacity(0.9f),
                ),
            )
            style.addLayer(
                LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineWidth(LINE_WIDTH),
                ),
            )
            style.addLayer(
                SymbolLayer(USER_LAYER, USER_SOURCE).withProperties(
                    PropertyFactory.iconImage(ARROW_IMAGE),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    // Turned with the map, not with the screen: the arrow points down the street.
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                ),
            )
        }
        return style
    }

    fun showRoute(points: List<MapPoint>, colorArgb: Int, stroke: RouteStroke) {
        val style = style() ?: return
        val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE) ?: return
        if (points.size < 2) {
            source.setGeoJson(GeoJson.EMPTY)
            return
        }
        source.setGeoJson(GeoJson.lineString(points))
        val layer = style.getLayer(ROUTE_LAYER) as? LineLayer ?: return
        layer.setProperties(
            PropertyFactory.lineColor(colorArgb),
            // The dash pattern is in line widths, not pixels, so it holds its look at every zoom.
            PropertyFactory.lineDasharray(
                if (stroke == RouteStroke.DOTTED) DOTTED_PATTERN else SOLID_PATTERN,
            ),
        )
    }

    fun showUser(point: MapPoint, bearingDegrees: Float, mark: UserMark) {
        val style = style() ?: return
        val source = style.getSourceAs<GeoJsonSource>(USER_SOURCE) ?: return
        source.setGeoJson(GeoJson.point(point))
        val layer = style.getLayer(USER_LAYER) as? SymbolLayer ?: return
        layer.setProperties(
            PropertyFactory.iconImage(if (mark == UserMark.WALKER) WALKER_IMAGE else ARROW_IMAGE),
            PropertyFactory.iconRotate(if (mark == UserMark.WALKER) 0f else bearingDegrees),
        )
    }

    fun clear() {
        val style = installedStyle ?: return
        runCatching {
            style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.setGeoJson(GeoJson.EMPTY)
            style.getSourceAs<GeoJsonSource>(USER_SOURCE)?.setGeoJson(GeoJson.EMPTY)
        }
    }

    private companion object {
        const val ROUTE_SOURCE = "directory-route"
        const val USER_SOURCE = "directory-user"
        const val ROUTE_CASING_LAYER = "directory-route-casing"
        const val ROUTE_LAYER = "directory-route-line"
        const val USER_LAYER = "directory-user-mark"
        const val ARROW_IMAGE = "directory-user-arrow"
        const val WALKER_IMAGE = "directory-user-walker"

        const val LINE_WIDTH = 7f
        const val CASING_WIDTH = 11f

        /** Round caps turn each short dash into a dot. */
        val DOTTED_PATTERN = arrayOf(0.05f, 1.6f)
        val SOLID_PATTERN = arrayOf(1f, 0f)

        /** A triangle, drawn rather than shipped, so it takes its colours from the tokens. */
        fun arrowBitmap(): Bitmap {
            val size = 72
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val centre = size / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            // A white disc under it, the way every map does, so the mark reads on any tile.
            paint.color = Color.WHITE
            canvas.drawCircle(centre, centre, centre - 2f, paint)
            paint.color = ACCENT
            canvas.drawCircle(centre, centre, centre - 7f, paint)
            paint.color = Color.WHITE
            val arrow = Path().apply {
                moveTo(centre, 14f)
                lineTo(centre + 15f, centre + 18f)
                lineTo(centre, centre + 9f)
                lineTo(centre - 15f, centre + 18f)
                close()
            }
            canvas.drawPath(arrow, paint)
            return bitmap
        }

        /** A walking figure inside the same disc the arrow wears, so the two read as a pair. */
        fun walkerBitmap(): Bitmap {
            val size = 72
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val centre = size / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.WHITE
            canvas.drawCircle(centre, centre, centre - 2f, paint)
            paint.color = ACCENT
            canvas.drawCircle(centre, centre, centre - 7f, paint)

            // Drawn rather than shipped, so it takes the same colours as everything else here.
            paint.color = Color.WHITE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            canvas.drawCircle(centre + 2f, centre - 13f, 4.5f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            // Body to hip, then the leading leg; then the trailing leg and the swinging arm.
            val stride = Path().apply {
                moveTo(centre + 1f, centre - 7f)
                lineTo(centre - 3f, centre + 3f)
                lineTo(centre + 5f, centre + 9f)
                lineTo(centre + 7f, centre + 20f)
            }
            canvas.drawPath(stride, paint)
            val trailing = Path().apply {
                moveTo(centre - 3f, centre + 3f)
                lineTo(centre - 10f, centre + 12f)
                lineTo(centre - 11f, centre + 20f)
            }
            canvas.drawPath(trailing, paint)
            canvas.drawLine(centre + 1f, centre - 4f, centre + 10f, centre + 1f, paint)
            return bitmap
        }

        /** The brand's deep green, the same value the tokens carry. */
        const val ACCENT = 0xFF042623.toInt()
    }
}
