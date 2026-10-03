package com.servacode.directory.core.transport

import kotlin.math.abs

/**
 * How much of a position leaves the phone, as on Android (`core/network/Coordinates.kt`).
 *
 * The backend uses a position for two things: to name the province somebody is in, and to sort
 * facilities by how near they are. Neither needs more than about ten metres — and a GPS fix is
 * rarely better than that anyway, so nothing is actually lost by rounding.
 *
 * What is gained is that nothing downstream can ever hold more. A coordinate travels in a query
 * string, and a query string is written down by every access log, proxy and error report it
 * passes. Sent at the phone's full precision — seven decimal places, a centimetre — that is a
 * record of where a person stood, inside which room, kept wherever those logs are kept. Four
 * places is a street corner.
 *
 * The map and every distance shown in the app use the full fix; this is only the wire.
 */
internal const val SENT_DECIMALS = 4

/** The value a request carries, or null when there is no position to send. */
internal fun Double?.asCoordinate(): String? = this?.let { fixedDecimals(it, SENT_DECIMALS) }

/**
 * [value] with exactly [decimals] places, written the way Android's
 * `String.format(Locale.US, "%.4f", value)` writes it; common Kotlin has no formatter.
 *
 * Java formats the shortest decimal that reads back as the same double — what
 * `Double.toString` prints — and rounds that half-up, so `35.00015` becomes `35.0002` although
 * the double nearest to it is a hair below. The same is done here, from the same digits:
 *  - always a point, never the phone's own separator, and no grouping;
 *  - the sign is the double's, so `-0.0` and a small negative value that rounds to nothing
 *    are written `-0.0000`, as Java writes them;
 *  - NaN and the infinities are written as Java writes them, though no position is ever one.
 */
internal fun fixedDecimals(value: Double, decimals: Int): String {
    require(decimals >= 0)
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
    val negative = value < 0.0 || (value == 0.0 && 1.0 / value < 0.0)

    // The shortest digits: "35.9513463", "1.0E-4", "1.2345E7".
    val text = abs(value).toString()
    val exponentAt = text.indexOfFirst { it == 'E' || it == 'e' }
    val mantissa = if (exponentAt < 0) text else text.substring(0, exponentAt)
    val exponent = if (exponentAt < 0) 0 else text.substring(exponentAt + 1).toInt()
    val pointAt = mantissa.indexOf('.').let { if (it < 0) mantissa.length else it }
    val digits = mantissa.removeRange(pointAt, minOf(pointAt + 1, mantissa.length))
    // The value is 0.digits × 10^point, and scaling by 10^decimals moves the point right.
    val point = pointAt + exponent + decimals

    // The whole part of value × 10^decimals, and the first digit dropped from it.
    val whole = when {
        point <= 0 -> ""
        point >= digits.length -> digits + "0".repeat(point - digits.length)
        else -> digits.substring(0, point)
    }
    val firstDropped = if (point in digits.indices) digits[point] else '0'
    val rounded = if (firstDropped >= '5') incremented(whole) else whole

    val padded = rounded.trimStart('0').padStart(decimals + 1, '0')
    val body = if (decimals == 0) {
        padded
    } else {
        padded.substring(0, padded.length - decimals) + "." + padded.substring(padded.length - decimals)
    }
    return if (negative) "-$body" else body
}

/** [digits], a whole number written in decimal, plus one; "" is zero. */
private fun incremented(digits: String): String {
    val result = digits.toCharArray()
    var index = result.size - 1
    while (index >= 0) {
        if (result[index] != '9') {
            result[index] = result[index] + 1
            return result.concatToString()
        }
        result[index] = '0'
        index--
    }
    return "1" + result.concatToString()
}
