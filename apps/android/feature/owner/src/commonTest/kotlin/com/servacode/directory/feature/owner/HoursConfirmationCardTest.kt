package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilityCapabilities
import com.servacode.directory.core.model.HoursConfirmationPolicy
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.Province
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

/** A facility for the confirmation card: published, with hours, confirmed [confirmedAt]. */
internal fun confirmable(
    confirmedAt: Long? = null,
    status: OwnerFacilityStatus = OwnerFacilityStatus.ACTIVE,
    hours: Boolean = true,
) = OwnerFacilityDetail(
    summary = OwnerFacilitySummary(
        id = "f-1",
        nameAr = "صيدلية الشفاء",
        category = Category("c", "صيدليات"),
        province = Province("raqqa", "الرقة"),
        status = status,
        lastUpdateEpochMillis = 0,
        capabilities = FacilityCapabilities(
            supportsHours = hours,
            supportsPhotos = true,
            supportsDuty = true,
            supportsSpecialtyFilter = false,
            supportsServiceFilter = false,
            supportsTemporaryClosure = true,
            supportsOwnerOnboarding = true,
        ),
    ),
    hoursConfirmedAtEpochMillis = confirmedAt,
)

class HoursConfirmationCardTest {
    private val now = 1_790_589_600_000L
    private val day = 86_400_000L

    @Test fun `asked when never confirmed or confirmed a week or more ago`() {
        assertTrue(HoursConfirmationCard.shows(confirmable(null), now))
        assertTrue(HoursConfirmationCard.shows(confirmable(now - 7 * day), now))
        assertFalse(HoursConfirmationCard.shows(confirmable(now - 6 * day), now))
        assertFalse(HoursConfirmationPolicy.isDue(now - day, now))
    }

    @Test fun `not asked of a facility nobody sees or one without hours`() {
        assertFalse(HoursConfirmationCard.shows(confirmable(status = OwnerFacilityStatus.SUBMITTED), now))
        assertFalse(HoursConfirmationCard.shows(confirmable(hours = false), now))
    }

    @Test fun `a missing endpoint or a category without hours withdraws the card`() {
        fun refusal(kind: AppError.Kind, code: String? = null, status: Int? = null) =
            AppException(AppError(kind, code = code, status = status))

        assertTrue(HoursConfirmationCard.withdrawsOn(refusal(AppError.Kind.NOT_FOUND, status = 404)))
        assertTrue(HoursConfirmationCard.withdrawsOn(refusal(AppError.Kind.CONFLICT, "HOURS_NOT_SUPPORTED", 409)))
        assertFalse(HoursConfirmationCard.withdrawsOn(refusal(AppError.Kind.OFFLINE)))
        assertFalse(HoursConfirmationCard.withdrawsOn(refusal(AppError.Kind.SERVER, status = 500)))
    }
}
