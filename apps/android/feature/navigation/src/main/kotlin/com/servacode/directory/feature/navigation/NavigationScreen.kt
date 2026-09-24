package com.servacode.directory.feature.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryCompactFilterChip
import com.servacode.directory.core.designsystem.DirectoryIconButton
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
import com.servacode.directory.core.maps.RouteStroke
import com.servacode.directory.core.maps.RoutingProfile
import com.servacode.directory.core.maps.UserMark
import com.servacode.directory.core.model.DistanceText
import kotlin.math.roundToInt

/**
 * Screen 11. The way there, on the map, with the instruction that matters now under it.
 *
 * It used to be two screens: one that showed the route and one that followed it, with a button
 * between them. The button was removed on the owner's reading of it — someone who pressed
 * "الطريق" has already decided to go — and the two became this. Nothing was lost by the merge:
 * the whole route is framed on opening with its distance and its time, which is what the first
 * screen existed to show.
 */
@Composable
fun BuiltInNavigationScreen(
    styleUrl: String,
    destination: MapPoint,
    onClose: () -> Unit,
    destinationName: String? = null,
    /**
     * Offered only where a build offers it: a trip driven by made-up readings, so guidance and
     * its voice can be watched without anyone getting into a car. Null in a release build, and
     * then no such control exists.
     */
    onSimulate: ((RoutingProfile) -> Unit)? = null,
    viewModel: NavigationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.retry() }
    val openExternalMaps = { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri(destination))) }

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
                onSecondaryAction = openExternalMaps,
            )
            return@DirectoryPage
        }

        Box(Modifier.fillMaxSize().padding(padding)) {
            NavigationMap(
                styleUrl = styleUrl,
                route = state.route,
                location = state.progress?.location,
                modifier = Modifier.fillMaxSize(),
                stroke = state.profile.stroke(),
                mark = state.profile.mark(),
                bearingDegrees = state.bearingDegrees ?: 0f,
                destination = destination,
                destinationName = destinationName,
            )

            // The three ways of travelling sit at the top, present from the moment the screen
            // opens: the choice is not something to be waited for, and one of them is already
            // being computed behind it.
            TravelModeRow(
                selected = state.profile,
                onSelect = viewModel::selectProfile,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(Space.base),
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

                DirectoryCard {
                    when (val navigation = state.navigation) {
                        NavigationState.Idle,
                        NavigationState.Routing,
                        -> DirectoryInlineLoading(NavigationCopy.ROUTING)

                        is NavigationState.Navigating ->
                            NavigationProgressPanel(navigation.progress, state.switching, openExternalMaps)

                        is NavigationState.Rerouting -> Column(
                            verticalArrangement = Arrangement.spacedBy(Space.sm),
                        ) {
                            Text(
                                text = NavigationCopy.REROUTING,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            NavigationProgressPanel(navigation.progress, state.switching, openExternalMaps)
                        }

                        is NavigationState.Arrived -> Text(
                            text = NavigationCopy.ARRIVED,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        is NavigationState.Error -> Column(
                            verticalArrangement = Arrangement.spacedBy(Space.md),
                        ) {
                            Text(
                                text = errorMessage(navigation.reason),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            DirectoryPrimaryButton(
                                text = NavigationCopy.RETRY,
                                onClick = viewModel::retry,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DirectorySecondaryButton(
                                text = NavigationCopy.EXTERNAL_MAPS,
                                onClick = openExternalMaps,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                onSimulate?.let { simulate ->
                    DirectorySecondaryButton(
                        text = NavigationCopy.SIMULATE,
                        onClick = { simulate(state.profile) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * On foot, on a motorcycle or by car.
 *
 * Each is a question put to the routing engine, not a label over the same line: a walk goes the
 * wrong way up a one-way street quite happily, and a car does not.
 */
@Composable
private fun TravelModeRow(
    selected: RoutingProfile,
    onSelect: (RoutingProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        RoutingProfile.entries.forEach { profile ->
            DirectoryCompactFilterChip(
                text = NavigationCopy.mode(profile),
                selected = profile == selected,
                onClick = { onSelect(profile) },
                icon = profile.icon(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavigationProgressPanel(
    progress: NavigationProgress,
    switching: Boolean,
    onExternalMaps: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        progress.maneuver?.let {
            Text(
                text = ArabicManeuverPhraseBuilder.phrase(it),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            if (switching) {
                // The route on screen is the old one for a moment longer. Saying so is better
                // than blanking it, which is what made the map appear to reset on every choice.
                DirectoryInlineLoading(NavigationCopy.ROUTING, modifier = Modifier.weight(1f))
            } else {
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
            // Handing the trip to another maps app is a corner of the bar, not a line of its
            // own: it is the way out, not the thing this screen is for.
            DirectoryIconButton(
                icon = DirectoryIcons.map,
                label = NavigationCopy.EXTERNAL_MAPS,
                onClick = onExternalMaps,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@DrawableRes
private fun RoutingProfile.icon(): Int = when (this) {
    RoutingProfile.WALKING -> DirectoryIcons.walk
    RoutingProfile.MOTORCYCLE -> DirectoryIcons.motorcycle
    RoutingProfile.DRIVING -> DirectoryIcons.car
}

/** A walk is not bound to the carriageway, so its line is not drawn as though it were. */
private fun RoutingProfile.stroke(): RouteStroke =
    if (this == RoutingProfile.WALKING) RouteStroke.DOTTED else RouteStroke.SOLID

private fun RoutingProfile.mark(): UserMark =
    if (this == RoutingProfile.WALKING) UserMark.WALKER else UserMark.ARROW

/** The destination as another maps app would take it. */
private fun geoUri(destination: MapPoint): Uri =
    ("geo:${destination.latitude},${destination.longitude}?q=" +
        "${destination.latitude},${destination.longitude}").toUri()

private fun formatDuration(seconds: Double): String {
    val minutes = (seconds / 60.0).roundToInt().coerceAtLeast(1)
    return "$minutes دقيقة"
}

private fun errorMessage(code: String): String = when (code) {
    "LOCATION_PERMISSION_REQUIRED" -> "يلزم السماح بالموقع أثناء استخدام التطبيق للملاحة."
    "LOCATION_UNAVAILABLE" -> "تعذر تحديد موقعك الحالي."
    "NO_ROUTE" -> "لا يوجد طريق بين موقعك والمنشأة بهذا النمط."
    "UNROUTABLE_POINT" -> "لا توجد طريق صالحة لهذا النمط قرب أحد الموقعين."
    "TOO_FAR" -> "المسافة أبعد من أن تُحسب بهذا النمط."
    "INVALID_POINTS" -> "أحد الموقعين غير صالح."
    "UNREACHABLE" -> "خدمة التوجيه لا تستجيب."
    "NOT_CONFIGURED" -> "خدمة التوجيه غير مهيأة في هذه النسخة."
    "MALFORMED", "ENGINE_ERROR", "ROUTING_UNAVAILABLE" -> "تعذر حساب المسار من خدمة التوجيه."
    else -> "تعذر بدء الملاحة."
}

private fun warningMessage(code: String): String = when (code) {
    "REROUTE_NETWORK_FAILED" -> "تعذر إعادة التوجيه الآن؛ سيستمر عرض المسار الحالي."
    "LOCATION_TEMPORARILY_UNAVAILABLE" -> "إشارة الموقع غير متاحة مؤقتًا."
    else -> code
}

/** The words of the navigation, provisional until product copy is approved. */
object NavigationCopy {
    const val TITLE = "الطريق"
    const val ROUTING = "جارٍ حساب المسار…"
    const val REROUTING = "جارٍ إعادة حساب المسار…"
    const val ARRIVED = "لقد وصلت إلى وجهتك"
    const val ALLOW_LOCATION = "السماح بالموقع"
    const val RETRY = "إعادة المحاولة"
    const val EXTERNAL_MAPS = "فتح تطبيق خرائط خارجي"
    const val PERMISSION_TITLE = "الملاحة تحتاج موقعك"
    const val SIMULATE = "رحلة تجريبية"
    const val SIMULATED = "رحلة تجريبية — الموقع مُحاكى ولست تتحرك فعلًا"

    internal fun mode(profile: RoutingProfile): String = when (profile) {
        RoutingProfile.WALKING -> "مشي"
        RoutingProfile.MOTORCYCLE -> "موتور"
        RoutingProfile.DRIVING -> "سيارة"
    }
}
