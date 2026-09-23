package com.servacode.directory.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.AvailabilityPill
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMessageState
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.Radius
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.maps.FacilityMapPin
import com.servacode.directory.core.maps.MapLibreController
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.MapStyle
import com.servacode.directory.core.maps.rememberMapViewWithLifecycle
import com.servacode.directory.core.model.PublicMapFacility

/**
 * Screen 05. The province's facilities where they stand.
 *
 * The map itself is unchanged: the camera it opens on, what it keeps while its view comes and
 * goes, and what a marker does when it is pressed all stay as they were. What is new is the
 * frame around it — a bar that says where the user is and a way back — and the card that names
 * the facility the map is holding onto.
 */
@Composable
fun MapScreen(
    styleUrl: String,
    onFacility: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DirectoryPage(
        topBar = { DirectoryTopBar(title = MapCopy.TITLE, onBack = onBack) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !MapStyle.isConfigured(styleUrl) -> DirectoryMessageState(
                    icon = DirectoryIcons.map,
                    title = MapCopy.UNCONFIGURED,
                    body = MapCopy.UNCONFIGURED_BODY,
                )
                // Shown only once the start is known, so it never opens on the whole world.
                state.cameraResolved -> FacilityMap(styleUrl, state, viewModel, onFacility)
                else -> DirectoryLoading()
            }
            val selected = state.facilities.firstOrNull { it.id == state.selectedFacilityId }
            if (selected != null && state.cameraResolved) {
                SelectedFacilityCard(
                    facility = selected,
                    onClick = { onFacility(selected.id) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Space.base),
                )
            }
        }
    }
}

/** The facility the map is holding onto, named rather than left as a marker among markers. */
@Composable
private fun SelectedFacilityCard(
    facility: PublicMapFacility,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DirectoryCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.categoryCircle)
                    .clip(RoundedCornerShape(Radius.medium))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                DirectoryIcon(
                    icon = DirectoryIcons.category(facility.categoryIconKey),
                    contentDescription = null,
                    size = IconSize.large,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Text(
                    text = facility.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AvailabilityPill(facility.availability)
            }
            DirectoryIcon(
                icon = DirectoryIcons.chevron,
                contentDescription = null,
                modifier = Modifier.clearAndSetSemantics { },
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
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

/** The words of the map, provisional until product copy is approved. */
object MapCopy {
    const val TITLE = "الخريطة"
    const val UNCONFIGURED = "الخريطة غير متاحة"
    const val UNCONFIGURED_BODY = "يجب ضبط مزود خرائط الإنتاج قبل عرض الخريطة"
}
