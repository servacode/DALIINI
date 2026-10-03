package com.servacode.directory.core.datastore

import kotlinx.coroutines.flow.Flow

data class DirectoryPreferences(
    val selectedProvinceId: String? = null,
    val locationPreference: LocationPreference = LocationPreference.ASK,
    val onboardingHintsSeen: Boolean = false,
    /** The welcome and the location question have been through once; they are not shown again. */
    val welcomeCompleted: Boolean = false,
    /**
     * The last place the platform resolved for this device, as the header showed it.
     *
     * Kept so the header reads the same the moment the app opens, before any fix arrives, and
     * so a device that loses its location keeps the name it had rather than falling back to a
     * province the user never chose. It is a label and a province id, never a coordinate.
     */
    val placeLabel: String? = null,
    val placeProvinceId: String? = null,
    /**
     * Whether the reader has said they do not want the province's map kept on the device.
     *
     * Stored as a refusal rather than a wish so that the default — keep it, when the connection is
     * one nobody pays for — needs nothing written. A reader who deletes the pack has said no, and
     * the app does not fetch it again behind their back; asking for it in settings says yes again.
     */
    val offlineMapDeclined: Boolean = false,
    /** Light, dark, or whatever the phone is set to — the last is the default. */
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    /** Which notices the reader wants; every one on until turned off. */
    val notifications: NotificationPreferences = NotificationPreferences(),
    /** «توفير البيانات»: smaller downloads, and nothing fetched that is not needed. Off by default. */
    val dataSaver: Boolean = false,
    /** Whether the app has already offered data saver on a metered connection; it asks once. */
    val dataSaverSuggested: Boolean = false,
    /**
     * The newest build the reader was told about and set aside («لاحقاً»). A newer build than
     * this one is offered again; the same one is not.
     */
    val updateOfferedVersionCode: Int = 0,
)

/**
 * The notices a reader can turn off. Signed in, this is the device's copy of the account's
 * choices, which the backend keeps and honours when it sends (DECISION-077); signed out, it is
 * the device's own. Either way a push is checked against it when it arrives.
 */
data class NotificationPreferences(
    val dutyReminders: Boolean = true,
    val provinceNews: Boolean = true,
    val applicationStatus: Boolean = true,
)

enum class LocationPreference { ASK, ENABLED, DISABLED }

/** The reader's choice in Settings: تلقائي (follow the phone), فاتح or داكن. */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** Whether the app is dark, given whether the phone is. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

/**
 * The device preferences repositories read. DataStore implements it in the app; tests use an
 * in-memory one. None of it is private: the selected province and location choice only.
 */
interface DirectoryPreferencesStore {
    val values: Flow<DirectoryPreferences>
    suspend fun selectProvince(id: String)
    suspend fun setLocationPreference(value: LocationPreference)
    suspend fun setWelcomeCompleted()

    /** Remember the place the backend resolved, so the next start opens with the same name. */
    suspend fun rememberPlace(label: String, provinceId: String?)

    /** Remember that the reader does, or does not, want the province's map kept. */
    suspend fun setOfflineMapDeclined(value: Boolean)

    /** Remember the reader's theme; applied at the root of the app. */
    suspend fun setThemePreference(value: ThemePreference)

    suspend fun setNotificationPreferences(value: NotificationPreferences)

    suspend fun setDataSaver(enabled: Boolean)

    /** The data-saver suggestion has been shown, whatever the answer. */
    suspend fun setDataSaverSuggested()

    /** The offer of build [versionCode] has been answered, whichever way. */
    suspend fun setUpdateOffered(versionCode: Int)
}
