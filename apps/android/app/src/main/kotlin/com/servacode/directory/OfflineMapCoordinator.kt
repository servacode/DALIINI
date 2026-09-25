package com.servacode.directory

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.maps.MapPackAction
import com.servacode.directory.core.maps.MapPackState
import com.servacode.directory.core.maps.MapPackTarget
import com.servacode.directory.core.maps.OfflineMapPacks
import com.servacode.directory.core.maps.mapPackAction
import com.servacode.directory.core.maps.toMapPoint
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.NetworkMonitor
import com.servacode.directory.feature.province.ProvinceUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * When the province's map is fetched, and when it is left alone.
 *
 * The rule, in one place: keep the map of the province the reader chose, fetch it on a connection
 * nobody pays by the megabyte for, and never fetch it again after they have deleted it. A trip in
 * Syria that outlives the connection is the point of the whole thing, and it is worth nothing if
 * the twenty megabytes were taken out of someone's bundle without asking.
 *
 * The mechanics are `OfflineMapPacks`, which knows nothing about provinces or preferences; the
 * province and the point its map opens on are read here, where the app already has both.
 */
@Singleton
class OfflineMapCoordinator @Inject constructor(
    private val packs: OfflineMapPacks,
    private val preferences: DirectoryPreferencesStore,
    private val provinces: ProvinceUseCase,
    private val networkMonitor: NetworkMonitor,
) {
    /**
     * Whether it was this coordinator that started the download, rather than the reader.
     *
     * It decides one thing: whether leaving the Wi-Fi stops the download. What the app started for
     * free it stops rather than continuing on someone's bundle; what the reader asked for, they
     * asked for, and it carries on.
     */
    private var startedByApp = false

    fun start(scope: CoroutineScope) {
        scope.launch {
            target().collect(packs::follow)
        }
        scope.launch {
            combine(
                packs.state,
                networkMonitor.unmetered,
                preferences.values.map { !it.offlineMapDeclined }.distinctUntilChanged(),
            ) { state, unmetered, wanted -> Decision(state, unmetered, wanted) }
                .collect(::act)
        }
    }

    /**
     * Do what the policy says.
     *
     * The policy itself is `mapPackAction`, which is pure and tested; this only remembers who
     * started the download, because that is the one thing the policy cannot see from the state.
     */
    private fun act(decision: Decision) {
        val (state, unmetered, wanted) = decision
        // A pack that is not there is not one this coordinator is in the middle of.
        if (state is MapPackState.Absent) startedByApp = false
        when (mapPackAction(state, unmetered, wanted, startedByApp)) {
            MapPackAction.DOWNLOAD -> {
                startedByApp = true
                packs.download()
            }
            MapPackAction.PAUSE -> packs.pause()
            MapPackAction.NOTHING -> Unit
        }
    }

    /**
     * The province's map to keep, and the point to centre it on.
     *
     * The chosen province and the list it belongs to: the list is cache-first, so this answers on
     * a device that has been offline since it started. A province the platform has no point for
     * gives no target rather than a guess — a pack centred on the wrong city is worse than none,
     * because it is twenty megabytes of somewhere the reader is not.
     */
    private fun target() = combine(
        preferences.values.map { it.selectedProvinceId }.distinctUntilChanged(),
        provinces.provinces().map(::provincesOf).distinctUntilChanged(),
    ) { id, list ->
        val province = list.firstOrNull { it.id == id } ?: return@combine null
        val centre = province.mapCenter ?: return@combine null
        MapPackTarget(province.id, centre.toMapPoint())
    }.distinctUntilChanged()

    private fun provincesOf(loaded: Loaded<List<Province>>): List<Province> = when (loaded) {
        is Loaded.Cached -> loaded.value
        is Loaded.Fresh -> loaded.value
        is Loaded.Stale -> loaded.value
        is Loaded.Failed -> emptyList()
    }

    private data class Decision(
        val state: MapPackState,
        val unmetered: Boolean,
        val wanted: Boolean,
    )
}
