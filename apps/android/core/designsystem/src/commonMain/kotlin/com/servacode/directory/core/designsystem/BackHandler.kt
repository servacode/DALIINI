package com.servacode.directory.core.designsystem

import androidx.compose.runtime.Composable

/**
 * What the system's back does while [enabled]: close what the screen opened over itself (a
 * picture made large, a page of photos) before leaving the screen. Android's back button and
 * gesture; the iPhone has no system back, so there the screen's own back control is the way out.
 */
@Composable
expect fun DirectoryBackHandler(enabled: Boolean = true, onBack: () -> Unit)
