package com.servacode.directory.feature.map

import kotlin.math.abs

/** The visible map area, sent to the backend as `west,south,east,north`. */
data class MapViewport(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
) {
    init {
        require(west in -180.0..180.0 && east in -180.0..180.0)
        require(south in -90.0..90.0 && north in -90.0..90.0)
    }

    /**
     * The same area, give or take rounding: a map restored to the same camera computes its
     * bounds again and may differ in the last digits. A millionth of a degree is about 10 cm.
     */
    fun sameAs(other: MapViewport?): Boolean = other != null &&
        abs(west - other.west) < TOLERANCE &&
        abs(south - other.south) < TOLERANCE &&
        abs(east - other.east) < TOLERANCE &&
        abs(north - other.north) < TOLERANCE

    private companion object {
        const val TOLERANCE = 1e-6
    }
}
