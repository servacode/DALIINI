package com.servacode.directory.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionsTest {
    @Test fun `the app asks for the two foreground location permissions and nothing else`() {
        assertEquals(
            listOf(
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
            ),
            FOREGROUND_LOCATION_PERMISSIONS,
        )
    }

    @Test fun `background location is never asked for`() {
        assertTrue(FOREGROUND_LOCATION_PERMISSIONS.none { "BACKGROUND" in it })
    }
}
