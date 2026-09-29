package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.HoursConfirmation
import com.servacode.directory.core.testing.MainDispatcherRule
import com.servacode.directory.core.testing.ScriptedOwnerApi
import com.servacode.directory.core.testing.missingEndpoint
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HoursConfirmationViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val now = 1_790_589_600_000L
    private val api = ScriptedOwnerApi()
    private fun viewModel() = HoursConfirmationViewModel(ConfirmHoursUseCase(OwnerRepository(api))) { now }

    @Test fun `one tap confirms, and the card says so`() = runTest(main.dispatcher) {
        api.confirmAnswer = { HoursConfirmation(now, now) }
        val model = viewModel()
        model.show(confirmable(null))
        assertEquals(HoursConfirmationUiState.Due(), model.state.value)

        model.confirm()
        advanceUntilIdle()

        assertEquals(HoursConfirmationUiState.Confirmed, model.state.value)
        assertEquals(listOf("confirm:f-1"), api.calls)
        // The screen reloads the facility after; the thanks stays rather than flickering away.
        model.show(confirmable(now))
        assertEquals(HoursConfirmationUiState.Confirmed, model.state.value)
    }

    @Test fun `a backend without the endpoint hides the card`() = runTest(main.dispatcher) {
        api.confirmAnswer = { throw missingEndpoint }
        val model = viewModel()
        model.show(confirmable(null))

        model.confirm()
        advanceUntilIdle()

        assertEquals(HoursConfirmationUiState.Hidden, model.state.value)
    }

    @Test fun `HOURS_NOT_SUPPORTED hides it too`() = runTest(main.dispatcher) {
        api.confirmAnswer = {
            throw AppException(AppError(AppError.Kind.CONFLICT, code = "HOURS_NOT_SUPPORTED", status = 409))
        }
        val model = viewModel()
        model.show(confirmable(null))

        model.confirm()
        advanceUntilIdle()

        assertEquals(HoursConfirmationUiState.Hidden, model.state.value)
    }

    @Test fun `offline, the card stays with the reason and can be tapped again`() = runTest(main.dispatcher) {
        val model = viewModel()
        model.show(confirmable(null))

        model.confirm()
        advanceUntilIdle()

        val due = model.state.value as HoursConfirmationUiState.Due
        assertEquals(AppError.Kind.OFFLINE, due.failure?.kind)
    }

    @Test fun `confirmed this week, nothing is asked`() = runTest(main.dispatcher) {
        val model = viewModel()

        model.show(confirmable(now - 86_400_000L))

        assertEquals(HoursConfirmationUiState.Hidden, model.state.value)
    }
}
