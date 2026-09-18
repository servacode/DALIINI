package com.servacode.directory.feature.onboarding

import com.servacode.directory.core.model.BusinessHour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HoursFormTest {
    @Test fun `several spans in a day are numbered by sequence`() {
        val days = List(HoursForm.DAYS) { emptyList<HourSpan>() }.toMutableList()
        days[0] = listOf(HourSpan("09:00", "13:00"), HourSpan("16:00", "22:00"))

        val hours = HoursForm.toHours(days)

        assertEquals(
            listOf(BusinessHour(0, "09:00", "13:00", 0), BusinessHour(0, "16:00", "22:00", 1)),
            hours,
        )
    }

    @Test fun `an overnight span is passed through for the backend to judge`() {
        val days = List(HoursForm.DAYS) { listOf<HourSpan>() }.toMutableList()
        days[4] = listOf(HourSpan("20:00", "02:00"))

        assertEquals(listOf(BusinessHour(4, "20:00", "02:00", 0)), HoursForm.toHours(days))
    }

    @Test fun `reading back orders each day's spans by sequence and trims seconds`() {
        val days = HoursForm.fromHours(
            listOf(
                BusinessHour(1, "16:00:00", "22:00:00", 1),
                BusinessHour(1, "09:00:00", "13:00:00", 0),
            ),
        )

        assertEquals(listOf(HourSpan("09:00", "13:00"), HourSpan("16:00", "22:00")), days[1])
        assertTrue(days[0].isEmpty())
        assertEquals(HoursForm.DAYS, days.size)
    }
}
