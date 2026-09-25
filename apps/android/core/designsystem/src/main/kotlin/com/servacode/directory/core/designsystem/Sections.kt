package com.servacode.directory.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The shapes every page of this app is built from.
 *
 * They were being rebuilt in each screen instead: four private `Section` helpers with the same
 * body, six features reaching for `Surface` to draw the same soft green field or the same
 * outlined card, two different components for the line above a group. A page that is drawn
 * again in every file drifts in every file, and then it is corrected one element at a time.
 *
 * Anything here is the app's answer to "what does a group of things look like". A screen that
 * needs a shape this file does not have adds it here, so the next screen inherits it.
 */

/**
 * A subject: the line that names it, and the card that holds it.
 *
 * The line is small and quiet on purpose — it labels, it does not announce. What it may carry
 * beside it is one thing to press, for a section whose whole content can be acted on at once.
 */
@Composable
fun DirectorySection(
    label: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        DirectorySectionLabel(label, action = action, onAction = onAction)
        DirectoryCard {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm), content = content)
        }
    }
}

/**
 * The same subject, when what it holds is a menu rather than content.
 *
 * The rows go straight inside, divided by [DirectoryMenuDivider]; the card has no padding of
 * its own so a row's touch target reaches its edges.
 */
@Composable
fun DirectoryMenuSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        DirectorySectionLabel(label)
        DirectoryMenuGroup(content = content)
    }
}

/**
 * The soft green field a page opens with: the brand's own colour, with a rounded foot and room
 * for the status bar above it. Home wears it and so do the account pages, which is the point.
 */
@Composable
fun DirectoryBrandHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BrandColors.softer,
        shape = RoundedCornerShape(bottomStart = Radius.xl, bottomEnd = Radius.xl),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Space.base)
                .padding(top = Space.sm, bottom = Space.base),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
            content = content,
        )
    }
}

/** A block on the brand's soft green: an identity, a notice, anything that is not a card. */
@Composable
fun DirectoryBrandPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.xl),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = Space.lg, horizontal = Space.base),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.sm),
            content = content,
        )
    }
}

/**
 * A card that can be picked out from its neighbours.
 *
 * [highlighted] is the difference between a message that has been read and one that has not, or
 * between a row that wants attention and the rest; it is never the only difference, because a
 * colour on its own is not a signal everyone receives.
 */
@Composable
fun DirectoryOutlinedCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = HAIRLINE,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
        content = { Column(content = content) },
    )
}

/**
 * A round control that has to read against whatever is under it — a map, a photograph.
 *
 * It carries its own white ground and a ring of the brand's soft green: a white circle on a
 * pale map loses its edge, and a grey outline belongs to no palette this app has.
 */
@Composable
fun DirectoryRoundControl(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(Sizes.button),
        shape = RoundedCornerShape(Radius.pill),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(HAIRLINE, MaterialTheme.colorScheme.primaryContainer),
        shadowElevation = Elevation.low,
        enabled = enabled,
    ) {
        Box(contentAlignment = Alignment.Center) {
            DirectoryIcon(
                icon = icon,
                contentDescription = label,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        }
    }
}

/** Thin enough to be an edge rather than a frame. */
private val HAIRLINE = 1.dp

/**
 * A pill: a small thing that carries one line — a place, a province, a notice.
 *
 * [brand] paints it in the brand's soft green, for a line that is being pointed at rather than
 * merely stated; [onClick] makes it something that opens, which is exactly what two lines of
 * grey text never looked like.
 */
@Composable
fun DirectoryPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    brand: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(Radius.pill)
    val colour = if (brand) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = colour, content = content)
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = colour, content = content)
    }
}

/**
 * A chip that sits on top of a map or a photograph.
 *
 * It carries its own ground and a shadow rather than a tint, because whatever is under it is a
 * picture: a tinted chip over a satellite view is a chip nobody can read.
 */
@Composable
fun DirectoryOverlayChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Radius.pill),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = Elevation.low,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = Space.base, vertical = Space.sm),
        )
    }
}

/** The same idea with room for more than a word: the map's rail of sections. */
@Composable
fun DirectoryOverlayTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Radius.large),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = Elevation.low,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xs),
            content = content,
        )
    }
}
