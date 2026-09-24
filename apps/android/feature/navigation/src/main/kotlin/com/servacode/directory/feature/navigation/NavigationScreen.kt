package com.servacode.directory.feature.navigation

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryInlineLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPermissionState
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.MetaRow
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.model.DistanceText
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

/**
 * Screen 11. The route on the map, and under it the one instruction that matters now.
 *
 * The engine behind it is untouched: the same routing, the same following of the user's
 * location, the same rerouting and the same spoken guidance. None of this has been tried on a
 * device yet, and this screen does not claim otherwise.
 */
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

    DirectoryPage(
        topBar = { DirectoryTopBar(title = NavigationCopy.TITLE, onBack = onClose) },
    ) { padding ->
        val permissionRequired = (state.navigation as? NavigationState.Error)
            ?.reason == "LOCATION_PERMISSION_REQUIRED"
        if (permissionRequired) {
            // Nothing can be drawn on the map until the user's position is known, so the whole
            // screen is the ask rather than a card over an empty map.
            DirectoryPermissionState(
                title = NavigationCopy.PERMISSION_TITLE,
                body = errorMessage("LOCATION_PERMISSION_REQUIRED"),
                action = NavigationCopy.ALLOW_LOCATION,
                onAction = { permissionLauncher.launch(FOREGROUND_LOCATION_PERMISSIONS.toTypedArray()) },
                modifier = Modifier.padding(padding),
                secondaryAction = NavigationCopy.EXTERNAL_MAPS,
                onSecondaryAction = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, geoUri(destination)))
                },
            )
            return@DirectoryPage
        }
        Box(Modifier.fillMaxSize().padding(padding)) {
            val progress = progressOf(state.navigation)
            NavigationMap(
                styleUrl = styleUrl,
                route = progress?.route,
                location = progress?.location,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(Space.base),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                state.warning?.let { DirectoryOfflineNotice(text = warningMessage(it)) }
                // A made-up trip says so for as long as it runs. Guidance that looks identical
                // to the real thing and is not is the one thing this screen must never be.
                if (state.simulated) DirectoryOfflineNotice(text = NavigationCopy.SIMULATED)
                when (val navigation = state.navigation) {
                    NavigationState.Idle,
                    NavigationState.Routing,
                    -> DirectoryCard { DirectoryInlineLoading(NavigationCopy.ROUTING) }
                    is NavigationState.Navigating -> DirectoryCard {
                        NavigationProgressPanel(navigation.progress)
                    }
                    is NavigationState.Rerouting -> DirectoryCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                            Text(
                                text = NavigationCopy.REROUTING,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            NavigationProgressPanel(navigation.progress)
                        }
                    }
                    is NavigationState.Arrived -> DirectoryCard {
                        Text(
                            text = NavigationCopy.ARRIVED,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    is NavigationState.Error -> DirectoryCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                            Text(
                                text = errorMessage(navigation.reason),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (navigation.reason == "LOCATION_PERMISSION_REQUIRED") {
                                DirectoryPrimaryButton(
                                    text = NavigationCopy.ALLOW_LOCATION,
                                    onClick = {
                                        permissionLauncher.launch(
                                            FOREGROUND_LOCATION_PERMISSIONS.toTypedArray(),
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                DirectoryPrimaryButton(
                                    text = NavigationCopy.RETRY,
                                    onClick = viewModel::retry,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            DirectorySecondaryButton(
                                text = NavigationCopy.EXTERNAL_MAPS,
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, geoUri(destination)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                DirectorySecondaryButton(
                    text = NavigationCopy.END,
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NavigationProgressPanel(progress: NavigationProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        progress.maneuver?.let {
            Text(
                text = ArabicManeuverPhraseBuilder.phrase(it),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            MetaRow(
                icon = DirectoryIcons.route,
                text = DistanceText.of(progress.remainingDistanceMeters),
                modifier = Modifier.weight(1f),
            )
            MetaRow(
                icon = DirectoryIcons.clock,
                text = formatDuration(progress.remainingDurationSeconds),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The destination as another maps app would take it. */
private fun geoUri(destination: MapPoint): Uri =
    ("geo:${destination.latitude},${destination.longitude}?q=" +
        "${destination.latitude},${destination.longitude}").toUri()

private fun progressOf(state: NavigationState): NavigationProgress? = when (state) {
    is NavigationState.Navigating -> state.progress
    is NavigationState.Rerouting -> state.progress
    else -> null
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

/** The words of the navigation, provisional until product copy is approved. */
object NavigationCopy {
    const val TITLE = "الملاحة"
    const val ROUTING = "جارٍ حساب المسار…"
    const val REROUTING = "جارٍ إعادة حساب المسار…"
    const val ARRIVED = "لقد وصلت إلى وجهتك"
    const val ALLOW_LOCATION = "السماح بالموقع"
    const val RETRY = "إعادة المحاولة"
    const val EXTERNAL_MAPS = "فتح تطبيق خرائط خارجي"
    const val PERMISSION_TITLE = "الملاحة تحتاج موقعك"
    const val END = "إنهاء الملاحة"
    const val SIMULATED = "رحلة تجريبية — الموقع مُحاكى ولست تتحرك فعلًا"
}
