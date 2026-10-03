package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.FacilityTag
import com.servacode.directory.core.model.OwnerCategoryConfig
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.OwnerFacilityPatch
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

/** What category "c" lets owners pick, in the operators' order. */
internal val clinicTags = CategoryTags(
    specialties = listOf(FacilityTag("3", "قلبية"), FacilityTag("4", "أطفال"), FacilityTag("5", "جلدية")),
    services = listOf(FacilityTag("12", "قياس ضغط"), FacilityTag("13", "تخطيط قلب")),
)

/** Facility "f-1" of category "c" in Raqqa, carrying [specialties] and [services]. */
internal fun tagged(specialties: List<String> = emptyList(), services: List<String> = emptyList()) =
    confirmable().copy(specialtyIds = specialties, serviceTagIds = services)

/** Raqqa's owner configuration, with category "c" offering [tags]. */
internal fun ownerConfig(tags: CategoryTags, categoryId: String = "c") = OwnerConfig(
    province = Province("raqqa", "الرقة"),
    categories = listOf(
        OwnerCategoryConfig(
            category = Category(categoryId, "عيادات"),
            specialization = "MEDICAL_CLINIC",
            capabilities = confirmable().summary.capabilities,
            verificationRequirements = emptyList(),
            tags = tags,
        ),
    ),
)

class FacilityTagsFormTest {
    private val form = FacilityTagsForm.of(tagged(specialties = listOf("3"), services = listOf("12")), clinicTags)

    @Test fun `what the facility carries is ticked and nothing has changed`() {
        assertEquals(setOf("3"), form.specialties)
        assertEquals(setOf("12"), form.services)
        assertFalse(form.changed)
        assertNull(form.patch())
    }

    @Test fun `a tick toggles on and off and back where it began is no change`() {
        val ticked = form.toggleSpecialty("4")

        assertEquals(setOf("3", "4"), ticked.specialties)
        assertTrue(ticked.changed)
        assertFalse(ticked.toggleSpecialty("4").changed)
    }

    @Test fun `only the list that changed is sent in the category's order`() {
        val patch = form.toggleSpecialty("5").toggleSpecialty("4").patch()

        assertEquals(OwnerFacilityPatch(specialtyIds = listOf("3", "4", "5")), patch)
        assertNull(patch?.serviceTagIds)
    }

    @Test fun `both lists go when both changed`() {
        val patch = form.toggleSpecialty("4").toggleService("13").patch()

        assertEquals(OwnerFacilityPatch(specialtyIds = listOf("3", "4"), serviceTagIds = listOf("12", "13")), patch)
    }

    @Test fun `untying the last one sends an empty list which is how the set is cleared`() {
        assertEquals(OwnerFacilityPatch(serviceTagIds = emptyList()), form.toggleService("12").patch())
    }

    @Test fun `an id the category does not offer is never sent since the backend refuses it`() {
        val carried = FacilityTagsForm.of(tagged(specialties = listOf("3", "99")), clinicTags)

        assertEquals(listOf("3", "4"), carried.toggleSpecialty("4").patch()?.specialtyIds)
        // Untouched, it is not sent at all, and stays on the backend as it is.
        assertNull(carried.toggleService("13").patch()?.specialtyIds)
    }

    @Test fun `read again an untouched list follows the backend and an edited one keeps its ticks`() {
        val editing = form.toggleService("13")

        val rebased = editing.rebase(tagged(specialties = listOf("4"), services = listOf("12")))

        assertEquals(setOf("4"), rebased.specialties)
        assertFalse(rebased.specialtiesChanged)
        assertEquals(setOf("12", "13"), rebased.services)
        assertTrue(rebased.servicesChanged)
    }

    @Test fun `a fresh list drops a new tick whose choice is gone and not what the facility carries`() {
        val fresh = clinicTags.copy(specialties = listOf(FacilityTag("4", "أطفال")))

        val kept = form.toggleSpecialty("5").withChoices(fresh)

        assertEquals(fresh, kept.choices)
        assertEquals(setOf("3"), kept.specialties)
        assertFalse(kept.changed)
    }

    @Test fun `a refusal naming either list means the choices changed underneath`() {
        fun refused(field: String) = AppError(
            AppError.Kind.VALIDATION,
            code = "VALIDATION_ERROR",
            fieldErrors = mapOf(field to listOf("One or more specialties are invalid.")),
            status = 400,
        )

        assertEquals(FacilityTagsFailure.ChoicesOutdated, FacilityTagsFailure.of(refused("specialtyIds")))
        assertEquals(FacilityTagsFailure.ChoicesOutdated, FacilityTagsFailure.of(refused("serviceTagIds")))
        assertEquals(FacilityTagsFailure.Failed(refused("phone")), FacilityTagsFailure.of(refused("phone")))
    }

    @Test fun `any other failure keeps the error for its own sentence`() {
        val offline = AppError(AppError.Kind.OFFLINE)
        val forbidden = AppError(
            AppError.Kind.FORBIDDEN,
            fieldErrors = mapOf("specialtyIds" to listOf("x")),
            status = 403,
        )

        assertEquals(FacilityTagsFailure.Failed(offline), FacilityTagsFailure.of(offline))
        assertEquals(FacilityTagsFailure.Failed(forbidden), FacilityTagsFailure.of(forbidden))
    }
}
