package com.servacode.directory.feature.duty

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.testing.MainDispatcherRule
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DutyViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val hour = 3_600_000L
    private val now = 1_790_589_600_000L
    private val api = ScriptedOwnerApi()
    private val repository = DutyRepository(api)

    private fun viewModel(date: String? = null) =
        DutyViewModel("f-1", date, LoadDutyUseCase(repository), ManageDutyUseCase(repository)) { now }

    @Test fun `a gap nudge's day arrives as tonight's shift on that day`() = runTest(main.dispatcher) {
        api.dutyAnswer = { emptyList() }
        api.closuresAnswer = { emptyList() }

        val draft = viewModel("2026-09-30").draft.value!!

        assertEquals("2026-09-30 20:00", DamascusTime.format(draft.startsAt))
        assertEquals("2026-10-01 08:00", DamascusTime.format(draft.endsAt))
    }

    @Test fun `a clash with the owner's own shift is named and nothing is sent`() = runTest(main.dispatcher) {
        val existing = DutyShift("s-1", now + hour, now + 5 * hour)
        api.dutyAnswer = { listOf(existing) }
        api.closuresAnswer = { emptyList() }
        val model = viewModel()
        advanceUntilIdle()

        model.schedule(now + 2 * hour, now + 3 * hour)

        val state = model.state.value as DutyUiState.Content
        assertEquals(DutyProblem.Overlaps(existing), state.problem)
        assertTrue(api.calls.none { it.startsWith("createDuty") })
    }

    @Test fun `a shift inside a closure is caught before it is sent`() = runTest(main.dispatcher) {
        val closure = TemporaryClosure("c-1", now, now + 48 * hour)
        api.dutyAnswer = { emptyList() }
        api.closuresAnswer = { listOf(closure) }
        val model = viewModel()
        advanceUntilIdle()

        model.schedule(now + hour, now + 2 * hour)

        assertEquals(DutyProblem.DuringClosure(closure), (model.state.value as DutyUiState.Content).problem)
    }

    @Test fun `the backend's closure refusal keeps its own code for the words`() = runTest(main.dispatcher) {
        api.dutyAnswer = { emptyList() }
        api.closuresAnswer = { throw AppException(AppError(AppError.Kind.SERVER)) }
        api.createDutyAnswer = { _, _ ->
            throw AppException(AppError(AppError.Kind.CONFLICT, code = "DUTY_DURING_CLOSURE", status = 409))
        }
        val model = viewModel()
        advanceUntilIdle()

        model.schedule(now + hour, now + 2 * hour)
        advanceUntilIdle()

        val state = model.state.value as DutyUiState.Content
        assertEquals("DUTY_DURING_CLOSURE", state.failure?.code)
        assertNull(state.problem)
    }

    @Test fun `presets fill the form rather than sending anything`() = runTest(main.dispatcher) {
        api.dutyAnswer = { emptyList() }
        api.closuresAnswer = { emptyList() }
        val model = viewModel()
        advanceUntilIdle()

        model.presetTonight()
        val tonight = model.draft.value!!
        model.presetTomorrow()
        val tomorrow = model.draft.value!!

        // now is 13:00 on 28 September in Damascus.
        assertEquals("2026-09-28 20:00", DamascusTime.format(tonight.startsAt))
        assertEquals("2026-09-29 08:00", DamascusTime.format(tonight.endsAt))
        assertEquals("2026-09-29 20:00", DamascusTime.format(tomorrow.startsAt))
        assertTrue(api.calls.none { it.startsWith("createDuty") })
        model.draftShown()
        assertNull(model.draft.value)
    }
}
