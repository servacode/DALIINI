package com.servacode.directory.feature.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapViewportTest {
    @Test fun acceptsValidCoordinates() {
        val value = MapViewport(38.0, 35.0, 39.0, 36.0)
        assertEquals(38.0, value.west, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidLongitude() {
        MapViewport(-181.0, 0.0, 1.0, 1.0)
    }
}
