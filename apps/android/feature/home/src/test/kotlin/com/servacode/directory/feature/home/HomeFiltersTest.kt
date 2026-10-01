package com.servacode.directory.feature.home

import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.FacilityCapabilities
import com.servacode.directory.core.model.FacilityTag
import com.servacode.directory.core.network.DirectorySort
import com.servacode.directory.core.testing.fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The chips above Home's list.
 *
 * They combine, because the questions they stand for combine: the nearest pharmacies that are
 * on tonight's roster and open right now is one question, not the last of three. Each chip is a
 * flag on the backend's own directory query, so nothing about hours or duty is worked out here.
 */
class HomeFiltersTest {
    private fun category(duty: Boolean, specialties: Boolean = false, services: Boolean = false) = Category(
        id = "category-1",
        nameAr = "صيدليات",
        capabilities = FacilityCapabilities(
            supportsHours = true,
            supportsPhotos = true,
            supportsDuty = duty,
            supportsSpecialtyFilter = specialties,
            supportsServiceFilter = services,
            supportsTemporaryClosure = true,
            supportsOwnerOnboarding = true,
        ),
    )

    private val tags = CategoryTags(
        specialties = listOf(FacilityTag("3", "قلبية"), FacilityTag("4", "أطفال")),
        services = listOf(FacilityTag("12", "قياس ضغط")),
    )

    @Test fun `nothing chosen is all, and all is what the backend is asked for`() {
        val query = HomeFilters().query("province-1", "category-1", null)

        assertTrue(HomeFilters().isAll)
        assertFalse(query.openNow)
        assertFalse(query.dutyToday)
    }

    @Test fun `there is no all chip, because an empty row already says it`() {
        // "All" was never a filter — it was the absence of the other three — so a chip for it
        // could only ever undo them, at the price of a quarter of the row.
        assertEquals(
            listOf(HomeChip.NEAREST, HomeChip.OPEN_NOW, HomeChip.DUTY_TODAY),
            HomeChip.entries,
        )
    }

    @Test fun `open now and on duty today are separate flags and travel together`() {
        val both = HomeFilters().toggle(HomeChip.OPEN_NOW).toggle(HomeChip.DUTY_TODAY)
        val query = both.query("province-1", null, null)

        assertTrue(query.openNow)
        assertTrue(query.dutyToday)
        // Never confused with a shift that happens to be running at this second.
        assertFalse(query.dutyNow)
    }

    @Test fun `nearest orders, it does not narrow`() {
        val nearest = HomeFilters().toggle(HomeChip.NEAREST)
        val query = nearest.query("province-1", null, fix(35.95, 39.00))

        assertEquals(DirectorySort.NEAREST, query.sort)
        assertFalse(query.openNow)
        assertFalse(query.dutyToday)
    }

    @Test fun `the position rides along whatever is chosen, so distances never disappear`() {
        val byName = HomeFilters().query("province-1", null, fix(35.95, 39.00))
        val byDistance = HomeFilters(nearest = true).query("province-1", null, fix(35.95, 39.00))

        assertEquals(35.95, byName.latitude)
        assertEquals(35.95, byDistance.latitude)
        assertEquals(DirectorySort.NAME, byName.sort)
    }

    @Test fun `turning the last chip off is how one gets back to everything`() {
        val narrowed = HomeFilters(openNow = true)

        assertTrue(narrowed.toggle(HomeChip.OPEN_NOW).isAll)
    }

    @Test fun `tapping a chosen chip again lets it go`() {
        val on = HomeFilters().toggle(HomeChip.OPEN_NOW)

        assertTrue(on.openNow)
        assertFalse(on.toggle(HomeChip.OPEN_NOW).openNow)
    }

    @Test fun `duty is offered only where the category keeps a roster`() {
        assertTrue(HomeChip.DUTY_TODAY.isOffered(hasLocation = true, category = category(duty = true)))
        assertFalse(HomeChip.DUTY_TODAY.isOffered(hasLocation = true, category = category(duty = false)))
        assertFalse(HomeChip.DUTY_TODAY.isOffered(hasLocation = true, category = null))
    }

    @Test fun `nearest is offered only while a position is known`() {
        assertTrue(HomeChip.NEAREST.isOffered(hasLocation = true, category = null))
        assertFalse(HomeChip.NEAREST.isOffered(hasLocation = false, category = null))
    }

    @Test fun `a category without a roster drops a duty chip that was already chosen`() {
        val chosen = HomeFilters(dutyToday = true, nearest = true)

        val moved = chosen.withinReach(hasLocation = true, category = category(duty = false))

        assertFalse(moved.dutyToday)
        assertTrue(moved.nearest)
    }

    @Test fun `losing the position drops nearest rather than ordering by a place unknown`() {
        val chosen = HomeFilters(nearest = true, openNow = true)

        val moved = chosen.withinReach(hasLocation = false, category = category(duty = true))

        assertFalse(moved.nearest)
        assertTrue(moved.openNow)
    }

    @Test fun `the chips offered are exactly the ones that can do something`() {
        assertEquals(
            listOf(HomeChip.NEAREST, HomeChip.OPEN_NOW, HomeChip.DUTY_TODAY),
            homeChips(hasLocation = true, category = category(duty = true)),
        )
        assertEquals(
            listOf(HomeChip.OPEN_NOW),
            homeChips(hasLocation = false, category = category(duty = false)),
        )
    }

    @Test fun `the chosen category is what the list is narrowed by`() {
        assertEquals("category-1", HomeFilters().query("province-1", "category-1", null).categoryId)
        assertNull(HomeFilters().query("province-1", null, null).categoryId)
    }

    @Test fun `the empty message names the question that was asked`() {
        assertEquals(HomeEmptyReason.DUTY, HomeFilters(dutyToday = true).emptyReason())
        assertEquals(HomeEmptyReason.OPEN, HomeFilters(openNow = true).emptyReason())
        assertEquals(
            HomeEmptyReason.DUTY_AND_OPEN,
            HomeFilters(openNow = true, dutyToday = true).emptyReason(),
        )
        assertEquals(HomeEmptyReason.CATEGORY, HomeFilters().emptyReason())
    }

    @Test fun `a specialty and a service narrow the query, one of each, as ids`() {
        val narrowed = HomeFilters().chooseSpecialty("3").chooseService("12")
        val query = narrowed.query("province-1", "category-1", null)

        assertEquals("3", query.specialtyId)
        assertEquals("12", query.serviceTagId)
        assertTrue(narrowed.hasTags)
        assertFalse(narrowed.isAll)
        // Nothing chosen sends neither.
        assertNull(HomeFilters().query("province-1", "category-1", null).specialtyId)
        assertNull(HomeFilters().query("province-1", "category-1", null).serviceTagId)
    }

    @Test fun `choosing another replaces the one before, since the backend takes one`() {
        assertEquals("4", HomeFilters().chooseSpecialty("3").chooseSpecialty("4").specialtyId)
    }

    @Test fun `the chosen one again, or all, lets it go`() {
        val chosen = HomeFilters().chooseSpecialty("3").chooseService("12")

        assertNull(chosen.chooseSpecialty("3").specialtyId)
        assertNull(chosen.chooseSpecialty(null).specialtyId)
        assertEquals("12", chosen.chooseSpecialty(null).serviceTagId)
        assertNull(chosen.chooseService(null).serviceTagId)
    }

    @Test fun `clearing lets both go and leaves the other chips as they were`() {
        val chosen = HomeFilters(openNow = true, nearest = true).chooseSpecialty("3").chooseService("12")

        assertEquals(HomeFilters(openNow = true, nearest = true), chosen.clearTags())
    }

    @Test fun `a category that does not filter by them drops them`() {
        val chosen = HomeFilters(openNow = true).chooseSpecialty("3").chooseService("12")

        val services = chosen.withinReach(hasLocation = false, category = category(duty = false, services = true))
        val neither = chosen.withinReach(hasLocation = false, category = category(duty = false))

        assertNull(services.specialtyId)
        assertEquals("12", services.serviceTagId)
        assertEquals(HomeFilters(openNow = true), neither)
    }

    @Test fun `a choice the category no longer offers is dropped rather than narrowing to nothing`() {
        val chosen = HomeFilters().chooseSpecialty("9").chooseService("12")

        val kept = chosen.withinTags(tags)

        assertNull(kept.specialtyId)
        assertEquals("12", kept.serviceTagId)
        assertEquals(chosen.chooseSpecialty(null), kept)
    }

    @Test fun `rows are offered only where the category's capabilities filter by them`() {
        assertEquals(tags, offeredTags(category(duty = false, specialties = true, services = true), tags))
        assertEquals(
            CategoryTags(specialties = tags.specialties),
            offeredTags(category(duty = false, specialties = true), tags),
        )
        assertTrue(offeredTags(category(duty = true), tags).isEmpty)
        assertTrue(offeredTags(null, tags).isEmpty)
        assertTrue(filtersByTags(category(duty = false, services = true)))
        assertFalse(filtersByTags(category(duty = true)))
        assertFalse(filtersByTags(null))
    }

    @Test fun `an empty list narrowed by a choice says so, whatever else is on`() {
        assertEquals(HomeEmptyReason.CHOICE, HomeFilters().chooseService("12").emptyReason())
        assertEquals(
            HomeEmptyReason.CHOICE,
            HomeFilters(openNow = true, dutyToday = true).chooseSpecialty("3").emptyReason(),
        )
    }
}
