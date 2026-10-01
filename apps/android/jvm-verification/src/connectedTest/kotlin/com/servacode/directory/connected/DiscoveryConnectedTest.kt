package com.servacode.directory.connected

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.DirectoryQuery
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PREFIX = "e2e-m-"
// Raqqa city centre, where the fixtures stand.
private const val LATITUDE = 35.9528
private const val LONGITUDE = 39.0085

/**
 * Public discovery as the app asks for it, answered by the launch baseline and the mobile
 * fixtures. Anonymous throughout: nothing here needs, or sends, a session.
 */
class DiscoveryConnectedTest {
    private val api = Device().public

    private fun raqqa(): Province = runBlocking { api.provinces() }.first { it.nameEn == "Raqqa" }

    private fun pharmacyId(province: Province): String =
        runBlocking { api.categories(province.id) }.single().id

    /** Every fixture row of a list, following cursors to the end. */
    private fun everything(query: DirectoryQuery): List<FacilitySummary> = runBlocking {
        val rows = mutableListOf<FacilitySummary>()
        var cursor: String? = null
        do {
            val page = api.directory(query, cursor)
            rows += page.items
            cursor = page.nextCursor
            assertEquals(cursor != null, page.hasMore)
        } while (cursor != null)
        rows.filter { it.nameAr.startsWith(PREFIX) }
    }

    @Test fun `the provinces come from the backend and include Raqqa`() {
        val provinces = runBlocking { api.provinces() }

        assertTrue(provinces.any { it.nameAr == "الرقة" && it.nameEn == "Raqqa" })
    }

    @Test fun `Raqqa shows pharmacy and nothing whose public switch is off`() {
        val categories = runBlocking { api.categories(raqqa().id) }

        assertEquals(1, categories.size)
        val pharmacy = categories.single()
        assertEquals("Pharmacies", pharmacy.nameEn)
        assertTrue(pharmacy.capabilities!!.supportsDuty)
        for (hidden in listOf("Medical Laboratories", "Medical Clinics", "Nursing Centers", "Medical Supplies")) {
            assertFalse(hidden, categories.any { it.nameEn == hidden })
        }
    }

    @Test fun `availability is the backend's answer for each state`() {
        val province = raqqa()
        val rows = everything(DirectoryQuery(province.id, pharmacyId(province)))
        fun state(label: String) = rows.single { it.nameAr.contains(label) }.availability

        assertEquals(AvailabilityState.OPEN, state("مفتوحة"))
        assertEquals(AvailabilityState.DUTY, state("مناوبة"))
        assertEquals(AvailabilityState.TEMP_CLOSED, state("مغلقة مؤقتا"))
        assertEquals(AvailabilityState.CLOSED, state(" 5 "))
    }

    @Test fun `filters narrow the list on the backend`() {
        val province = raqqa()
        val pharmacy = pharmacyId(province)

        val open = everything(DirectoryQuery(province.id, pharmacy, openNow = true)).map { it.nameAr }
        val duty = everything(DirectoryQuery(province.id, pharmacy, dutyNow = true)).map { it.nameAr }

        assertTrue(open.any { it.contains("مفتوحة") })
        assertFalse(open.any { it.contains("مغلقة") })
        assertEquals(listOf(true), duty.map { it.contains("مناوبة") }.distinct())
    }

    @Test fun `pages follow the opaque cursor, nearest first, without repeats`() = runBlocking {
        val province = raqqa()
        val query = DirectoryQuery(
            provinceId = province.id,
            categoryId = pharmacyId(province),
            latitude = LATITUDE,
            longitude = LONGITUDE,
            pageSize = 2,
        )
        val first = api.directory(query)

        assertEquals(2, first.items.size)
        assertTrue(first.hasMore)
        val rows = everything(query)
        assertEquals(rows.map { it.id }.distinct(), rows.map { it.id })
        val distances = rows.map { it.distanceMeters!! }
        assertEquals(distances.sorted(), distances)
        assertTrue(rows.first().nameAr.contains(" 1 "))
    }

    @Test fun `a cursor the backend did not issue is refused on the cursor field`() {
        val province = raqqa()

        val error = runBlocking {
            runCatching { api.directory(DirectoryQuery(province.id, pharmacyId(province)), "not-a-cursor") }
                .exceptionOrNull() as AppException
        }.error

        assertEquals(AppError.Kind.VALIDATION, error.kind)
        assertTrue("cursor" in error.fieldErrors)
        assertTrue(error.requestId!!.isNotBlank())
    }

    @Test fun `search is the backend's`() = runBlocking {
        val page = api.search(raqqa().id, "مناوبة", latitude = null, longitude = null)

        assertTrue(page.items.any { it.nameAr.contains("مناوبة") })
        assertFalse(page.items.any { it.nameAr.contains("مفتوحة") })
    }

    @Test fun `detail hours arrive ordered by sequence within the day`() = runBlocking {
        val province = raqqa()
        val split = everything(DirectoryQuery(province.id,
            pharmacyId(province))).single { it.nameAr.contains("فترتان") }

        val detail = api.facility(split.id)

        val monday = detail.hours.filter { it.weekday == 0 }
        assertEquals(listOf(0, 1), monday.map { it.sequence })
        assertEquals(listOf("08:00", "16:00"), monday.map { it.opensAt.take(5) })
        assertEquals(LATITUDE + 0.008, detail.latitude!!, 1e-6)
    }

    @Test fun `the map shows the markers inside the viewport`() = runBlocking {
        val markers = api.mapFacilities(raqqa().id, west = 38.99, south = 35.94, east = 39.03, north = 35.98)

        assertTrue(markers.count { it.label.startsWith(PREFIX) } >= 5)
        assertNull(markers.firstOrNull { it.latitude > 35.98 })
    }

    @Test fun `home is province-wide without a location and carries the category`() = runBlocking {
        val province = raqqa()

        val home = api.home(province, latitude = null, longitude = null)

        assertEquals(province, home.province)
        assertEquals(listOf("Pharmacies"), home.categories.map { it.nameEn })
        assertTrue(home.refreshedAtEpochMillis > 0)
    }
}
