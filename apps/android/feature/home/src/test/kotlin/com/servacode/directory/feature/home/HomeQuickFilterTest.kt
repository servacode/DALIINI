package com.servacode.directory.feature.home

import com.servacode.directory.core.testing.fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three chips on Home.
 *
 * Each one is the backend's own directory query, so the filtering and the ordering stay where
 * they already are. Nothing about opening hours or duty shifts is decided on the device.
 */
class HomeQuickFilterTest {
    @Test fun `open now and on duty are the backend's own flags, never both at once`() {
        val open = HomeQuickFilter.OPEN_NOW.query("province-1", null)
        val duty = HomeQuickFilter.DUTY_NOW.query("province-1", null)

        assertTrue(open.openNow)
        assertFalse(open.dutyNow)
        assertTrue(duty.dutyNow)
        assertFalse(duty.openNow)
    }

    @Test fun `nearest asks for no flag at all, only the position`() {
        val query = HomeQuickFilter.NEAREST.query("province-1", fix(35.95, 39.00))

        assertFalse(query.openNow)
        assertFalse(query.dutyNow)
        assertEquals(fix(35.95, 39.00).latitude, query.latitude)
        assertEquals(fix(35.95, 39.00).longitude, query.longitude)
    }

    @Test fun `a quick filter asks about the whole province, not one category`() {
        assertNull(HomeQuickFilter.OPEN_NOW.query("province-1", null).categoryId)
        assertEquals("province-1", HomeQuickFilter.OPEN_NOW.query("province-1", null).provinceId)
    }

    @Test fun `the position rides along with every filter when it is known`() {
        HomeQuickFilter.entries.forEach { filter ->
            val query = filter.query("province-1", fix(35.95, 39.00))
            assertEquals(fix(35.95, 39.00).latitude, query.latitude)
        }
    }

    @Test fun `nearest is offered only while a position is known`() {
        assertTrue(HomeQuickFilter.NEAREST.isAvailable(hasLocation = true))
        assertFalse(HomeQuickFilter.NEAREST.isAvailable(hasLocation = false))
        // The other two mean the same thing with or without a position.
        assertTrue(HomeQuickFilter.OPEN_NOW.isAvailable(hasLocation = false))
        assertTrue(HomeQuickFilter.DUTY_NOW.isAvailable(hasLocation = false))
    }
}
