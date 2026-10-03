package com.servacode.directory.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * A picture fetched from [url]: Coil on both platforms, each on the release its Kotlin can read
 * (DECISION-094). Android keeps the Coil it had; the iPhone's is the newest built with Kotlin 2.3,
 * the compiler this project is on.
 */
@Composable
internal expect fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
)
