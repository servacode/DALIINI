package com.servacode.directory.feature.home

import com.servacode.directory.core.location.LocationFix
import com.servacode.directory.core.network.DirectoryQuery

/**
 * The three questions people actually arrive with: what is near me, what is open, who is on
 * duty.
 *
 * They are not a second filtering system. Each one is the directory query the backend already
 * takes, with the flags it already understands, so the ordering and the contents are decided in
 * exactly one place — the server — and a filtered Home behaves like every other list in the app,
 * pagination included.
 */
enum class HomeQuickFilter {
    /**
     * Ordered by distance. The backend orders by distance whenever it is given a position, so
     * this filter is only offered while one is known; without it the chip would promise an
     * order the app cannot produce.
     */
    NEAREST,

    /** Open at this moment, by the facility's own business hours. */
    OPEN_NOW,

    /** On duty at this moment, by the duty shifts their owners registered. */
    DUTY_NOW,
}

/** Whether the chip can be chosen at all. Nearest needs a position; the others never do. */
fun HomeQuickFilter.isAvailable(hasLocation: Boolean): Boolean =
    this != HomeQuickFilter.NEAREST || hasLocation

/**
 * The query one chip means, as the backend's own directory query.
 *
 * No category: Home asks about the whole province. The coordinates go with it whenever they are
 * known, because they are what makes any of these lists ordered by distance rather than by name.
 */
fun HomeQuickFilter.query(provinceId: String, fix: LocationFix?): DirectoryQuery = DirectoryQuery(
    provinceId = provinceId,
    categoryId = null,
    openNow = this == HomeQuickFilter.OPEN_NOW,
    dutyNow = this == HomeQuickFilter.DUTY_NOW,
    latitude = fix?.latitude,
    longitude = fix?.longitude,
)
