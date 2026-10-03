package com.servacode.directory.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.datastore.DirectoryPreferences
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.datastore.NotificationPreferences
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.toAppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Whose the notice switches are, and how the last attempt to keep them went.
 *
 * [account] is true while signed in: the switches are then the account's, kept by the backend
 * and the same on every device. [failure] is a change the backend refused or could not be asked
 * about; the switch is back where it was.
 */
data class NotificationSyncState(
    val account: Boolean = false,
    val failure: AppError? = null,
)

/**
 * The reader's own switches: which notices to show, and whether to save data.
 *
 * Signed in, the notice switches are the account's (DECISION-077): read from the backend when
 * Settings opens, and each change sent to it. Signed out, they are this device's.
 */
open class PreferencesViewModel(
    private val preferences: DirectoryPreferencesStore,
    private val notifications: NotificationPreferencesSync,
) : ViewModel() {
    val values: StateFlow<DirectoryPreferences> = preferences.values
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DirectoryPreferences())

    private val _sync = MutableStateFlow(NotificationSyncState())
    val sync: StateFlow<NotificationSyncState> = _sync.asStateFlow()

    /** Called with the session as Settings shows it; signed in, the account's choices are read. */
    fun forAccount(signedIn: Boolean) {
        _sync.value = NotificationSyncState(account = signedIn)
        if (!signedIn) return
        viewModelScope.launch {
            // A failed read keeps the device's copy; there is nothing to undo, so nothing to say.
            notifications.pull()
        }
    }

    fun setNotifications(change: NotificationPreferences.() -> NotificationPreferences) {
        viewModelScope.launch {
            if (!_sync.value.account) {
                preferences.setNotificationPreferences(values.value.notifications.change())
                return@launch
            }
            notifications.change(change).fold(
                onSuccess = { _sync.update { it.copy(failure = null) } },
                onFailure = { error -> _sync.update { it.copy(failure = error.toAppError()) } },
            )
        }
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
