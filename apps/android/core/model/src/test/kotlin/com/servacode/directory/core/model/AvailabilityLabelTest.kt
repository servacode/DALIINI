package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AvailabilityLabelTest {
    private fun summary(state: AvailabilityState, nextOpen: Long? = null) = FacilitySummary(
        id = "f",
        nameAr = "صيدلية",
        category = Category("c", "صيدلية"),
        availability = state,
        nextOpenAtEpochMillis = nextOpen,
    )

    @Test fun `each state has its own wording`() {
        assertEquals("مفتوح الآن", AvailabilityLabel.of(AvailabilityState.OPEN))
        assertEquals("مناوب الآن", AvailabilityLabel.of(AvailabilityState.DUTY))
        assertEquals("مغلق مؤقتًا", AvailabilityLabel.of(AvailabilityState.TEMP_CLOSED))
        assertEquals("مغلق", AvailabilityLabel.of(AvailabilityState.CLOSED))
    }

    @Test fun `the next opening is shown in Syria's time, whatever the device's zone`() {
        // 2026-09-21T05:00:00Z is 08:00 in Damascus (UTC+3).
        val label = AvailabilityLabel.of(summary(AvailabilityState.CLOSED, nextOpen = 1_789_966_800_000L))

        assertEquals("مغلق • يفتح 08:00", label)
    }

    @Test fun `an open facility needs no next opening`() {
        assertEquals("مفتوح الآن", AvailabilityLabel.of(summary(AvailabilityState.OPEN, nextOpen = 1L)))
    }
}
