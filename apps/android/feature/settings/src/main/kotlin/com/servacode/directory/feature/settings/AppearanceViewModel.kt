package com.servacode.directory.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.datastore.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The reader's theme: follow the phone, light, or dark.
 *
 * Only the choice lives here; the app's root reads the same preference and applies it, so the
 * whole app changes the moment it is written, this screen included.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val preferences: DirectoryPreferencesStore,
) : ViewModel() {
    val theme: StateFlow<ThemePreference> = preferences.values
        .map { it.themePreference }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ThemePreference.SYSTEM)

    fun choose(value: ThemePreference) {
        viewModelScope.launch { preferences.setThemePreference(value) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
