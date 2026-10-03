package com.servacode.directory.feature.onboarding

import com.servacode.directory.core.model.BusinessHour

/** One opening span as the owner types it. */
data class HourSpan(val opensAt: String, val closesAt: String)

/**
 * Converts between the editor — seven days, each with zero or more spans — and the rows the
 * backend takes, where a day's spans are told apart by `sequence`.
 *
 * A span that closes earlier than it opens runs past midnight; the backend accepts it and the
 * editor passes it through unchanged. Validity is the backend's call, not this object's.
 */
object HoursForm {
    const val DAYS = 7

    fun fromHours(hours: List<BusinessHour>): List<List<HourSpan>> = (0 until DAYS).map { weekday ->
        hours.filter { it.weekday == weekday }
            .sortedBy { it.sequence }
            .map { HourSpan(it.opensAt.take(5), it.closesAt.take(5)) }
    }

    fun toHours(days: List<List<HourSpan>>): List<BusinessHour> = days.flatMapIndexed { weekday, spans ->
        spans.mapIndexed { sequence, span ->
            BusinessHour(weekday = weekday, opensAt = span.opensAt, closesAt = span.closesAt, sequence = sequence)
        }
    }
}
