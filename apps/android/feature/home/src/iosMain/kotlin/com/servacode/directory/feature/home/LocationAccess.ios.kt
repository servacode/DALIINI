package com.servacode.directory.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject

/**
 * Core Location's permission. iOS asks only once: once the reader has answered, asking again
 * shows nothing, so the answer already given is heard at once instead.
 */
@Composable
internal actual fun rememberLocationAccess(onAnswer: (allowed: Boolean) -> Unit): LocationAccess {
    val answer by rememberUpdatedState(onAnswer)
    val access = remember { CoreLocationAccess { answer(it) } }
    // The manager holds its delegate weakly; the screen holds it for as long as it is shown.
    DisposableEffect(access) { onDispose { access.release() } }
    return access
}

private class CoreLocationAccess(private val onAnswer: (Boolean) -> Unit) : LocationAccess {
    private val manager = CLLocationManager()
    private var asked = false
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        // Called when the manager is made, too; only an answer to this screen's question counts.
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            if (asked && manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
                asked = false
                onAnswer(allowed)
            }
        }
    }

    init {
        manager.delegate = delegate
    }

    override val allowed: Boolean
        get() = manager.authorizationStatus.let {
            it == kCLAuthorizationStatusAuthorizedWhenInUse || it == kCLAuthorizationStatusAuthorizedAlways
        }

    override fun ask() {
        if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
            onAnswer(allowed)
            return
        }
        asked = true
        manager.requestWhenInUseAuthorization()
    }

    fun release() {
        manager.delegate = null
    }
}
