package com.servacode.directory.core.network

import com.servacode.directory.core.model.PlaceResolution
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.ResolvedPlace
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What the header is allowed to cost.
 *
 * A map moves constantly; the name of a place does not. The resolver may ask the backend when
 * the device has actually travelled and never more than once a minute, and a failure must leave
 * the last known name standing rather than emptying the header.
 */
class LocationNameResolverTest {
    private val raqqa = Province(id = "province-1", nameAr = "الرقة")
    private val place = ResolvedPlace(
        province = raqqa,
        cityNameAr = "الرقة",
        neighborhoodNameAr = "المشلب",
        label = "الرقة — المشلب",
        resolvedBy = PlaceResolution.BOUNDARY,
    )

    private fun resolver(api: ScriptedPublicApi, now: () -> Long) =
        BackendLocationNameResolver(api, now)

    @Test fun `the first coordinate is always asked about`() = runTest {
        val api = ScriptedPublicApi().apply { placeAnswer = { _, _ -> place } }

        val answer = resolver(api) { 0L }.resolve(35.95, 39.00)

        assertEquals("الرقة — المشلب", answer?.label)
        assertEquals(listOf("resolve:35.95:39.0"), api.calls)
    }

    @Test fun `standing still asks once, however often it is called`() = runTest {
        val api = ScriptedPublicApi().apply { placeAnswer = { _, _ -> place } }
        val resolver = resolver(api) { 0L }

        resolver.resolve(35.95, 39.00)
        resolver.resolve(35.9501, 39.0001)
        resolver.resolve(35.9502, 39.0002)

        assertEquals(1, api.calls.size)
    }

    @Test fun `a minute must pass before the same journey is asked about again`() = runTest {
        val api = ScriptedPublicApi().apply { placeAnswer = { _, _ -> place } }
        var now = 0L
        val resolver = resolver(api) { now }

        resolver.resolve(35.95, 39.00)
        now = 30_000L
        resolver.resolve(36.50, 39.50)
        now = 61_000L
        resolver.resolve(36.50, 39.50)

        assertEquals(2, api.calls.size)
    }

    @Test fun `a failure keeps the name the header already had`() = runTest {
        val api = ScriptedPublicApi().apply { placeAnswer = { _, _ -> place } }
        var now = 0L
        val resolver = resolver(api) { now }
        resolver.resolve(35.95, 39.00)

        api.placeAnswer = { _, _ -> throw IllegalStateException("offline") }
        now = 120_000L
        val answer = resolver.resolve(36.50, 39.50)

        assertEquals("الرقة — المشلب", answer?.label)
        assertEquals("الرقة — المشلب", resolver.last()?.label)
    }

    @Test fun `a point outside every province leaves the header alone`() = runTest {
        val api = ScriptedPublicApi().apply {
            placeAnswer = { _, _ -> ResolvedPlace(province = null, resolvedBy = PlaceResolution.NONE) }
        }

        val answer = resolver(api) { 0L }.resolve(48.85, 2.35)

        assertNull(answer)
    }
}
