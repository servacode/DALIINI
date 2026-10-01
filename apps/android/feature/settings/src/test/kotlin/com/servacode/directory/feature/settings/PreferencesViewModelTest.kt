package com.servacode.directory.feature.settings

import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PreferencesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `every notice is on until turned off, and one switch changes one kind`() = runTest(main.dispatcher) {
        val preferences = FakePreferences()
        val model = PreferencesViewModel(preferences)
        val collecting = launch { model.values.collect { } }
        advanceUntilIdle()
        assertTrue(model.values.value.notifications.provinceNews)

        model.setNotifications { copy(provinceNews = false) }
        advanceUntilIdle()

        val stored = preferences.values.first().notifications
        assertFalse(stored.provinceNews)
        assertTrue(stored.dutyReminders)
        assertTrue(stored.applicationStatus)
        collecting.cancel()
    }

    @Test fun `choosing data saver also ends the suggestion`() = runTest(main.dispatcher) {
        val preferences = FakePreferences()
        val model = PreferencesViewModel(preferences)

        model.setDataSaver(true)
        advanceUntilIdle()

        val stored = preferences.values.first()
        assertTrue(stored.dataSaver)
        assertTrue(stored.dataSaverSuggested)
    }
}
