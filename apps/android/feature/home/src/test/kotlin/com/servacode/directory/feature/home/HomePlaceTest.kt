package com.servacode.directory.feature.home

import com.servacode.directory.core.model.PlaceResolution
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.ResolvedPlace
import com.servacode.directory.core.network.BackendLocationNameResolver
import com.servacode.directory.core.testing.FakeLocation
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.ScriptedPublicApi
import com.servacode.directory.core.testing.fix
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the header says, and in what order the app works it out.
 *
 * A normal user should never have to pick their province from a list. The device's own position
 * answers first, what was resolved before answers next, and the province they once chose answers
 * last — and refusing the location keeps the app whole rather than sending them to a picker.
 */
class HomePlaceTest {
    private val raqqa = Province("province-raqqa", "الرقة")
    private val api = ScriptedPublicApi()
    private val cache = FakePublicCache()

    private fun repository(
        preferences: FakePreferences,
        location: FakeLocation = FakeLocation(),
    ) = HomeRepository(cache, api, preferences, location, BackendLocationNameResolver(api) { 0L })

    @Test fun `a known position names the place and is remembered`() = runTest {
        api.placeAnswer = { _, _ ->
            ResolvedPlace(
                province = raqqa,
                cityNameAr = "الرقة",
                neighborhoodNameAr = "المشلب",
                label = "الرقة — المشلب",
                resolvedBy = PlaceResolution.BOUNDARY,
            )
        }
        val preferences = FakePreferences()

        val place = repository(preferences, FakeLocation(fix(35.95, 39.00))).place()

        assertEquals("الرقة — المشلب", place.label)
        assertEquals("province-raqqa", place.provinceId)
        assertTrue(place.fromLocation)
        // Remembered, so the next start opens with the same name before any fix arrives.
        assertEquals("الرقة — المشلب", preferences.values.first().placeLabel)
    }

    @Test fun `without a position the last resolved place still names the header`() = runTest {
        val preferences = FakePreferences(placeLabel = "الرقة — المشلب", placeProvinceId = "province-raqqa")

        val place = repository(preferences).place()

        assertEquals("الرقة — المشلب", place.label)
        assertEquals("province-raqqa", place.provinceId)
        assertFalse(place.fromLocation)
        // Nothing was asked of the backend: there was no position to ask about.
        assertTrue(api.calls.none { it.startsWith("resolve") })
    }

    @Test fun `with neither a position nor a memory the chosen province stands`() = runTest {
        val place = repository(FakePreferences(selectedProvinceId = "province-raqqa")).place()

        assertNull(place.label)
        assertEquals("province-raqqa", place.provinceId)
    }

    @Test fun `a backend that cannot resolve the point leaves the remembered name alone`() = runTest {
        api.placeAnswer = { _, _ -> throw IllegalStateException("offline") }
        val preferences = FakePreferences(placeLabel = "الرقة", placeProvinceId = "province-raqqa")

        val place = repository(preferences, FakeLocation(fix(35.95, 39.00))).place()

        assertEquals("الرقة", place.label)
        assertFalse(place.fromLocation)
    }
}
