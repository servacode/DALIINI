package com.servacode.directory.feature.facility

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.ActionCircle
import com.servacode.directory.core.designsystem.AvailabilityPill
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryImage
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryRoundControl
import com.servacode.directory.core.designsystem.DirectorySection
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.MetaRow
import com.servacode.directory.core.designsystem.PhotoPager
import com.servacode.directory.core.designsystem.Radius
import com.servacode.directory.core.designsystem.RatingBadge
import com.servacode.directory.core.designsystem.RatingSummary
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.StarPicker
import com.servacode.directory.core.model.BusinessHour

/**
 * Screen 08. What the facility is, then what can be done about it, then the rest.
 *
 * Everything on this screen comes from the facility the backend answered with: an action whose
 * data is missing is shown as unavailable rather than hidden, and nothing is filled in for it.
 */
@Composable
fun FacilityScreen(
    onDirections: (Double, Double) -> Unit,
    onSignIn: () -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: FacilityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Screen 10 lives here rather than on a route of its own: the pictures belong to the
    // facility already loaded, and nothing is fetched to show them larger.
    var photosOpen by remember { mutableStateOf(false) }
    val photos = (state as? FacilityUiState.Content)?.value?.imageUrls.orEmpty()
    BackHandler(enabled = photosOpen) { photosOpen = false }

    if (photosOpen && photos.isNotEmpty()) {
        val content = state as? FacilityUiState.Content
        PhotosPage(
            urls = photos,
            name = content?.value?.summary?.nameAr,
            onBack = { photosOpen = false },
        )
        return
    }

    DirectoryPage { padding ->
        when (val value = state) {
            FacilityUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is FacilityUiState.Error -> DirectoryErrorState(
                title = FacilityCopy.ERROR,
                modifier = Modifier.padding(padding),
                body = value.message,
            )
            is FacilityUiState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                Box {
                    PhotoPager(
                        urls = value.value.imageUrls,
                        contentDescription = value.value.summary.nameAr,
                        modifier = Modifier.height(Sizes.hero),
                        onPhoto = { photosOpen = true },
                    )
                    DirectoryRoundControl(
                        icon = DirectoryIcons.back,
                        label = FacilityCopy.BACK,
                        onClick = onBack,
                        modifier = Modifier.padding(Space.md).align(Alignment.TopStart),
                    )
                }
                FacilityBody(
                    value = value,
                    onDirections = onDirections,
                    onSignIn = onSignIn,
                    onCall = onCall,
                    onWhatsApp = onWhatsApp,
                    onRate = viewModel::rate,
                    onRemoveRating = viewModel::removeRating,
                    onToggleFavorite = viewModel::toggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun FacilityBody(
    value: FacilityUiState.Content,
    onDirections: (Double, Double) -> Unit,
    onSignIn: () -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onRate: (Int) -> Unit,
    onRemoveRating: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val detail = value.value
    val summary = detail.summary
    val placed = detail.latitude != null && detail.longitude != null
    val separator = stringResource(R.string.facility_list_separator)
    val address = listOfNotNull(detail.neighborhoodNameAr, detail.addressAr).joinToString(separator)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Space.screen, vertical = Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        // What it is, and what can be done about it, in one card: the name is read and acted on
        // in the same breath.
        DirectoryCard {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Text(
                    text = summary.nameAr,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = summary.category.nameAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    RatingBadge(summary.ratingAverage, summary.ratingCount)
                    AvailabilityPill(summary)
                }
                if (value.stale) DirectoryOfflineNotice()
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                // Four, and each of them does something to this facility from here. The map is
                // not one of them: it shows the same pin this page already stands on, and the
                // ratings are not one either - they are further down this very page.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ActionCircle(
                        label = FacilityCopy.CALL,
                        icon = DirectoryIcons.phone,
                        onClick = { detail.phone?.let(onCall) },
                        enabled = detail.phone != null,
                    )
                    ActionCircle(
                        label = FacilityCopy.WHATSAPP,
                        icon = DirectoryIcons.chat,
                        onClick = { detail.phone?.let(onWhatsApp) },
                        enabled = detail.phone != null,
                    )
                    ActionCircle(
                        label = FacilityCopy.DIRECTIONS,
                        icon = DirectoryIcons.route,
                        onClick = {
                            val latitude = detail.latitude ?: return@ActionCircle
                            val longitude = detail.longitude ?: return@ActionCircle
                            onDirections(latitude, longitude)
                        },
                        enabled = placed,
                    )
                    // A filled star for one that is saved and an outline for one that is not:
                    // a word changing under an identical icon is a change nobody notices.
                    ActionCircle(
                        label = if (summary.isFavorite) FacilityCopy.SAVED else FacilityCopy.SAVE,
                        icon = if (summary.isFavorite) DirectoryIcons.starFilled else DirectoryIcons.star,
                        onClick = { if (value.signedIn) onToggleFavorite() else onSignIn() },
                    )
                }
            }
        }

        if (address.isNotEmpty() || detail.phone != null) {
            DirectorySection(FacilityCopy.ADDRESS) {
                if (address.isNotEmpty()) MetaRow(DirectoryIcons.pin, address)
                detail.phone?.let { MetaRow(DirectoryIcons.phone, it) }
            }
        }
        if (detail.hours.isNotEmpty()) {
            DirectorySection(FacilityCopy.HOURS) {
                detail.hours.forEach { hour -> HourRow(hour) }
            }
        }
        detail.descriptionAr?.let { about ->
            DirectorySection(FacilityCopy.ABOUT) { Paragraph(about) }
        }
        if (detail.specialties.isNotEmpty()) {
            DirectorySection(FacilityCopy.SPECIALTIES) { Paragraph(detail.specialties.joinToString(separator)) }
        }
        if (detail.services.isNotEmpty()) {
            DirectorySection(FacilityCopy.SERVICES) { Paragraph(detail.services.joinToString(separator)) }
        }

        // The ratings, where they belong: at the end of what is being rated.
        DirectorySection(FacilityCopy.RATINGS) {
            RatingSummary(average = summary.ratingAverage, count = summary.ratingCount)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = FacilityCopy.YOUR_RATING,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (value.signedIn) {
                StarPicker(
                    stars = value.myRating,
                    onRate = onRate,
                    label = { stars -> FacilityCopy.rateLabel(stars) },
                )
                // A rating is a sentence one is allowed to take back: another star replaces it,
                // and this removes it. Before, it could only ever be said once.
                if (value.myRating != null) {
                    Text(
                        text = FacilityCopy.RATING_CHANGE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DirectoryTextButton(FacilityCopy.RATING_REMOVE, onRemoveRating)
                }
                value.ratingMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                DirectoryTextButton(FacilityCopy.SIGN_IN_TO_RATE, onSignIn)
            }
        }
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun HourRow(hour: BusinessHour) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        DirectoryIcon(
            icon = DirectoryIcons.clock,
            contentDescription = null,
            size = IconSize.small,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = weekdayName(hour.weekday),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${hour.opensAt.take(5)} – ${hour.closesAt.take(5)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 0 is Monday, matching the backend's weekday numbering; the names are the module's own. */
@Composable
@ReadOnlyComposable
private fun weekdayName(weekday: Int): String {
    val days = LocalContext.current.resources.getStringArray(R.array.facility_weekdays)
    return days.getOrNull(weekday) ?: stringResource(R.string.facility_weekday_unknown)
}

/** The words of a facility's page, provisional until product copy is approved. */
object FacilityCopy {
    val BACK: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_back)
    val ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_error)
    val CALL: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_call)
    val DIRECTIONS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_directions)
    val WHATSAPP: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_whatsapp)
    val RATINGS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_ratings)
    val SAVE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_save)
    val SAVED: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_saved)
    val ADDRESS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_address)
    val HOURS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_hours)
    val ABOUT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_about)
    val SPECIALTIES: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_specialties)
    val SERVICES: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_services)
    val YOUR_RATING: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_your_rating)
    val RATING_CHANGE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_rating_change)
    val RATING_REMOVE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_rating_remove)
    val SIGN_IN_TO_RATE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_sign_in_to_rate)
    val PHOTOS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_photos)
    val CLOSE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.facility_close)

    @Composable
    @ReadOnlyComposable
    fun rateLabel(stars: Int): String = stringResource(R.string.facility_rate_label, stars)
}

/**
 * Screen 10. Every picture the facility has, two to a row, with any one of them opened large.
 *
 * The pictures are the ones the facility's own detail carries; there is no separate gallery in
 * the contract, so nothing more is asked of the backend to show them.
 */
@Composable
private fun PhotosPage(urls: List<String>, name: String?, onBack: () -> Unit) {
    var opened by remember { mutableStateOf<Int?>(null) }
    BackHandler(enabled = opened != null) { opened = null }

    val openedIndex = opened
    if (openedIndex != null) {
        DirectoryPage(background = MaterialTheme.colorScheme.surfaceVariant) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                PhotoPager(
                    urls = urls.drop(openedIndex) + urls.take(openedIndex),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                )
                DirectoryRoundControl(
                    icon = DirectoryIcons.closeBox,
                    label = FacilityCopy.CLOSE,
                    onClick = { opened = null },
                    modifier = Modifier.padding(Space.md).align(Alignment.TopStart),
                    tinted = false,
                )
            }
        }
        return
    }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = FacilityCopy.PHOTOS, onBack = onBack) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(PHOTO_COLUMNS),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.screen),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            itemsIndexed(urls) { index, url ->
                DirectoryImage(
                    url = url,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { opened = index },
                    contentDescription = name,
                )
            }
        }
    }
}

private const val PHOTO_COLUMNS = 2
