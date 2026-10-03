package com.servacode.directory.core.transport

import com.servacode.directory.api.multiplatform.models.AdvertisementAction
import com.servacode.directory.api.multiplatform.models.AdvertisementActionTypeEnum
import com.servacode.directory.core.model.AdAction
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The port of Android's `AdActionMappingTest`. */
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
            assertEquals(AdAction.None, action(AdvertisementActionTypeEnum.EXTERNAL_URL, "url" to url).toDomain(), url)
        }
    }

    @Test fun `routes and none and malformed payloads are not clickable`() {
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

    @Test fun `the url check answers as java net URI answered on Android`() {
        // Each expected value is what Android's check, over java.net.URI, returns on the JVM.
        val expected = mapOf(
            "https://a.test:443/x?y=1#z" to true,
            "https://a.test:/x" to true,
            "https://a.test:99999999999/" to false,
            "https://a.test:8x/" to false,
            "https://1.2.3.4/" to true,
            "https://1.2.3.4:80/" to true,
            "https://1.2.3.400/" to false,
            "https://1.2.3/" to false,
            "https://123/" to true,
            "https://a.123/" to false,
            "https://a-.test/" to false,
            "https://-a.test/" to false,
            "https://a_b.test/" to false,
            "https://[::1]/" to true,
            "https://[::1]:8443/x" to true,
            "https://[fe80::1%25eth0]/" to true,
            "https://[fe80::1%eth0]/" to true,
            "https://[fe80::1%]/" to false,
            "https://[::1/" to false,
            "https://[1:2:3:4:5:6:7:8]/" to true,
            "https://[1:2:3:4:5:6:7]/" to false,
            "https://[1:2:3:4:5:6:7:8:9]/" to false,
            "https://[::ffff:1.2.3.4]/" to true,
            "https://[12345::]/" to false,
            "https://a.test/a b" to false,
            "https://a.test/%41" to true,
            "https://a.test/%4" to false,
            "https://a.test/%zz" to false,
            "https://a.test/é" to true,
            "https://é.test/" to false,
            "https://a.test/ " to false,
            "https://a.test/#a#b" to false,
            "https://a.test/?q=[1]" to true,
            "https://a.test/[1]" to false,
            "https://a.test/{x}" to false,
            "https://a.test/|" to false,
            "https://a.test/\\" to false,
            "https://a.test." to true,
            "https://a..test/" to false,
            "https://.a.test/" to false,
            "https:a.test" to false,
            "https:/a.test" to false,
            "https:" to false,
            "https://" to false,
            "https://@a.test/" to false,
            "https://a.test@b.test/" to false,
            "hTtPs://a.test/" to true,
            "https ://a.test/" to false,
            "https://a.test:443:1/" to false,
            "https://a%41.test/" to false,
            "https://a.test/\u0080" to false,
            "https://a.test/ " to false,
            "https://a.test/　" to false,
            "https://a.test/ÿ" to true,
            "https://a.test/;p=1" to true,
            "https://a.test/~user/*'()!" to true,
            "https://localhost" to true,
            "https://x" to true,
            "https://0/" to true,
            "https://00000001.2.3.4/" to true,
            "https://a.b-c.d1/" to true,
            "https://a.1b/" to false,
            "https://a.b1/" to true,
            "https://[::1]x/" to false,
            "https://[]/" to false,
            "https://[::1]]/" to false,
            "https://a.test?x" to true,
            "https://a.test#f" to true,
            "https://a.test/?#" to true,
            "https://a.test:/" to true,
        )
        expected.forEach { (url, safe) -> assertEquals(safe, isSafeExternalUrl(url), url) }
    }
}
