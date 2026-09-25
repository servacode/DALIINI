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

/**
 * The size of the mark on the app's own splash, which is what it grows to.
 *
 * The system splash hands it over smaller, at the size baked into `splash_symbol.webp`, because
 * Android masks a splash icon and this letter's ink reaches the corners of its box.
 */
val BrandMarkSize = 168.dp

/**
 * The size the system splash hands the mark over at: what `scripts/build-brand-assets.py` bakes
 * into `splash_symbol.webp`, as a share of the 288 dp box Android draws a splash icon in.
 */
val BrandMarkHandoverSize = 90.dp

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
                text = DirectoryWords.TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
