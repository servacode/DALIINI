package com.servacode.directory.core.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue

/**
 * The iPhone's [NetworkMonitor], on the Network framework's path monitor: one reading of the
 * route out when collected, and another whenever it changes.
 *
 * Both answers come from the same reading, as Android's do from the same capabilities.
 */
class IosNetworkMonitor : NetworkMonitor {
    override val online: Flow<Boolean> = paths().map { it.online }.distinctUntilChanged()

    override val unmetered: Flow<Boolean> = paths().map { it.unmetered }.distinctUntilChanged()

    private fun paths(): Flow<PathReading> = callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            trySend(
                PathReading(
                    satisfied = nw_path_get_status(path) == nw_path_status_satisfied,
                    expensive = nw_path_is_expensive(path),
                    constrained = nw_path_is_constrained(path),
                ),
            )
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u))
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }
}

/**
 * One reading of the route out.
 *
 * "Unmetered" is what Android calls NOT_METERED. Apple splits it in two: an expensive route
 * (mobile data, or a phone's hotspot) and a constrained one (the reader turned on Low Data Mode).
 * Either is a reason not to spend tens of megabytes unasked.
 */
internal data class PathReading(
    val satisfied: Boolean,
    val expensive: Boolean,
    val constrained: Boolean,
) {
    val online: Boolean get() = satisfied
    val unmetered: Boolean get() = satisfied && !expensive && !constrained
}
