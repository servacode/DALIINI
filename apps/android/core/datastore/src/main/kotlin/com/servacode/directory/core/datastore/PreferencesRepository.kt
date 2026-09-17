package com.servacode.directory.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.directoryDataStore by preferencesDataStore(name = "directory_preferences")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val values: Flow<DirectoryPreferences> = context.directoryDataStore.data.map { prefs ->
        DirectoryPreferences(
            selectedProvinceId = prefs[SELECTED_PROVINCE],
            locationPreference = prefs[LOCATION_PREFERENCE]
                ?.let { runCatching { LocationPreference.valueOf(it) }.getOrNull() }
                ?: LocationPreference.ASK,
            onboardingHintsSeen = prefs[ONBOARDING_HINTS] ?: false,
        )
    }

    suspend fun selectProvince(id: String) {
        context.directoryDataStore.edit { it[SELECTED_PROVINCE] = id }
    }

    suspend fun setLocationPreference(value: LocationPreference) {
        context.directoryDataStore.edit { it[LOCATION_PREFERENCE] = value.name }
    }

    private companion object {
        val SELECTED_PROVINCE = stringPreferencesKey("selected_province_id")
        val LOCATION_PREFERENCE = stringPreferencesKey("location_preference")
        val ONBOARDING_HINTS = booleanPreferencesKey("onboarding_hints_seen")
    }
}
