package com.servacode.directory.core.network.api

import com.servacode.directory.api.models.AdvertisementAction
import com.servacode.directory.api.models.AdvertisementActionTypeEnum
import com.servacode.directory.core.model.AdAction
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdActionMappingTest {
    private fun action(type: AdvertisementActionTypeEnum, vararg payload: Pair<String, String>) =
        AdvertisementAction(type, payload.associate { (key, value) -> key to JsonPrimitive(value) })

    @Test fun `facility and category open inside the app`() {
        assertEquals(
            AdAction.OpenFacility("f-1"),
            action(AdvertisementActionTypeEnum.FACILITY, "facilityId" to "f-1").toDomain(),
        )
        assertEquals(
            AdAction.OpenCategory("c-1"),
            action(AdvertisementActionTypeEnum.CATEGORY, "categoryId" to "c-1").toDomain(),
        )
    }

    @Test fun `only credential-free https addresses may leave the app`() {
        assertEquals(
            AdAction.OpenUrl("https://example.test/offer"),
            action(AdvertisementActionTypeEnum.EXTERNAL_URL, "url" to "https://example.test/offer").toDomain(),
        )
        listOf(
            "http://example.test/",
            "javascript:alert(1)",
            "intent://scan/#Intent;scheme=zxing;end",
            "file:///sdcard/x",
            "https://user:pw@example.test/",
            "https:///no-host",
            "not a url",
        ).forEach { url ->
            assertEquals(url, AdAction.None, action(AdvertisementActionTypeEnum.EXTERNAL_URL, "url" to url).toDomain())
        }
    }

    @Test fun `routes, none and malformed payloads are not clickable`() {
        assertEquals(AdAction.None, action(AdvertisementActionTypeEnum.IN_APP_ROUTE, "route" to "/search").toDomain())
        assertEquals(AdAction.None, action(AdvertisementActionTypeEnum.NONE).toDomain())
        assertEquals(AdAction.None, action(AdvertisementActionTypeEnum.FACILITY).toDomain())
        assertEquals(AdAction.None, action(AdvertisementActionTypeEnum.FACILITY, "facilityId" to "  ").toDomain())
        val nested = AdvertisementAction(
            AdvertisementActionTypeEnum.FACILITY,
            mapOf("facilityId" to buildJsonObject { put("a", JsonPrimitive(1)) }),
        )
        assertEquals(AdAction.None, nested.toDomain())
    }

    @Test fun `the url check is strict about the scheme`() {
        assertTrue(isSafeExternalUrl("https://a.test"))
        assertTrue(isSafeExternalUrl("HTTPS://a.test"))
        assertFalse(isSafeExternalUrl("http://a.test"))
    }
}
