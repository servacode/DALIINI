package com.servacode.directory.feature.facility

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.MainDispatcherRule
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FacilityReportViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val api = ScriptedPublicApi()
    private fun viewModel() = FacilityReportViewModel(
        ReportFacilityUseCase(FacilityRepository(FakePublicCache(), api, FakePreferences("raqqa"))),
    )

    @Test fun `nothing is sent until a reason is chosen`() = runTest(main.dispatcher) {
        val model = viewModel()
        assertFalse(model.state.value.canSend)

        model.submit("f-1")
        advanceUntilIdle()

        assertTrue(api.calls.isEmpty())
    }

    @Test fun `a sent report says so once, and the next sheet starts empty`() = runTest(main.dispatcher) {
        api.reportAnswer = { _, _, _ -> }
        val model = viewModel()
        model.choose(FacilityReportReason.CLOSED_PERMANENTLY)
        model.updateNote("أغلقت منذ شهر")

        model.submit("f-1")
        assertTrue(model.state.value.sending)
        advanceUntilIdle()

        assertTrue(model.state.value.sent)
        assertEquals(listOf("report:f-1:CLOSED_PERMANENTLY:أغلقت منذ شهر"), api.calls)
        model.consumeSent()
        assertEquals(FacilityReportUiState(), model.state.value)
    }

    @Test fun `too many reports is its own message, and the choice is kept`() = runTest(main.dispatcher) {
        api.reportAnswer = { _, _, _ -> throw AppException(AppError(AppError.Kind.RATE_LIMITED, status = 429)) }
        val model = viewModel()
        model.choose(FacilityReportReason.WRONG_INFO)

        model.submit("f-1")
        advanceUntilIdle()

        val state = model.state.value
        assertEquals(ReportFailure.THROTTLED, state.failure)
        assertEquals(FacilityReportReason.WRONG_INFO, state.reason)
        assertFalse(state.sending)
        assertFalse(state.sent)
    }

    @Test fun `no connection is its own message`() = runTest(main.dispatcher) {
        // The scripted API fails as offline when no answer is set.
        val model = viewModel()
        model.choose(FacilityReportReason.OTHER)

        model.submit("f-1")
        advanceUntilIdle()

        assertEquals(ReportFailure.OFFLINE, model.state.value.failure)
    }

    @Test fun `any other refusal can be tried again`() = runTest(main.dispatcher) {
        api.reportAnswer = { _, _, _ -> throw AppException(AppError(AppError.Kind.SERVER, status = 500)) }
        val model = viewModel()
        model.choose(FacilityReportReason.OTHER)
        model.submit("f-1")
        advanceUntilIdle()
        assertEquals(ReportFailure.OTHER, model.state.value.failure)

        api.reportAnswer = { _, _, _ -> }
        model.submit("f-1")
        advanceUntilIdle()
        assertTrue(model.state.value.sent)
        assertNull(model.state.value.failure)
    }

    @Test fun `the note stops at the backend's limit`() {
        val model = viewModel()

        model.updateNote("ن".repeat(700))

        assertEquals(FacilityRepository.REPORT_NOTE_MAX, model.state.value.note.length)
    }
}
