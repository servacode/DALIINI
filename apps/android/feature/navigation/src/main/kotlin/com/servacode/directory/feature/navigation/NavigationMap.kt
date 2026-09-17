package com.servacode.directory.feature.navigation

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapLibreController
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
internal fun NavigationMap(
    styleUrl: String,
    route: NavigationRoute?,
    location: MapPoint?,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var mapState by remember { mutableStateOf<MapLibreMap?>(null) }
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
    val configured = styleUrl.startsWith("https://") &&
        !styleUrl.contains("<ROOT_DOMAIN>")

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

    Box(Modifier.fillMaxWidth().height(320.dp)) {
        if (!configured) {
            Text("يجب ضبط مزود خرائط الإنتاج قبل عرض مسار الملاحة")
        } else {
            AndroidView(
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            mapState = map
                            map.setStyle(styleUrl)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(320.dp),
            )
        }
    }

    LaunchedEffect(route, location, mapState) {
        val map = mapState ?: return@LaunchedEffect
        val controller = MapLibreController(map)
        route?.let { controller.showRoute(it.geometry, routeColor) }
        location?.let {
            controller.showNavigationLocation(it)
            controller.moveCamera(MapCamera(it, zoom = 16.0), animated = true)
        }
    }
}
