package com.servacode.directory.core.datastore

import kotlinx.coroutines.flow.Flow

data class DirectoryPreferences(
    val selectedProvinceId: String? = null,
    val locationPreference: LocationPreference = LocationPreference.ASK,
    val onboardingHintsSeen: Boolean = false,
    /** The welcome and the location question have been through once; they are not shown again. */
    val welcomeCompleted: Boolean = false,
)

enum class LocationPreference { ASK, ENABLED, DISABLED }

/**
 * The device preferences repositories read. DataStore implements it in the app; tests use an
 * in-memory one. None of it is private: the selected province and location choice only.
 */
interface DirectoryPreferencesStore {
    val values: Flow<DirectoryPreferences>
    suspend fun selectProvince(id: String)
    suspend fun setLocationPreference(value: LocationPreference)
    suspend fun setWelcomeCompleted()
}
