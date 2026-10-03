package com.servacode.directory.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.model.OwnerFacilityStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * How a state reads, as the shared vocabulary (`packages/design-tokens/vocabulary.json`) assigns
 * it. The same six tones colour the same states in the app, the site and the console.
 */
enum class StatusTone { NEUTRAL, POSITIVE, WARNING, DANGER, INFO, BRAND, ACCENT }

/**
 * The tone of each state, copied from the vocabulary's `tone` fields. Plain functions, so a test
 * holds them to the shared file without a composition.
 */
object StatusTones {
    fun availability(state: AvailabilityState): StatusTone = when (state) {
        AvailabilityState.OPEN -> StatusTone.POSITIVE
        AvailabilityState.CLOSED -> StatusTone.DANGER
        AvailabilityState.DUTY -> StatusTone.ACCENT
        AvailabilityState.TEMP_CLOSED -> StatusTone.INFO
    }

    fun facilityStatus(status: OwnerFacilityStatus): StatusTone = when (status) {
        OwnerFacilityStatus.DRAFT -> StatusTone.NEUTRAL
        OwnerFacilityStatus.SUBMITTED -> StatusTone.INFO
        OwnerFacilityStatus.ACTIVE -> StatusTone.POSITIVE
        OwnerFacilityStatus.SUSPENDED -> StatusTone.WARNING
        OwnerFacilityStatus.CLOSED -> StatusTone.DANGER
        OwnerFacilityStatus.REVERIFICATION_REQUIRED -> StatusTone.WARNING
    }

    fun reportReason(reason: FacilityReportReason): StatusTone = when (reason) {
        FacilityReportReason.WRONG_INFO,
        FacilityReportReason.WRONG_LOCATION,
        FacilityReportReason.WRONG_HOURS,
        -> StatusTone.WARNING
        FacilityReportReason.CLOSED_PERMANENTLY, FacilityReportReason.NOT_ON_DUTY -> StatusTone.DANGER
        FacilityReportReason.OTHER -> StatusTone.NEUTRAL
    }
}

/**
 * The shared words for states: the `vocab_*` resources generated from the same vocabulary file
 * the site and the console read. A screen names a state through here and never spells it itself,
 * so "مفتوح الآن" cannot be written three slightly different ways in three modules.
 */
object DirectoryVocabulary {
    @Composable
    fun availability(state: AvailabilityState): String = stringResource(availabilityRes(state))

    @Composable
    fun facilityStatus(status: OwnerFacilityStatus): String = stringResource(
        when (status) {
            OwnerFacilityStatus.DRAFT -> Res.string.vocab_facility_status_draft
            OwnerFacilityStatus.SUBMITTED -> Res.string.vocab_facility_status_submitted
            OwnerFacilityStatus.ACTIVE -> Res.string.vocab_facility_status_active
            OwnerFacilityStatus.SUSPENDED -> Res.string.vocab_facility_status_suspended
            OwnerFacilityStatus.CLOSED -> Res.string.vocab_facility_status_closed
            OwnerFacilityStatus.REVERIFICATION_REQUIRED -> Res.string.vocab_facility_status_reverification_required
        },
    )

    @Composable
    fun reportReason(reason: FacilityReportReason): String = stringResource(
        when (reason) {
            FacilityReportReason.WRONG_INFO -> Res.string.vocab_report_reason_wrong_info
            FacilityReportReason.CLOSED_PERMANENTLY -> Res.string.vocab_report_reason_closed_permanently
            FacilityReportReason.WRONG_LOCATION -> Res.string.vocab_report_reason_wrong_location
            FacilityReportReason.WRONG_HOURS -> Res.string.vocab_report_reason_wrong_hours
            FacilityReportReason.NOT_ON_DUTY -> Res.string.vocab_report_reason_not_on_duty
            FacilityReportReason.OTHER -> Res.string.vocab_report_reason_other
        },
    )

    /** One day by the backend's number: 0 is Monday. */
    @Composable
    fun weekday(weekday: Int): String? = WEEKDAYS.getOrNull(weekday)?.let { stringResource(it) }

    /** Monday first, as the backend numbers them. */
    @Composable
    fun weekdays(): List<String> = WEEKDAYS.map { stringResource(it) }

    private fun availabilityRes(state: AvailabilityState): StringResource = when (state) {
        AvailabilityState.OPEN -> Res.string.vocab_availability_open
        AvailabilityState.CLOSED -> Res.string.vocab_availability_closed
        AvailabilityState.DUTY -> Res.string.vocab_availability_duty
        AvailabilityState.TEMP_CLOSED -> Res.string.vocab_availability_temp_closed
    }

    private val WEEKDAYS = listOf(
        Res.string.vocab_weekday_monday,
        Res.string.vocab_weekday_tuesday,
        Res.string.vocab_weekday_wednesday,
        Res.string.vocab_weekday_thursday,
        Res.string.vocab_weekday_friday,
        Res.string.vocab_weekday_saturday,
        Res.string.vocab_weekday_sunday,
    )
}

/**
 * A state in a word, in the colour its tone gives it: a soft ground, a dot, the word.
 *
 * The one chip for every state the app shows — availability, an owner's facility, a
 * requirement — so a colour means one thing everywhere. The word carries the meaning; the colour
 * only agrees with it, which keeps the chip readable to someone who cannot tell green from red.
 */
@Composable
fun StatusChip(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    val colours = LocalDirectoryTones.current.of(tone)
    Row(
        modifier = modifier
            .background(colours.container, RoundedCornerShape(Radius.pill))
            .padding(horizontal = Space.md, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        if (showDot) {
            Box(Modifier.size(Space.sm).clip(CircleShape).background(colours.content))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = colours.content,
            maxLines = 1,
        )
    }
}
