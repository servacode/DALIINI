package com.servacode.directory.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource

/**
 * The words that belong to no single screen.
 *
 * A weekday is not the facility page's word, nor the hours editor's: it is the app's. Held here,
 * in the module every screen already depends on, it is translated once — and two screens can
 * never disagree about which day is Monday. The same goes for the mark between items of a list,
 * which is not a comma in every language.
 *
 * Read in composition, so the words follow the phone's language without anything being restarted.
 */
object DirectoryWords {
    /** Monday first, matching the backend's weekday numbering. */
    @Composable
    @ReadOnlyComposable
    fun weekdays(): List<String> = stringArrayResource(R.array.directory_weekdays).toList()

    /** One day by its backend number, or a dash where the number means nothing. */
    @Composable
    @ReadOnlyComposable
    fun weekday(weekday: Int): String =
        weekdays().getOrNull(weekday) ?: stringResource(R.string.directory_weekday_unknown)

    val LIST_SEPARATOR: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.directory_list_separator)
}
