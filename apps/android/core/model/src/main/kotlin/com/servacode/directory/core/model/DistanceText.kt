package com.servacode.directory.core.model

import kotlin.math.floor

/**
 * How a distance is shown, on every screen. The distance itself is the backend's: this only
 * rounds it and picks the unit, and never computes one.
 *
 * Below 1000 m after rounding, whole metres; from there, kilometres with one decimal. Both
 * round half up, so 483.9 m reads 484 م, 999.6 m reads 1.0 كم rather than "1000 م", and
 * 1094.9 m reads 1.1 كم. Digits are Latin, like every other number in the app.
 */
object DistanceText {
    fun of(meters: Double): String {
        val metres = meters.coerceAtLeast(0.0)
        val wholeMetres = roundHalfUp(metres)
        if (wholeMetres < 1_000) return "$wholeMetres م"
        val tenthsOfKilometre = roundHalfUp(metres / 100.0)
        return "${tenthsOfKilometre / 10}.${tenthsOfKilometre % 10} كم"
    }

    private fun roundHalfUp(value: Double): Long = floor(value + 0.5).toLong()
}
