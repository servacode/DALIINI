package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerLabelsTest {
    private val latin = Regex("[A-Za-z_]")

    @Test fun `every facility status reads in Arabic, never as its code`() {
        OwnerFacilityStatus.entries.forEach { status ->
            val label = OwnerLabels.status(status)
            assertFalse("$status -> $label", latin.containsMatchIn(label))
        }
        assertEquals("مسودة", OwnerLabels.status(OwnerFacilityStatus.DRAFT))
        assertEquals("فعّالة", OwnerLabels.status(OwnerFacilityStatus.ACTIVE))
    }

    @Test fun `every required action the contract lists reads in Arabic`() {
        listOf("REVIEW_REJECTION", "COMPLETE_AND_SUBMIT", "REVERIFY_AND_SUBMIT", "WAIT_FOR_REVIEW", "CONTACT_SUPPORT")
            .forEach { code ->
                val label = OwnerLabels.requiredAction(code)
                assertFalse("$code -> $label", latin.containsMatchIn(label))
                assertTrue("$code falls back", label != OwnerLabels.UNKNOWN_ACTION)
            }
    }

    @Test fun `an action this app does not know yet gets a neutral label, not the code`() {
        val label = OwnerLabels.requiredAction("SOMETHING_NEW")

        assertEquals(OwnerLabels.UNKNOWN_ACTION, label)
        assertFalse(label.contains("SOMETHING_NEW"))
    }

    @Test fun `every role reads in Arabic`() {
        assertEquals("مالك", OwnerLabels.role(FacilityMemberRole.OWNER))
        assertEquals("مدير", OwnerLabels.role(FacilityMemberRole.MANAGER))
    }
}
