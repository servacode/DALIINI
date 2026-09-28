package com.servacode.directory.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.directoryDataStore by preferencesDataStore(name = "directory_preferences")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : DirectoryPreferencesStore {
    override val values: Flow<DirectoryPreferences> = context.directoryDataStore.data.map { prefs ->
        DirectoryPreferences(
            selectedProvinceId = prefs[SELECTED_PROVINCE],
            locationPreference = prefs[LOCATION_PREFERENCE]
                ?.let { runCatching { LocationPreference.valueOf(it) }.getOrNull() }
                ?: LocationPreference.ASK,
            onboardingHintsSeen = prefs[ONBOARDING_HINTS] ?: false,
            welcomeCompleted = prefs[WELCOME_COMPLETED] ?: false,
            placeLabel = prefs[PLACE_LABEL],
            placeProvinceId = prefs[PLACE_PROVINCE],
            offlineMapDeclined = prefs[OFFLINE_MAP_DECLINED] ?: false,
            themePreference = prefs[THEME_PREFERENCE]
                ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
        )
    }

    override suspend fun selectProvince(id: String) {
        context.directoryDataStore.edit { it[SELECTED_PROVINCE] = id }
    }

    override suspend fun setLocationPreference(value: LocationPreference) {
        context.directoryDataStore.edit { it[LOCATION_PREFERENCE] = value.name }
    }

    override suspend fun setWelcomeCompleted() {
        context.directoryDataStore.edit { it[WELCOME_COMPLETED] = true }
    }

    override suspend fun rememberPlace(label: String, provinceId: String?) {
        context.directoryDataStore.edit { prefs ->
            prefs[PLACE_LABEL] = label
            if (provinceId == null) prefs.remove(PLACE_PROVINCE) else prefs[PLACE_PROVINCE] = provinceId
        }
    }

    override suspend fun setOfflineMapDeclined(value: Boolean) {
        context.directoryDataStore.edit { it[OFFLINE_MAP_DECLINED] = value }
    }

    override suspend fun setThemePreference(value: ThemePreference) {
        context.directoryDataStore.edit { it[THEME_PREFERENCE] = value.name }
    }

    private companion object {
        val SELECTED_PROVINCE = stringPreferencesKey("selected_province_id")
        val LOCATION_PREFERENCE = stringPreferencesKey("location_preference")
        val ONBOARDING_HINTS = booleanPreferencesKey("onboarding_hints_seen")
        val WELCOME_COMPLETED = booleanPreferencesKey("welcome_completed")
        val PLACE_LABEL = stringPreferencesKey("place_label")
        val PLACE_PROVINCE = stringPreferencesKey("place_province_id")
        val OFFLINE_MAP_DECLINED = booleanPreferencesKey("offline_map_declined")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesBindings {
    @Binds
    @Singleton
    abstract fun bindPreferencesStore(impl: PreferencesRepository): DirectoryPreferencesStore
}
