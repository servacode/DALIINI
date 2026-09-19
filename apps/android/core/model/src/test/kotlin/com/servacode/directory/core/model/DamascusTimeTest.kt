package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class DamascusTimeTest {
    private fun instant(value: String) = Instant.parse(value).toEpochMilli()

    @Test fun `a Damascus time today is UTC plus three, summer and winter alike`() {
        assertEquals(instant("2026-09-20T05:00:00Z"), DamascusTime.toEpochMillis(LocalDate.of(2026, 9, 20), LocalTime.of(8, 0)))
        assertEquals(instant("2026-01-15T05:00:00Z"), DamascusTime.toEpochMillis(LocalDate.of(2026, 1, 15), LocalTime.of(8, 0)))
    }

    @Test fun `a picked date and time survive the round trip`() {
        val date = LocalDate.of(2026, 11, 3)
        val time = LocalTime.of(22, 45)

        assertEquals(date.atTime(time), DamascusTime.localDateTime(DamascusTime.toEpochMillis(date, time)))
    }

    @Test fun `the zone rules decide, not a fixed offset, so winter 2021 was UTC plus two`() {
        assertEquals(instant("2021-01-15T10:00:00Z"), DamascusTime.toEpochMillis(LocalDate.of(2021, 1, 15), LocalTime.NOON))
        assertEquals(instant("2021-07-01T09:00:00Z"), DamascusTime.toEpochMillis(LocalDate.of(2021, 7, 1), LocalTime.NOON))
    }

    @Test fun `a time skipped by the spring change moves forward by the gap`() {
        // 26 March 2021: clocks went from 00:00 straight to 01:00, so 00:30 never happened.
        assertEquals(instant("2021-03-25T22:30:00Z"), DamascusTime.toEpochMillis(LocalDate.of(2021, 3, 26), LocalTime.of(0, 30)))
    }

    @Test fun `a time repeated by the autumn change means its first occurrence`() {
        // 28 October 2021: 23:00 to 00:00 happened twice, first at UTC+3.
        assertEquals(instant("2021-10-28T20:30:00Z"), DamascusTime.toEpochMillis(LocalDate.of(2021, 10, 28), LocalTime.of(23, 30)))
    }

    @Test fun `display is the Damascus clock with Latin digits`() {
        assertEquals("2026-09-20 08:00", DamascusTime.format(instant("2026-09-20T05:00:00Z")))
        assertEquals(
            "من 2026-09-20 08:00 إلى 2026-09-20 16:00",
            DamascusTime.period(instant("2026-09-20T05:00:00Z"), instant("2026-09-20T13:00:00Z")),
        )
    }

    @Test fun `the date picker's UTC midnight is the day it shows, whatever the offset`() {
        assertEquals(LocalDate.of(2026, 9, 20), DamascusTime.dateFromPicker(instant("2026-09-20T00:00:00Z")))
        assertEquals(instant("2026-09-20T00:00:00Z"), DamascusTime.pickerMillis(LocalDate.of(2026, 9, 20)))
    }
}
