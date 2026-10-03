package com.servacode.directory.core.designsystem

import androidx.compose.runtime.Composable

/**
 * The iPhone's part of the theme: nothing yet. Its icons and illustrations follow the theme from
 * common code; the status bar's style is the app's window's (apps/ios).
 */
@Composable
internal actual fun PlatformTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    content()
}
