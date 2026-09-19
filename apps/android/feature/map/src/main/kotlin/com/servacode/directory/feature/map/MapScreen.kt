package com.servacode.directory.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.maps.FacilityMapPin
import com.servacode.directory.core.maps.MapLibreController
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.MapStyle
import com.servacode.directory.core.maps.rememberMapViewWithLifecycle

@Composable
fun MapScreen(
    styleUrl: String,
    onFacility: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when {
            !MapStyle.isConfigured(styleUrl) ->
                Text("يجب ضبط مزود خرائط الإنتاج قبل عرض الخريطة", Modifier.padding(20.dp))
            // Shown only once the start is known, so it never opens on the whole world.
            state.cameraResolved -> FacilityMap(styleUrl, state, viewModel, onFacility)
        }
    }
}

@Composable
private fun FacilityMap(
    styleUrl: String,
    state: MapUiState,
    viewModel: MapViewModel,
    onFacility: (String) -> Unit,
) {
    val mapView = rememberMapViewWithLifecycle()
    var controller by remember(mapView) { mutableStateOf<MapLibreController?>(null) }
    val openFacility by rememberUpdatedState(onFacility)

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    val mapController = MapLibreController(map)
                    // The ViewModel's camera: the start, or where the user left this map.
                    viewModel.state.value.camera?.let { mapController.moveCamera(it, animated = false) }
                    map.addOnCameraIdleListener {
                        val camera = mapController.camera ?: return@addOnCameraIdleListener
                        val bounds = map.projection.visibleRegion.latLngBounds
                        viewModel.cameraIdle(
                            camera,
                            MapViewport(
                                west = bounds.longitudeWest,
                                south = bounds.latitudeSouth,
                                east = bounds.longitudeEast,
                                north = bounds.latitudeNorth,
                            ),
                        )
                    }
                    mapController.setOnFacilitySelected { id ->
                        viewModel.facilityChosen(id)
                        openFacility(id)
                    }
                    map.setStyle(styleUrl) { controller = mapController }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )

    // Drawn on every new view from what the ViewModel already holds; nothing is fetched for it.
    LaunchedEffect(controller, state.facilities, state.selectedFacilityId) {
        val map = controller ?: return@LaunchedEffect
        map.showFacilities(
            state.facilities.map { FacilityMapPin(it.id, MapPoint(it.latitude, it.longitude), it.label) },
            state.selectedFacilityId,
        )
    }
}
