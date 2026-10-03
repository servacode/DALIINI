package com.servacode.directory.feature.settings

import com.servacode.directory.core.datastore.NotificationPreferences
import com.servacode.directory.core.model.NotificationSwitches
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class NotificationPreferencesSyncTest {
    private val api = ScriptedPublicApi()
    private val store = FakePreferences()
    private val sync = NotificationPreferencesSync(api, store)

    @Test fun `the account's choices replace the device's`() = runTest {
        api.switchesAnswer = { NotificationSwitches(provinceNews = false) }

        sync.pull()

        assertEquals(NotificationPreferences(provinceNews = false), store.values.first().notifications)
    }

    @Test fun `a failed read leaves the device as it was`() = runTest {
        store.setNotificationPreferences(NotificationPreferences(dutyReminders = false))

        assertTrue(sync.pull().isFailure)

        assertEquals(NotificationPreferences(dutyReminders = false), store.values.first().notifications)
    }

    @Test fun `a change sends only the switch that moved and keeps the backend's answer`() = runTest {
        api.updateSwitchesAnswer = { _, news, _ -> NotificationSwitches(provinceNews = news ?: true) }

        sync.change { copy(provinceNews = false) }

        assertEquals(listOf("update-switches:news=false"), api.calls)
        assertEquals(NotificationPreferences(provinceNews = false), store.values.first().notifications)
    }

    @Test fun `a refused change puts the switch back`() = runTest {
        val result = sync.change { copy(applicationStatus = false) }

        assertTrue(result.isFailure)
        assertEquals(NotificationPreferences(), store.values.first().notifications)
    }

    @Test fun `no change makes no request`() = runTest {
        sync.change { copy() }

        assertTrue(api.calls.isEmpty())
    }
}
