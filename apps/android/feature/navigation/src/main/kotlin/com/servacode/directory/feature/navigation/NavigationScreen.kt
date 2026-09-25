package com.servacode.directory.feature.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryCompactFilterChip
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryInlineLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPermissionState
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectoryRoundControl
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.MetaRow
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS
import com.servacode.directory.core.maps.MapLibreController
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
    // The map rides with the traveller until a hand moves it, and the button brings it back.
    var following by rememberSaveable { mutableStateOf(true) }
    var map by remember { mutableStateOf<MapLibreController?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.retry() }
    val openExternalMaps = { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri(destination))) }

    // No bar over the map: a navigator needs every row of pixels it can have, and the way
    // out is a button on the map rather than a title above it.
    DirectoryPage { padding ->
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
                following = following,
                onUserMovedMap = { following = false },
                onController = { map = it },
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(Space.base),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    // The way out stands beside the choices rather than on top of them: as a
                    // free-floating button it landed squarely on the first travel mode.
                    DirectoryRoundControl(
                        icon = DirectoryIcons.closeBox,
                        label = NavigationCopy.CLOSE,
                        onClick = onClose,
                        tinted = false,
                    )
                    // The three ways of travelling, present from the moment the screen opens:
                    // the choice is not something to be waited for, and one of them is already
                    // being computed behind it.
                    TravelModeRow(
                        selected = state.profile,
                        onSelect = viewModel::selectProfile,
                        modifier = Modifier.weight(1f),
                    )
                }
                // How far and how long, under the three ways of travelling — the two figures
                // that change when the choice above them changes, so they are read together.
                (state.navigation as? NavigationState.Navigating)?.progress?.let { progress ->
                    TripSummary(progress, state.switching)
                }
            }

            // Everything one presses on a map, in one stack half way down the edge: closer,
            // further, back to me, and out to another maps app. The map tab wears the same set
            // in the same place, which is the point of it being a set.
            Column(
                // Start, not end: the page is read right to left, so this is the right edge —
                // under the thumb of a hand holding the phone.
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(Space.base),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                DirectoryRoundControl(
                    icon = DirectoryIcons.plus,
                    label = NavigationCopy.ZOOM_IN,
                    onClick = { map?.zoomBy(ZOOM_STEP) },
                )
                DirectoryRoundControl(
                    icon = DirectoryIcons.minus,
                    label = NavigationCopy.ZOOM_OUT,
                    onClick = { map?.zoomBy(-ZOOM_STEP) },
                )
                DirectoryRoundControl(
                    icon = DirectoryIcons.myLocation,
                    label = NavigationCopy.RECENTRE,
                    onClick = { following = true },
                    enabled = !following,
                )
                DirectoryRoundControl(
                    icon = DirectoryIcons.map,
                    label = NavigationCopy.EXTERNAL_MAPS,
                    onClick = openExternalMaps,
                )
            }



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

                        is NavigationState.Navigating -> ManeuverBanner(navigation.progress)

                        is NavigationState.Rerouting -> Column(
                            verticalArrangement = Arrangement.spacedBy(Space.sm),
                        ) {
                            Text(
                                text = NavigationCopy.REROUTING,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            ManeuverBanner(navigation.progress)
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

/**
 * The turn that is coming, as a navigator says it: the instruction, and how far to it.
 *
 * At the top of the screen and on the brand's own green, because it is the one thing being read
 * while moving. Everything else about the trip is at the foot of the screen, where it is read
 * when stopped.
 */
/**
 * How far is left and how long it will take, in the profile that is selected.
 *
 * Walking a kilometre is not driving a kilometre, and the two numbers are the answer to "should
 * I walk?" — so they sit under the choice that changes them.
 */
@Composable
private fun TripSummary(progress: NavigationProgress, switching: Boolean) {
    DirectoryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            if (switching) {
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
        }
    }
}

@Composable
private fun ManeuverBanner(progress: NavigationProgress) {
    val maneuver = progress.maneuver ?: return
    DirectoryCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            DirectoryIcon(
                icon = DirectoryIcons.route,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ArabicManeuverPhraseBuilder.phrase(maneuver),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // How far to the turn, not how long the turn's own leg is: "in 300 m" is the
                // sentence a navigator says, and the engine already measures it for the voice.
                Text(
                    text = DistanceText.of(progress.distanceToManeuverMeters),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    const val ZOOM_IN = "تقريب"
    const val ZOOM_OUT = "تبعيد"
    const val CLOSE = "إغلاق"
    const val RECENTRE = "إعادة التوسيط"
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

/** One step of scale per press: enough to notice, small enough to aim with. */
private const val ZOOM_STEP = 1.0
