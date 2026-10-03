package com.servacode.directory.feature.facility

import com.servacode.directory.core.designsystem.DirectoryVocabulary
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.model.FacilityReportReason
import androidx.compose.ui.semantics.Role
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.ActionCircle
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.designsystem.AvailabilityPill
import com.servacode.directory.core.designsystem.DirectoryBackHandler
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
import com.servacode.directory.core.designsystem.DirectoryWords
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
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/**
 * Screen 08. What the facility is, then what can be done about it, then the rest.
 *
 * Everything on this screen comes from the facility the backend answered with: an action whose
 * data is missing is shown as unavailable rather than hidden, and nothing is filled in for it.
 */
@Composable
fun FacilityScreen(
    viewModel: FacilityViewModel,
    reportViewModel: FacilityReportViewModel,
    onDirections: (Double, Double) -> Unit,
    onSignIn: () -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    /** Hands the facility's name and its public link to whatever the phone shares with. */
    onShare: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val report by reportViewModel.state.collectAsStateWithLifecycle()
    var reporting by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val thanks = FacilityCopy.REPORT_THANKS
    LaunchedEffect(report.sent) {
        if (report.sent) {
            reporting = false
            reportViewModel.consumeSent()
            snackbar.showSnackbar(thanks)
        }
    }
    // Screen 10 lives here rather than on a route of its own: the pictures belong to the
    // facility already loaded, and nothing is fetched to show them larger.
    var photosOpen by remember { mutableStateOf(false) }
    val photos = (state as? FacilityUiState.Content)?.value?.imageUrls.orEmpty()
    DirectoryBackHandler(enabled = photosOpen) { photosOpen = false }

    if (photosOpen && photos.isNotEmpty()) {
        val content = state as? FacilityUiState.Content
        PhotosPage(
            urls = photos,
            name = content?.value?.summary?.nameAr,
            onBack = { photosOpen = false },
        )
        return
    }

    val facilityId = (state as? FacilityUiState.Content)?.value?.summary?.id
    if (reporting && facilityId != null) {
        ReportSheet(
            state = report,
            onReason = reportViewModel::choose,
            onNote = reportViewModel::updateNote,
            onSend = { reportViewModel.submit(facilityId) },
            onDismiss = {
                reporting = false
                reportViewModel.clearFailure()
            },
        )
    }

    DirectoryPage(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        when (val value = state) {
            FacilityUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is FacilityUiState.Error -> DirectoryErrorState(
                title = FacilityCopy.ERROR,
                modifier = Modifier.padding(padding),
                error = value.error,
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
                    // The two actions worth counting are wrapped here rather than at the
                    // button: the screen is given them from outside, and what the app does
                    // with a tap is not the same question as whether a tap happened.
                    onDirections = { latitude, longitude ->
                        viewModel.directionsStarted()
                        onDirections(latitude, longitude)
                    },
                    onSignIn = onSignIn,
                    onCall = { phone ->
                        viewModel.phoneTapped()
                        onCall(phone)
                    },
                    onWhatsApp = onWhatsApp,
                    onShare = onShare,
                    onRate = viewModel::rate,
                    onRemoveRating = viewModel::removeRating,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onReport = { reporting = true },
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
    onShare: (String) -> Unit,
    onRate: (Int) -> Unit,
    onRemoveRating: () -> Unit,
    onToggleFavorite: () -> Unit,
    onReport: () -> Unit,
) {
    val detail = value.value
    val whatsApp = detail.whatsapp?.let(::whatsAppLink)
    val summary = detail.summary
    val placed = detail.latitude != null && detail.longitude != null
    val separator = DirectoryWords.LIST_SEPARATOR
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
                TrustLine(
                    TrustFacts.of(
                        verifiedAt = detail.lastVerifiedAtEpochMillis,
                        infoConfirmedAt = detail.infoConfirmedAtEpochMillis,
                        updatedAt = detail.updatedAtEpochMillis,
                    ),
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
                    // The facility's own WhatsApp number when it published one; a landline
                    // is not a WhatsApp account, so the phone is never used in its place.
                    ActionCircle(
                        label = FacilityCopy.WHATSAPP,
                        icon = DirectoryIcons.whatsapp,
                        onClick = { whatsApp?.let(onWhatsApp) },
                        enabled = whatsApp != null,
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

        if (address.isNotEmpty() || detail.phone != null || detail.whatsapp != null) {
            DirectorySection(FacilityCopy.ADDRESS) {
                if (address.isNotEmpty()) MetaRow(DirectoryIcons.pin, address)
                detail.phone?.let { MetaRow(DirectoryIcons.phone, it) }
                detail.whatsapp?.let {
                    // Said as "واتساب: …" so it is not heard as a second phone number.
                    val spoken = FacilityCopy.whatsAppNumber(it)
                    MetaRow(
                        DirectoryIcons.whatsapp,
                        it,
                        Modifier.semantics { contentDescription = spoken },
                    )
                }
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
                value.ratingFailure?.let {
                    Text(
                        text = appErrorText(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                DirectoryTextButton(FacilityCopy.SIGN_IN_TO_RATE, onSignIn)
            }
        }

        // Last, after everything the reader could have checked against what they know. Sharing
        // passes the facility on; reporting is how a reader tells us what they found is wrong,
        // and the two belong together at the foot of the page rather than one being hidden.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            DirectoryTextButton(
                text = FacilityCopy.SHARE,
                onClick = { onShare(detail.summary.nameAr) },
                modifier = Modifier.weight(1f),
            )
            DirectoryTextButton(
                text = FacilityCopy.REPORT,
                onClick = onReport,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * When the details were last checked by an operator, and when they were last confirmed (or,
 * from an older backend, changed): the facts a reader weighs before driving somewhere. Nothing
 * is drawn when none is known.
 */
@Composable
private fun TrustLine(facts: TrustFacts) {
    if (facts.isEmpty) return
    val now = Clock.System.now().toEpochMilliseconds()
    val verifiedAt = facts.verifiedAt
    val parts = listOfNotNull(
        verifiedAt?.let { FacilityCopy.verified(FacilityAge.of(it, now)) },
        facts.confirmedAt?.let { FacilityCopy.confirmed(FacilityAge.of(it, now)) },
        facts.updatedAt?.let { FacilityCopy.updated(FacilityAge.of(it, now)) },
    )
    Row(
        // One statement for a screen reader, not an icon and two fragments.
        modifier = Modifier.semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        if (verifiedAt != null) {
            DirectoryIcon(
                icon = DirectoryIcons.verified,
                contentDescription = null,
                size = IconSize.small,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = parts.joinToString(DirectoryWords.LIST_SEPARATOR),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Screen 08's "report a problem": a reason, an optional note, send. It works signed out, and a
 * failure keeps what was chosen so trying again is one tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(
    state: FacilityReportUiState,
    onReason: (FacilityReportReason) -> Unit,
    onNote: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen, vertical = Space.base),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Text(
                text = FacilityCopy.REPORT,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Column(Modifier.selectableGroup()) {
                FacilityReportReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Sizes.touchTarget)
                            .selectable(
                                selected = state.reason == reason,
                                onClick = { onReason(reason) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    ) {
                        // The row is the control; the button only shows the choice.
                        RadioButton(selected = state.reason == reason, onClick = null)
                        Text(
                            text = FacilityCopy.reason(reason),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            DirectoryTextField(
                value = state.note,
                onValueChange = onNote,
                label = FacilityCopy.REPORT_NOTE,
                singleLine = false,
                minLines = 3,
            )
            Text(
                text = FacilityCopy.noteCount(state.note.length, FacilityRepository.REPORT_NOTE_MAX),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End),
            )
            state.failure?.let { failure ->
                Text(
                    text = FacilityCopy.reportFailure(failure),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            DirectoryPrimaryButton(
                text = FacilityCopy.REPORT_SEND,
                onClick = onSend,
                enabled = state.reason != null,
                loading = state.sending,
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
            )
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
        // The day and its hours are read as one line, not as two unrelated fragments.
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
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
            text = DirectoryWords.weekday(hour.weekday),
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

/** The words of a facility's page, provisional until product copy is approved. */
object FacilityCopy {
    val BACK: String @Composable get() = stringResource(Res.string.facility_back)
    val ERROR: String @Composable get() = stringResource(Res.string.facility_error)
    val CALL: String @Composable get() = stringResource(Res.string.facility_call)
    val DIRECTIONS: String @Composable get() = stringResource(Res.string.facility_directions)
    val WHATSAPP: String @Composable get() = stringResource(Res.string.facility_whatsapp)
    val RATINGS: String @Composable get() = stringResource(Res.string.facility_ratings)
    val SAVE: String @Composable get() = stringResource(Res.string.facility_save)
    val SAVED: String @Composable get() = stringResource(Res.string.facility_saved)
    val ADDRESS: String @Composable get() = stringResource(Res.string.facility_address)
    val HOURS: String @Composable get() = stringResource(Res.string.facility_hours)
    val ABOUT: String @Composable get() = stringResource(Res.string.facility_about)
    val SPECIALTIES: String @Composable get() = stringResource(Res.string.facility_specialties)
    val SERVICES: String @Composable get() = stringResource(Res.string.facility_services)
    val YOUR_RATING: String @Composable get() = stringResource(Res.string.facility_your_rating)
    val RATING_CHANGE: String @Composable get() = stringResource(Res.string.facility_rating_change)
    val RATING_REMOVE: String @Composable get() = stringResource(Res.string.facility_rating_remove)
    val SIGN_IN_TO_RATE: String
        @Composable get() = stringResource(Res.string.facility_sign_in_to_rate)
    val SHARE: String @Composable get() = stringResource(Res.string.facility_share)
    val PHOTOS: String @Composable get() = stringResource(Res.string.facility_photos)
    val PHOTO_OPEN: String @Composable get() = stringResource(Res.string.facility_photo_open)

    /** One photo of the gallery, named by the facility and its place among the others. */
    @Composable
    fun photo(name: String, position: Int, count: Int): String =
        stringResource(Res.string.facility_photo, name, position, count)
    val CLOSE: String @Composable get() = stringResource(Res.string.facility_close)
    val REPORT: String @Composable get() = stringResource(Res.string.facility_report)
    val REPORT_NOTE: String @Composable get() = stringResource(Res.string.facility_report_note)
    val REPORT_SEND: String @Composable get() = stringResource(Res.string.facility_report_send)
    val REPORT_THANKS: String @Composable get() = stringResource(Res.string.facility_report_thanks)

    @Composable
    fun whatsAppNumber(number: String): String = stringResource(Res.string.facility_whatsapp_number, number)

    @Composable
    fun noteCount(length: Int, max: Int): String = stringResource(Res.string.facility_report_note_count, length, max)

    @Composable
    fun reason(reason: FacilityReportReason): String = DirectoryVocabulary.reportReason(reason)

    @Composable
    fun reportFailure(failure: ReportFailure): String = stringResource(
        when (failure) {
            ReportFailure.THROTTLED -> Res.string.facility_report_throttled
            ReportFailure.OFFLINE -> Res.string.facility_report_offline
            ReportFailure.OTHER -> Res.string.facility_report_failed
        },
    )

    /** "تم التحقق اليوم" / "تم التحقق قبل ٣ أيام". */
    @Composable
    fun verified(age: FacilityAge): String = stringResource(Res.string.facility_verified, age(age))

    /** "آخر تحديث قبل شهر". */
    @Composable
    fun updated(age: FacilityAge): String = stringResource(Res.string.facility_updated, age(age))

    /** "آخر تأكيد للمعلومات اليوم": the owner's confirmation, or the operator's check. */
    @Composable
    fun confirmed(age: FacilityAge): String = stringResource(Res.string.facility_confirmed, age(age))

    @Composable
    private fun age(age: FacilityAge): String = when (age) {
        FacilityAge.Today -> stringResource(Res.string.facility_age_today)
        is FacilityAge.Days -> pluralStringResource(Res.plurals.facility_age_days, age.count, age.count)
        is FacilityAge.Months -> pluralStringResource(Res.plurals.facility_age_months, age.count, age.count)
        is FacilityAge.Years -> pluralStringResource(Res.plurals.facility_age_years, age.count, age.count)
    }

    @Composable
    fun rateLabel(stars: Int): String = stringResource(Res.string.facility_rate_label, stars)
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
    DirectoryBackHandler(enabled = opened != null) { opened = null }

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
                        .clickable(onClickLabel = FacilityCopy.PHOTO_OPEN, role = Role.Button) {
                            opened = index
                        },
                    contentDescription = FacilityCopy.photo(name ?: FacilityCopy.PHOTOS, index + 1, urls.size),
                )
            }
        }
    }
}

private const val PHOTO_COLUMNS = 2
