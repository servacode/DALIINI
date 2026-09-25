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

    /** Remember the place the backend resolved, so the next start opens with the same name. */
    suspend fun rememberPlace(label: String, provinceId: String?)

    /** Remember that the reader does, or does not, want the province's map kept. */
    suspend fun setOfflineMapDeclined(value: Boolean)
}
