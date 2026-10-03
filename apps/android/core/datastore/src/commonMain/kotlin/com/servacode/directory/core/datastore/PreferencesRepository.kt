package com.servacode.directory.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The device preferences, kept in the preferences DataStore. The same class and the same keys
 * on Android and on the iPhone (DECISION-092); only where the file lives differs.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    file: DirectoryDataStore,
) : DirectoryPreferencesStore {
    private val store = file.store

    override val values: Flow<DirectoryPreferences> = store.data.map { prefs ->
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
            notifications = NotificationPreferences(
                dutyReminders = prefs[NOTIFY_DUTY] ?: true,
                provinceNews = prefs[NOTIFY_NEWS] ?: true,
                applicationStatus = prefs[NOTIFY_APPLICATIONS] ?: true,
            ),
            dataSaver = prefs[DATA_SAVER] ?: false,
            dataSaverSuggested = prefs[DATA_SAVER_SUGGESTED] ?: false,
            updateOfferedVersionCode = prefs[UPDATE_OFFERED] ?: 0,
        )
    }

    override suspend fun selectProvince(id: String) {
        store.edit { it[SELECTED_PROVINCE] = id }
    }

    override suspend fun setLocationPreference(value: LocationPreference) {
        store.edit { it[LOCATION_PREFERENCE] = value.name }
    }

    override suspend fun setWelcomeCompleted() {
        store.edit { it[WELCOME_COMPLETED] = true }
    }

    override suspend fun rememberPlace(label: String, provinceId: String?) {
        store.edit { prefs ->
            prefs[PLACE_LABEL] = label
            if (provinceId == null) prefs.remove(PLACE_PROVINCE) else prefs[PLACE_PROVINCE] = provinceId
        }
    }

    override suspend fun setOfflineMapDeclined(value: Boolean) {
        store.edit { it[OFFLINE_MAP_DECLINED] = value }
    }

    override suspend fun setThemePreference(value: ThemePreference) {
        store.edit { it[THEME_PREFERENCE] = value.name }
    }

    override suspend fun setNotificationPreferences(value: NotificationPreferences) {
        store.edit {
            it[NOTIFY_DUTY] = value.dutyReminders
            it[NOTIFY_NEWS] = value.provinceNews
            it[NOTIFY_APPLICATIONS] = value.applicationStatus
        }
    }

    override suspend fun setDataSaver(enabled: Boolean) {
        store.edit { it[DATA_SAVER] = enabled }
    }

    override suspend fun setDataSaverSuggested() {
        store.edit { it[DATA_SAVER_SUGGESTED] = true }
    }

    override suspend fun setUpdateOffered(versionCode: Int) {
        store.edit { it[UPDATE_OFFERED] = versionCode }
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
        val NOTIFY_DUTY = booleanPreferencesKey("notify_duty_reminders")
        val NOTIFY_NEWS = booleanPreferencesKey("notify_province_news")
        val NOTIFY_APPLICATIONS = booleanPreferencesKey("notify_application_status")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val DATA_SAVER_SUGGESTED = booleanPreferencesKey("data_saver_suggested")
        val UPDATE_OFFERED = intPreferencesKey("update_offered_version_code")
    }
}
