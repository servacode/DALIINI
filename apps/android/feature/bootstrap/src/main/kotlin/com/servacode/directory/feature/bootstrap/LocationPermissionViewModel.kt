package com.servacode.directory.feature.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Remembers the answer to the location question, and that the first run is over. */
@HiltViewModel
class LocationPermissionViewModel @Inject constructor(
    private val preferences: DirectoryPreferencesStore,
) : ViewModel() {
    fun answered(answer: LocationAnswer) {
        viewModelScope.launch {
            preferences.setLocationPreference(answer.preference())
            preferences.setWelcomeCompleted()
        }
    }
}
