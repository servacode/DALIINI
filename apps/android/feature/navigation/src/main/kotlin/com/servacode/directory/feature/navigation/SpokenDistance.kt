package com.servacode.directory.feature.navigation

import kotlin.math.roundToInt

/**
 * A distance as it is said, not as it is measured.
 *
 * "After 483 metres" is a number nobody drives by. Rounded to fifty below a kilometre and to a
 * half above it, which is how the distance is said out loud anyway — and how the recorded voice
 * pack says it.
 *
 * The rounding is here, in Kotlin, because it is arithmetic; the sentence it goes into is in the
 * module's `strings.xml`, because where the number sits in a sentence is the language's business.
 */
internal sealed interface SpokenDistance {
    /** Rounded to fifty metres, never below fifty. */
    data class Metres(val value: Int) : SpokenDistance

    /** Counted in halves of a kilometre, so that 1480 metres is said as one and a half. */
    data class Kilometres(val halves: Int) : SpokenDistance
}

internal fun spokenDistance(meters: Double): SpokenDistance = if (meters >= METRES_IN_KILOMETRE) {
    SpokenDistance.Kilometres((meters / (METRES_IN_KILOMETRE / 2)).roundToInt())
} else {
    SpokenDistance.Metres(((meters / METRES_STEP).roundToInt() * METRES_STEP).coerceAtLeast(METRES_STEP))
}

private const val METRES_IN_KILOMETRE = 1000
private const val METRES_STEP = 50
