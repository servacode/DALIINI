package com.servacode.directory.feature.map

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
}
