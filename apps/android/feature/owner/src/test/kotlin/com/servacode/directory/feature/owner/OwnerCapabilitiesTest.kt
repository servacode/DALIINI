package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilityCapabilities
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.Province
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerCapabilitiesTest {
    private fun facility(duty: Boolean, closures: Boolean) = OwnerFacilitySummary(
        id = "f",
        nameAr = "منشأة",
        category = Category("c", "تصنيف"),
        province = Province("raqqa", "الرقة"),
        status = OwnerFacilityStatus.ACTIVE,
        lastUpdateEpochMillis = 0,
        capabilities = FacilityCapabilities(
            supportsHours = true,
            supportsPhotos = true,
            supportsDuty = duty,
            supportsSpecialtyFilter = false,
            supportsServiceFilter = false,
            supportsTemporaryClosure = closures,
            supportsOwnerOnboarding = true,
            supportsRatings = true,
        ),
    )

    @Test fun `the controls follow the capabilities served with the facility`() {
        assertTrue(OwnerCapabilities.supportsDuty(facility(duty = true, closures = false)))
        assertFalse(OwnerCapabilities.supportsDuty(facility(duty = false, closures = true)))
        assertTrue(OwnerCapabilities.supportsTemporaryClosure(facility(duty = false, closures = true)))
        assertFalse(OwnerCapabilities.supportsTemporaryClosure(facility(duty = true, closures = false)))
    }
}
