package com.servacode.directory.feature.facility

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FacilityTrustTest {
    private val now = 1_789_812_000_000L
    private val day = 86_400_000L

    @Test fun `ages are said in the unit a person would use`() {
        assertEquals(FacilityAge.Today, FacilityAge.of(now - 3_600_000, now))
        assertEquals(FacilityAge.Days(1), FacilityAge.of(now - day, now))
        assertEquals(FacilityAge.Days(3), FacilityAge.of(now - 3 * day - 5, now))
        assertEquals(FacilityAge.Days(29), FacilityAge.of(now - 29 * day, now))
        assertEquals(FacilityAge.Months(1), FacilityAge.of(now - 30 * day, now))
        assertEquals(FacilityAge.Months(12), FacilityAge.of(now - 364 * day, now))
        assertEquals(FacilityAge.Years(2), FacilityAge.of(now - 800 * day, now))
    }

    @Test fun `a time ahead of the device clock reads as today`() {
        assertEquals(FacilityAge.Today, FacilityAge.of(now + 5 * day, now))
    }

    @Test fun `a WhatsApp link is the international number as ASCII digits`() {
        assertEquals("https://wa.me/963933000000", whatsAppLink("+963 933 000 000"))
        assertEquals("https://wa.me/963933000000", whatsAppLink("+963-933-000-000"))
        assertNull(whatsAppLink("+963"))
        assertNull(whatsAppLink("٠٩٣٣"))
    }
}

class FacilityReportRepositoryTest {
    private val api = ScriptedPublicApi()
    private val repository = FacilityRepository(FakePublicCache(), api, FakePreferences("raqqa"))

    @Test fun `a report sends the reason and the trimmed note`() = runTest {
        api.reportAnswer = { _, _, _ -> }

        val result = repository.report("f-1", FacilityReportReason.WRONG_HOURS, "  يفتح الساعة ٩  ")

        assertTrue(result.isSuccess)
        assertEquals(listOf("report:f-1:WRONG_HOURS:يفتح الساعة ٩"), api.calls)
    }

    @Test fun `a blank note is no note, and a long one is cut at the backend's limit`() = runTest {
        var sent: String? = "unset"
        api.reportAnswer = { _, _, note -> sent = note }

        repository.report("f-1", FacilityReportReason.OTHER, "   ")
        assertNull(sent)

        repository.report("f-1", FacilityReportReason.OTHER, "x".repeat(900))
        assertEquals(FacilityRepository.REPORT_NOTE_MAX, sent?.length)
    }

    @Test fun `a refusal comes back as a failure, not a crash`() = runTest {
        api.reportAnswer = { _, _, _ -> throw AppException(AppError(AppError.Kind.RATE_LIMITED, status = 429)) }

        val error = repository.report("f-1", FacilityReportReason.OTHER, null).exceptionOrNull() as AppException

        assertEquals(AppError.Kind.RATE_LIMITED, error.error.kind)
    }
}
