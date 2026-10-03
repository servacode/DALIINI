package com.servacode.directory.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import com.servacode.directory.core.model.AppError
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The shared design system drawn on the iPhone simulator: the theme, a word from the resources,
 * an error's sentence, an icon and an illustration, as the iPhone app draws them.
 */
@OptIn(ExperimentalTestApi::class)
class DesignSystemOnIosTest {
    @Test
    fun `the theme draws its words and marks from the shared resources`() = runComposeUiTest {
        var direction: LayoutDirection? = null
        setContent {
            DirectoryTheme(darkTheme = false) {
                direction = LocalLayoutDirection.current
                Text(DirectoryWords.TAGLINE, style = MaterialTheme.typography.titleLarge)
                Text(appErrorText(AppError(AppError.Kind.OFFLINE)))
                DirectoryIcon(DirectoryIcons.phone, contentDescription = "phone")
                DirectoryIllustration(DirectoryIllustrations.offline)
            }
        }

        onNodeWithText("أقرب الخدمات الصحية إليك").assertExists()
        onNodeWithText("لا يوجد اتصال بالإنترنت.").assertExists()
        onNodeWithContentDescription("phone").assertExists()
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun `the dark theme draws too`() = runComposeUiTest {
        setContent {
            DirectoryTheme(darkTheme = true) {
                DirectoryErrorState(title = "x", error = AppError(AppError.Kind.SERVER), onRetry = {})
            }
        }

        onNodeWithText("حدث خطأ في الخادم. حاول لاحقًا.").assertExists()
    }
}
