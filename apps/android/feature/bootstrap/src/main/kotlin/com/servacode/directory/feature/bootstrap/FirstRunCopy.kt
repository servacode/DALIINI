package com.servacode.directory.feature.bootstrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

/**
 * What the first run says, read from the module's own resources.
 *
 * These live apart from `WelcomeFlow.kt` deliberately: that file decides where a start goes and
 * what an answer is remembered as, which is logic the platform-free harness compiles and tests,
 * while a word read from `strings.xml` is Android. Keeping them in separate files is what lets
 * the decisions stay under test. See `HomeCopy` for why each member is read in composition.
 */
object WelcomeCopy {
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.welcome_title)
    val BODY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.welcome_body)
    val CONTINUE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.welcome_continue)
}

/** The words of the permissions question, provisional until product copy is approved. */
object LocationCopy {
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.location_title)
    val NOTE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.location_note)
    val ALLOW: String @Composable @ReadOnlyComposable get() = stringResource(R.string.location_allow)
    val LATER: String @Composable @ReadOnlyComposable get() = stringResource(R.string.location_later)

    /**
     * Why the app is asking, as a list.
     *
     * A list of reasons is one resource rather than four named ones because the screen shows all
     * of them in order, and a language that needs a fifth reason — or three — should be able to
     * say so in its own file without a Kotlin change.
     */
    @Composable
    @ReadOnlyComposable
    fun reasons(): List<String> =
        LocalContext.current.resources.getStringArray(R.array.location_reasons).toList()
}
