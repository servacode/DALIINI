package com.servacode.directory.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
internal actual fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    AsyncImage(model = url, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
}
