package com.servacode.directory.feature.facility

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.servacode.directory.core.analytics.NoOpAnalyticsTracker
import com.servacode.directory.core.auth.MemoryAccessTokenStore
import com.servacode.directory.core.auth.RefreshGateway
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionTokens
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.network.RealtimeInvalidationBus
import com.servacode.directory.core.testing.facility
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.FakeRecentlyViewedStore
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlin.test.Test
import kotlin.time.Clock

/**
 * Screen 08 Android shows, drawn on the iPhone simulator from the same code (DECISION-097): the
 * facility from the shared view model, its words and its plurals from the shared resources, and
 * its call handed to whatever the platform does with it.
 */
@OptIn(ExperimentalTestApi::class)
class FacilityScreenOnIosTest {
    private class Resumed : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private object SignedOut : RefreshTokenVault {
        override fun read(): String? = null
        override fun write(value: String) = Unit
        override fun clear() = Unit
    }

    private object NoRefresh : RefreshGateway {
        override suspend fun rotate(refreshToken: String): SessionTokens = error("never signed in")
    }

    @Test
    fun `the shared page draws the facility with its age in Arabic and hands over its number`() = runComposeUiTest {
        val threeDaysAgo = Clock.System.now().toEpochMilliseconds() - 3 * 24 * 3_600_000L - 60_000L
        val detail = FacilityDetail(facility("f-1"), phone = "+963933000000", lastVerifiedAtEpochMillis = threeDaysAgo)
        val api = ScriptedPublicApi().apply { facilityAnswer = { detail } }
        val repository = FacilityRepository(FakePublicCache(), api, FakePreferences("raqqa"))
        val page = FacilityViewModel(
            "f-1",
            FacilityUseCase(repository),
            RealtimeInvalidationBus(),
            SessionCoordinator(MemoryAccessTokenStore(), SignedOut, NoRefresh),
            RecordVisitUseCase(FakeRecentlyViewedStore()),
            NoOpAnalyticsTracker,
        )
        val owner = Resumed()
        var called: String? = null
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                DirectoryTheme(darkTheme = false) {
                    FacilityScreen(
                        page,
                        FacilityReportViewModel(ReportFacilityUseCase(repository)),
                        onDirections = { _, _ -> },
                        onSignIn = {},
                        onCall = { called = it },
                        onWhatsApp = {},
                        onShare = {},
                        onBack = {},
                    )
                }
            }
        }

        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("صيدلية f-1").fetchSemanticsNodes().isNotEmpty() }
        // Arabic's "few": the number in the sentence, not a placeholder (DECISION-097).
        onNodeWithText("تم التحقق قبل 3 أيام", substring = true).assertExists()
        onNodeWithText("اتصال").performClick()
        waitUntil { called == "+963933000000" }
    }
}
