package com.servacode.directory.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Every screen's frame: the app's background, a bar at the top when a screen has one, and the
 * insets handled in one place so no screen decides for itself how to sit under the system bars.
 */
@Composable
fun DirectoryPage(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingAction: @Composable () -> Unit = {},
    background: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingAction,
        containerColor = background,
        content = content,
    )
}

/**
 * The bar the app wears: its own name or the screen's, a line under it for where the user is,
 * a way back where there is one, and at most one action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSubtitle: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    backLabel: String = "رجوع",
    @DrawableRes leadingIcon: Int? = null,
    leadingLabel: String? = null,
    onLeading: (() -> Unit)? = null,
    @DrawableRes actionIcon: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = if (onSubtitle == null) {
                            Modifier
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(Radius.pill))
                                .clickable(onClick = onSubtitle)
                                .padding(horizontal = Space.sm, vertical = Space.xs)
                        },
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                DirectoryIconButton(
                    icon = DirectoryIcons.back,
                    label = backLabel,
                    onClick = onBack,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            } else if (leadingIcon != null && onLeading != null) {
                BrandCircleIconButton(leadingIcon, leadingLabel.orEmpty(), onLeading)
            }
        },
        actions = {
            if (actionIcon != null && onAction != null) {
                BrandCircleIconButton(actionIcon, actionLabel.orEmpty(), onAction)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
    )
}

/** An action in a soft brand circle, as the top of Home carries it. */
@Composable
fun BrandCircleIconButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Sizes.touchTarget)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(Radius.pill)),
        contentAlignment = Alignment.Center,
    ) {
        DirectoryIconButton(icon, label, onClick, tint = MaterialTheme.colorScheme.primary)
    }
}

/** What the app's main places are called and what takes the user to each. */
data class DirectoryDestination(
    val label: String,
    @DrawableRes val icon: Int,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

/** The bar along the bottom, for the app's few main places and nothing else. */
@Composable
fun DirectoryBottomBar(destinations: List<DirectoryDestination>, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier,
        containerColor = BrandColors.bar,
        tonalElevation = Elevation.none,
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = destination.selected,
                onClick = destination.onSelect,
                icon = { DirectoryIcon(destination.icon, null) },
                label = { Text(destination.label, style = MaterialTheme.typography.labelMedium) },
                alwaysShowLabel = true,
                // The indicator is a pale pill and the icon inside it goes as dark as the bar,
                // which is the strongest mark this bar can carry. The label sits below the
                // pill, on the bar itself, so it stays white. Everything unchosen is the muted
                // tone: quieter than white, and still well clear of the contrast floor.
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandColors.bar,
                    selectedTextColor = BrandColors.onBar,
                    indicatorColor = BrandColors.soft,
                    unselectedIconColor = BrandColors.onBarMuted,
                    unselectedTextColor = BrandColors.onBarMuted,
                ),
            )
        }
    }
}

/** The line that opens a section, with at most one thing to press beside it. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        if (action != null && onAction != null) {
            DirectoryTextButton(action, onAction)
        }
    }
}

/** The app's card: white, soft, rounded, and never nested inside another one. */
@Composable
fun DirectoryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = Elevation.low)
    val shape = RoundedCornerShape(Radius.large)
    if (onClick == null) {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation) {
            Column(Modifier.padding(Space.base)) { content() }
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
        ) {
            Column(Modifier.padding(Space.base)) { content() }
        }
    }
}

/**
 * Where the user is in a form that takes more than one page: the phases as circles, the one
 * they are on filled, the ones behind it marked done.
 */
@Composable
fun StepIndicator(
    labels: List<String>,
    current: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        labels.forEachIndexed { index, label ->
            val done = index < current
            val here = index == current
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(Sizes.touchTarget)
                        .background(
                            color = when {
                                here -> MaterialTheme.colorScheme.primary
                                done -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(Radius.pill),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        DirectoryIcon(
                            icon = DirectoryIcons.check,
                            contentDescription = null,
                            size = IconSize.small,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (here) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (here) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** What a screen's one action sits on when it stays in view: a surface, the app's gutter, the button. */
@Composable
fun DirectoryActionBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Elevation.none,
        shadowElevation = Elevation.low,
    ) {
        Column(Modifier.padding(horizontal = Space.screen, vertical = Space.md)) { content() }
    }
}
