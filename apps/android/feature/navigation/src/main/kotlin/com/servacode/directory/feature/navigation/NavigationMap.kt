package com.servacode.directory.feature.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.maps.GeoMath
import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapLibreController
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.MapStyle
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RouteStroke
import com.servacode.directory.core.maps.UserMark
import com.servacode.directory.core.maps.rememberMapViewWithLifecycle

@Composable
internal fun NavigationMap(
    styleUrl: String,
    route: NavigationRoute?,
    location: MapPoint?,
    modifier: Modifier = Modifier,
    /** Broken for a walk, unbroken for a vehicle. */
    stroke: RouteStroke = RouteStroke.SOLID,
    /** A turning arrow for a vehicle, a plain dot on foot. */
    mark: UserMark = UserMark.ARROW,
    bearingDegrees: Float = 0f,
    destination: MapPoint? = null,
    destinationName: String? = null,
    /** True while the map rides with the traveller; false after a hand has moved it. */
    following: Boolean = true,
    onUserMovedMap: () -> Unit = {},
    /** Handed back so the screen's own controls can work the map they are floating over. */
    onController: (MapLibreController) -> Unit = {},
) {
    Box(modifier.fillMaxWidth()) {
        if (MapStyle.isConfigured(styleUrl)) {
            RouteMap(
                styleUrl = styleUrl,
                route = route,
                location = location,
                stroke = stroke,
                mark = mark,
                bearingDegrees = bearingDegrees,
                destination = destination,
                destinationName = destinationName,
                following = following,
                onUserMovedMap = onUserMovedMap,
                onController = onController,
            )
        } else {
            Text(
                text = "يجب ضبط مزود خرائط الإنتاج قبل عرض مسار الملاحة",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteMap(
    styleUrl: String,
    route: NavigationRoute?,
    location: MapPoint?,
    stroke: RouteStroke,
    mark: UserMark,
    bearingDegrees: Float,
    destination: MapPoint?,
    destinationName: String?,
    following: Boolean,
    onUserMovedMap: () -> Unit,
    onController: (MapLibreController) -> Unit,
) {
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
    val density = LocalDensity.current
    val sidePadding = with(density) { Space.xl.roundToPx() }
    val bottomPadding = with(density) { Sizes.mapCardClearance.roundToPx() }
    val mapView = rememberMapViewWithLifecycle()
    // One controller per map: it holds the route line and the location marker it replaces on
    // every fix (INT-096).
    var controller by remember(mapView) { mutableStateOf<MapLibreController?>(null) }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    val mapController = MapLibreController(map)
                    map.setStyle(styleUrl) { controller = mapController }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )

    LaunchedEffect(controller, destination, destinationName) {
        val map = controller ?: return@LaunchedEffect
        destination?.let { map.showDestination(it, destinationName) }
    }

    // The line and the mark are one update: they describe the same instant, and drawing them
    // apart is what makes a map look like it is catching up with itself.
    LaunchedEffect(controller, route, location, stroke, mark, bearingDegrees) {
        val map = controller ?: return@LaunchedEffect
        val geometry = route?.geometry ?: return@LaunchedEffect
        // Only what is still ahead: a line that stays whole while someone walks along it is a
        // picture of a plan, not of a trip.
        val ahead = location?.let { GeoMath.remainingGeometry(geometry, it) } ?: geometry
        map.showGuidance(ahead, routeColor, stroke, location, bearingDegrees, mark)
    }

    // A hand on the map means "I am looking at something": the screen stops dragging the view
    // back and offers to resume instead.
    LaunchedEffect(controller) {
        val map = controller ?: return@LaunchedEffect
        map.onUserMovedMap(onUserMovedMap)
        onController(map)
    }

    // The whole way, once, when a route arrives: what the reader needs before setting off is
    // how far and which way, and that is the one moment it can be seen at all.
    LaunchedEffect(controller, route?.geometry?.size, route?.distanceMeters) {
        val map = controller ?: return@LaunchedEffect
        val geometry = route?.geometry ?: return@LaunchedEffect
        map.frameRoute(geometry, sidePadding, bottomPadding)
    }

    // Then it rides along: centred on the traveller, turned the way they are going and tilted,
    // so the road ahead takes the screen and the road behind does not. Re-framing the whole
    // route on every reading is what used to make the map look like it was jumping about.
    LaunchedEffect(controller, location, bearingDegrees, following) {
        val map = controller ?: return@LaunchedEffect
        if (!following) return@LaunchedEffect
        val here = location ?: return@LaunchedEffect
        map.moveCamera(
            MapCamera(
                center = here,
                zoom = if (route == null) OVERVIEW_ZOOM else FOLLOW_ZOOM,
                bearing = bearingDegrees.toDouble(),
                tilt = if (route == null) 0.0 else FOLLOW_TILT,
            ),
            animated = true,
        )
    }
}

/** Close enough to see the next turn, wide enough to see what is after it. */
private const val FOLLOW_ZOOM = 17.0

/** Before there is a route there is still a person, and the map opens where they are. */
private const val OVERVIEW_ZOOM = 15.0

/** Off straight down, in degrees: enough for the road to have a horizon. */
private const val FOLLOW_TILT = 50.0
