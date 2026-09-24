package com.servacode.directory.core.maps

/**
 * The little GeoJSON the map's own layers are fed.
 *
 * MapLibre will take either its own geometry types or a string, and the string is what is used
 * here for two reasons. It keeps the geometry classes — which ship as an artifact of their own
 * beside the map SDK — out of this module's compile classpath, and it makes the one part of the
 * drawing that can silently be wrong into plain logic that the platform-free harness can test.
 * A longitude and a latitude the wrong way round still draws a line; it just draws it in the
 * Indian Ocean.
 */
object GeoJson {
    /** GeoJSON is longitude first. Every bug in this file would be that, so it is written once. */
    private fun StringBuilder.coordinate(point: MapPoint): StringBuilder =
        append('[').append(point.longitude).append(',').append(point.latitude).append(']')

    fun lineString(points: List<MapPoint>): String = buildString {
        append("""{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[""")
        points.forEachIndexed { index, point ->
            if (index > 0) append(',')
            coordinate(point)
        }
        append("]}}")
    }

    fun point(point: MapPoint): String = buildString {
        append("""{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":""")
        coordinate(point)
        append("}}")
    }

    /** Nothing to draw, which is not the same as leaving the last thing drawn on the map. */
    val EMPTY: String = """{"type":"FeatureCollection","features":[]}"""
}
