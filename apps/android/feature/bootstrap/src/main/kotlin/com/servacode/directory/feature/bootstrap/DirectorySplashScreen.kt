package com.servacode.directory.feature.bootstrap

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.servacode.directory.core.designsystem.BrandColors
import com.servacode.directory.core.designsystem.BrandMark
import com.servacode.directory.core.designsystem.DirectoryTextStyles
import com.servacode.directory.core.designsystem.SystemBarsColor
import com.servacode.directory.designsystem.generated.DirectoryTokens

/**
 * The app's splash, shown while the app starts and while it hands over to the first screen.
 *
 * Its first frame continues the system splash: the same background under the same bars, and the
 * brand mark at the same place and size. The rest appears over the tokens' emphasized duration,
 * with a slow ping under the pin. It waits for nothing: navigation leaves as soon as the start
 * is known.
 */
@Composable
fun DirectorySplashScreen(modifier: Modifier = Modifier) {
    SystemBarsColor(BrandColors.splashBackground)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val appear by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(DirectoryTokens.MotionDurationEmphasized, easing = LinearOutSlowInEasing),
        label = "splash-appear",
    )
    val ping by rememberInfiniteTransition(label = "splash-ping").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PING_MILLIS, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "splash-ping-progress",
    )
    val view = LocalView.current
    // Set while placing; the backdrop reads it to draw under the mark.
    val markCentreY = remember { mutableFloatStateOf(Float.NaN) }

    Layout(
        modifier = modifier.fillMaxSize().background(BrandColors.splashBackground),
        content = {
            Canvas(Modifier.fillMaxSize()) {
                val centreY = markCentreY.floatValue.takeUnless { it.isNaN() } ?: (size.height / 2)
                drawBackdrop(appear)
                drawPing(Offset(size.width / 2, centreY + PIN_TIP_BELOW_CENTRE.toPx()), ping, appear)
            }
            BrandMark()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = appear
                    translationY = (1f - appear) * RISE.toPx()
                },
            ) {
                Text(
                    text = SplashCopy.NAME,
                    style = DirectoryTextStyles.display,
                    color = BrandColors.mark,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = SplashCopy.TAGLINE,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandColors.contentSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = SplashCopy.FOOTER,
                style = DirectoryTextStyles.labelMedium,
                color = BrandColors.contentSecondary,
                modifier = Modifier.graphicsLayer { alpha = appear },
            )
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val backdrop = measurables[0].measure(Constraints.fixed(width, height))
        val mark = measurables[1].measure(loose)
        val side = SIDE.roundToPx()
        val title = measurables[2].measure(loose.copy(maxWidth = (width - 2 * side).coerceAtLeast(0)))
        val footer = measurables[3].measure(loose)
        layout(width, height) {
            // Placement runs after the window has laid this view out, so its offset is known.
            val location = IntArray(2).also(view::getLocationInWindow)
            val centreY = SplashGeometry.markCentreY(
                windowHeight = view.rootView.height,
                contentTop = location[1],
                contentHeight = height,
                markHeight = mark.height,
            )
            markCentreY.floatValue = centreY.toFloat()
            backdrop.place(0, 0)
            mark.place((width - mark.width) / 2, centreY - mark.height / 2)
            title.place((width - title.width) / 2, centreY + mark.height / 2 + TITLE_GAP.roundToPx())
            footer.place((width - footer.width) / 2, height - footer.height - FOOTER_GAP.roundToPx())
        }
    }
}

/** Two soft shapes and a faint map grid in the upper one; decoration only, no semantics. */
private fun DrawScope.drawBackdrop(appear: Float) {
    val upper = Rect(Offset(size.width * 0.92f, size.height * 0.1f), size.width * 0.62f)
    val lower = Rect(Offset(size.width * 0.06f, size.height * 0.94f), size.width * 0.55f)
    drawCircle(BrandColors.softer, upper.width / 2, upper.center, alpha = appear)
    drawCircle(BrandColors.softer, lower.width / 2, lower.center, alpha = appear)
    val step = GRID_STEP.toPx()
    val dot = GRID_DOT.toPx()
    clipPath(Path().apply { addOval(upper) }) {
        var y = upper.top + step / 2
        while (y < upper.bottom) {
            var x = upper.left + step / 2
            while (x < upper.right) {
                drawCircle(BrandColors.soft, dot, Offset(x, y), alpha = appear)
                x += step
            }
            y += step
        }
    }
}

/** A ground ring that spreads from under the pin's tip and fades, like a location ping. */
private fun DrawScope.drawPing(tip: Offset, progress: Float, appear: Float) {
    val width = (PING_FROM + (PING_TO - PING_FROM) * progress).toPx()
    drawOval(
        color = BrandColors.mark,
        topLeft = Offset(tip.x - width / 2, tip.y - width * PING_FLATTEN / 2),
        size = Size(width, width * PING_FLATTEN),
        alpha = appear * PING_ALPHA * (1f - progress),
    )
}

private const val PING_MILLIS = 1600
private const val PING_FLATTEN = 0.26f
private const val PING_ALPHA = 0.22f
private val PING_FROM = 28.dp
private val PING_TO = 76.dp

// The pin's tip sits 51 dp below the mark's centre: y 195 of brand_mark.xml's 288-unit canvas.
private val PIN_TIP_BELOW_CENTRE = 51.dp
private val TITLE_GAP = 20.dp
private val FOOTER_GAP = 28.dp
private val SIDE = 24.dp
private val RISE = 8.dp
private val GRID_STEP = 16.dp
private val GRID_DOT = 1.5.dp
