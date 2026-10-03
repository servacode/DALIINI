package com.servacode.directory.core.model

import kotlin.math.floor

/**
 * A distance as it is shown, rounded but not yet worded.
 *
 * The distance itself is the backend's: this only rounds it and picks the unit, and never computes
 * one. Below 1000 m after rounding, whole metres; from there, kilometres with one decimal. Both
 * round half up, so 483.9 m reads 484 metres, 999.6 m reads 1.0 kilometres rather than "1000
 * metres", and 1094.9 m reads 1.1.
 *
 * The unit is a word, so it is in the design system's resources and `DirectoryWords.distance`
 * reads it. The arithmetic is here, where a test can check it without Android.
 */
sealed interface RoundedDistance {
    data class Metres(val value: Long) : RoundedDistance

    /** Kept as two whole numbers so that the decimal mark itself belongs to the language. */
    data class Kilometres(val whole: Long, val tenth: Long) : RoundedDistance
}

fun roundedDistance(meters: Double): RoundedDistance {
    val metres = meters.coerceAtLeast(0.0)
    val wholeMetres = roundHalfUp(metres)
    if (wholeMetres < 1_000) return RoundedDistance.Metres(wholeMetres)
    val tenthsOfKilometre = roundHalfUp(metres / 100.0)
    return RoundedDistance.Kilometres(tenthsOfKilometre / 10, tenthsOfKilometre % 10)
}

private fun roundHalfUp(value: Double): Long = floor(value + 0.5).toLong()
