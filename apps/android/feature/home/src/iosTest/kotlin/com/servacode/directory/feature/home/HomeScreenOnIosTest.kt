package com.servacode.directory.feature.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.servacode.directory.core.analytics.NoOpAnalyticsTracker
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.BackendLocationNameResolver
import com.servacode.directory.core.network.NetworkMonitor
import com.servacode.directory.core.network.RealtimeInvalidationBus
import com.servacode.directory.core.testing.facility
import com.servacode.directory.core.testing.FakeLocation
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.FakeRecentlyViewedStore
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The home Android shows, drawn on the iPhone simulator from the same code (DECISION-095): the
 * province's facilities from the shared view model, the shared words, and the offer to use the
 * position, which on the iPhone is Core Location's own question.
 */
@OptIn(ExperimentalTestApi::class)
class HomeScreenOnIosTest {
    private class Resumed : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private object Online : NetworkMonitor {
        override val online = MutableStateFlow(true)
        override val unmetered = MutableStateFlow(true)
    }

    @Test
    fun `the shared home draws the province's facilities and offers to use the position`() = runComposeUiTest {
        val raqqa = Province("raqqa", "الرقة")
        val cache = FakePublicCache().apply { provinces = listOf(raqqa) }
        val snapshot = HomeSnapshot(raqqa, emptyList(), listOf(facility("a")), refreshedAtEpochMillis = 1)
        val api = ScriptedPublicApi().apply {
            homeAnswer = { snapshot }
            // The list under the offers is the directory's own page, not the snapshot's.
            directoryAnswer = { _, _ -> Page(listOf(facility("a")), nextCursor = null, hasMore = false) }
        }
        val preferences = FakePreferences("raqqa")
        val home = HomeViewModel(
            HomeUseCase(HomeRepository(cache, api, preferences, FakeLocation(), BackendLocationNameResolver(api))),
            HomeAdsUseCase(HomeAdsRepository(cache, api)),
            RealtimeInvalidationBus(),
            NoOpAnalyticsTracker,
        )
        val extras = HomeExtrasViewModel(FakeRecentlyViewedStore(), preferences, Online)
        val owner = Resumed()
        var opened: String? = null
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                DirectoryTheme(darkTheme = false) {
                    HomeScreen(
                        home,
                        extras,
                        onProvince = {},
                        onSearch = {},
                        onFacility = { opened = it },
                        onNotifications = {},
                    )
                }
            }
        }

        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("الرقة").fetchSemanticsNodes().isNotEmpty() }
        // The simulator's test process has never been asked for the position.
        onNodeWithText("السماح بالموقع").assertExists()
        // The list is under the offers, and a lazy list draws only what is on screen: it is
        // scrolled to, as a reader would, once its page has arrived.
        waitUntil(timeoutMillis = 5_000) {
            runCatching {
                onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("صيدلية a"))
            }.isSuccess
        }
        onNodeWithText("صيدلية a").performClick()
        waitUntil { opened == "a" }
    }
}
