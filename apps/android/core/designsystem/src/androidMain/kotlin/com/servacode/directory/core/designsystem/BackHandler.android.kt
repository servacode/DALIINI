package com.servacode.directory.core.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun DirectoryBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
