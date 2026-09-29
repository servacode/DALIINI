package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DeepLinksTest {
    private val hosts = setOf("daliini.example")
    private val id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

    @Test fun `a facility link opens its page`() {
        assertEquals(DeepLinkTarget.Facility(id), DeepLinks.parse("https://daliini.example/f/$id", hosts))
        assertEquals(DeepLinkTarget.Facility(id), DeepLinks.parse("https://www.daliini.example/f/$id/", hosts))
        assertEquals(DeepLinkTarget.Facility(id), DeepLinks.parse("https://DALIINI.example/f/${id.uppercase()}?utm=x", hosts))
    }

    @Test fun `duty and a province code are their own places`() {
        assertEquals(DeepLinkTarget.DutyNow, DeepLinks.parse("https://daliini.example/duty", hosts))
        assertEquals(DeepLinkTarget.DutyNow, DeepLinks.parse("https://daliini.example/duty/today", hosts))
        assertEquals(DeepLinkTarget.Province("raqqa"), DeepLinks.parse("https://daliini.example/Raqqa", hosts))
        assertEquals(DeepLinkTarget.Province("deir-ez-zor"), DeepLinks.parse("https://daliini.example/deir-ez-zor/", hosts))
    }

    @Test fun `anything else opens nothing in particular`() {
        listOf(
            "http://daliini.example/duty",
            "https://evil.example/f/$id",
            "https://daliini.example.evil.example/duty",
            "https://daliini.example/f/not-an-id",
            "https://daliini.example/f/$id/extra",
            "https://daliini.example/privacy",
            "https://daliini.example/how-we-verify",
            "https://daliini.example/duty/tomorrow",
            "https://daliini.example/",
            "https://daliini.example/a",
            "https://daliini.example/%3Cscript%3E",
            "intent://daliini.example/duty",
            "not a link",
            null,
        ).forEach { assertNull(it, DeepLinks.parse(it, hosts)) }
    }

    @Test fun `duty presets are night shifts on Damascus clocks`() {
        val day = LocalDate.of(2026, 9, 28)
        val (start, end) = DutyPresets.tonight(day)
        assertEquals("2026-09-28 20:00", DamascusTime.format(start))
        assertEquals("2026-09-29 08:00", DamascusTime.format(end))
        assertEquals("2026-09-29 20:00", DamascusTime.format(DutyPresets.tomorrow(day).first))
        assertEquals(LocalDate.of(2026, 9, 30), DutyPresets.parseDate("2026-09-30"))
        assertEquals(LocalDate.of(2026, 9, 30), DutyPresets.parseDate("2026-09-30T00:00:00Z"))
        assertNull(DutyPresets.parseDate("tomorrow"))
    }

    @Test fun `push types fall into the categories a reader can turn off`() {
        assertEquals(NotificationCategory.DUTY_REMINDER, NotificationCategory.of("duty.gap_nudge"))
        assertEquals(NotificationCategory.APPLICATION_STATUS, NotificationCategory.of("facility.application.approved"))
        assertEquals(NotificationCategory.APPLICATION_STATUS, NotificationCategory.of("facility.hours.confirm_request"))
        assertEquals(NotificationCategory.PROVINCE_NEWS, NotificationCategory.of("platform.broadcast"))
        // Staff changing the owner's own shift cannot be turned off.
        assertNull(NotificationCategory.of("duty.shift.admin_changed"))
        assertNull(NotificationCategory.of("account.security"))
    }

    @Test fun `each notice opens where it is about`() {
        assertEquals(
            NotificationTarget.DutyScheduling(null, "2026-09-30"),
            NotificationTarget.of("duty.gap_nudge", MessageDestination.OWNER_FACILITIES, null, "2026-09-30"),
        )
        assertEquals(
            NotificationTarget.DutyScheduling("f-1", null),
            NotificationTarget.of("duty.shift.admin_changed", MessageDestination.OWNER_FACILITIES, "f-1"),
        )
        assertEquals(
            NotificationTarget.HoursConfirmation(null),
            NotificationTarget.of("facility.hours.confirm_request", MessageDestination.OWNER_FACILITIES, null),
        )
        assertEquals(
            NotificationTarget.Facility("f-2"),
            NotificationTarget.of("facility.application.approved", MessageDestination.FACILITY, "f-2"),
        )
        assertEquals(
            NotificationTarget.None,
            NotificationTarget.of("platform.broadcast", MessageDestination.NONE, null),
        )
        assertEquals(
            NotificationTarget.DutyScheduling(null, null),
            NotificationTarget.of("duty.gap_nudge", MessageDestination.OWNER_FACILITIES, null, "soon"),
        )
    }
}
