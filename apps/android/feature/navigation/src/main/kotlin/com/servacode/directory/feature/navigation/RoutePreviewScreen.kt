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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.DrawableRes
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryCompactFilterChip
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryInlineLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPermissionState
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.MetaRow
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS
import com.servacode.directory.core.maps.RoutingProfile
import com.servacode.directory.core.model.DistanceText
import kotlin.math.roundToInt

/**
 * The route, before it is followed.
 *
 * The whole way is on screen at once with how far and how long it is, and starting to be guided
 * along it is a separate decision. Someone who only wanted to know whether it is worth the trip
 * has their answer without a voice starting to speak.
 */
@Composable
fun RoutePreviewScreen(
    styleUrl: String,
    onStart: (Double, Double, RoutingProfile) -> Unit,
    onBack: () -> Unit,
    destinationName: String?,
    /**
     * Offered only where a build offers it: a trip driven by made-up readings, so guidance and
     * its voice can be watched without anyone getting into a car. Null in a release build, and
     * then no such button exists at all.
     */
    onSimulate: ((Double, Double, RoutingProfile) -> Unit)? = null,
    viewModel: RoutePreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val askLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.compute() }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = RoutePreviewCopy.TITLE, onBack = onBack) },
    ) { padding ->
        if (state.failure == "LOCATION_PERMISSION_REQUIRED") {
            DirectoryPermissionState(
                title = RoutePreviewCopy.PERMISSION_TITLE,
                body = RoutePreviewCopy.PERMISSION_BODY,
                action = RoutePreviewCopy.ALLOW_LOCATION,
                onAction = { askLocation.launch(FOREGROUND_LOCATION_PERMISSIONS.toTypedArray()) },
                modifier = Modifier.padding(padding),
                secondaryAction = RoutePreviewCopy.EXTERNAL_MAPS,
                onSecondaryAction = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, viewModel.destination.geoUri()),
                    )
                },
            )
            return@DirectoryPage
        }

        Box(Modifier.fillMaxSize().padding(padding)) {
            NavigationMap(
                styleUrl = styleUrl,
                route = state.route,
                location = state.origin,
                modifier = Modifier.fillMaxSize(),
                frameWholeRoute = true,
                destination = viewModel.destination,
                destinationName = destinationName,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(Space.base),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                DirectoryCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        destinationName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        TravelModeRow(
                            selected = state.profile,
                            onSelect = viewModel::selectProfile,
                        )
                        when {
                            state.loading -> DirectoryInlineLoading(RoutePreviewCopy.COMPUTING)
                            state.route != null -> Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Space.lg),
                            ) {
                                MetaRow(
                                    icon = DirectoryIcons.route,
                                    text = DistanceText.of(state.route!!.distanceMeters),
                                    modifier = Modifier.weight(1f),
                                )
                                MetaRow(
                                    icon = DirectoryIcons.clock,
                                    text = RoutePreviewCopy.minutes(state.route!!.durationSeconds),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            else -> Text(
                                text = RoutePreviewCopy.failure(state.failure),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                if (state.canStart) {
                    DirectoryPrimaryButton(
                        text = RoutePreviewCopy.START,
                        onClick = {
                            onStart(
                                viewModel.destination.latitude,
                                viewModel.destination.longitude,
                                state.profile,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // The same trip, driven by the app rather than by the road. It exists only
                    // where the build puts it there, and the screen it opens says so throughout.
                    onSimulate?.let { simulate ->
                        DirectorySecondaryButton(
                            text = RoutePreviewCopy.SIMULATE,
                            onClick = {
                                simulate(
                                    viewModel.destination.latitude,
                                    viewModel.destination.longitude,
                                    state.profile,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else if (!state.loading) {
                    DirectoryPrimaryButton(
                        text = RoutePreviewCopy.RETRY,
                        onClick = viewModel::compute,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DirectorySecondaryButton(
                    text = RoutePreviewCopy.EXTERNAL_MAPS,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, viewModel.destination.geoUri()),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * On foot, on a motorcycle or by car.
 *
 * Each of the three is a question put to the routing engine, not a label over the same line:
 * the walk goes the wrong way up a one-way street quite happily, and the car does not.
 */
@Composable
private fun TravelModeRow(
    selected: RoutingProfile,
    onSelect: (RoutingProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        RoutingProfile.entries.forEach { profile ->
            DirectoryCompactFilterChip(
                text = RoutePreviewCopy.mode(profile),
                selected = profile == selected,
                onClick = { onSelect(profile) },
                icon = profile.icon(),
                modifier = Modifier.weight(1f),
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

/** The destination as another maps app would take it. */
private fun com.servacode.directory.core.maps.MapPoint.geoUri(): Uri =
    "geo:$latitude,$longitude?q=$latitude,$longitude".toUri()

/** The words of the route preview, provisional until product copy is approved. */
object RoutePreviewCopy {
    const val TITLE = "الطريق"
    const val COMPUTING = "جارٍ حساب المسار…"
    const val START = "ابدأ الملاحة"
    const val SIMULATE = "رحلة تجريبية"
    const val RETRY = "إعادة المحاولة"
    const val EXTERNAL_MAPS = "فتح تطبيق خرائط خارجي"
    const val PERMISSION_TITLE = "حساب الطريق يحتاج موقعك"
    const val PERMISSION_BODY = "نحتاج موقعك الحالي لنحسب الطريق من مكانك إلى المنشأة."
    const val ALLOW_LOCATION = "السماح بالموقع"

    fun minutes(seconds: Double): String {
        val value = (seconds / 60.0).roundToInt().coerceAtLeast(1)
        return "$value دقيقة"
    }

    fun mode(profile: RoutingProfile): String = when (profile) {
        RoutingProfile.WALKING -> "مشي"
        RoutingProfile.MOTORCYCLE -> "موتور"
        RoutingProfile.DRIVING -> "سيارة"
    }

    /**
     * Each refusal in its own words.
     *
     * "There is no road near you that a car may use" and "the routing service is not answering"
     * are different pieces of news, and one of them is the user's to act on.
     */
    fun failure(code: String?): String = when (code) {
        "LOCATION_UNAVAILABLE" -> "تعذر تحديد موقعك الحالي."
        "NO_ROUTE" -> "لا يوجد طريق بين موقعك والمنشأة بهذا النمط."
        "UNROUTABLE_POINT" -> "لا توجد طريق صالحة لهذا النمط قرب أحد الموقعين."
        "TOO_FAR" -> "المسافة أبعد من أن تُحسب بهذا النمط."
        "INVALID_POINTS" -> "أحد الموقعين غير صالح."
        "UNREACHABLE" -> "خدمة التوجيه لا تستجيب."
        "NOT_CONFIGURED" -> "خدمة التوجيه غير مهيأة في هذه النسخة."
        "MALFORMED", "ENGINE_ERROR" -> "تعذر حساب المسار من خدمة التوجيه."
        else -> "تعذر حساب الطريق."
    }
}
