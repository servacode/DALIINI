package com.servacode.directory.feature.settings

import com.servacode.directory.core.testing.FakeEmergencyNumbersCache
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.MainDispatcherRule
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EmergencyNumbersViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val repository =
        EmergencyNumbersRepository(ScriptedPublicApi(), FakeEmergencyNumbersCache(), FakePreferences("raqqa"), LABELS)

    @Test fun `the screen shows the built-in list with its warning`() = runTest(main.dispatcher) {
        val model = EmergencyNumbersViewModel(repository)
        advanceUntilIdle()

        val content = model.state.value as EmergencyUiState.Content
        assertTrue(content.builtIn)
    }
}
