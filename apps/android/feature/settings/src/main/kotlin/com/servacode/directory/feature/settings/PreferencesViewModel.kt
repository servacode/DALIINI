package com.servacode.directory.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.datastore.DirectoryPreferences
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.datastore.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The reader's own switches: which notices to show, and whether to save data.
 *
 * The notice switches are this device's: the backend has no preferences endpoint yet, so a
 * push is still sent and the app decides, when it arrives, whether to show it.
 */
@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferences: DirectoryPreferencesStore,
) : ViewModel() {
    val values: StateFlow<DirectoryPreferences> = preferences.values
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DirectoryPreferences())

    fun setNotifications(change: NotificationPreferences.() -> NotificationPreferences) {
        viewModelScope.launch { preferences.setNotificationPreferences(values.value.notifications.change()) }
    }

    fun setDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDataSaver(enabled)
            // Chosen here, so there is nothing left to suggest.
            preferences.setDataSaverSuggested()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
