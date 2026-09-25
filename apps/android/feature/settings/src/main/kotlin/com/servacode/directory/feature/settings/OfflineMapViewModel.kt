package com.servacode.directory.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.maps.MapPackState
import com.servacode.directory.core.maps.MapPackTarget
import com.servacode.directory.core.maps.OfflineMapPacks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The reader's say over the map kept on their device.
 *
 * The app fetches the province's map by itself on a connection nobody pays for; this is where
 * somebody who wants it sooner asks for it, and where somebody who wants the space back says so.
 * A deletion is remembered as a refusal, so the app does not quietly fetch it again the next time
 * they are on Wi-Fi — and asking for it here withdraws the refusal.
 *
 * The refusal is written before the pack is touched, in that order: the coordinator watching both
 * would otherwise see a pack that is suddenly absent while the preference still says it is wanted,
 * and start the download the reader has just cancelled.
 */
@HiltViewModel
class OfflineMapViewModel @Inject constructor(
    private val packs: OfflineMapPacks,
    private val preferences: DirectoryPreferencesStore,
) : ViewModel() {
    val state: StateFlow<MapPackState> = packs.state
    val target: StateFlow<MapPackTarget?> = packs.target

    fun download() {
        viewModelScope.launch {
            preferences.setOfflineMapDeclined(false)
            packs.download()
        }
    }

    fun pause() {
        packs.pause()
    }

    fun remove() {
        viewModelScope.launch {
            preferences.setOfflineMapDeclined(true)
            packs.remove()
        }
    }
}
