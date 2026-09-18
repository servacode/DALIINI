package com.servacode.directory.core.network

import com.servacode.directory.core.network.api.ApiEnvironment
import com.servacode.directory.core.network.api.ApiNotConfiguredException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeInvalidationTest {
    private fun event(name: String, type: String, scopeId: String, resourceId: String? = null) =
        RealtimeEnvelope(1, name, RealtimeScope(type, scopeId), resourceId, "2026-09-19T10:00:00Z")

    @Test fun `a public event refreshes the lists of its own province only`() {
        val raqqa = event("public.province.configuration_changed", "province", "raqqa-id")

        assertTrue(RealtimeInvalidation.refreshesProvinceLists(raqqa, "raqqa-id"))
        assertFalse(RealtimeInvalidation.refreshesProvinceLists(raqqa, "aleppo-id"))
        assertFalse(RealtimeInvalidation.refreshesProvinceLists(raqqa, null))
    }

    @Test fun `a facility event refreshes that facility only`() {
        val changed = event("public.facility.availability_changed", "province", "raqqa-id", "facility-1")

        assertTrue(RealtimeInvalidation.refreshesFacility(changed, "facility-1"))
        assertFalse(RealtimeInvalidation.refreshesFacility(changed, "facility-2"))
        assertFalse(
            RealtimeInvalidation.refreshesFacility(
                event("public.province.configuration_changed", "province", "raqqa-id", "facility-1"),
                "facility-1",
            ),
        )
    }

    @Test fun `owner state follows user events, not public ones`() {
        assertTrue(RealtimeInvalidation.refreshesOwnerState(event("user.application.changed", "user", "u")))
        assertFalse(RealtimeInvalidation.refreshesOwnerState(event("public.facility.changed", "province", "p")))
    }
}

class ApiEnvironmentTest {
    @Test fun `a configured https address is used with a trailing slash`() {
        assertEquals("https://api.example.test/", ApiEnvironment("https://api.example.test").requireConfiguredBaseUrl())
    }

    @Test fun `placeholders, cleartext and queries are refused`() {
        for (value in listOf("https://api.<ROOT_DOMAIN>/", "", "http://api.example.test/", "https://api.example.test/?x=1")) {
            assertThrows(value, ApiNotConfiguredException::class.java) {
                ApiEnvironment(value).requireConfiguredBaseUrl()
            }
        }
    }

    @Test fun `only the local flavor may use cleartext`() {
        assertEquals(
            "http://10.0.2.2:8000/",
            ApiEnvironment("http://10.0.2.2:8000/", allowCleartext = true).requireConfiguredBaseUrl(),
        )
    }
}
