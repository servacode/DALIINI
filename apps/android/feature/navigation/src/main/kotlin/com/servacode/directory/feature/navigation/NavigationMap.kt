package com.servacode.directory.feature.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
) {
    Box(Modifier.fillMaxWidth().height(320.dp)) {
        if (MapStyle.isConfigured(styleUrl)) {
            RouteMap(styleUrl, route, location)
        } else {
            Text("يجب ضبط مزود خرائط الإنتاج قبل عرض مسار الملاحة")
        }
    }
}

@Composable
private fun RouteMap(
    styleUrl: String,
    route: NavigationRoute?,
    location: MapPoint?,
) {
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
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
        modifier = Modifier.fillMaxWidth().height(320.dp),
    )

    LaunchedEffect(controller, route) {
        val map = controller ?: return@LaunchedEffect
        route?.let { map.showRoute(it.geometry, routeColor) }
    }
    LaunchedEffect(controller, location) {
        val map = controller ?: return@LaunchedEffect
        location?.let {
            map.showNavigationLocation(it)
            map.moveCamera(MapCamera(it, zoom = 16.0), animated = true)
        }
    }
}
