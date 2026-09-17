package com.servacode.directory.feature.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.maps.MapPoint
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BuiltInNavigationScreen(
    styleUrl: String,
    destination: MapPoint,
    onClose: () -> Unit,
    viewModel: NavigationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.retry() }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("الملاحة", style = MaterialTheme.typography.headlineLarge)
        val progress = progressOf(state.navigation)
        NavigationMap(
            styleUrl = styleUrl,
            route = progress?.route,
            location = progress?.location,
        )
        when (val navigation = state.navigation) {
            NavigationState.Idle,
            NavigationState.Routing,
            -> CircularProgressIndicator()
            is NavigationState.Navigating -> NavigationProgressPanel(navigation.progress)
            is NavigationState.Rerouting -> {
                Text("جارٍ إعادة حساب المسار…")
                NavigationProgressPanel(navigation.progress)
            }
            is NavigationState.Arrived -> Text(
                "لقد وصلت إلى وجهتك",
                style = MaterialTheme.typography.headlineSmall,
            )
            is NavigationState.Error -> {
                Text(errorMessage(navigation.reason))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (navigation.reason == "LOCATION_PERMISSION_REQUIRED") {
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                    )
                                )
                            },
                        ) { Text("السماح بالموقع") }
                    } else {
                        Button(onClick = viewModel::retry) { Text("إعادة المحاولة") }
                    }
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse(
                                "geo:${destination.latitude},${destination.longitude}?q=" +
                                    "${destination.latitude},${destination.longitude}"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                    ) { Text("فتح تطبيق خرائط خارجي") }
                }
            }
        }
        state.warning?.let { Text(warningMessage(it)) }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("إنهاء الملاحة")
        }
    }
}

@Composable
private fun NavigationProgressPanel(progress: NavigationProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        progress.maneuver?.let {
            Text(
                ArabicManeuverPhraseBuilder.phrase(it),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Text("المتبقي: ${formatDistance(progress.remainingDistanceMeters)}")
        Text("الوقت التقريبي: ${formatDuration(progress.remainingDurationSeconds)}")
    }
}

private fun progressOf(state: NavigationState): NavigationProgress? = when (state) {
    is NavigationState.Navigating -> state.progress
    is NavigationState.Rerouting -> state.progress
    else -> null
}

private fun formatDistance(meters: Double): String = if (meters < 1_000.0) {
    "${meters.roundToInt()} م"
} else {
    String.format(Locale.US, "%.1f كم", meters / 1_000.0)
}

private fun formatDuration(seconds: Double): String {
    val minutes = (seconds / 60.0).roundToInt().coerceAtLeast(1)
    return "$minutes دقيقة"
}

private fun errorMessage(code: String): String = when (code) {
    "LOCATION_PERMISSION_REQUIRED" -> "يلزم السماح بالموقع أثناء استخدام التطبيق للملاحة."
    "LOCATION_UNAVAILABLE" -> "تعذر تحديد موقعك الحالي."
    "ROUTING_UNAVAILABLE" -> "تعذر حساب المسار من مزود التوجيه."
    else -> "تعذر بدء الملاحة."
}

private fun warningMessage(code: String): String = when (code) {
    "REROUTE_NETWORK_FAILED" -> "تعذر إعادة التوجيه الآن؛ سيستمر عرض المسار الحالي."
    "LOCATION_TEMPORARILY_UNAVAILABLE" -> "إشارة الموقع غير متاحة مؤقتًا."
    else -> code
}
