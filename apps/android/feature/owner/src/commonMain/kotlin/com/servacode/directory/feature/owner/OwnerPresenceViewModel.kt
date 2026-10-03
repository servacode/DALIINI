package com.servacode.directory.feature.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Whether this account has facilities of its own, for the shell to decide what the bar holds.
 *
 * Someone who manages a pharmacy opens its hours, its duty roster and its photographs far more
 * often than they browse the directory — so for them the facilities are a place, not a page
 * inside a menu. For everyone else the tab does not exist, because an empty tab is a promise
 * the app cannot keep.
 *
 * It follows the session: signing out takes the tab away with it, and signing in asks again.
 * A failure is read as "none", which hides the tab rather than showing an empty one.
 */
open class OwnerPresenceViewModel(
    private val facilities: LoadOwnerFacilitiesUseCase,
    session: SessionCoordinator,
) : ViewModel() {
    private val _ownsFacility = MutableStateFlow(false)
    val ownsFacility: StateFlow<Boolean> = _ownsFacility.asStateFlow()

    init {
        viewModelScope.launch {
            session.state.collect { state ->
                _ownsFacility.value = state == SessionState.SIGNED_IN &&
                    facilities().getOrDefault(emptyList()).isNotEmpty()
            }
        }
    }
}
