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
) {
    Box(modifier.fillMaxWidth()) {
        if (MapStyle.isConfigured(styleUrl)) {
            RouteMap(styleUrl, route, location, stroke, mark, bearingDegrees, destination, destinationName)
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
        map.showGuidance(geometry, routeColor, stroke, location, bearingDegrees, mark)
    }

    // The camera is framed once per route, not once per reading. Re-framing on every fix is
    // what made the map appear to jump about on its own while nobody was touching it.
    LaunchedEffect(controller, route?.geometry?.size, route?.distanceMeters) {
        val map = controller ?: return@LaunchedEffect
        val geometry = route?.geometry ?: return@LaunchedEffect
        map.frameRoute(geometry, sidePadding, bottomPadding)
    }

    // Before there is a route there is still a person, and the map opens where they are.
    LaunchedEffect(controller, route == null, location != null) {
        val map = controller ?: return@LaunchedEffect
        if (route != null) return@LaunchedEffect
        location?.let { map.moveCamera(MapCamera(it, zoom = 15.0), animated = true) }
    }
}
