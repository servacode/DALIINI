package com.servacode.directory.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.servacode.directory.core.model.DirectoryBrand

/** The size of the mark itself on the splash: the middle 120 units of brand_mark.xml. */
val BrandMarkSize = 120.dp

/**
 * The size brand_mark.xml's whole 288-unit canvas is drawn at, so the mark is BrandMarkSize:
 * the box Android 12+ draws a splash icon without a background in, so both splashes match.
 */
val BrandMarkCanvas = 288.dp

/**
 * The symbol: the road and the pin inside the letter.
 *
 * Cut from the approved artwork by scripts/build-brand-assets.py, which is a raster with
 * gradients and bevels that no vector reproduces. The system splash draws the same file
 * through `brand_mark.xml`, at the same size; `SplashThemeTest` holds the two together.
 */
@Composable
fun BrandSymbol(modifier: Modifier = Modifier, size: Dp = BrandMarkSize) {
    Image(
        painter = painterResource(R.drawable.brand_symbol),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

/**
 * The symbol with the name under it, as the app introduces itself.
 *
 * The name is set in the app's own bold, which is Tajawal: the same typeface every other word in
 * the app is set in, so the title belongs to the page rather than to the picture above it.
 */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    symbolSize: Dp = BrandMarkSize,
    showTagline: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        BrandSymbol(size = symbolSize)
        Text(
            text = DirectoryBrand.NAME,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        if (showTagline) {
            Text(
                text = DirectoryBrand.TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
