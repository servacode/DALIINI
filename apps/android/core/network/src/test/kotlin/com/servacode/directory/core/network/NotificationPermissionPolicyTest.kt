package com.servacode.directory.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {
    @Test fun `on Android 13 and later, a build with push asks once the permission is missing`() {
        for (sdk in listOf(33, 34, 36)) {
            assertTrue(NotificationPermissionPolicy.shouldRequest(sdk, granted = false, pushEnabled = true))
        }
    }

    @Test fun `below Android 13 the permission is granted at install and never asked for`() {
        for (sdk in listOf(24, 30, 32)) {
            assertFalse(NotificationPermissionPolicy.shouldRequest(sdk, granted = false, pushEnabled = true))
        }
    }

    @Test fun `a granted permission is not asked for again`() {
        assertFalse(NotificationPermissionPolicy.shouldRequest(34, granted = true, pushEnabled = true))
    }

    @Test fun `a build that cannot receive push does not ask for a permission it cannot use`() {
        assertFalse(NotificationPermissionPolicy.shouldRequest(34, granted = false, pushEnabled = false))
    }
}
