package com.servacode.directory.core.maps

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The province's map, kept on the device.
 *
 * One pack at a time: the province the reader chose. A route is guided from the phone once it has
 * been computed, so what a cut connection actually takes away is the picture under the line — and
 * on a road in Raqqa that is the difference between following a map and following a grey field.
 *
 * This is the machinery: MapLibre's own offline database, which is also the cache the map reads
 * from, so a tile fetched here is a tile the map does not ask for again. What to keep and when to
 * fetch it is decided elsewhere ([follow] is called with the province, and [download] by the
 * reader or by the app on a connection nobody pays by the megabyte for). The arithmetic of how
 * much that is lives in `MapPack.kt`, where a test can check it.
 *
 * **Everything here runs on the main thread.** MapLibre's offline API builds its callback handler
 * from whichever thread first asks for the manager, and answers on it; the work itself happens on
 * the engine's own threads, so the main thread only starts it and receives progress.
 */
@Singleton
class OfflineMapPacks @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: MapProviderConfig,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _target = MutableStateFlow<MapPackTarget?>(null)
    val target: StateFlow<MapPackTarget?> = _target.asStateFlow()

    private val _state = MutableStateFlow<MapPackState>(MapPackState.Unknown)
    val state: StateFlow<MapPackState> = _state.asStateFlow()

    private var manager: OfflineManager? = null
    private var region: OfflineRegion? = null

    /**
     * Which province's map to keep.
     *
     * Called when the reader's province becomes known and whenever it changes. A pack for another
     * province is deleted: the reader has one province at a time, and a map of somewhere they left
     * is space taken from the one they are in. Null — no province yet, or one the platform has no
     * point for — leaves the pack alone and says so.
     */
    fun follow(next: MapPackTarget?) = onMain {
        if (next == _target.value && next != null) return@onMain
        _target.value = next
        region?.setObserver(null)
        region = null
        if (next == null || !MapStyle.isConfigured(config.styleUrl)) {
            _state.value = MapPackState.Unknown
            return@onMain
        }
        _state.value = MapPackState.Absent
        adopt(next)
    }

    /**
     * Fetch what is missing, or carry on from where a stopped download left off.
     *
     * Safe to call when the pack is already complete: the engine finds every resource in its own
     * database and finishes immediately.
     */
    fun download() = onMain {
        val wanted = _target.value ?: return@onMain
        val existing = region
        if (existing == null) create(wanted) else start(existing)
    }

    /** Stop fetching, keeping what has arrived. */
    fun pause() = onMain {
        val existing = region ?: return@onMain
        existing.setDownloadState(OfflineRegion.STATE_INACTIVE)
        val current = _state.value
        if (current is MapPackState.Downloading) _state.value = current.copy(running = false)
    }

    /** Forget the pack and give the space back. */
    fun remove() = onMain {
        val existing = region ?: run {
            _state.value = if (_target.value == null) MapPackState.Unknown else MapPackState.Absent
            return@onMain
        }
        region = null
        existing.setObserver(null)
        existing.setDownloadState(OfflineRegion.STATE_INACTIVE)
        existing.delete(
            object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() {
                    _state.value = if (_target.value == null) MapPackState.Unknown else MapPackState.Absent
                }

                override fun onError(error: String) {
                    _state.value = MapPackState.Failed(MapPackFailure.OTHER)
                }
            },
        )
    }

    /** Take over the pack that is already on the device for this target, or report none. */
    private fun adopt(wanted: MapPackTarget) {
        val offline = manager()
        offline.listOfflineRegions(
            object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    if (wanted != _target.value) return
                    val regions = offlineRegions?.toList() ?: emptyList()
                    val mine = regions.firstOrNull { it.matches(wanted) }
                    // A pack for a province the reader left, or one built to an older box, style
                    // or zoom, is not what this device needs any more.
                    regions.filter { it !== mine && it.isOurs() }.forEach { stale ->
                        stale.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        stale.delete(NoopDelete)
                    }
                    if (mine == null) {
                        _state.value = MapPackState.Absent
                        return
                    }
                    region = mine
                    mine.setObserver(Progress(wanted))
                    mine.getStatus(
                        object : OfflineRegion.OfflineRegionStatusCallback {
                            override fun onStatus(status: OfflineRegionStatus?) {
                                if (status != null && wanted == _target.value) publish(status)
                            }

                            override fun onError(error: String?) {
                                if (wanted == _target.value) _state.value = MapPackState.Absent
                            }
                        },
                    )
                }

                override fun onError(error: String) {
                    _state.value = MapPackState.Failed(MapPackFailure.OTHER)
                }
            },
        )
    }

    /** Make the pack this device should have; the engine hands it back, and it starts there. */
    private fun create(wanted: MapPackTarget) {
        val box = wanted.box
        val definition = OfflineTilePyramidRegionDefinition(
            config.styleUrl,
            LatLngBounds.from(box.north, box.east, box.south, box.west),
            PACK_MIN_ZOOM.toDouble(),
            PACK_MAX_ZOOM.toDouble(),
            context.resources.displayMetrics.density,
        )
        manager().createOfflineRegion(
            definition,
            metadataFor(wanted),
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    // The reader moved on while the engine was writing the row: the pack they no
                    // longer need is deleted rather than filled.
                    if (wanted != _target.value) {
                        offlineRegion.delete(NoopDelete)
                        return
                    }
                    region = offlineRegion
                    start(offlineRegion)
                }

                override fun onError(error: String) {
                    _state.value = MapPackState.Failed(MapPackFailure.OTHER)
                }
            },
        )
    }

    private fun start(offlineRegion: OfflineRegion) {
        offlineRegion.setObserver(Progress(_target.value ?: return))
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
        val current = _state.value
        _state.value = when (current) {
            is MapPackState.Downloading -> current.copy(running = true)
            is MapPackState.Ready -> current
            else -> MapPackState.Downloading(completed = 0, required = 0, bytes = 0)
        }
    }

    private fun publish(status: OfflineRegionStatus) {
        _state.value = if (status.isComplete) {
            MapPackState.Ready(tiles = status.completedTileCount, bytes = status.completedResourceSize)
        } else {
            MapPackState.Downloading(
                completed = status.completedResourceCount,
                required = status.requiredResourceCount,
                bytes = status.completedResourceSize,
                running = status.downloadState == OfflineRegion.STATE_ACTIVE,
            )
        }
    }

    private inner class Progress(private val wanted: MapPackTarget) : OfflineRegion.OfflineRegionObserver {
        override fun onStatusChanged(status: OfflineRegionStatus) {
            if (wanted == _target.value) publish(status)
        }

        override fun onError(error: OfflineRegionError) {
            if (wanted != _target.value) return
            // A connection that went is not a failure of the pack: what arrived is kept and the
            // rest is fetched the next time there is a network. The state says stopped, not broken.
            val reason = when (error.reason) {
                OfflineRegionError.REASON_CONNECTION -> MapPackFailure.CONNECTION
                OfflineRegionError.REASON_SERVER, OfflineRegionError.REASON_NOT_FOUND -> MapPackFailure.SERVER
                else -> MapPackFailure.OTHER
            }
            val current = _state.value
            _state.value = when {
                reason == MapPackFailure.CONNECTION && current is MapPackState.Downloading ->
                    current.copy(running = false)
                else -> MapPackState.Failed(reason)
            }
        }

        override fun mapboxTileCountLimitExceeded(limit: Long) {
            if (wanted == _target.value) _state.value = MapPackState.Failed(MapPackFailure.TILE_LIMIT)
        }
    }

    /**
     * The engine's offline API, on the thread it was built from.
     *
     * `Dispatchers.Main.immediate` runs the body in place when the caller is already on the main
     * thread, so a reader pressing a button does not wait a frame for it.
     */
    private fun onMain(body: () -> Unit) {
        scope.launch { body() }
    }

    private fun manager(): OfflineManager = manager ?: OfflineManager.getInstance(context).also {
        // A province is about a thousand tiles; the ceiling is set well above that so a wider
        // province cannot silently stop halfway, and far below anything that would fill a phone.
        it.setOfflineMapboxTileCountLimit(TILE_CEILING)
        manager = it
    }

    /** Whether this region is the one this device should be keeping. */
    private fun OfflineRegion.matches(wanted: MapPackTarget): Boolean {
        if (String(metadata, Charsets.UTF_8) != metadataString(wanted)) return false
        val shape = definition
        if (shape.styleURL != config.styleUrl) return false
        if (shape.minZoom.toInt() != PACK_MIN_ZOOM || shape.maxZoom.toInt() != PACK_MAX_ZOOM) return false
        val bounds = shape.bounds ?: return false
        return wanted.box.sameAs(
            MapPackBox(
                north = bounds.latitudeNorth,
                east = bounds.longitudeEast,
                south = bounds.latitudeSouth,
                west = bounds.longitudeWest,
            ),
        )
    }

    /** Whether this app made the region, so that nothing else on the device is deleted. */
    private fun OfflineRegion.isOurs(): Boolean =
        String(metadata, Charsets.UTF_8).startsWith(METADATA_PREFIX)

    private fun metadataFor(wanted: MapPackTarget): ByteArray =
        metadataString(wanted).toByteArray(Charsets.UTF_8)

    private fun metadataString(wanted: MapPackTarget): String = "$METADATA_PREFIX${wanted.provinceId}"

    private object NoopDelete : OfflineRegion.OfflineRegionDeleteCallback {
        override fun onDelete() = Unit
        override fun onError(error: String) = Unit
    }

    private companion object {
        /** Version 1 of this app's own regions, so a later shape can be told from this one. */
        const val METADATA_PREFIX = "dalini:pack:1:"
        const val TILE_CEILING = 12_000L
    }
}
