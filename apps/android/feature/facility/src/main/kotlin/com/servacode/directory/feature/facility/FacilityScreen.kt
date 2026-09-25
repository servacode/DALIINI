package com.servacode.directory.feature.facility

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.ActionCircle
import com.servacode.directory.core.designsystem.AvailabilityPill
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryImage
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySectionLabel
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
                    Surface(
                        modifier = Modifier.padding(Space.md).align(Alignment.TopStart),
                        shape = RoundedCornerShape(Radius.pill),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        DirectoryIconButton(
                            icon = DirectoryIcons.back,
                            label = FacilityCopy.BACK,
                            onClick = onBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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
    val address = listOfNotNull(detail.neighborhoodNameAr, detail.addressAr).joinToString("، ")
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
                    ActionCircle(
                        label = if (summary.isFavorite) FacilityCopy.SAVED else FacilityCopy.SAVE,
                        icon = DirectoryIcons.star,
                        onClick = { if (value.signedIn) onToggleFavorite() else onSignIn() },
                    )
                }
            }
        }

        if (address.isNotEmpty() || detail.phone != null) {
            Section(FacilityCopy.ADDRESS) {
                if (address.isNotEmpty()) MetaRow(DirectoryIcons.pin, address)
                detail.phone?.let { MetaRow(DirectoryIcons.phone, it) }
            }
        }
        if (detail.hours.isNotEmpty()) {
            Section(FacilityCopy.HOURS) {
                detail.hours.forEach { hour -> HourRow(hour) }
            }
        }
        detail.descriptionAr?.let { about ->
            Section(FacilityCopy.ABOUT) { Paragraph(about) }
        }
        if (detail.specialties.isNotEmpty()) {
            Section(FacilityCopy.SPECIALTIES) { Paragraph(detail.specialties.joinToString("، ")) }
        }
        if (detail.services.isNotEmpty()) {
            Section(FacilityCopy.SERVICES) { Paragraph(detail.services.joinToString("، ")) }
        }

        // The ratings, where they belong: at the end of what is being rated.
        Section(FacilityCopy.RATINGS) {
            RatingSummary(average = summary.ratingAverage, count = summary.ratingCount)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = FacilityCopy.YOUR_RATING,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (value.signedIn) {
                StarPicker(stars = value.myRating, onRate = onRate, label = FacilityCopy::rateLabel)
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

/** A label and the card under it, the shape the rest of the app's pages are read in. */
@Composable
private fun Section(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        DirectorySectionLabel(label)
        DirectoryCard {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm), content = content)
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

/** 0 is Monday, matching the backend's weekday numbering. */
private fun weekdayName(weekday: Int): String = when (weekday) {
    0 -> "الاثنين"
    1 -> "الثلاثاء"
    2 -> "الأربعاء"
    3 -> "الخميس"
    4 -> "الجمعة"
    5 -> "السبت"
    6 -> "الأحد"
    else -> "—"
}

/** The words of a facility's page, provisional until product copy is approved. */
object FacilityCopy {
    const val BACK = "رجوع"
    const val ERROR = "تعذر تحميل المنشأة"
    const val CALL = "اتصال"
    const val DIRECTIONS = "الطريق"
    const val WHATSAPP = "واتساب"
    const val RATINGS = "التقييمات"
    const val SAVE = "إضافة للمفضلة"
    const val SAVED = "في المفضلة"
    const val ADDRESS = "العنوان"
    const val HOURS = "ساعات العمل"
    const val ABOUT = "نبذة"
    const val SPECIALTIES = "الاختصاصات"
    const val SERVICES = "الخدمات"
    const val YOUR_RATING = "تقييمك"
    const val RATING_CHANGE = "اضغط نجمة أخرى لتغيير تقييمك."
    const val RATING_REMOVE = "حذف تقييمي"
    const val SIGN_IN_TO_RATE = "سجّل الدخول لتقييم المنشأة"
    const val PHOTOS = "الصور"
    const val CLOSE = "إغلاق"

    fun rateLabel(stars: Int): String = "$stars من 5"
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
                Surface(
                    modifier = Modifier.padding(Space.md).align(Alignment.TopStart),
                    shape = RoundedCornerShape(Radius.pill),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    DirectoryIconButton(
                        icon = DirectoryIcons.close,
                        label = FacilityCopy.CLOSE,
                        onClick = { opened = null },
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
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
