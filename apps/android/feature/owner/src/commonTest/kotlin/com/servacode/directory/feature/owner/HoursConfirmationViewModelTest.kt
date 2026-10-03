package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.HoursConfirmation
import com.servacode.directory.core.testing.missingEndpoint
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.Test

class HoursConfirmationViewModelTest {
    private val now = 1_790_589_600_000L
    private val api = ScriptedOwnerApi()
    private fun viewModel() = HoursConfirmationViewModel(ConfirmHoursUseCase(OwnerRepository(api))) { now }

    @Test fun `one tap confirms and the card says so`() = runMainTest {
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

    @Test fun `a backend without the endpoint hides the card`() = runMainTest {
        api.confirmAnswer = { throw missingEndpoint }
        val model = viewModel()
        model.show(confirmable(null))

        model.confirm()
        advanceUntilIdle()

        assertEquals(HoursConfirmationUiState.Hidden, model.state.value)
    }

    @Test fun `HOURS_NOT_SUPPORTED hides it too`() = runMainTest {
        api.confirmAnswer = {
            throw AppException(AppError(AppError.Kind.CONFLICT, code = "HOURS_NOT_SUPPORTED", status = 409))
        }
        val model = viewModel()
        model.show(confirmable(null))

        model.confirm()
        advanceUntilIdle()

        assertEquals(HoursConfirmationUiState.Hidden, model.state.value)
    }

    @Test fun `offline the card stays with the reason and can be tapped again`() = runMainTest {
        val model = viewModel()
        model.show(confirmable(null))

        model.confirm()
        advanceUntilIdle()

        val due = model.state.value as HoursConfirmationUiState.Due
        assertEquals(AppError.Kind.OFFLINE, due.failure?.kind)
    }

    @Test fun `confirmed this week nothing is asked`() = runMainTest {
        val model = viewModel()

        model.show(confirmable(now - 86_400_000L))

        assertEquals(HoursConfirmationUiState.Hidden, model.state.value)
    }
}
