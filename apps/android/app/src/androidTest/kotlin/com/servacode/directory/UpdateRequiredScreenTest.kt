package com.servacode.directory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.core.designsystem.UpdateRequiredScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The screen a person sees when their build is refused — drawn on a real device.
 *
 * The first instrumentation test in this app, and this screen is why: it is the only thing a
 * blocked person can see, it is reached by a path nobody exercises by hand, and every rule in
 * it exists so that they are not stranded. A unit test can check the verdict; only a device can
 * check that the words arrive on a screen, that the button is really absent, and that the app
 * underneath really cannot be touched.
 */
@RunWith(AndroidJUnit4::class)
class UpdateRequiredScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun it_shows_what_the_backend_chose_to_say() {
        compose.setContent {
            DirectoryTheme(darkTheme = false) {
                UpdateRequiredScreen(message = "حدّث التطبيق للمتابعة.", onOpenStore = {})
            }
        }

        compose.onNodeWithText("حدّث التطبيق للمتابعة.").assertIsDisplayed()
    }

    @Test fun it_speaks_for_itself_when_the_backend_said_nothing() {
        compose.setContent {
            DirectoryTheme(darkTheme = false) {
                UpdateRequiredScreen(message = null, onOpenStore = {})
            }
        }

        // The app's own wording, rather than a screen that explains nothing.
        compose.onNodeWithText("هذه النسخة قديمة").assertIsDisplayed()
        compose.onNodeWithText("تحديث التطبيق").assertIsDisplayed()
    }

    @Test fun the_way_out_is_offered_only_when_there_is_one() {
        compose.setContent {
            DirectoryTheme(darkTheme = false) {
                UpdateRequiredScreen(message = null, onOpenStore = null)
            }
        }

        // No address configured, so no button at all: one that does nothing is worse than none.
        compose.onNodeWithText("تحديث التطبيق").assertDoesNotExist()
        compose.onNodeWithText("هذه النسخة قديمة").assertIsDisplayed()
    }

    @Test fun the_button_leads_somewhere() {
        var opened = 0
        compose.setContent {
            DirectoryTheme(darkTheme = false) {
                UpdateRequiredScreen(message = null, onOpenStore = { opened += 1 })
            }
        }

        compose.onNodeWithText("تحديث التطبيق").performClick()

        assertEquals(1, opened)
    }

    @Test fun nothing_underneath_can_be_reached() {
        var tapped = 0
        compose.setContent {
            DirectoryTheme(darkTheme = false) {
                Box {
                    Text(
                        text = "الشاشة تحت",
                        modifier = Modifier.fillMaxSize().clickable { tapped += 1 },
                    )
                    UpdateRequiredScreen(message = null, onOpenStore = null)
                }
            }
        }

        // The whole point of a gate: the app is still composed below, and still unreachable.
        compose.onNodeWithText("الشاشة تحت").performClick()

        assertEquals(0, tapped)
    }
}
