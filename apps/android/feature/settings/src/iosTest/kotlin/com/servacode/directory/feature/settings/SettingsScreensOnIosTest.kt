package com.servacode.directory.feature.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.core.testing.FakeEmergencyNumbersCache
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Settings and the emergency numbers Android shows, drawn on the iPhone simulator from the same
 * code (DECISION-100): the phone's own switches read and opened through [SystemSettings], the
 * built-in lines named from the shared words, and a number handed to whatever dials it.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsScreensOnIosTest {
    private class Resumed : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    /** The phone's answer and its pages, without the notification centre a test binary has not got. */
    private class FakeSystem(private val allowed: Boolean) : SystemSettings {
        val opened = mutableListOf<String>()
        override suspend fun noticesAllowed() = allowed
        override fun openNotices() { opened += "notices" }
        override fun openApp() { opened += "app" }
    }

    @Test
    fun `settings read the phone's answer and open its pages`() = runComposeUiTest {
        val preferences = FakePreferences("raqqa")
        val api = ScriptedPublicApi()
        val system = FakeSystem(allowed = false)
        val owner = Resumed()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                DirectoryTheme(darkTheme = false) {
                    SettingsScreen(
                        PreferencesViewModel(preferences, NotificationPreferencesSync(api, preferences)),
                        AppearanceViewModel(preferences),
                        onChangePassword = {},
                        onChangePhone = {},
                        onBack = {},
                        signedIn = false,
                        offlineMap = { Text("خريطة المحافظة على الجهاز") },
                        system = system,
                    )
                }
            }
        }

        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("غير مسموح").fetchSemanticsNodes().isNotEmpty() }
        // Signed out there is no password to change.
        assertEquals(0, onAllNodesWithText("الحساب والأمان").fetchSemanticsNodes().size)
        onNodeWithText("السماح بالإشعارات").performClick()
        onNodeWithText("داكن").performScrollTo().performClick()
        onNodeWithText("خريطة المحافظة على الجهاز").performScrollTo()
        onNodeWithText("إعدادات التطبيق في النظام").performScrollTo().performClick()

        waitUntil { system.opened == listOf("notices", "app") }
        // The choice went to the preferences and came back from them as the selected one.
        waitUntil { onAllNodes(hasText("داكن") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        // Signed out the switches are this device's, and nothing is sent.
        assertEquals(emptyList<String>(), api.calls.toList())
    }

    @Test
    fun `with nothing kept and no network the built-in lines are named and dialled`() = runComposeUiTest {
        val repository = EmergencyNumbersRepository(
            ScriptedPublicApi(),
            FakeEmergencyNumbersCache(),
            FakePreferences("raqqa"),
            ResourceEmergencyLabels,
        )
        var called: String? = null
        val owner = Resumed()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                DirectoryTheme(darkTheme = false) {
                    EmergencyNumbersScreen(EmergencyNumbersViewModel(repository), onBack = {}, onCall = { called = it })
                }
            }
        }

        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(LABELS.ambulance).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("تحقق من الأرقام مع الجهات الرسمية").assertExists()
        onNodeWithText(LABELS.police).assertExists()
        onNodeWithText(LABELS.ambulance).performClick()
        waitUntil { called == "110" }
    }
}
