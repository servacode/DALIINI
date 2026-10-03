package com.servacode.directory.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLErrorDenied
import platform.CoreLocation.kCLErrorDomain
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * The iPhone's [LocationProvider], on Core Location, keeping the Android provider's rules:
 *
 *  - it never asks for the permission; without it every call answers [LocationResult.PermissionDenied]
 *    (the screen that asks is the app's);
 *  - a fresh fix is waited for up to the timeout, and then the last known one stands in for it;
 *  - [LocationFix.precise] is whether the reader allowed the precise position.
 *
 * Core Location calls back on the run loop of the thread a manager was made on, so every manager
 * that waits for a fix is made, and stopped, on the main thread.
 */
@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {
    /** For what needs no callback: the permission, the precision, the last known position. */
    private val status by lazy { CLLocationManager() }

    override suspend fun current(timeoutMillis: Long): LocationResult {
        if (!isAllowed(status.authorizationStatus)) return LocationResult.PermissionDenied
        val fresh = withTimeoutOrNull(timeoutMillis) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<CLLocation?> { continuation ->
                    val manager = CLLocationManager()
                    val delegate = Callbacks(
                        onLocation = { location ->
                            manager.stopUpdatingLocation()
                            if (continuation.isActive) continuation.resume(location)
                        },
                        onError = { _ ->
                            manager.stopUpdatingLocation()
                            if (continuation.isActive) continuation.resume(null)
                        },
                    )
                    manager.delegate = delegate
                    manager.desiredAccuracy = kCLLocationAccuracyBest
                    manager.requestLocation()
                    continuation.invokeOnCancellation {
                        manager.stopUpdatingLocation()
                        // The manager holds its delegate weakly: naming it here is also what keeps
                        // it alive while the request waits.
                        if (manager.delegate === delegate) manager.delegate = null
                    }
                }
            }
        }
        return fresh?.toFix(precise())?.let(LocationResult::Available)
            ?: lastKnown()?.let(LocationResult::Available)
            ?: LocationResult.Unavailable
    }

    /**
     * Core Location keeps one last position, already the best it has; Android chose between its
     * satellite and network ones, which the iPhone merges itself.
     */
    override fun lastKnown(): LocationFix? {
        if (!isAllowed(status.authorizationStatus)) return null
        return status.location?.toFix(precise())
    }

    override fun updates(minTimeMillis: Long): Flow<LocationResult> = callbackFlow {
        if (!isAllowed(status.authorizationStatus)) {
            trySend(LocationResult.PermissionDenied)
            close()
            return@callbackFlow
        }
        val throttle = Throttle(minTimeMillis.coerceAtLeast(MIN_INTERVAL_MILLIS))
        val manager = CLLocationManager()
        val delegate = Callbacks(
            onLocation = { location ->
                val fix = location.toFix(precise())
                if (fix != null && throttle.admit(fix.capturedAtEpochMillis)) {
                    trySend(LocationResult.Available(fix))
                }
            },
            onError = { error ->
                // A denial ends the stream, as a withdrawn permission does on Android; anything
                // else ("no fix yet") is Core Location saying it is still trying.
                if (error.isDenial()) {
                    trySend(LocationResult.PermissionDenied)
                    close()
                }
            },
        )
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.startUpdatingLocation()
        awaitClose {
            manager.stopUpdatingLocation()
            // As in current(): the manager holds its delegate weakly, and this keeps it alive.
            if (manager.delegate === delegate) manager.delegate = null
        }
    }.flowOn(Dispatchers.Main)

    private fun precise(): Boolean =
        status.accuracyAuthorization == CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy

    private class Callbacks(
        val onLocation: (CLLocation) -> Unit,
        val onError: (NSError) -> Unit,
    ) : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            (didUpdateLocations.lastOrNull() as? CLLocation)?.let(onLocation)
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            onError(didFailWithError)
        }
    }

    private companion object {
        /** As on Android: no stream asks for positions faster than twice a second. */
        const val MIN_INTERVAL_MILLIS = 500L
    }
}

/** Whether the reader has let the app read the position while it is in use. */
internal fun isAllowed(status: CLAuthorizationStatus): Boolean =
    status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways

/** A position as the shared code knows it; null for one Core Location marks as invalid. */
@OptIn(ExperimentalForeignApi::class)
internal fun CLLocation.toFix(precise: Boolean): LocationFix? {
    // A negative accuracy is Core Location's way of saying the coordinate is not valid.
    if (horizontalAccuracy < 0) return null
    return coordinate.useContents {
        LocationFix(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = horizontalAccuracy.toFloat(),
            precise = precise,
            capturedAtEpochMillis = (timestamp.timeIntervalSince1970 * 1000).toLong(),
        )
    }
}

internal fun NSError.isDenial(): Boolean = domain == kCLErrorDomain && code == kCLErrorDenied.toLong()

/**
 * Lets through at most one position per [intervalMillis]. Core Location has no interval of its own
 * (Android's `minTime`); it reports whenever it has something, often several times a second.
 */
internal class Throttle(private val intervalMillis: Long) {
    private var last: Long? = null

    fun admit(at: Long): Boolean {
        val previous = last
        if (previous != null && at - previous < intervalMillis) return false
        last = at
        return true
    }
}
