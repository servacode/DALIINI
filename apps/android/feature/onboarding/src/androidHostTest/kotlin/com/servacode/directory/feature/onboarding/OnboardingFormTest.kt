package com.servacode.directory.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What follows a draft's creation. Creating takes only the name and the category; the phone,
 * address and description typed on the same screen were lost until they were sent right after.
 */
class OnboardingFormTest {
    @Test fun `a form with only a name has nothing to send after the draft is made`() {
        assertFalse(OnboardingForm(nameAr = "صيدلية").hasDetails)
    }

    @Test fun `a phone, an address or a description is sent after the draft is made`() {
        assertTrue(OnboardingForm(nameAr = "صيدلية", phone = "0933000002").hasDetails)
        assertTrue(OnboardingForm(nameAr = "صيدلية", addressAr = "وسط المدينة").hasDetails)
        assertTrue(OnboardingForm(nameAr = "صيدلية", descriptionAr = "مناوبة ليلية").hasDetails)
    }

    @Test fun `the patch carries every field, trimmed`() {
        val patch = OnboardingForm(
            nameAr = " صيدلية ",
            descriptionAr = " وصف ",
            phone = " 0933000002 ",
            whatsapp = "",
            addressAr = " وسط المدينة ",
        ).toPatch()

        assertEquals("صيدلية", patch.nameAr)
        assertEquals("وصف", patch.descriptionAr)
        assertEquals("0933000002", patch.phone)
        assertEquals("", patch.whatsapp)
        assertEquals("وسط المدينة", patch.addressAr)
    }
}
