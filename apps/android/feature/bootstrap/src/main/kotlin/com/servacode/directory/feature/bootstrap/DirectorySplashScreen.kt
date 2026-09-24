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
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import com.servacode.directory.core.designsystem.BrandSymbol
import com.servacode.directory.core.designsystem.DirectoryTextStyles
import com.servacode.directory.core.designsystem.SystemBarsColor
import com.servacode.directory.designsystem.generated.DirectoryTokens

/**
 * The app's splash, shown while the app starts and while it hands over to the first screen.
 *
 * Its first frame is the system splash's last: the same background under the same bars and the
 * brand mark at the same place and size, and nothing else, so the frame that ends the system
 * splash costs no more than before this redesign. The name, the tagline, the footer, the soft
 * shapes and a slow ping under the pin join on the next frame and fade in over the tokens'
 * emphasized duration. It waits for nothing: navigation leaves as soon as the start is known.
 */
@Composable
fun DirectorySplashScreen(modifier: Modifier = Modifier) {
    SystemBarsColor(BrandColors.splashBackground)
    var details by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        details = true
    }
    val appear by animateFloatAsState(
        targetValue = if (details) 1f else 0f,
        animationSpec = tween(DirectoryTokens.MotionDurationEmphasized, easing = LinearOutSlowInEasing),
        label = "splash-appear",
    )
    val view = LocalView.current
    // Set while placing; the ping reads it to draw under the mark.
    val markCentreY = remember { mutableFloatStateOf(Float.NaN) }

    Layout(
        modifier = modifier.fillMaxSize().background(BrandColors.splashBackground),
        content = {
            BrandSymbol()
            if (details) {
                // Drawn once and faded as a layer, not redrawn on every frame of the fade.
                Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = appear }) { drawBackdrop() }
                SplashPing(markCentreY, appear = { appear })
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
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val mark = measurables[0].measure(loose)
        val full = Constraints.fixed(width, height)
        val side = SIDE.roundToPx()
        // Present from the second frame: backdrop, ping, title, footer.
        val backdrop = measurables.getOrNull(1)?.measure(full)
        val ping = measurables.getOrNull(2)?.measure(full)
        val title = measurables.getOrNull(3)?.measure(loose.copy(maxWidth = (width - 2 * side).coerceAtLeast(0)))
        val footer = measurables.getOrNull(4)?.measure(loose)
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
            // Placed first, so drawn below the mark.
            backdrop?.place(0, 0)
            ping?.place(0, 0)
            mark.place((width - mark.width) / 2, centreY - mark.height / 2)
            title?.let { it.place((width - it.width) / 2, centreY + mark.height / 2 + TITLE_GAP.roundToPx()) }
            footer?.let { it.place((width - it.width) / 2, height - it.height - FOOTER_GAP.roundToPx()) }
        }
    }
}

/** The ping, on its own so that only it redraws on each of its frames. */
@Composable
private fun SplashPing(markCentreY: MutableFloatState, appear: () -> Float) {
    val progress by rememberInfiniteTransition(label = "splash-ping").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PING_MILLIS, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "splash-ping-progress",
    )
    Canvas(Modifier.fillMaxSize()) {
        val centreY = markCentreY.floatValue.takeUnless { it.isNaN() } ?: (size.height / 2)
        drawPing(Offset(size.width / 2, centreY + MARK_BASE_BELOW_CENTRE.toPx()), progress, appear())
    }
}

/** Two soft shapes and a faint map grid in the upper one; decoration only, no semantics. */
private fun DrawScope.drawBackdrop() {
    val upper = Rect(Offset(size.width * 0.92f, size.height * 0.1f), size.width * 0.62f)
    val lower = Rect(Offset(size.width * 0.06f, size.height * 0.94f), size.width * 0.55f)
    drawCircle(BrandColors.softer, upper.width / 2, upper.center)
    drawCircle(BrandColors.softer, lower.width / 2, lower.center)
    val step = GRID_STEP.toPx()
    val dot = GRID_DOT.toPx()
    clipPath(Path().apply { addOval(upper) }) {
        var y = upper.top + step / 2
        while (y < upper.bottom) {
            var x = upper.left + step / 2
            while (x < upper.right) {
                drawCircle(BrandColors.soft, dot, Offset(x, y))
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

// The letter's base sits 52 dp below the mark's centre at BrandMarkSize, measured from the
// symbol's own ink (scripts/build-brand-assets.py): the ping spreads from where it stands.
private val MARK_BASE_BELOW_CENTRE = 52.dp
private val TITLE_GAP = 20.dp
private val FOOTER_GAP = 28.dp
private val SIDE = 24.dp
private val RISE = 8.dp
private val GRID_STEP = 16.dp
private val GRID_DOT = 1.5.dp
