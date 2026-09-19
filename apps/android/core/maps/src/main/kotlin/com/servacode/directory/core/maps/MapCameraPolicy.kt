package com.servacode.directory.core.maps

/**
 * Where a map opens (INT-092). One rule for every map in the app, kept out of the composables.
 *
 * Each source is asked only when the ones before it have nothing, so a slow one (a fresh
 * location fix, the province list) is never waited for when it is not needed. When no source
 * has an answer the result is null and the map keeps its own default rather than a guessed place.
 */
object MapCameraPolicy {
    /** One facility and the streets around it; also close enough for an owner's tap to be precise. */
    const val FACILITY_ZOOM = 16.0

    /** The user's neighbourhood: the nearest facilities are on screen. */
    const val NEARBY_ZOOM = 14.0

    /** The province's main city, edge to edge. */
    const val PROVINCE_ZOOM = 12.0

    /**
     * The public map: the camera the user left it at, else the facility it was opened for,
     * else the user's neighbourhood, else the province.
     */
    suspend fun forMap(
        restored: MapCamera?,
        facility: suspend () -> MapPoint?,
        user: suspend () -> MapPoint?,
        province: suspend () -> MapPoint?,
    ): MapCamera? = restored
        ?: facility()?.let { MapCamera(it, FACILITY_ZOOM) }
        ?: user()?.let { MapCamera(it, NEARBY_ZOOM) }
        ?: province()?.let { MapCamera(it, PROVINCE_ZOOM) }

    /**
     * The owner's location picker: where the owner is, else the province. A point already
     * saved is shown as a marker but does not move the camera.
     */
    suspend fun forPicker(
        user: suspend () -> MapPoint?,
        province: suspend () -> MapPoint?,
    ): MapCamera? = user()?.let { MapCamera(it, FACILITY_ZOOM) }
        ?: province()?.let { MapCamera(it, PROVINCE_ZOOM) }
}
