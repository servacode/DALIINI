package com.servacode.directory.feature.duty

import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.DutyWindow
import kotlinx.datetime.LocalDate

/**
 * How one facility's duty sits on the day it is listed under.
 *
 * The week listed a pharmacy under a day with no hours at all, and under it «مناوبة اليوم», which
 * is about today: a Tuesday that ended at 08:00 read like a Tuesday on duty all night. The site
 * says «منذ اليوم السابق حتى 08:00، ومن 22:00 إلى 08:00 من اليوم التالي»; this is the same.
 *
 * [from] is null when the duty began on an earlier day, [nextDay] when it ends on a later one.
 */
data class RosterSpan(val from: String?, val to: String, val nextDay: Boolean)

object RosterSpans {
    fun of(window: DutyWindow, date: String): RosterSpan {
        val day = LocalDate.parse(date)
        val start = DamascusTime.localDateTime(window.startsAtEpochMillis)
        val end = DamascusTime.localDateTime(window.endsAtEpochMillis)
        return RosterSpan(
            from = if (start.date < day) null else DamascusTime.clock(window.startsAtEpochMillis),
            to = DamascusTime.clock(window.endsAtEpochMillis),
            nextDay = end.date > day,
        )
    }

    /** The spans [facilityId] has under [date], earliest first. */
    fun forFacility(shifts: List<DutyWindow>, facilityId: String, date: String): List<RosterSpan> =
        shifts.filter { it.facilityId == facilityId }
            .sortedBy { it.startsAtEpochMillis }
            .map { of(it, date) }
}
