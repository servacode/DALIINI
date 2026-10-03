package com.servacode.directory.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.servacode.directory.core.model.AppError
import com.servacode.directory.designsystem.generated.IllustrationPaths
import org.jetbrains.compose.resources.stringResource

/** The app is fetching. One spinner, one place, one wording. */
@Composable
fun DirectoryLoading(modifier: Modifier = Modifier, message: String? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(Space.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The app's own mark with the wait turning around it, in one place, so every screen
        // waits the same way. A bare spinner is a different pause on every page and tells the
        // reader nothing about whose page they are on.
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(LOADER_RING),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = LOADER_STROKE,
            )
            BrandSymbol(size = LOADER_MARK)
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Space.base),
            )
        }
    }
}

/**
 * Nothing to show, something went wrong, or something is needed first: one shape for all three,
 * so the app never explains itself in three different ways.
 */
@Composable
fun DirectoryMessageState(
    icon: DirectoryGlyph,
    title: String,
    modifier: Modifier = Modifier,
    /** A shared illustration shown in place of [icon]; see [DirectoryIllustrations]. */
    illustration: IllustrationPaths? = null,
    body: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    primaryAction: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Space.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (illustration != null) {
            DirectoryIllustration(illustration)
        } else {
            Box(
                modifier = Modifier
                    .size(Sizes.avatar + Space.xxl)
                    .background(tint.copy(alpha = 0.10f), RoundedCornerShape(Radius.pill)),
                contentAlignment = Alignment.Center,
            ) {
                DirectoryIcon(icon, null, size = IconSize.large, tint = tint)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.lg).semantics { heading() },
        )
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Space.sm),
            )
        }
        if (primaryAction != null && onPrimaryAction != null) {
            DirectoryPrimaryButton(
                text = primaryAction,
                onClick = onPrimaryAction,
                modifier = Modifier.padding(top = Space.lg),
            )
        }
        if (secondaryAction != null && onSecondaryAction != null) {
            DirectoryTextButton(secondaryAction, onSecondaryAction, Modifier.padding(top = Space.xs))
        }
    }
}

/**
 * A list with nothing in it yet. [illustration] is the shared empty picture; a search that found
 * nothing passes [DirectoryIllustrations.noResults], a list that needs a place first
 * [DirectoryIllustrations.location].
 */
@Composable
fun DirectoryEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    illustration: IllustrationPaths = DirectoryIllustrations.empty,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    DirectoryMessageState(
        icon = DirectoryIcons.search,
        illustration = illustration,
        title = title,
        modifier = modifier,
        body = body,
        tint = MaterialTheme.colorScheme.primary,
        primaryAction = action,
        onPrimaryAction = onAction,
    )
}

/**
 * Something failed, and the user can try it again. [offline] shows the no-connection picture
 * rather than the error one: the fix is the reader's connection, not a retry of the same thing.
 */
@Composable
fun DirectoryErrorState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    retry: String? = stringResource(Res.string.ds_retry),
    onRetry: (() -> Unit)? = null,
    offline: Boolean = false,
) {
    DirectoryMessageState(
        icon = DirectoryIcons.warning,
        illustration = if (offline) DirectoryIllustrations.offline else DirectoryIllustrations.error,
        title = title,
        modifier = modifier,
        body = body,
        tint = MaterialTheme.colorScheme.error,
        primaryAction = if (onRetry != null) retry else null,
        onPrimaryAction = onRetry,
    )
}

/** A failure read from the backend's own error: its words, and the offline picture when it is one. */
@Composable
fun DirectoryErrorState(
    title: String,
    error: AppError,
    modifier: Modifier = Modifier,
    retry: String? = stringResource(Res.string.ds_retry),
    onRetry: (() -> Unit)? = null,
) {
    DirectoryErrorState(
        title = title,
        modifier = modifier,
        body = appErrorText(error),
        retry = retry,
        onRetry = onRetry,
        offline = error.kind == AppError.Kind.OFFLINE,
    )
}

/** The device has no connection, and what is on screen came from what was kept. */
@Composable
fun DirectoryOfflineNotice(
    modifier: Modifier = Modifier,
    text: String = stringResource(Res.string.ds_offline),
    onRetry: (() -> Unit)? = null,
    retryLabel: String = stringResource(Res.string.ds_refresh),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LocalDirectoryTones.current.warning.container, RoundedCornerShape(Radius.medium))
            .padding(horizontal = Space.base, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        DirectoryIcon(DirectoryIcons.offline, null, tint = LocalDirectoryTones.current.warning.content)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            DirectoryTextButton(retryLabel, onRetry)
        }
    }
}

/** A permission the user has not given, explained rather than demanded. */
@Composable
fun DirectoryPermissionState(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    DirectoryMessageState(
        icon = DirectoryIcons.pin,
        illustration = DirectoryIllustrations.location,
        title = title,
        modifier = modifier,
        body = body,
        primaryAction = action,
        onPrimaryAction = onAction,
        secondaryAction = secondaryAction,
        onSecondaryAction = onSecondaryAction,
    )
}

/** More of a list is on its way, or can be asked for. */
@Composable
fun LoadMoreRow(
    loading: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    label: String = stringResource(Res.string.ds_load_more),
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space.base),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize.large),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            DirectorySecondaryButton(
                text = if (error != null) stringResource(Res.string.ds_retry) else label,
                onClick = onLoadMore,
            )
        }
    }
}


/** The same wait, inside a card or a row rather than filling a screen of its own. */
@Composable
fun DirectoryInlineLoading(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(IconSize.large),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The mark, the ring around it, and how heavy that ring is drawn. */
private val LOADER_MARK = 56.dp
private val LOADER_RING = 88.dp
private val LOADER_STROKE = 3.dp
