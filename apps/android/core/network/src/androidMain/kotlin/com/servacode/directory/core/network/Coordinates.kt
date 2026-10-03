package com.servacode.directory.core.network

import java.util.Locale

/**
 * How much of a position leaves the phone.
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
internal fun Double?.asCoordinate(): String? =
    this?.let { String.format(Locale.US, "%.${SENT_DECIMALS}f", it) }
