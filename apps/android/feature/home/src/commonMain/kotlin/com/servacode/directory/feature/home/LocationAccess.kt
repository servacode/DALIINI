package com.servacode.directory.feature.home

import androidx.compose.runtime.Composable

/** Whether the app may read the position while in use, and the platform's question if not. */
internal interface LocationAccess {
    /** Read again each time: the reader can change it in the phone's settings at any moment. */
    val allowed: Boolean

    /** Asks, in the platform's own dialog; the answer arrives at `rememberLocationAccess`. */
    fun ask()
}

/**
 * The location permission as the home offers it. [onAnswer] hears whether the reader allowed it,
 * once per question (DECISION-095).
 */
@Composable
internal expect fun rememberLocationAccess(onAnswer: (allowed: Boolean) -> Unit): LocationAccess
