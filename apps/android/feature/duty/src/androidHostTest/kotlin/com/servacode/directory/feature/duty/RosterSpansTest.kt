package com.servacode.directory.feature.duty

import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.DutyWindow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** The hours each pharmacy is on duty on the day it is listed under, as the site words them. */
class RosterSpansTest {
    private fun at(date: String, time: String) =
        DamascusTime.toEpochMillis(LocalDate.parse(date), LocalTime.parse(time))

    private fun window(from: Long, to: Long) = DutyWindow("p", from, to)

    @Test fun `a night shift starts that day and ends the next`() {
        val span = RosterSpans.of(window(at("2026-10-13", "22:00"), at("2026-10-14", "08:00")), "2026-10-13")

        assertEquals(RosterSpan(from = "22:00", to = "08:00", nextDay = true), span)
    }

    @Test fun `the morning after begins on the day before`() {
        val span = RosterSpans.of(window(at("2026-10-12", "22:00"), at("2026-10-13", "08:00")), "2026-10-13")

        assertEquals(RosterSpan(from = null, to = "08:00", nextDay = false), span)
    }

    @Test fun `a day's two spans come earliest first`() {
        val shifts = listOf(
            window(at("2026-10-13", "22:00"), at("2026-10-14", "08:00")),
            window(at("2026-10-12", "22:00"), at("2026-10-13", "08:00")),
            DutyWindow("other", at("2026-10-13", "09:00"), at("2026-10-13", "17:00")),
        )

        val spans = RosterSpans.forFacility(shifts, "p", "2026-10-13")

        assertEquals(listOf(null, "22:00"), spans.map { it.from })
    }
}
