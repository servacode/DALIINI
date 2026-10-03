package com.servacode.directory.feature.province

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
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlin.test.Test

/**
 * The province picker Android shows, drawn on the iPhone simulator from the same code: its words
 * from the shared resources, its list from the shared view model (DECISION-095).
 */
@OptIn(ExperimentalTestApi::class)
class ProvinceScreenOnIosTest {
    private class Resumed : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    @Test
    fun `the shared picker lists the backend's provinces and moves on once one is chosen`() = runComposeUiTest {
        val api = ScriptedPublicApi().apply { provincesAnswer = { listOf(Province("raqqa", "الرقة")) } }
        val model = ProvinceViewModel(ProvinceUseCase(ProvinceRepository(FakePublicCache(), api, FakePreferences())))
        val owner = Resumed()
        var chosen = false
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                DirectoryTheme(darkTheme = false) {
                    ProvinceScreen(model, onSelected = { chosen = true })
                }
            }
        }

        waitUntil { onAllNodesWithText("الرقة").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("اختر المحافظة").assertExists()
        onNodeWithText("الرقة").performClick()
        waitUntil { chosen }
    }
}
