package com.servacode.directory.core.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * How availability reads on screen. The state itself is the backend's: the app only names it,
 * and shows the next opening time the backend computed, in Syria's time zone rather than
 * whatever zone the device happens to be set to.
 */
object AvailabilityLabel {
    private val syria: ZoneId = ZoneId.of("Asia/Damascus")
    private val clock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun of(state: AvailabilityState): String = when (state) {
        AvailabilityState.OPEN -> "مفتوح الآن"
        AvailabilityState.DUTY -> "مناوب الآن"
        AvailabilityState.TEMP_CLOSED -> "مغلق مؤقتًا"
        AvailabilityState.CLOSED -> "مغلق"
    }

    /** "مغلق • يفتح 08:00" when the backend said when; the bare state otherwise. */
    fun of(summary: FacilitySummary): String {
        val next = summary.nextOpenAtEpochMillis
        if (summary.availability == AvailabilityState.OPEN || next == null) return of(summary.availability)
        return "${of(summary.availability)} • يفتح ${clock.format(Instant.ofEpochMilli(next).atZone(syria))}"
    }
}
