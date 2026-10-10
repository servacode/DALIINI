package com.servacode.directory.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.servacode.directory.designsystem.generated.DirectoryIllustrationPaths
import com.servacode.directory.designsystem.generated.DirectoryTokens
import com.servacode.directory.designsystem.generated.IllustrationPaths

/**
 * The picture the first-run screens carry: a quiet skyline of a town, the brand's pin standing in
 * it, on a soft brand-coloured ground.
 *
 * It is drawn, not photographed: no stock imagery, no invented city, nothing that pretends to be
 * a real place. Decorative throughout, so nothing here is announced to a screen reader.
 */
@Composable
fun TownIllustration(
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    mark: Dp = 108.dp,
) {
    val soft = BrandColors.softNow
    val softer = BrandColors.softerNow
    Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawTown(soft, softer) }
        BrandSymbol(size = mark)
    }
}

/** The same ground and buildings without the pin, for a screen that draws its own subject. */
@Composable
fun TownBackdrop(modifier: Modifier = Modifier, height: Dp = 200.dp) {
    val soft = BrandColors.softNow
    val softer = BrandColors.softerNow
    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxSize()) { drawTown(soft, softer) }
    }
}

private fun DrawScope.drawTown(soft: Color, softer: Color) {
    val ground = Offset(size.width / 2, size.height * 0.72f)
    // The soft ground the town stands on.
    drawOval(
        color = softer,
        topLeft = Offset(size.width * 0.06f, size.height * 0.28f),
        size = Size(size.width * 0.88f, size.height * 0.62f),
    )
    val base = ground.y
    val unit = size.width * 0.072f
    // Buildings: rounded blocks of two soft tones, tallest towards the middle.
    val blocks = listOf(
        Triple(-3.1f, 2.6f, soft),
        Triple(-2.1f, 3.9f, softer),
        Triple(-1.1f, 3.2f, soft),
        Triple(1.1f, 4.2f, soft),
        Triple(2.2f, 2.9f, softer),
        Triple(3.2f, 3.5f, soft),
    )
    blocks.forEach { (offset, tall, colour) ->
        val width = unit * 0.9f
        val height = unit * tall
        drawRoundRect(
            color = colour,
            topLeft = Offset(size.width / 2 + unit * offset - width / 2, base - height),
            size = Size(width, height),
            cornerRadius = CornerRadius(width * 0.22f, width * 0.22f),
        )
    }
    // Windows: a few lighter marks, enough to read as a town and no more.
    blocks.take(3).forEachIndexed { index, (offset, tall, _) ->
        val width = unit * 0.9f
        val height = unit * tall
        val left = size.width / 2 + unit * offset - width / 2
        var y = base - height + width * 0.35f
        while (y < base - width * 0.5f) {
            drawRoundRect(
                color = Color.White.copy(alpha = if (index % 2 == 0) 0.55f else 0.35f),
                topLeft = Offset(left + width * 0.22f, y),
                size = Size(width * 0.56f, width * 0.22f),
                cornerRadius = CornerRadius(width * 0.08f, width * 0.08f),
            )
            y += width * 0.5f
        }
    }
    // The ground line, as a single soft stroke.
    drawLine(
        color = soft,
        start = Offset(size.width * 0.12f, base),
        end = Offset(size.width * 0.88f, base),
        strokeWidth = 2.dp.toPx(),
    )
}

/**
 * The picture the location question carries: the pin with the rings of a position being found.
 * Decorative; the screen says in words what it is for.
 */
@Composable
fun LocationIllustration(
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    mark: Dp = 96.dp,
) {
    val softer = BrandColors.softerNow
    Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2, size.height / 2)
            drawCircle(softer, size.minDimension * 0.42f, centre)
            listOf(0.30f to 1f, 0.42f to 0.55f, 0.54f to 0.3f).forEach { (fraction, alpha) ->
                drawCircle(
                    color = BrandColors.mark,
                    radius = size.minDimension * fraction,
                    center = centre,
                    alpha = alpha * 0.35f,
                    style = Stroke(1.5.dp.toPx()),
                )
            }
        }
        BrandSymbol(size = mark)
    }
}

/**
 * The shared state illustrations (`packages/design-tokens/illustrations`): the same pictures the
 * site and the console show for the same states, from the same path data (DECISION-094).
 * Two-tone, drawn in the semantic token colours of the theme in use, light or dark.
 */
object DirectoryIllustrations {
    val empty = DirectoryIllustrationPaths.empty
    val noResults = DirectoryIllustrationPaths.noResults
    val offline = DirectoryIllustrationPaths.offline
    val error = DirectoryIllustrationPaths.error
    val maintenance = DirectoryIllustrationPaths.maintenance
    val success = DirectoryIllustrationPaths.success
    val location = DirectoryIllustrationPaths.location
}

/** One of [DirectoryIllustrations], at the size every state screen shows it. Decorative. */
@Composable
fun DirectoryIllustration(illustration: IllustrationPaths, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val dark = LocalDirectoryDark.current
    val vector = remember(illustration, dark) { illustrationVector(illustration, dark) }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

/** The three layers: the soft ground filled, then the muted lines, then the brand's accent. */
internal fun illustrationVector(illustration: IllustrationPaths, dark: Boolean): ImageVector {
    val soft = hexColor(
        if (dark) DirectoryTokens.SemanticDarkSurfaceBrandSoft else DirectoryTokens.SemanticSurfaceBrandSoft,
    )
    val line = hexColor(if (dark) DirectoryTokens.SemanticDarkContentMuted else DirectoryTokens.SemanticContentMuted)
    val accent = hexColor(
        if (dark) DirectoryTokens.SemanticDarkActionPrimary else DirectoryTokens.SemanticActionPrimary,
    )
    val builder = ImageVector.Builder(
        defaultWidth = 120.dp,
        defaultHeight = 120.dp,
        viewportWidth = 120f,
        viewportHeight = 120f,
    )
    illustration.soft.forEach { builder.addPath(addPathNodes(it), fill = SolidColor(soft)) }
    listOf(illustration.line to line, illustration.accent to accent).forEach { (paths, colour) ->
        paths.forEach {
            builder.addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(colour),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }
    return builder.build()
}
