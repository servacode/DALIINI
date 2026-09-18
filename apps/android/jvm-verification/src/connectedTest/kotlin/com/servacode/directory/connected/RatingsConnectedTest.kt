package com.servacode.directory.connected

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.network.DirectoryQuery
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingsConnectedTest {
    private val device = Citizen.device
    private val api = device.public

    private fun openPharmacyId(): String = runBlocking {
        val raqqa = api.provinces().first { it.nameEn == "Raqqa" }
        val pharmacy = api.categories(raqqa.id).single()
        api.directory(DirectoryQuery(raqqa.id, pharmacy.id, pageSize = 50)).items
            .first { it.nameAr.startsWith("e2e-m-") && it.nameAr.contains("مفتوحة") }.id
    }

    @Test fun `a rating is stored, shown in the account, counted publicly, changed and removed`() = runBlocking {
        val facility = openPharmacyId()

        assertEquals(4, api.upsertRating(facility, 4))
        assertEquals(4, api.ratings().single { it.facilityId == facility }.stars)
        val counted = api.facility(facility).summary
        assertTrue(counted.ratingCount >= 1)

        assertEquals(5, api.upsertRating(facility, 5))
        assertEquals(5, api.ratings().single { it.facilityId == facility }.stars)

        api.deleteRating(facility)
        assertFalse(api.ratings().any { it.facilityId == facility })
    }

    @Test fun `the backend refuses stars outside one to five on its own`() = runBlocking {
        // Straight to the adapter, past the app's own check, to prove the backend's.
        val error = runCatching { api.upsertRating(openPharmacyId(), 6) }.exceptionOrNull() as AppException

        assertEquals(AppError.Kind.VALIDATION, error.error.kind)
        assertTrue(error.error.fieldErrors.containsKey("stars"))
    }
}
