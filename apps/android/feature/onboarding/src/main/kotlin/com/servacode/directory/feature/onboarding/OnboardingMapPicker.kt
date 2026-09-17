package com.servacode.directory.feature.onboarding

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.servacode.directory.core.maps.MapLibreController
import com.servacode.directory.core.maps.MapPoint
import org.maplibre.android.maps.MapView

@Composable
fun OnboardingMapPicker(
    styleUrl: String,
    current: MapPoint?,
    onSelected: (MapPoint) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
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

    Box(Modifier.fillMaxWidth().height(260.dp)) {
        if (!configured) {
            Text("يجب ضبط مزود خرائط الإنتاج لاختيار النقطة من الخريطة")
        } else {
            AndroidView(
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            map.setStyle(styleUrl)
                            val controller = MapLibreController(map)
                            controller.setOnPointSelected { point ->
                                controller.showSelectionPoint(point)
                                onSelected(point)
                            }
                            current?.let(controller::showSelectionPoint)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }
    }
}
