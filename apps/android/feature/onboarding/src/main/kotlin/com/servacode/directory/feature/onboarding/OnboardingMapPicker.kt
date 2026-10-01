package com.servacode.directory.feature.onboarding

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapLibreController
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.MapStyle
import com.servacode.directory.core.maps.rememberMapViewWithLifecycle

/**
 * The owner's location picker. A tap marks a point and nothing more: saving is the owner's
 * separate, explicit step once they have seen the pin where they meant it.
 *
 * [camera] is where the picker looks: it opens there and moves when it changes, as when the
 * owner asks for their own position. [point] is the pin, the point not yet saved or else the one
 * that is.
 */
@Composable
fun OnboardingMapPicker(
    styleUrl: String,
    camera: MapCamera?,
    point: MapPoint?,
    onTap: (MapPoint) -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(MAP_HEIGHT)) {
        if (MapStyle.isConfigured(styleUrl)) {
            PickerMap(styleUrl, camera, point, onTap)
        } else {
            Text(stringResource(R.string.onboarding_map_not_configured))
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun PickerMap(
    styleUrl: String,
    camera: MapCamera?,
    point: MapPoint?,
    onTap: (MapPoint) -> Unit,
) {
    val mapView = rememberMapViewWithLifecycle()
    var controller by remember(mapView) { mutableStateOf<MapLibreController?>(null) }
    val currentCamera by rememberUpdatedState(camera)
    val tap by rememberUpdatedState(onTap)

    AndroidView(
        factory = {
            mapView.apply {
                // The picker sits in a scrolling list; a drag that starts on the map moves the map.
                setOnTouchListener { view, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    false
                }
                getMapAsync { map ->
                    val mapController = MapLibreController(map)
                    currentCamera?.let { mapController.moveCamera(it, animated = false) }
                    mapController.setOnPointSelected { tap(it) }
                    map.setStyle(styleUrl) { controller = mapController }
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(MAP_HEIGHT),
    )

    LaunchedEffect(controller, camera) {
        val map = controller ?: return@LaunchedEffect
        if (camera != null && map.camera?.center != camera.center) map.moveCamera(camera, animated = true)
    }
    LaunchedEffect(controller, point) {
        val map = controller ?: return@LaunchedEffect
        if (point != null) map.showSelectionPoint(point) else map.clearFacilities()
    }
}

/* How tall the map a person drops their pin on is, written once rather than at both call sites. */
private val MAP_HEIGHT = 260.dp
