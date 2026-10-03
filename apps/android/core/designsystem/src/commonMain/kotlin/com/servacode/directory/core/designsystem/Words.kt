package com.servacode.directory.core.designsystem

import androidx.compose.runtime.Composable
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.OwnerAction
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.RoundedDistance
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.model.ownerAction
import com.servacode.directory.core.model.roundedDistance
import com.servacode.directory.core.model.shownReason
import org.jetbrains.compose.resources.stringResource

/**
 * The words that belong to no single screen.
 *
 * A weekday is not the facility page's word, nor the hours editor's: it is the app's. Held here,
 * in the module every screen already depends on, it is translated once — and two screens can
 * never disagree about which day is Monday. The same goes for the mark between items of a list,
 * which is not a comma in every language.
 *
 * Read in composition, so the words follow the phone's language without anything being restarted.
 */
object DirectoryWords {
    /** Monday first, matching the backend's weekday numbering; the shared vocabulary's names. */
    @Composable
    fun weekdays(): List<String> = DirectoryVocabulary.weekdays()

    /** One day by its backend number, or a dash where the number means nothing. */
    @Composable
    fun weekday(weekday: Int): String =
        DirectoryVocabulary.weekday(weekday) ?: stringResource(Res.string.directory_weekday_unknown)

    val LIST_SEPARATOR: String
        @Composable get() = stringResource(Res.string.directory_list_separator)

    /** What the app is for. Its name is `DirectoryBrand.NAME`, which is not a translation. */
    val TAGLINE: String @Composable get() = stringResource(Res.string.ds_tagline)

    /**
     * How far it is, in whichever unit the figure deserves.
     *
     * The rounding is the model's, so that a test can check it without Android; the unit is a
     * word, so it is here.
     */
    @Composable
    fun distance(meters: Double): String = when (val rounded = roundedDistance(meters)) {
        is RoundedDistance.Metres -> stringResource(Res.string.ds_distance_metres, rounded.value)
        is RoundedDistance.Kilometres ->
            stringResource(Res.string.ds_distance_kilometres, rounded.whole, rounded.tenth)
    }

    /** From when to when, on Damascus clocks. */
    @Composable
    fun period(startEpochMillis: Long, endEpochMillis: Long): String = stringResource(
        Res.string.ds_period,
        DamascusTime.format(startEpochMillis),
        DamascusTime.format(endEpochMillis),
    )
}

/**
 * Whether the doors are open, and when they open next.
 *
 * The state is the backend's; this only names it, and shows the next opening time the backend
 * computed in Syria's clock rather than whatever zone the device happens to be set to.
 */
object AvailabilityWords {
    @Composable
    fun of(state: AvailabilityState): String = DirectoryVocabulary.availability(state)

    /** "closed, opens 08:00" where the backend said when; the bare state otherwise. */
    @Composable
    fun of(summary: FacilitySummary): String {
        val next = summary.nextOpenAtEpochMillis
        val state = of(summary.availability)
        if (summary.availability == AvailabilityState.OPEN || next == null) return state
        return stringResource(Res.string.ds_availability_next_open, state, DamascusTime.clock(next))
    }
}

/**
 * The words an owner reads for their own facility.
 *
 * The values stay as the backend and the domain define them; only what reaches the screen is
 * translated, here and nowhere else. An action code this app has not met yet gets a neutral
 * sentence, never the code itself.
 */
object OwnerWords {
    @Composable
    fun status(value: OwnerFacilityStatus): String = DirectoryVocabulary.facilityStatus(value)

    @Composable
    fun requiredAction(code: String): String = stringResource(
        when (ownerAction(code)) {
            OwnerAction.REVIEW_REJECTION -> Res.string.ds_owner_action_review_rejection
            OwnerAction.COMPLETE_AND_SUBMIT -> Res.string.ds_owner_action_complete_and_submit
            OwnerAction.REVERIFY_AND_SUBMIT -> Res.string.ds_owner_action_reverify_and_submit
            OwnerAction.WAIT_FOR_REVIEW -> Res.string.ds_owner_action_wait_for_review
            OwnerAction.CONTACT_SUPPORT -> Res.string.ds_owner_action_contact_support
            OwnerAction.UNKNOWN -> Res.string.ds_owner_action_unknown
        },
    )

    @Composable
    fun role(value: FacilityMemberRole): String = stringResource(
        when (value) {
            FacilityMemberRole.OWNER -> Res.string.ds_owner_role_owner
            FacilityMemberRole.MANAGER -> Res.string.ds_owner_role_manager
        },
    )
}

/**
 * How a temporary closure reads in a list (INT-095): its reason, where there is one worth
 * showing, and then when it runs.
 */
@Composable
fun closureText(closure: TemporaryClosure): String {
    val period = DirectoryWords.period(closure.startsAtEpochMillis, closure.endsAtEpochMillis)
    val reason = closure.shownReason() ?: return period
    return stringResource(Res.string.ds_closure_with_reason, reason, period)
}
