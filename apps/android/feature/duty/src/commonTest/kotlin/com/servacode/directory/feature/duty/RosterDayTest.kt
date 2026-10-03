package com.servacode.directory.feature.duty

import kotlinx.datetime.DayOfWeek
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class RosterDayTest {
    @Test fun `a day is its weekday and its date as day and month in two digits`() {
        val day = RosterDay.of("2026-09-29")!!

        assertEquals(DayOfWeek.TUESDAY, day.weekday)
        assertEquals("29/09", day.digits)
        assertEquals("05/01", RosterDay.of("2027-01-05")!!.digits)
    }

    @Test fun `anything but an ISO date is left for the heading to show as it came`() {
        assertNull(RosterDay.of("tomorrow"))
        assertNull(RosterDay.of("2026-13-01"))
    }
}
