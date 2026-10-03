package com.servacode.directory.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.database.RecentlyViewedStore
import com.servacode.directory.core.model.RecentFacility
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** «شوهدت مؤخراً», as the account lists it: every one kept, and a way to forget them all. */
open class RecentlyViewedViewModel(
    private val store: RecentlyViewedStore,
) : ViewModel() {
    /** Null until the device has answered; an empty list is "nothing viewed yet". */
    val items: StateFlow<List<RecentFacility>?> = store.observe()
        .map<List<RecentFacility>, List<RecentFacility>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun clear() {
        viewModelScope.launch { store.clear() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
