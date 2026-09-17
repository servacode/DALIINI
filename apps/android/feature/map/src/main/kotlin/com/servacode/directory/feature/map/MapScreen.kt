package com.servacode.directory.feature.map

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.maps.FacilityMapPin
import com.servacode.directory.core.maps.MapLibreController
import com.servacode.directory.core.maps.MapPoint
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun MapScreen(
    styleUrl: String,
    onFacility: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mapView = remember { MapView(context) }
    val mapState = remember { arrayOfNulls<MapLibreMap>(1) }
    val configured = styleUrl.startsWith("https://") && !styleUrl.contains("<ROOT_DOMAIN>")

    DisposableEffect(mapView, lifecycleOwner) {
        mapView.onCreate(Bundle())
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (!configured) {
            Text("يجب ضبط مزود خرائط الإنتاج قبل عرض الخريطة", Modifier.padding(20.dp))
        } else {
            AndroidView(
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            mapState[0] = map
                            map.setStyle(styleUrl)
                            map.addOnCameraIdleListener {
                                val bounds = map.projection.visibleRegion.latLngBounds
                                viewModel.viewportChanged(
                                    MapViewport(
                                        west = bounds.longitudeWest,
                                        south = bounds.latitudeSouth,
                                        east = bounds.longitudeEast,
                                        north = bounds.latitudeNorth,
                                    ),
                                )
                            }
                            map.setOnMarkerClickListener { marker ->
                                marker.snippet?.let(onFacility)
                                true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    LaunchedEffect(state, mapState[0]) {
        val map = mapState[0] ?: return@LaunchedEffect
        val content = state as? MapUiState.Content ?: return@LaunchedEffect
        val controller = MapLibreController(map)
        controller.clearFacilities()
        val pins = content.facilities.map {
            FacilityMapPin(it.id, MapPoint(it.latitude, it.longitude), it.label)
        }
        controller.showFacilities(pins)
    }
}
