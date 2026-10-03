package com.servacode.directory.feature.ratings

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingsRepositoryTest {
    private val api = ScriptedPublicApi()
    private val repository = RatingsRepository(api)

    @Test fun `out of range stars never reach the backend`() = runTest {
        for (stars in listOf(0, 6, -1)) {
            val error = repository.rate("f", stars).exceptionOrNull()!!.toAppError()
            assertEquals(AppError.Kind.VALIDATION, error.kind)
            assertTrue("stars" in error.fieldErrors)
        }
        assertTrue(api.calls.isEmpty())
    }

    @Test fun `a valid rating is what the backend stored`() = runTest {
        api.upsertAnswer = { _, _ -> 4 }

        assertEquals(4, repository.rate("f", 4).getOrThrow())
        assertEquals(listOf("rate:f:4"), api.calls)
    }

    @Test fun `the user's own rating is looked up in their account, not in any cache`() = runTest {
        api.ratingsAnswer = { listOf(UserRating("f", "f", "صيدلية", stars = 5, updatedAtEpochMillis = 0)) }

        assertEquals(5, repository.mine("f").getOrThrow())
        assertNull(repository.mine("other").getOrThrow())
    }
}
