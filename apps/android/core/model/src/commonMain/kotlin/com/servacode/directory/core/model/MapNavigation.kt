package com.servacode.directory.core.model

/**
 * How the map and a facility's detail open each other without piling up copies (INT-094).
 *
 * A detail opens the map for its facility, replacing any map already on the back stack, so
 * there is never more than one. The map opens a facility by going back when the screen under
 * it is that facility's detail, which is where the map was opened from, and by opening the
 * detail otherwise.
 */
object MapNavigation {
    /** The map a detail opens: centred on the facility, which it shows selected. */
    fun mapFor(facilityId: String): DirectoryRoute.Map = DirectoryRoute.Map(focusFacilityId = facilityId)

    /** True when showing [facilityId] from the map means going back to the screen under it. */
    fun returnsToDetail(below: DirectoryRoute?, facilityId: String): Boolean =
        below == DirectoryRoute.FacilityDetailRoute(facilityId)
}
