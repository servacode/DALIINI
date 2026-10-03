package com.servacode.directory.feature.facility

import com.servacode.directory.core.analytics.NoOpAnalyticsTracker
import com.servacode.directory.core.auth.MemoryAccessTokenStore
import com.servacode.directory.core.auth.RefreshGateway
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionTokens
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.network.RealtimeInvalidationBus
import com.servacode.directory.core.testing.facility
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.FakeRecentlyViewedStore
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

/** A facility's page, shared since DECISION-095, on the JVM and on the iPhone simulator. */
class FacilityViewModelTest {
    private val cache = FakePublicCache()
    private val api = ScriptedPublicApi()
    private val recent = FakeRecentlyViewedStore()

    private object SignedOut : RefreshTokenVault {
        override fun read(): String? = null
        override fun write(value: String) = Unit
        override fun clear() = Unit
    }

    private object NoRefresh : RefreshGateway {
        override suspend fun rotate(refreshToken: String): SessionTokens = error("never signed in")
    }

    private fun viewModel(id: String = "f-1") = FacilityViewModel(
        id,
        FacilityUseCase(FacilityRepository(cache, api, FakePreferences("raqqa"))),
        RealtimeInvalidationBus(),
        SessionCoordinator(MemoryAccessTokenStore(), SignedOut, NoRefresh),
        RecordVisitUseCase(recent) { 1L },
        NoOpAnalyticsTracker,
    )

    @Test fun `the facility the page was opened for is shown and kept among the recent`() = runMainTest {
        api.facilityAnswer = { FacilityDetail(facility(it)) }

        val model = viewModel()
        advanceUntilIdle()

        val content = model.state.value as FacilityUiState.Content
        assertEquals("f-1", content.value.summary.id)
        assertFalse(content.stale)
        assertFalse(content.signedIn)
        assertEquals(listOf("f-1"), recent.state.value.map { it.id })
    }

    @Test fun `offline the saved page is shown and said to be saved`() = runMainTest {
        cache.details["f-1"] = FacilityDetail(facility("f-1"))

        val model = viewModel()
        advanceUntilIdle()

        assertTrue((model.state.value as FacilityUiState.Content).stale)
    }

    @Test fun `offline with nothing saved is an error`() = runMainTest {
        val model = viewModel()
        advanceUntilIdle()

        assertEquals(AppError.Kind.OFFLINE, (model.state.value as FacilityUiState.Error).error.kind)
    }
}
