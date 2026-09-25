package com.servacode.directory.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A menu: a few things one can go to, grouped by what they are about.
 *
 * A long list of rows on the page's own background is a list of everything at once — the reader
 * has to read every line to find where one subject ends and the next begins. These draw the
 * subjects instead: a label names the group, a card holds its rows, and the space between the
 * cards is what separates them. Inside a card the rows are divided by a line that starts where
 * the words start, so the eye follows one column down the card rather than a ladder of full
 * width rules.
 */
@Composable
fun DirectorySectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Space.md, end = Space.md, bottom = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        // One thing, for a section whose whole content can be acted on at once. There used to
        // be a second component for exactly this, twice the size and in another file.
        if (action != null && onAction != null) DirectoryTextButton(action, onAction)
    }
}

/** The card a group of rows lives in. Rows go straight inside it, with no padding of their own. */
@Composable
fun DirectoryMenuGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(content = content)
    }
}

/** The line between two rows of one group: it starts where their words do. */
@Composable
fun DirectoryMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = MENU_TEXT_START, end = Space.base),
        color = MaterialTheme.colorScheme.outline,
    )
}

/**
 * One thing in a menu: what it is, what it says about itself, and that it leads somewhere.
 *
 * The icon sits in a tinted square of its own, so a row reads as icon, then words, then the
 * way onwards, rather than as four things competing on one line. Anything the row knows — the
 * province on the details row, the invitation under "join" — goes on a second line under the
 * title, where it is an answer rather than another heading.
 */
@Composable
fun DirectoryMenuRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    danger: Boolean = false,
    trailing: Boolean = true,
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget + Space.md)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.base, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(MENU_ICON_BOX)
                    .background(
                        // The brand's own soft green, and for a danger row the danger colour
                        // thinned rather than Material's pink, which belongs to no palette here.
                        color = if (danger) {
                            tint.copy(alpha = DANGER_TINT)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(Radius.medium),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                DirectoryIcon(icon, null, tint = tint)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing) {
            DirectoryIcon(
                icon = DirectoryIcons.chevron,
                contentDescription = null,
                modifier = Modifier.clearAndSetSemantics { },
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/** The square the icon sits in, and where the words after it start. */
private const val DANGER_TINT = 0.10f
private val MENU_ICON_BOX = 40.dp
private val MENU_TEXT_START = Space.base + MENU_ICON_BOX + Space.md
