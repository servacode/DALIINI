package com.servacode.directory.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreferencesRepositoryTest {
    private val file = DirectoryDataStore(MemoryDataStore())
    private val repository = PreferencesRepository(file)

    @Test
    fun `a device that has written nothing reads the defaults`() = runTest {
        assertEquals(DirectoryPreferences(), repository.values.first())
    }

    @Test
    fun `every setter is read back`() = runTest {
        repository.selectProvince("damascus")
        repository.setLocationPreference(LocationPreference.ENABLED)
        repository.setWelcomeCompleted()
        repository.rememberPlace("المزة", "damascus")
        repository.setOfflineMapDeclined(true)
        repository.setThemePreference(ThemePreference.DARK)
        repository.setNotificationPreferences(
            NotificationPreferences(dutyReminders = false, provinceNews = true, applicationStatus = false),
        )
        repository.setDataSaver(true)
        repository.setDataSaverSuggested()
        repository.setUpdateOffered(42)

        assertEquals(
            DirectoryPreferences(
                selectedProvinceId = "damascus",
                locationPreference = LocationPreference.ENABLED,
                welcomeCompleted = true,
                placeLabel = "المزة",
                placeProvinceId = "damascus",
                offlineMapDeclined = true,
                themePreference = ThemePreference.DARK,
                notifications = NotificationPreferences(
                    dutyReminders = false,
                    provinceNews = true,
                    applicationStatus = false,
                ),
                dataSaver = true,
                dataSaverSuggested = true,
                updateOfferedVersionCode = 42,
            ),
            repository.values.first(),
        )
    }

    @Test
    fun `a place without a province forgets the province it had`() = runTest {
        repository.rememberPlace("دمشق", "damascus")
        repository.rememberPlace("على الطريق", null)

        val values = repository.values.first()
        assertEquals("على الطريق", values.placeLabel)
        assertNull(values.placeProvinceId)
    }

    @Test
    fun `a stored choice this build does not know falls back to the default`() = runTest {
        // A newer build may write a value an older one has no name for, and a downgrade must not
        // crash on it.
        file.store.edit {
            it[stringPreferencesKey("location_preference")] = "SOMETIMES"
            it[stringPreferencesKey("theme_preference")] = "SEPIA"
        }

        val values = repository.values.first()
        assertEquals(LocationPreference.ASK, values.locationPreference)
        assertEquals(ThemePreference.SYSTEM, values.themePreference)
    }

    @Test
    fun `the keys are the ones Android has always written`() = runTest {
        // Shared code must read what the Android app already stored on a phone before it was
        // shared; a renamed key would quietly reset every reader's choices.
        repository.selectProvince("aleppo")
        repository.setThemePreference(ThemePreference.LIGHT)
        repository.setUpdateOffered(7)

        val stored = file.store.data.first().asMap().mapKeys { it.key.name }
        assertEquals("aleppo", stored["selected_province_id"])
        assertEquals("LIGHT", stored["theme_preference"])
        assertEquals(7, stored["update_offered_version_code"])
    }
}
