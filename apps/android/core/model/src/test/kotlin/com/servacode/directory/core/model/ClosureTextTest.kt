package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ClosureTextTest {
    // 2026-09-20 08:00 to 16:00 on Damascus clocks (UTC+3).
    private val start = 1_789_880_400_000L
    private val end = 1_789_909_200_000L
    private val period = "من 2026-09-20 08:00 إلى 2026-09-20 16:00"

    private fun closure(reason: String?) = TemporaryClosure("c1", start, end, reason)

    @Test fun `no reason reads as the period alone`() {
        assertEquals(period, ClosureText.of(closure(null)))
    }

    @Test fun `an empty reason reads as the period alone, with no separator`() {
        assertEquals(period, ClosureText.of(closure("")))
    }

    @Test fun `a reason of spaces reads as the period alone`() {
        assertEquals(period, ClosureText.of(closure("   ")))
    }

    @Test fun `a real reason comes first, then the period`() {
        assertEquals("سبب فعلي • $period", ClosureText.of(closure("سبب فعلي")))
    }

    @Test fun `the period is the one every closure list shows`() {
        assertEquals(DamascusTime.period(start, end), ClosureText.of(closure(null)))
    }
}
