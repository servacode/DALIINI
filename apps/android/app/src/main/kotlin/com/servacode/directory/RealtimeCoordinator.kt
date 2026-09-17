package com.servacode.directory

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.servacode.directory.core.datastore.PreferencesRepository
import com.servacode.directory.core.network.NetworkMonitor
import com.servacode.directory.core.network.RealtimeEventDeduplicator
import com.servacode.directory.core.network.RealtimeInvalidationBus
import com.servacode.directory.core.network.RealtimeSignal
import com.servacode.directory.core.network.RealtimeStream
import com.servacode.directory.core.network.ReconnectPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeCoordinator @Inject constructor(
    private val preferences: PreferencesRepository,
    private val networkMonitor: NetworkMonitor,
    private val stream: RealtimeStream,
    private val bus: RealtimeInvalidationBus,
    private val deduplicator: RealtimeEventDeduplicator,
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reconnectPolicy = ReconnectPolicy()
    private var job: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        if (job?.isActive == true) return
        job = scope.launch {
            combine(
                networkMonitor.online,
                preferences.values.map { it.selectedProvinceId }.distinctUntilChanged(),
            ) { online, provinceId -> online to provinceId }
                .distinctUntilChanged()
                .collectLatest { (online, provinceId) ->
                    if (!online || provinceId.isNullOrBlank()) return@collectLatest
                    collectWithReconnect(provinceId)
                }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        job?.cancel()
        job = null
    }

    private suspend fun collectWithReconnect(provinceId: String) {
        var consecutiveFailures = 0
        while (currentCoroutineContext().isActive) {
            var connectedAt: Long? = null
            try {
                stream.events(provinceId).collect { signal ->
                    when (signal) {
                        RealtimeSignal.Connected -> connectedAt = SystemClock.elapsedRealtime()
                        is RealtimeSignal.Event -> {
                            if (deduplicator.shouldDeliver(signal.value)) bus.publish(signal.value)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Reconnect below; REST/cache remain the truth and fallback.
            }
            val stableMillis = connectedAt?.let { SystemClock.elapsedRealtime() - it } ?: 0L
            consecutiveFailures = if (stableMillis >= 15_000L) 0 else consecutiveFailures + 1
            val attempt = (consecutiveFailures - 1).coerceAtLeast(0)
            delay(reconnectPolicy.delayMillis(attempt, Random.nextDouble(-1.0, 1.0)))
        }
    }
}
