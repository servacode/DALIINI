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

    /** Several lines as one thing to draw, for the ways there that were not taken. */
    fun lineStrings(lines: List<List<MapPoint>>): String = buildString {
        append("""{"type":"FeatureCollection","features":[""")
        lines.filter { it.size >= 2 }.forEachIndexed { index, points ->
            if (index > 0) append(',')
            append(lineString(points))
        }
        append("]}")
    }

    /**
     * The same, with something written on each line and a number it can be found by.
     *
     * A way drawn on a map is only an offer if it can be told apart and pressed: the key is what
     * a tap on the map reads back to know which way was chosen, and the label is what is written
     * along it — the minutes, because that is what anyone compares two roads by.
     */
    fun labelledLines(lines: List<LabelledLine>): String = buildString {
        append("""{"type":"FeatureCollection","features":[""")
        var written = 0
        for (line in lines) {
            if (line.points.size < 2) continue
            if (written > 0) append(',')
            written += 1
            append("""{"type":"Feature","properties":{"""")
            append(KEY_PROPERTY).append("""":""").append(line.key)
            append(""","""").append(LABEL_PROPERTY).append("""":"""")
            append(line.label.replace("\\", "").replace(""""""", ""))
            append(""""},"geometry":{"type":"LineString","coordinates":[""")
            line.points.forEachIndexed { index, point ->
                if (index > 0) append(',')
                coordinate(point)
            }
            append("]}}")
        }
        append("]}")
    }

    /** The property a tap reads to know which way was pressed. */
    const val KEY_PROPERTY = "choice"

    /** The property written along the line. */
    const val LABEL_PROPERTY = "label"

    /** Nothing to draw, which is not the same as leaving the last thing drawn on the map. */
    val EMPTY: String = """{"type":"FeatureCollection","features":[]}"""
}

/** One way there, as the map needs it: the line, what to write on it, and which one it is. */
data class LabelledLine(
    val key: Int,
    val label: String,
    val points: List<MapPoint>,
) {

    init {
        points.forEach(MapPoint::requireValid)
    }
}
