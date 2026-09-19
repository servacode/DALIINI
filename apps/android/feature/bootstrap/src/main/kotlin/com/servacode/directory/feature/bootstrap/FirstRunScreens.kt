package com.servacode.directory.feature.bootstrap

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.servacode.directory.core.designsystem.BrandColors
import com.servacode.directory.core.designsystem.BrandMark
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectoryTextStyles
import com.servacode.directory.core.designsystem.SystemBarsColor
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS
import com.servacode.directory.designsystem.generated.DirectoryTokens

/**
 * Screen 02. One page, shown once: what the app is for, and a single way on. Nothing to swipe
 * through and nothing to read twice.
 */
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    FirstRunPage(
        title = WelcomeCopy.TITLE,
        body = WelcomeCopy.BODY,
        primary = WelcomeCopy.CONTINUE,
        onPrimary = onContinue,
        illustration = { NearbyIllustration() },
    )
}

/**
 * Screen 03. Why the app asks for the location, in the user's terms, with the system's dialog
 * behind a button they press themselves. Refusing is a plain second choice and costs the
 * distances alone; nothing here asks twice.
 */
@Composable
fun LocationPermissionScreen(
    onDone: () -> Unit,
    viewModel: LocationPermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val alreadyAllowed = remember {
        FOREGROUND_LOCATION_PERMISSIONS.any {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }
    // Granted already, on a reinstall for instance: the question would be noise.
    LaunchedEffect(alreadyAllowed) {
        if (alreadyAllowed) {
            viewModel.answered(LocationAnswer.ALLOWED)
            onDone()
        }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.answered(
            if (result.values.any { it }) LocationAnswer.ALLOWED else LocationAnswer.REFUSED,
        )
        onDone()
    }

    FirstRunPage(
        title = LocationCopy.TITLE,
        body = LocationCopy.NOTE,
        reasons = LocationCopy.REASONS.lines(),
        primary = LocationCopy.ALLOW,
        onPrimary = { ask.launch(FOREGROUND_LOCATION_PERMISSIONS.toTypedArray()) },
        secondary = LocationCopy.LATER,
        onSecondary = {
            viewModel.answered(LocationAnswer.LATER)
            onDone()
        },
        illustration = { NearbyIllustration(rings = true) },
    )
}

/** The shape both first-run pages share: the brand above, one message, and the way on below. */
@Composable
private fun FirstRunPage(
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    illustration: @Composable () -> Unit,
    reasons: List<String> = emptyList(),
    secondary: String? = null,
    onSecondary: () -> Unit = {},
) {
    SystemBarsColor(BrandColors.splashBackground)
    Box(Modifier.fillMaxSize().background(BrandColors.splashBackground)) {
        Canvas(Modifier.fillMaxSize()) { drawSoftShapes() }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = DirectoryTokens.SpacingXl.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(DirectoryTokens.Spacing5xl.dp))
            BrandMark(size = BRAND_ON_PAGE)
            Spacer(Modifier.height(DirectoryTokens.SpacingSm.dp))
            Text(
                text = SplashCopy.NAME,
                style = DirectoryTextStyles.display,
                color = BrandColors.mark,
            )
            Spacer(Modifier.height(DirectoryTokens.Spacing2xl.dp))
            illustration()
            Spacer(Modifier.height(DirectoryTokens.Spacing2xl.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(DirectoryTokens.SpacingMd.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = BrandColors.contentSecondary,
                textAlign = TextAlign.Center,
            )
            reasons.forEach { reason ->
                Spacer(Modifier.height(DirectoryTokens.SpacingMd.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(DirectoryTokens.SpacingSm.dp)
                            .clip(RoundedCornerShape(DirectoryTokens.RadiusPill.dp))
                            .background(BrandColors.mark),
                    )
                    Spacer(Modifier.width(DirectoryTokens.SpacingMd.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            DirectoryPrimaryButton(primary, onPrimary, Modifier.fillMaxWidth())
            if (secondary != null) {
                TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                    Text(secondary, style = MaterialTheme.typography.bodyLarge, color = BrandColors.contentSecondary)
                }
            } else {
                Spacer(Modifier.height(DirectoryTokens.SpacingLg.dp))
            }
            Spacer(Modifier.height(DirectoryTokens.Spacing2xl.dp))
        }
    }
}

/** A place and what is near it: a soft card of ground, the pin on it, two facilities beside it. */
@Composable
private fun NearbyIllustration(rings: Boolean = false) {
    Box(Modifier.fillMaxWidth().height(ILLUSTRATION_HEIGHT), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2, size.height / 2)
            val card = Rect(size.width * 0.10f, size.height * 0.14f, size.width * 0.90f, size.height * 0.86f)
            val corner = CornerRadius(DirectoryTokens.RadiusXl.dp.toPx(), DirectoryTokens.RadiusXl.dp.toPx())
            drawRoundRect(BrandColors.softer, card.topLeft, card.size, corner)
            val step = GRID_STEP.toPx()
            clipPath(Path().apply { addRect(card) }) {
                var y = card.top + step / 2
                while (y < card.bottom) {
                    var x = card.left + step / 2
                    while (x < card.right) {
                        drawCircle(BrandColors.soft, GRID_DOT.toPx(), Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
            if (rings) {
                listOf(0.30f to 1f, 0.42f to 0.6f, 0.54f to 0.35f).forEach { (fraction, alpha) ->
                    drawCircle(
                        color = BrandColors.mark,
                        radius = size.minDimension * fraction,
                        center = centre,
                        alpha = alpha * RING_ALPHA,
                        style = Stroke(RING_STROKE.toPx()),
                    )
                }
            } else {
                drawNeighbour(Offset(centre.x - size.width * 0.22f, centre.y + size.height * 0.16f))
                drawNeighbour(Offset(centre.x + size.width * 0.22f, centre.y - size.height * 0.14f))
            }
        }
        BrandMark(size = PIN_ON_ILLUSTRATION)
    }
}

/** A facility beside the pin: a rounded chip, no text, nothing to read. */
private fun DrawScope.drawNeighbour(centre: Offset) {
    val width = NEIGHBOUR_WIDTH.toPx()
    val height = NEIGHBOUR_HEIGHT.toPx()
    drawRoundRect(
        color = BrandColors.soft,
        topLeft = Offset(centre.x - width / 2, centre.y - height / 2),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2, height / 2),
    )
}

/** The same two soft shapes the splash uses, so the first run looks like one place. */
private fun DrawScope.drawSoftShapes() {
    drawCircle(BrandColors.softer, size.width * 0.62f, Offset(size.width * 0.92f, size.height * 0.08f))
    drawCircle(BrandColors.softer, size.width * 0.5f, Offset(size.width * 0.04f, size.height * 0.96f))
}

private val BRAND_ON_PAGE = 72.dp
private val ILLUSTRATION_HEIGHT = 220.dp
private val PIN_ON_ILLUSTRATION = 96.dp
private val NEIGHBOUR_WIDTH = 56.dp
private val NEIGHBOUR_HEIGHT = 18.dp
private val GRID_STEP = 16.dp
private val GRID_DOT = 1.5.dp
private val RING_STROKE = 1.5.dp
private const val RING_ALPHA = 0.35f
