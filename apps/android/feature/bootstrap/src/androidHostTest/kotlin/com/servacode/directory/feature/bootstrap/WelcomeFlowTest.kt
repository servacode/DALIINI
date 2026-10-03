package com.servacode.directory.feature.bootstrap

import com.servacode.directory.core.datastore.DirectoryPreferences
import com.servacode.directory.core.datastore.LocationPreference
import org.junit.Assert.assertEquals
import org.junit.Test

/** Who sees the first run, and what the answer to the location question is remembered as. */
class WelcomeFlowTest {
    @Test fun `a fresh install starts at the welcome`() {
        assertEquals(StartDestination.WELCOME, DirectoryPreferences().startDestination())
    }

    @Test fun `a device that has been through it goes straight home`() {
        val preferences = DirectoryPreferences(welcomeCompleted = true)

        assertEquals(StartDestination.HOME, preferences.startDestination())
        assertEquals(
            "even without a province, having answered is enough",
            StartDestination.HOME,
            preferences.copy(selectedProvinceId = null).startDestination(),
        )
    }

    @Test fun `a device from before the welcome existed is not sent back through it`() {
        val installedEarlier = DirectoryPreferences(selectedProvinceId = "raqqa", welcomeCompleted = false)

        assertEquals(StartDestination.HOME, installedEarlier.startDestination())
    }

    @Test fun `every answer to the location question is remembered`() {
        assertEquals(LocationPreference.ENABLED, LocationAnswer.ALLOWED.preference())
        assertEquals(LocationPreference.DISABLED, LocationAnswer.REFUSED.preference())
        assertEquals("not now leaves the offer open", LocationPreference.ASK, LocationAnswer.LATER.preference())
    }
}
