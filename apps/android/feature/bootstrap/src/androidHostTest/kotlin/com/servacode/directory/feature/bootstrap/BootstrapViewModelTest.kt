package com.servacode.directory.feature.bootstrap

import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The state the navigation reacts to. The splash is held while the mark grows: the work runs
 * beside the wait, so Ready arrives when the wait is over rather than when the work is done —
 * and a start slower than the wait is not made slower still.
 */
class BootstrapViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun viewModel(province: String?) =
        BootstrapViewModel(BootstrapUseCase(DefaultBootstrapRepository(FakePreferences(province))))

    private fun ready(province: String?, start: StartDestination) = BootstrapUiState.Ready(province, start)

    @Test fun `the saved province is found at once, and the splash is still held`() = runTest(main.dispatcher) {
        val model = viewModel("raqqa")
        assertEquals(BootstrapUiState.Loading, model.state.value)

        runCurrent()

        // The work is done; the mark has not finished growing, so navigation waits.
        assertEquals(BootstrapUiState.Loading, model.state.value)

        advanceUntilIdle()

        assertEquals(ready("raqqa", StartDestination.HOME), model.state.value)
        assertEquals(MINIMUM_ON_SCREEN, testScheduler.currentTime)
    }

    @Test fun `a first start goes to the welcome, after the same wait`() = runTest(main.dispatcher) {
        val model = viewModel(null)

        advanceUntilIdle()

        assertEquals(ready(null, StartDestination.WELCOME), model.state.value)
        assertEquals(MINIMUM_ON_SCREEN, testScheduler.currentTime)
    }

    private companion object {
        /** BootstrapViewModel's own, which is private: this is the promise it makes. */
        const val MINIMUM_ON_SCREEN = 1200L
    }
}
