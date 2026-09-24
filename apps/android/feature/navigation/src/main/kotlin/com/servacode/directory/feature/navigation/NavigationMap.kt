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
import com.servacode.directory.core.maps.rememberMapViewWithLifecycle

@Composable
internal fun NavigationMap(
    styleUrl: String,
    route: NavigationRoute?,
    location: MapPoint?,
    modifier: Modifier = Modifier,
    /**
     * Whether the camera holds the whole way or follows the person along it.
     *
     * The preview frames the route: someone deciding whether to walk needs to see where they
     * are going. Live navigation does the opposite and stays with them.
     */
    frameWholeRoute: Boolean = false,
    destination: MapPoint? = null,
    destinationName: String? = null,
) {
    Box(modifier.fillMaxWidth()) {
        if (MapStyle.isConfigured(styleUrl)) {
            RouteMap(styleUrl, route, location, frameWholeRoute, destination, destinationName)
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
    frameWholeRoute: Boolean,
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
    LaunchedEffect(controller, route, frameWholeRoute) {
        val map = controller ?: return@LaunchedEffect
        val found = route ?: return@LaunchedEffect
        map.showRoute(found.geometry, routeColor)
        if (frameWholeRoute) map.frameRoute(found.geometry, sidePadding, bottomPadding)
    }
    LaunchedEffect(controller, location, frameWholeRoute) {
        val map = controller ?: return@LaunchedEffect
        location?.let {
            map.showNavigationLocation(it)
            // While the route is being computed there is nothing to frame, so the map opens
            // where the person is. Once it arrives the frame takes over and is not fought.
            if (!frameWholeRoute || route == null) {
                map.moveCamera(MapCamera(it, zoom = 16.0), animated = true)
            }
        }
    }
}
