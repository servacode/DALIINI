package com.servacode.directory.feature.map

import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.AvailabilityPill
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMessageState
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.Elevation
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.Radius
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS
import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.FacilityMapPin
import com.servacode.directory.core.maps.MapLibreController
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.MapStyle
import com.servacode.directory.core.maps.rememberMapViewWithLifecycle
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.PublicMapFacility

/**
 * Screen 05. The province's facilities where they stand.
 *
 * The map itself is unchanged: the camera it opens on, what it keeps while its view comes and
 * goes, and which markers it draws all stay as they were. What changed is what a marker does:
 * it names its facility at the foot of the map, and the page opens from those words rather
 * than from the pin. Nothing frames the map — a title reading "map" above a map says nothing —
 * and a control in the corner points at the user when they ask.
 */
@Composable
fun MapScreen(
    styleUrl: String,
    onFacility: (String) -> Unit,
    onRoute: (String, Double, Double) -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val askLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted -> if (granted.values.any { it }) viewModel.locate() }
    val context = LocalContext.current

    // No bar over the map. A title that says "map" above a map tells the reader nothing they
    // cannot see, and a back arrow on one of the app's three main places leads nowhere the
    // bottom bar does not already go. The height goes to the map instead.
    DirectoryPage(
        bottomBar = bottomBar,
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
            // Over the map, not instead of it: the bar is at the top and the rail hugs the
            // start edge, both narrow enough to leave the map itself the screen.
            MapQuickFilters(
                filters = state.filters,
                offersDuty = state.offersDuty,
                onOpenNow = { viewModel.filter { current -> current.toggleOpenNow() } },
                onDutyNow = { viewModel.filter { current -> current.toggleDutyNow() } },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            if (state.categories.isNotEmpty()) {
                MapCategoryRail(
                    categories = state.categories,
                    selectedId = state.filters.categoryId,
                    onCategory = { id -> viewModel.filter { current -> current.withCategory(id) } },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
            val selected = state.facilities.firstOrNull { it.id == state.selectedFacilityId }
            if (state.cameraResolved) {
                LocateButton(
                    working = state.locating,
                    onClick = {
                        val granted = FOREGROUND_LOCATION_PERMISSIONS.any {
                            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
                        }
                        if (granted) {
                            viewModel.locate()
                        } else {
                            askLocation.launch(FOREGROUND_LOCATION_PERMISSIONS.toTypedArray())
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        // Clear of the card that names a marker, so one never covers the other.
                        .padding(
                            start = Space.base,
                            top = Space.base,
                            end = Space.base,
                            bottom = if (selected != null) Sizes.mapCardClearance else Space.base,
                        ),
                )
            }
            if (selected != null && state.cameraResolved) {
                SelectedFacilityCard(
                    facility = selected,
                    onDetails = { onFacility(selected.id) },
                    onRoute = { onRoute(selected.id, selected.latitude, selected.longitude) },
                    onDismiss = viewModel::facilityDismissed,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Space.base),
                )
            }
        }
    }
}

/**
 * The same two questions Home asks, over the map.
 *
 * They are the backend's own filters: the markers change because the query changed, not because
 * the map hid anything. Nearest is not among them — a map is already showing distance.
 */
@Composable
private fun MapQuickFilters(
    filters: MapFilters,
    offersDuty: Boolean,
    onOpenNow: () -> Unit,
    onDutyNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        MapFilterChip(MapCopy.OPEN_NOW, filters.openNow, onOpenNow)
        if (offersDuty) MapFilterChip(MapCopy.DUTY_NOW, filters.dutyNow, onDutyNow)
    }
}

/** A chip that has to read against a map, so it carries its own surface rather than a tint. */
@Composable
private fun MapFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.pill),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = Elevation.low,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = Space.base, vertical = Space.sm),
        )
    }
}

/** Closer and further, a step at a time. */
@Composable
private fun ZoomControls(onIn: () -> Unit, onOut: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        MapRoundButton(DirectoryIcons.plus, MapCopy.ZOOM_IN, onIn)
        // A minus, not a cross. The pair reads as one scale; a cross beside a plus reads as
        // "close", and someone pressing it expects the map to go away rather than widen.
        MapRoundButton(DirectoryIcons.minus, MapCopy.ZOOM_OUT, onOut)
    }
}

/** A control that has to read against a map, so it carries its own surface. */
@Composable
private fun MapRoundButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(Sizes.button),
        shape = RoundedCornerShape(Radius.pill),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Elevation.low,
        enabled = enabled,
    ) {
        Box(contentAlignment = Alignment.Center) {
            DirectoryIcon(
                icon = icon,
                contentDescription = label,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        }
    }
}

/**
 * "Where am I", as a button and not as a habit.
 *
 * A map that follows a person is a different product from one that points at them when asked,
 * and the difference is the whole of the privacy argument. This asks once per press, draws the
 * answer, and forgets about it.
 */
@Composable
private fun LocateButton(working: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MapRoundButton(
        icon = DirectoryIcons.pin,
        label = MapCopy.MY_LOCATION,
        onClick = onClick,
        enabled = !working,
        modifier = modifier,
    )
}

/** One step of scale per press: enough to notice, small enough to aim with. */
private const val ZOOM_STEP = 1.0

/**
 * The province's categories, down the start edge.
 *
 * Whatever the backend serves, in its own order: one category today, and clinics, laboratories
 * or anything else the moment a province starts serving them. It scrolls, so the rail holds as
 * many as arrive without the map losing room.
 */
@Composable
private fun MapCategoryRail(
    categories: List<Category>,
    selectedId: String?,
    onCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .padding(Space.md)
            .heightIn(max = Sizes.railMaxHeight),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedId
            Surface(
                onClick = { onCategory(category.id) },
                shape = RoundedCornerShape(Radius.large),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shadowElevation = Elevation.low,
            ) {
                // The name under the mark, not only in the accessibility tree. An icon alone
                // is a guess for anyone meeting it the first time, and a rail of five guesses
                // is a rail nobody uses — the labels are what make it a list of sections
                // rather than a row of symbols.
                Column(
                    modifier = Modifier
                        .width(Sizes.categoryLabel)
                        .padding(vertical = Space.sm, horizontal = Space.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    DirectoryIcon(
                        icon = DirectoryIcons.category(category.iconKey),
                        // The label beside it already says this; announcing both would read
                        // the name twice.
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Text(
                        text = category.nameAr,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The facility the map is holding onto, named rather than left as a marker among markers. */
@Composable
private fun SelectedFacilityCard(
    facility: PublicMapFacility,
    onDetails: () -> Unit,
    onRoute: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Not itself a button. Pressing a marker is how one reads a name off the map, and reading
    // a name should not cost the map: the page opens from the words below, and nowhere else.
    DirectoryCard(modifier = modifier) {
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
            DirectoryIconButton(
                icon = DirectoryIcons.close,
                label = MapCopy.DISMISS,
                onClick = onDismiss,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
        // Two ways on from a marker: read about it, or go to it. Going is the commoner of
        // the two from a map, so it sits first in the reading order.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Space.md),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            DirectoryPrimaryButton(
                text = MapCopy.ROUTE,
                onClick = onRoute,
                modifier = Modifier.weight(1f),
            )
            DirectorySecondaryButton(
                text = MapCopy.OPEN_DETAILS,
                onClick = onDetails,
                modifier = Modifier.weight(1f),
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

    Box(Modifier.fillMaxSize()) {
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
                    // A marker names its facility at the foot of the map and stops there. The
                    // page is a place one chooses to go, from the card, rather than somewhere
                    // a finger lands by touching a pin the size of a fingertip.
                    mapController.setOnFacilitySelected { id -> viewModel.facilityChosen(id) }
                    map.setStyle(styleUrl) { controller = mapController }
                }
            }
        },
            modifier = Modifier.fillMaxSize(),
        )
        // Beside the map rather than on a menu: two taps is how most people change a map's
        // scale, and pinching with one hand holding a phone is not always available.
        ZoomControls(
            onIn = { controller?.zoomBy(ZOOM_STEP) },
            onOut = { controller?.zoomBy(-ZOOM_STEP) },
            modifier = Modifier.align(Alignment.CenterEnd).padding(Space.md),
        )
    }

    // Drawn on every new view from what the ViewModel already holds; nothing is fetched for it.
    LaunchedEffect(controller, state.facilities, state.selectedFacilityId) {
        val map = controller ?: return@LaunchedEffect
        map.showFacilities(
            state.facilities.map { FacilityMapPin(it.id, MapPoint(it.latitude, it.longitude), it.label) },
            state.selectedFacilityId,
        )
    }

    // The user's own position, marked and brought into view — once, when they asked for it.
    LaunchedEffect(controller, state.userPoint) {
        val map = controller ?: return@LaunchedEffect
        val point = state.userPoint ?: return@LaunchedEffect
        val here = MapPoint(point.latitude, point.longitude)
        map.showNavigationLocation(here)
        map.moveCamera(MapCamera(here, MY_LOCATION_ZOOM), animated = true)
    }
}

/** Close enough to read the streets around someone without losing the pins near them. */
private const val MY_LOCATION_ZOOM = 15.0

/** The words of the map, provisional until product copy is approved. */
object MapCopy {
    const val OPEN_NOW = "مفتوح الآن"
    const val DUTY_NOW = "مناوب الآن"
    const val MY_LOCATION = "موقعي"
    const val OPEN_DETAILS = "عرض التفاصيل"
    const val DISMISS = "إغلاق"
    const val ROUTE = "الطريق"
    const val ZOOM_IN = "تكبير"
    const val ZOOM_OUT = "تصغير"
    const val UNCONFIGURED = "الخريطة غير متاحة"
    const val UNCONFIGURED_BODY = "يجب ضبط مزود خرائط الإنتاج قبل عرض الخريطة"
}
