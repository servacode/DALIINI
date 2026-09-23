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
import androidx.annotation.DrawableRes
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

/** The app is fetching. One spinner, one place, one wording. */
@Composable
fun DirectoryLoading(modifier: Modifier = Modifier, message: String? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(Space.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
    @DrawableRes icon: Int,
    title: String,
    modifier: Modifier = Modifier,
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
        Box(
            modifier = Modifier
                .size(Sizes.avatar + Space.xxl)
                .background(tint.copy(alpha = 0.10f), RoundedCornerShape(Radius.pill)),
            contentAlignment = Alignment.Center,
        ) {
            DirectoryIcon(icon, null, size = IconSize.large, tint = tint)
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

/** A list with nothing in it yet. */
@Composable
fun DirectoryEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    @DrawableRes icon: Int = DirectoryIcons.search,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    DirectoryMessageState(
        icon = icon,
        title = title,
        modifier = modifier,
        body = body,
        tint = MaterialTheme.colorScheme.primary,
        primaryAction = action,
        onPrimaryAction = onAction,
    )
}

/** Something failed, and the user can try it again. */
@Composable
fun DirectoryErrorState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    retry: String? = "إعادة المحاولة",
    onRetry: (() -> Unit)? = null,
) {
    DirectoryMessageState(
        icon = DirectoryIcons.warning,
        title = title,
        modifier = modifier,
        body = body,
        tint = MaterialTheme.colorScheme.error,
        primaryAction = if (onRetry != null) retry else null,
        onPrimaryAction = onRetry,
    )
}

/** The device has no connection, and what is on screen came from what was kept. */
@Composable
fun DirectoryOfflineNotice(
    modifier: Modifier = Modifier,
    text: String = "غير متصل — تعرض بيانات محفوظة قد لا تكون محدثة",
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "تحديث",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BrandColors.warning.copy(alpha = 0.10f), RoundedCornerShape(Radius.medium))
            .padding(horizontal = Space.base, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        DirectoryIcon(DirectoryIcons.info, null, tint = BrandColors.warning)
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
    label: String = "عرض المزيد",
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
            DirectorySecondaryButton(if (error != null) "إعادة المحاولة" else label, onLoadMore)
        }
    }
}

