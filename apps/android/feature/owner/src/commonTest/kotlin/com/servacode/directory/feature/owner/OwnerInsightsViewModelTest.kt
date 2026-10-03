package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class OwnerInsightsViewModelTest {
    private val api = ScriptedOwnerApi()
    private fun viewModel() = OwnerInsightsViewModel(LoadOwnerInsightsUseCase(OwnerRepository(api)))

    @Test fun `loading and then the three figures`() = runMainTest {
        api.insightsAnswer = { insights() }
        val model = viewModel()

        model.show("f-1")
        assertEquals(OwnerInsightsUiState.Loading, model.state.value)
        advanceUntilIdle()

        assertEquals(OwnerInsightsUiState.Content(insights()), model.state.value)
    }

    @Test fun `a quiet month is empty and not three zeros`() = runMainTest {
        api.insightsAnswer = { insights(0, 0, 0) }
        val model = viewModel()

        model.show("f-1")
        advanceUntilIdle()

        assertEquals(OwnerInsightsUiState.Empty(30), model.state.value)
    }

    @Test fun `a failure is shown and retry asks again`() = runMainTest {
        val model = viewModel()
        model.show("f-1")
        advanceUntilIdle()
        assertEquals(AppError.Kind.OFFLINE, (model.state.value as OwnerInsightsUiState.Error).error.kind)

        api.insightsAnswer = { insights() }
        model.refresh()
        advanceUntilIdle()

        assertTrue(model.state.value is OwnerInsightsUiState.Content)
        assertEquals(listOf("insights:f-1", "insights:f-1"), api.calls)
    }

    @Test fun `showing the same facility again does not ask again`() = runMainTest {
        api.insightsAnswer = { insights() }
        val model = viewModel()

        model.show("f-1")
        advanceUntilIdle()
        model.show("f-1")
        advanceUntilIdle()

        assertEquals(listOf("insights:f-1"), api.calls)
    }
}
