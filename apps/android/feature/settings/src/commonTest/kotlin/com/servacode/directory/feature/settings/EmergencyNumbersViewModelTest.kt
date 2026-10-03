package com.servacode.directory.feature.settings

import com.servacode.directory.core.testing.FakeEmergencyNumbersCache
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertTrue
import kotlin.test.Test

class EmergencyNumbersViewModelTest {
    private val repository =
        EmergencyNumbersRepository(ScriptedPublicApi(), FakeEmergencyNumbersCache(), FakePreferences("raqqa"), LABEL_SOURCE)

    @Test fun `the screen shows the built-in list with its warning`() = runMainTest {
        val model = EmergencyNumbersViewModel(repository)
        advanceUntilIdle()

        val content = model.state.value as EmergencyUiState.Content
        assertTrue(content.builtIn)
    }
}
