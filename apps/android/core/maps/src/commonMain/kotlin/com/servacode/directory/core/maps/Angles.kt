package com.servacode.directory.core.maps

import kotlin.math.PI

// java.lang.Math's conversions, which common code does not have (DECISION-089). The same
// multiplication by the same constants, so a result is what the JVM's gave, to the last bit.
private const val DEGREES_TO_RADIANS = PI / 180.0
private const val RADIANS_TO_DEGREES = 180.0 / PI

internal fun Double.toRadians(): Double = this * DEGREES_TO_RADIANS

internal fun Double.toDegrees(): Double = this * RADIANS_TO_DEGREES
