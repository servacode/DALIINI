package com.servacode.directory.feature.settings

import com.servacode.directory.core.datastore.ThemePreference
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.runMainTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class AppearanceViewModelTest {
    @Test fun `the phone decides until the reader chooses and the choice is stored`() = runMainTest {
        val preferences = FakePreferences()
        val model = AppearanceViewModel(preferences)
        val collecting = launch { model.theme.collect { } }
        advanceUntilIdle()
        assertEquals(ThemePreference.SYSTEM, model.theme.value)

        model.choose(ThemePreference.DARK)
        advanceUntilIdle()

        assertEquals(ThemePreference.DARK, model.theme.value)
        assertEquals(ThemePreference.DARK, preferences.values.first().themePreference)
        collecting.cancel()
    }

    @Test fun `system follows the phone while light and dark do not`() {
        assertTrue(ThemePreference.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemePreference.SYSTEM.isDark(systemDark = false))
        assertFalse(ThemePreference.LIGHT.isDark(systemDark = true))
        assertTrue(ThemePreference.DARK.isDark(systemDark = false))
    }
}
