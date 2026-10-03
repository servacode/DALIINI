package com.servacode.directory.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.database.RecentlyViewedStore
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.model.RecentFacility
import com.servacode.directory.core.network.NetworkMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What Home shows beside the directory: the facilities opened lately, whether data saver is on,
 * and whether to offer it. Its own view model, so none of it can hold up the list. Android's
 * navigation asks Hilt for the subclass in androidMain (DECISION-095).
 */
open class HomeExtrasViewModel(
    recentlyViewed: RecentlyViewedStore,
    private val preferences: DirectoryPreferencesStore,
    network: NetworkMonitor,
) : ViewModel() {
    val recent: StateFlow<List<RecentFacility>> = recentlyViewed.observe()
        .map { it.take(HOME_RECENT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val dataSaver: StateFlow<Boolean> = preferences.values
        .map { it.dataSaver }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val offerDataSaver: StateFlow<Boolean> = combine(
        network.online,
        network.unmetered,
        preferences.values,
    ) { online, unmetered, values ->
        DataSaverSuggestion.shouldOffer(online, unmetered, values.dataSaver, values.dataSaverSuggested)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    fun acceptDataSaver() {
        viewModelScope.launch {
            preferences.setDataSaver(true)
            preferences.setDataSaverSuggested()
        }
    }

    fun dismissDataSaver() {
        viewModelScope.launch { preferences.setDataSaverSuggested() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        /** Home shows a handful; the account lists them all. */
        const val HOME_RECENT = 8
    }
}
