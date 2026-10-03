package com.servacode.directory.feature.settings

import com.servacode.directory.core.datastore.NotificationPreferences
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.NotificationSwitches
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class PreferencesViewModelTest {
    private val api = ScriptedPublicApi()
    private fun viewModel(preferences: FakePreferences) =
        PreferencesViewModel(preferences, NotificationPreferencesSync(api, preferences))

    @Test fun `every notice is on until turned off and one switch changes one kind`() = runMainTest {
        val preferences = FakePreferences()
        val model = viewModel(preferences)
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

    @Test fun `choosing data saver also ends the suggestion`() = runMainTest {
        val preferences = FakePreferences()
        val model = viewModel(preferences)

        model.setDataSaver(true)
        advanceUntilIdle()

        val stored = preferences.values.first()
        assertTrue(stored.dataSaver)
        assertTrue(stored.dataSaverSuggested)
    }

    @Test fun `signed out a switch stays on this device`() = runMainTest {
        val preferences = FakePreferences()
        val model = viewModel(preferences)
        model.forAccount(signedIn = false)

        model.setNotifications { copy(dutyReminders = false) }
        advanceUntilIdle()

        assertFalse(preferences.values.first().notifications.dutyReminders)
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `signed in the account's switches are read and a change is sent`() = runMainTest {
        val preferences = FakePreferences()
        api.switchesAnswer = { NotificationSwitches(dutyReminders = false) }
        api.updateSwitchesAnswer = { _, news, _ -> NotificationSwitches(dutyReminders = false, provinceNews = news!!) }
        val model = viewModel(preferences)
        val collecting = launch { model.values.collect { } }

        model.forAccount(signedIn = true)
        advanceUntilIdle()
        assertFalse(preferences.values.first().notifications.dutyReminders)

        model.setNotifications { copy(provinceNews = false) }
        advanceUntilIdle()

        assertEquals(listOf("switches", "update-switches:news=false"), api.calls)
        assertEquals(
            NotificationPreferences(dutyReminders = false, provinceNews = false),
            preferences.values.first().notifications,
        )
        assertNull(model.sync.value.failure)
        collecting.cancel()
    }

    @Test fun `signed in a refused change is said and undone`() = runMainTest {
        val preferences = FakePreferences()
        api.switchesAnswer = { NotificationSwitches() }
        val model = viewModel(preferences)
        model.forAccount(signedIn = true)
        advanceUntilIdle()

        model.setNotifications { copy(provinceNews = false) }
        advanceUntilIdle()

        assertTrue(preferences.values.first().notifications.provinceNews)
        assertEquals(AppError.Kind.OFFLINE, model.sync.value.failure?.kind)
    }
}
