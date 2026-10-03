package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.OwnerFacilityInsights
import com.servacode.directory.core.testing.ScriptedOwnerApi
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

internal fun insights(views: Int = 12, calls: Int = 3, directions: Int = 5) = OwnerFacilityInsights(
    facilityId = "f-1",
    windowDays = 30,
    sinceEpochMillis = 1_787_220_000_000L,
    views = views,
    calls = calls,
    directions = directions,
)

class OwnerInsightsRepositoryTest {
    private val api = ScriptedOwnerApi()
    private val load = LoadOwnerInsightsUseCase(OwnerRepository(api))

    @Test fun `the facility's figures come back as the backend counted them`() = runTest {
        api.insightsAnswer = { insights() }

        assertEquals(insights(), load("f-1").getOrThrow())
        assertEquals(listOf("insights:f-1"), api.calls)
    }

    @Test fun `a refusal is a failure the card can show`() = runTest {
        api.insightsAnswer = { throw AppException(AppError(AppError.Kind.FORBIDDEN, status = 403)) }

        val error = load("f-1").exceptionOrNull() as AppException

        assertEquals(AppError.Kind.FORBIDDEN, error.error.kind)
    }

    @Test fun `a window with nothing in it is empty`() {
        assertTrue(insights(0, 0, 0).isEmpty)
        assertFalse(insights(0, 1, 0).isEmpty)
    }
}
