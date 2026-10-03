package com.servacode.directory.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource

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
    /** A short confirmation at the foot of the page, when a screen has one to give. */
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingAction,
        snackbarHost = snackbarHost,
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
    backLabel: String = stringResource(Res.string.ds_back),
    leadingIcon: DirectoryGlyph? = null,
    leadingLabel: String? = null,
    onLeading: (() -> Unit)? = null,
    actionIcon: DirectoryGlyph? = null,
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
    icon: DirectoryGlyph,
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
    val icon: DirectoryGlyph,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

/** The bar along the bottom, for the app's few main places and nothing else. */
@Composable
fun DirectoryBottomBar(destinations: List<DirectoryDestination>, modifier: Modifier = Modifier) {
    // A white bar on a near-white page needs a lift, not a line: the shadow under the rounded
    // top edge is what separates it, and it separates without drawing anything.
    Surface(
        modifier = modifier.clip(RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Elevation.medium,
    ) {
        NavigationBar(
            // Shorter than Material's eighty points, and rounded where it meets the page, so the
            // page above appears to run over it rather than stopping against a slab.
            //
            // It used to be the deep bar green, dark enough to read as black and a colour that
            // appears nowhere else in this app. Two near-black bands around a light page framed
            // it like a photograph and matched nothing inside it. The bar is the app's own
            // surface now, and the one green on it is the brand's, on the place being looked at.
            modifier = Modifier.height(Sizes.bottomBar),
            containerColor = Color.Transparent,
            tonalElevation = Elevation.none,
            // The height above is the bar itself; the gesture or button area below it is the
            // system's, and the scaffold already leaves room for it.
            windowInsets = WindowInsets(0),
        ) {
            destinations.forEach { destination ->
                NavigationBarItem(
                    selected = destination.selected,
                    onClick = destination.onSelect,
                    // The mark alone, and larger for it. Three places do not need naming twice —
                    // a house, a map and a person are read faster than they are read aloud — and
                    // the words were taking the room the marks needed to be legible. The name is
                    // still there for a screen reader, which is who the words were for.
                    icon = {
                        DirectoryIcon(
                            icon = destination.icon,
                            contentDescription = destination.label,
                            size = IconSize.large,
                        )
                    },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = BrandColors.soft,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
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
