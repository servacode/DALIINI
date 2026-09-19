package com.servacode.directory.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/** The size of the mark itself on the splash: the middle 120 units of brand_mark.xml. */
val BrandMarkSize = 120.dp

/** The size brand_mark.xml's whole 256-unit canvas is drawn at, so the mark is BrandMarkSize. */
val BrandMarkCanvas = 256.dp

/**
 * The brand mark, currently a placeholder (docs/design/BRAND-ASSETS.md). It takes
 * BrandMarkSize in the layout and draws its canvas around it, as the system splash does.
 * Decorative: the app's name is always written next to it, so TalkBack does not stop on it.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.brand_mark),
        contentDescription = null,
        modifier = modifier.layout { measurable, _ ->
            val canvas = BrandMarkCanvas.roundToPx()
            val mark = BrandMarkSize.roundToPx()
            val placeable = measurable.measure(Constraints.fixed(canvas, canvas))
            layout(mark, mark) { placeable.place((mark - canvas) / 2, (mark - canvas) / 2) }
        },
    )
}
