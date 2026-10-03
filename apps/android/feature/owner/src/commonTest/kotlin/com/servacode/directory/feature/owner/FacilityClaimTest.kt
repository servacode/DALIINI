package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.ClaimEvidence
import com.servacode.directory.core.model.ClaimRequirement
import com.servacode.directory.core.model.ClaimStatus
import com.servacode.directory.core.model.FacilityClaim
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

internal val licence =
    ClaimRequirement("7", "رخصة مزاولة المهنة", required = true, minFiles = 1, maxFiles = 2)
internal val storefront = ClaimRequirement("8", "صورة الواجهة", required = false, minFiles = 0, maxFiles = 1)

internal fun claim(
    id: String = "c-1",
    status: ClaimStatus = ClaimStatus.DRAFT,
    evidence: List<ClaimEvidence> = emptyList(),
) = FacilityClaim(
    id = id,
    status = status,
    facilityId = "f-9",
    facilityNameAr = "صيدلية النور",
    categoryNameAr = "صيدليات",
    provinceNameAr = "حلب",
    requirements = listOf(licence, storefront),
    evidence = evidence,
)

class FacilityClaimTest {
    @Test fun `a draft may be sent once every required document is there`() {
        assertEquals(listOf(licence), claim().missing)
        assertFalse(claim().canSubmit)

        val ready = claim(evidence = listOf(ClaimEvidence("e-1", "7", 0)))
        assertTrue(ready.missing.isEmpty())
        assertTrue(ready.canSubmit)
        assertFalse(ready.copy(status = ClaimStatus.SUBMITTED).canSubmit)
    }

    @Test fun `only an open claim can be withdrawn`() {
        assertTrue(claim(status = ClaimStatus.DRAFT).canWithdraw)
        assertTrue(claim(status = ClaimStatus.SUBMITTED).canWithdraw)
        assertFalse(claim(status = ClaimStatus.APPROVED).canWithdraw)
        assertFalse(claim(status = ClaimStatus.REJECTED).canWithdraw)
    }
}
