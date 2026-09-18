package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilityCapabilities
import com.servacode.directory.core.model.OwnerCategoryConfig
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.Province
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerCapabilitiesTest {
    private val raqqa = Province("raqqa", "الرقة")

    private fun facility(categoryId: String) = OwnerFacilitySummary(
        id = "f",
        nameAr = "منشأة",
        category = Category(categoryId, "تصنيف"),
        province = raqqa,
        status = OwnerFacilityStatus.ACTIVE,
        lastUpdateEpochMillis = 0,
    )

    private fun config(categoryId: String, duty: Boolean) = OwnerConfig(
        province = raqqa,
        categories = listOf(
            OwnerCategoryConfig(
                category = Category(categoryId, "تصنيف"),
                specialization = "PHARMACY",
                capabilities = FacilityCapabilities(
                    supportsHours = true,
                    supportsPhotos = true,
                    supportsDuty = duty,
                    supportsSpecialtyFilter = false,
                    supportsServiceFilter = false,
                    supportsTemporaryClosure = true,
                    supportsOwnerOnboarding = true,
                ),
                verificationRequirements = emptyList(),
            ),
        ),
    )

    @Test fun `duty is offered when the category supports it`() {
        assertTrue(OwnerCapabilities.supportsDuty(facility("pharmacy"), config("pharmacy", duty = true)))
    }

    @Test fun `duty is hidden when the category does not support it`() {
        assertFalse(OwnerCapabilities.supportsDuty(facility("clinic"), config("clinic", duty = false)))
    }

    @Test fun `duty is hidden when the capability is unknown, rather than guessed`() {
        assertFalse(OwnerCapabilities.supportsDuty(facility("clinic"), config("pharmacy", duty = true)))
        assertFalse(OwnerCapabilities.supportsDuty(facility("pharmacy"), null))
    }
}
