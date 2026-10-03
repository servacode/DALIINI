package com.servacode.directory.core.model

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class MapNavigationTest {
    @Test fun `a detail opens the map for its own facility`() {
        assertEquals(DirectoryRoute.Map(focusFacilityId = "f1"), MapNavigation.mapFor("f1"))
    }

    @Test fun `the map opened from a facility goes back to it instead of stacking a copy`() {
        assertTrue(MapNavigation.returnsToDetail(DirectoryRoute.FacilityDetailRoute("f1"), "f1"))
    }

    @Test fun `another facility or a map opened from home opens a detail`() {
        assertFalse(MapNavigation.returnsToDetail(DirectoryRoute.FacilityDetailRoute("f1"), "f2"))
        assertFalse(MapNavigation.returnsToDetail(DirectoryRoute.Home, "f1"))
        assertFalse(MapNavigation.returnsToDetail(null, "f1"))
    }

    @Test fun `the map route carries only the facility id`() {
        assertEquals(null, DirectoryRoute.Map().focusFacilityId)
        assertEquals("f1", DirectoryRoute.Map("f1").focusFacilityId)
    }
}
