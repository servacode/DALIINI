package com.servacode.directory.core.maps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.maps.MapView

/**
 * A MapView that lives as long as the calling composable and follows its screen's lifecycle.
 *
 * The view is released when the composable leaves, for instance when another screen opens on
 * top of it, so nothing that must outlive that (the camera, the selection) may live in it: that
 * belongs to the screen's ViewModel, which hands it back when the view is created again.
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember(context, lifecycle) { MapView(context) }
    DisposableEffect(mapView) {
        val steps = MapLifecycleSteps(
            object : MapViewCalls {
                override fun create() = mapView.onCreate(null)
                override fun start() = mapView.onStart()
                override fun resume() = mapView.onResume()
                override fun pause() = mapView.onPause()
                override fun stop() = mapView.onStop()
                override fun destroy() = mapView.onDestroy()
            },
        )
        // Added observers are brought up to the current state, so this also creates the view.
        val observer = LifecycleEventObserver { _, event -> steps.moveTo(event.targetState.toLevel()) }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            steps.destroy()
        }
    }
    return mapView
}

private fun Lifecycle.State.toLevel(): MapLifecycleSteps.Level = when (this) {
    Lifecycle.State.DESTROYED -> MapLifecycleSteps.Level.DESTROYED
    Lifecycle.State.INITIALIZED -> MapLifecycleSteps.Level.NONE
    Lifecycle.State.CREATED -> MapLifecycleSteps.Level.CREATED
    Lifecycle.State.STARTED -> MapLifecycleSteps.Level.STARTED
    Lifecycle.State.RESUMED -> MapLifecycleSteps.Level.RESUMED
}
