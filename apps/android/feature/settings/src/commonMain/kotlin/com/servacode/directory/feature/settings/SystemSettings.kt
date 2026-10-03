package com.servacode.directory.feature.settings

import androidx.compose.runtime.Composable

/**
 * The phone's own switches for this app, which Settings reads and opens but never keeps a copy
 * of: the user can change them behind the app's back, in the system's settings.
 */
interface SystemSettings {
    /** Whether the system lets this app show a notice, as it answers now. */
    suspend fun noticesAllowed(): Boolean

    /** The system's page for this app's notices. */
    fun openNotices()

    /** The system's page for this app: its permissions, its storage. */
    fun openApp()
}

/** Android's answer from its notification manager; the iPhone's from its notification centre. */
@Composable
expect fun rememberSystemSettings(): SystemSettings
