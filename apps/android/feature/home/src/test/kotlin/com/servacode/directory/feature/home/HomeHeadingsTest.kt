package com.servacode.directory.feature.home

import com.servacode.directory.core.testing.facility
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeHeadingsTest {
    @Test fun `a list the backend ordered by the user's location is the nearest`() {
        val located = listOf(facility("a").copy(distanceMeters = 120.0), facility("b").copy(distanceMeters = 480.0))

        assertEquals("الأقرب إليك", HomeHeadings.nearby(located))
    }

    @Test fun `without a location the list is not presented as the nearest`() {
        val provinceWide = listOf(facility("a"), facility("b"))

        assertEquals("المنشآت", HomeHeadings.nearby(provinceWide))
    }

    @Test fun `an empty list claims nothing about location`() {
        assertEquals("المنشآت", HomeHeadings.nearby(emptyList()))
    }
}
