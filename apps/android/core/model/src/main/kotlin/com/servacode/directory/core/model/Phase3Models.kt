package com.servacode.directory.core.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

/** Whether an emergency number is the country's or the province's. */
enum class EmergencyScope { NATIONAL, PROVINCE }

/** One number to call in an emergency: ambulance, fire, police, the province's hospital line. */
@Serializable
data class EmergencyNumber(
    val nameAr: String,
    val number: String,
    val scope: EmergencyScope,
    val provinceId: String? = null,
)

/** One day of the public duty roster: who is on duty, and when each shift runs. */
data class DutyDay(
    /** "YYYY-MM-DD", the day as the backend numbered it. */
    val date: String,
    val facilities: List<FacilitySummary>,
    val shifts: List<DutyWindow>,
)

/** When one facility's duty runs, on the day it is listed under. */
data class DutyWindow(val facilityId: String, val startsAtEpochMillis: Long, val endsAtEpochMillis: Long)

/** The owner's confirmation that the opening hours are still right, as the backend recorded it. */
data class HoursConfirmation(val hoursConfirmedAtEpochMillis: Long, val infoConfirmedAtEpochMillis: Long)

/**
 * Whether an owner is asked to confirm a facility's hours: once a week, as the backend's own
 * reminder does. Never confirmed is asked at once.
 */
object HoursConfirmationPolicy {
    const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

    fun isDue(confirmedAtEpochMillis: Long?, nowEpochMillis: Long): Boolean =
        confirmedAtEpochMillis == null || nowEpochMillis - confirmedAtEpochMillis >= WEEK_MS
}

/** What the emergency screen shows, and whether it came from the platform or the fallback. */
data class EmergencyNumbers(
    val national: List<EmergencyNumber>,
    val province: List<EmergencyNumber>,
    /** True for the few numbers built into the app, shown only when nothing else is known. */
    val builtIn: Boolean = false,
) {
    val isEmpty: Boolean get() = national.isEmpty() && province.isEmpty()
}

/** A facility the reader opened, kept on the device for «شوهدت مؤخراً». */
data class RecentFacility(
    val id: String,
    val nameAr: String,
    val categoryNameAr: String?,
    val viewedAtEpochMillis: Long,
)

/**
 * The kinds of notice a reader can turn off in Settings. A push whose type is none of these is
 * always shown: an unknown type is not a reason to hide something the platform sent.
 */
enum class NotificationCategory {
    /** A day nobody covers on the province's duty roster (`duty.gap_nudge`). */
    DUTY_REMINDER,

    /** What the platform announces to the reader's province (`platform.broadcast`). */
    PROVINCE_NEWS,

    /** An owner's applications and facilities: approved, rejected, hours to confirm. */
    APPLICATION_STATUS,
    ;

    companion object {
        fun of(type: String): NotificationCategory? {
            val value = type.trim().lowercase()
            return when {
                // A change staff made to the owner's own shift is always shown: it is not news,
                // it is their roster.
                value == NotificationTypes.DUTY_SHIFT_ADMIN_CHANGED -> null
                // An invitation is addressed to this person, not news about a facility: never muted.
                value == NotificationTypes.INVITATION_RECEIVED -> null
                value == NotificationTypes.DUTY_GAP_NUDGE || value == "duty_gap" -> DUTY_REMINDER
                value.startsWith("duty.") -> DUTY_REMINDER
                value.startsWith("facility.") -> APPLICATION_STATUS
                value == NotificationTypes.PLATFORM_BROADCAST -> PROVINCE_NEWS
                value.startsWith("province.") -> PROVINCE_NEWS
                else -> null
            }
        }
    }
}

/**
 * The kinds of notice an account wants pushed, as the backend keeps them
 * (`account/notification-preferences/`). Every one is on until turned off; the inbox receives
 * every message whatever these say.
 */
data class NotificationSwitches(
    val dutyReminders: Boolean = true,
    val provinceNews: Boolean = true,
    val applicationStatus: Boolean = true,
)

/** The notification types the backend sends and the app treats specially. */
object NotificationTypes {
    const val DUTY_GAP_NUDGE = "duty.gap_nudge"
    const val DUTY_SHIFT_ADMIN_CHANGED = "duty.shift.admin_changed"
    const val HOURS_CONFIRM_REQUEST = "facility.hours.confirm_request"
    const val PLATFORM_BROADCAST = "platform.broadcast"
    const val INVITATION_RECEIVED = "facility.invitation.received"
}

/** Where opening a notice leads, from its type and whatever identifiers came with it. */
sealed interface NotificationTarget {
    /** Schedule duty: on [facilityId] when known, prefilled for [date] when known. */
    data class DutyScheduling(val facilityId: String?, val date: String?) : NotificationTarget

    /** Confirm a facility's opening hours; its management page, or the owner's list. */
    data class HoursConfirmation(val facilityId: String?) : NotificationTarget

    data class Facility(val id: String) : NotificationTarget

    data object OwnerFacilities : NotificationTarget

    /** The invitations waiting for this account, to accept or decline. */
    data object Invitations : NotificationTarget

    data object None : NotificationTarget

    companion object {
        fun of(
            type: String,
            destination: MessageDestination,
            facilityId: String?,
            date: String? = null,
        ): NotificationTarget = when (type.trim().lowercase()) {
            NotificationTypes.DUTY_GAP_NUDGE, "duty_gap" ->
                DutyScheduling(facilityId, DutyPresets.parseDate(date)?.toString())
            NotificationTypes.DUTY_SHIFT_ADMIN_CHANGED -> DutyScheduling(facilityId, null)
            NotificationTypes.HOURS_CONFIRM_REQUEST -> HoursConfirmation(facilityId)
            NotificationTypes.INVITATION_RECEIVED -> Invitations
            else -> when (destination) {
                MessageDestination.FACILITY -> facilityId?.let(::Facility) ?: None
                MessageDestination.OWNER_FACILITIES -> OwnerFacilities
                MessageDestination.NONE -> None
            }
        }
    }
}

/**
 * The two shifts an owner schedules most: tonight, and tomorrow night. Prefilled rather than
 * submitted, on Damascus clocks, so the owner confirms the times before anything is sent.
 */
object DutyPresets {
    val NIGHT_START: LocalTime = LocalTime.of(20, 0)
    val NIGHT_END: LocalTime = LocalTime.of(8, 0)

    /** A night's shift starting on [date]: 20:00 to 08:00 the next morning. */
    fun night(date: LocalDate): Pair<Long, Long> =
        DamascusTime.toEpochMillis(date, NIGHT_START) to DamascusTime.toEpochMillis(date.plusDays(1), NIGHT_END)

    fun tonight(today: LocalDate = DamascusTime.now().toLocalDate()): Pair<Long, Long> = night(today)

    fun tomorrow(today: LocalDate = DamascusTime.now().toLocalDate()): Pair<Long, Long> = night(today.plusDays(1))

    /** A day sent by a gap nudge ("2026-09-30"), or null when it is not a date. */
    fun parseDate(value: String?): LocalDate? =
        value?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
}

/**
 * What this build must be for the backend to keep serving it.
 *
 * Zero for both is what an unconfigured backend answers, and no version is below zero: a
 * platform that works never locks its own users out because nobody filled in a form.
 */
data class AppRelease(
    val minimumVersionCode: Int,
    val latestVersionCode: Int,
    /** Where to get a newer build. Empty when nobody configured one. */
    val storeUrl: String,
    /** What to show instead of the app's own wording. Empty when the backend said nothing. */
    val noticeAr: String,
) {
    /** Whether [versionCode] is too old to run against the backend that sent this. */
    fun blocks(versionCode: Int): Boolean = versionCode < minimumVersionCode

    /** Whether a newer build exists, without this one being refused. */
    fun supersedes(versionCode: Int): Boolean =
        !blocks(versionCode) && versionCode < latestVersionCode
}
