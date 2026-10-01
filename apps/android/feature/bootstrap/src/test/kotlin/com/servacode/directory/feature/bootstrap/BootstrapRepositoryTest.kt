package com.servacode.directory.feature.bootstrap

import com.servacode.directory.core.datastore.DirectoryPreferences
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.datastore.LocationPreference
import com.servacode.directory.core.datastore.NotificationPreferences
import com.servacode.directory.core.datastore.ThemePreference
import com.servacode.directory.core.testing.FakePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * What the splash waits on: the saved province, read from the device. The repository has no
 * network collaborator to call, and nothing on the way takes time.
 */
class BootstrapRepositoryTest {
    @Test fun `the start is known from the saved province alone, at once`() = runTest {
        val result = DefaultBootstrapRepository(FakePreferences(selectedProvinceId = "raqqa")).initialize()

        assertEquals(BootstrapResult.Ready("raqqa", StartDestination.HOME), result)
        assertEquals("no wait on the way", 0L, testScheduler.currentTime)
    }

    @Test fun `a first start has no province and goes to the welcome`() = runTest {
        assertEquals(
            BootstrapResult.Ready(null, StartDestination.WELCOME),
            DefaultBootstrapRepository(FakePreferences()).initialize(),
        )
    }

    @Test fun `a device that answered the first run goes home, province or not`() = runTest {
        val preferences = FakePreferences(selectedProvinceId = null, welcomeCompleted = true)

        assertEquals(
            BootstrapResult.Ready(null, StartDestination.HOME),
            DefaultBootstrapRepository(preferences).initialize(),
        )
    }

    @Test fun `storage that cannot be read ends the splash with a failure instead of holding it`() = runTest {
        val broken = object : DirectoryPreferencesStore {
            override val values: Flow<DirectoryPreferences> = flow { throw IOException("disk") }
            override suspend fun selectProvince(id: String) = Unit
            override suspend fun setLocationPreference(value: LocationPreference) = Unit
            override suspend fun setWelcomeCompleted() = Unit
            override suspend fun rememberPlace(label: String, provinceId: String?) = Unit
            override suspend fun setOfflineMapDeclined(value: Boolean) = Unit
            override suspend fun setThemePreference(value: ThemePreference) = Unit
            override suspend fun setNotificationPreferences(value: NotificationPreferences) = Unit
            override suspend fun setDataSaver(enabled: Boolean) = Unit
            override suspend fun setDataSaverSuggested() = Unit
        }

        assertEquals(BootstrapResult.Failed("BOOTSTRAP_STORAGE_UNAVAILABLE"),
            DefaultBootstrapRepository(broken).initialize())
    }
}
