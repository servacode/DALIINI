package com.servacode.directory.feature.bootstrap

import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The state the navigation reacts to. The splash adds no wait of its own: Ready arrives with
 * the first run of pending work, without the clock moving.
 */
class BootstrapViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun viewModel(province: String?) =
        BootstrapViewModel(BootstrapUseCase(DefaultBootstrapRepository(FakePreferences(province))))

    @Test fun `ready with the saved province, with no time passing`() = runTest(main.dispatcher) {
        val model = viewModel("raqqa")
        assertEquals(BootstrapUiState.Loading, model.state.value)

        runCurrent()

        assertEquals(BootstrapUiState.Ready("raqqa"), model.state.value)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun `a first start goes on to choose a province, still without waiting`() = runTest(main.dispatcher) {
        val model = viewModel(null)

        runCurrent()

        assertEquals(BootstrapUiState.Ready(null), model.state.value)
        assertEquals(0L, testScheduler.currentTime)
    }
}
