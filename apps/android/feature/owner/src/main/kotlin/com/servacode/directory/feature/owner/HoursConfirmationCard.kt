package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.HoursConfirmationPolicy
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.isMissingEndpoint

/**
 * When the owner is asked «هل ما زالت أوقات الدوام صحيحة؟», and when the question goes away.
 *
 * The same rule as the backend's weekly reminder: a published facility whose category keeps
 * opening hours, never confirmed or confirmed more than a week ago. Confirming moves the public
 * «آخر تأكيد للمعلومات» line, so a facility nobody sees yet has nothing to confirm.
 */
object HoursConfirmationCard {
    fun shows(facility: OwnerFacilityDetail, nowEpochMillis: Long): Boolean =
        facility.summary.status == OwnerFacilityStatus.ACTIVE &&
            facility.summary.capabilities.supportsHours &&
            HoursConfirmationPolicy.isDue(facility.hoursConfirmedAtEpochMillis, nowEpochMillis)

    /**
     * Whether a refusal removes the card rather than being reported on it: a backend without the
     * endpoint (404) or a category without hours (409 HOURS_NOT_SUPPORTED) will refuse again.
     */
    fun withdrawsOn(failure: Throwable): Boolean {
        if (failure.isMissingEndpoint()) return true
        val error = failure.toAppError()
        return error.code == HOURS_NOT_SUPPORTED || error.kind == AppError.Kind.CONFLICT
    }

    const val HOURS_NOT_SUPPORTED = "HOURS_NOT_SUPPORTED"
}
