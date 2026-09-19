package com.servacode.directory.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * The size the brand mark is drawn at on the splash: the same size the system splash draws it,
 * through the inset in `brand_mark_splash.xml`, so one hands over to the other without a jump.
 */
val BrandMarkSize = 120.dp

/**
 * The brand mark, currently a placeholder (docs/design/BRAND-ASSETS.md). Decorative: the app's
 * name is always written next to it, so TalkBack does not stop on the picture.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.brand_mark),
        contentDescription = null,
        modifier = modifier.size(BrandMarkSize),
    )
}
