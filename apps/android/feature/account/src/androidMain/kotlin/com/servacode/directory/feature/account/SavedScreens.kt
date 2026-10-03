package com.servacode.directory.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOutlinedCard
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.FacilityCard
import com.servacode.directory.core.designsystem.LoadMoreRow
import com.servacode.directory.core.designsystem.Radius
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.model.InboxMessage
import com.servacode.directory.core.model.MessageDestination
import com.servacode.directory.core.model.NotificationTarget

/**
 * Screen 16. The facilities this account saved, wherever it signed in from.
 *
 * The list is the account's, not the device's: it is served by the same public query every
 * other list uses, so a facility that closes or is suspended simply stops being in it.
 */
@Composable
fun FavoritesScreen(
    onFacility: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DirectoryPage(
        topBar = { DirectoryTopBar(title = SavedCopy.FAVORITES, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            FavoritesUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is FavoritesUiState.Error -> DirectoryErrorState(
                title = SavedCopy.FAVORITES_ERROR,
                modifier = Modifier.padding(padding),
                error = value.error,
                onRetry = viewModel::refresh,
            )
            is FavoritesUiState.Content -> if (value.items.isEmpty()) {
                DirectoryEmptyState(
                    title = SavedCopy.FAVORITES_EMPTY,
                    modifier = Modifier.padding(padding),
                    body = SavedCopy.FAVORITES_EMPTY_BODY,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = Space.xxl),
                ) {
                    items(value.items, key = { it.id }) { facility ->
                        Box {
                            FacilityCard(
                                facility = facility,
                                onClick = { onFacility(facility.id) },
                                modifier = Modifier.padding(
                                    horizontal = Space.screen,
                                    vertical = Space.xs,
                                ),
                            )
                            DirectoryIconButton(
                                icon = DirectoryIcons.close,
                                label = SavedCopy.UNSAVE,
                                onClick = { viewModel.unsave(facility.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(horizontal = Space.base),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    if (value.hasMore || value.moreError != null) {
                        item(key = "more") {
                            LoadMoreRow(
                                loading = value.loadingMore,
                                onLoadMore = viewModel::loadMore,
                                error = value.moreError?.let { appErrorText(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 17. Everything the platform has told this account.
 *
 * The inbox is the record; a push is only an announcement of it, so a user who never granted
 * the notification permission still finds every message here, in order.
 */
@Composable
fun NotificationsScreen(
    onFacility: (String) -> Unit,
    onOwnerFacilities: () -> Unit,
    /** An owner's duty roster, prefilled for a day when the notice named one. */
    onDuty: (facilityId: String, date: String?) -> Unit,
    /** A facility's management page, where its hours are confirmed. */
    onManageFacility: (String) -> Unit,
    /** The invitations to help run a facility, waiting for an answer. */
    onInvitations: () -> Unit,
    onBack: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = state as? InboxUiState.Content

    DirectoryPage(
        topBar = {
            DirectoryTopBar(
                title = SavedCopy.NOTIFICATIONS,
                onBack = onBack,
                actionIcon = if ((content?.unreadCount ?: 0) > 0) DirectoryIcons.check else null,
                actionLabel = SavedCopy.READ_ALL,
                onAction = if ((content?.unreadCount ?: 0) > 0) viewModel::readAll else null,
            )
        },
    ) { padding ->
        when (val value = state) {
            InboxUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is InboxUiState.Error -> DirectoryErrorState(
                title = SavedCopy.NOTIFICATIONS_ERROR,
                modifier = Modifier.padding(padding),
                error = value.error,
                onRetry = viewModel::refresh,
            )
            is InboxUiState.Content -> if (value.items.isEmpty()) {
                DirectoryEmptyState(
                    title = SavedCopy.NOTIFICATIONS_EMPTY,
                    modifier = Modifier.padding(padding),
                    body = SavedCopy.NOTIFICATIONS_EMPTY_BODY,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(
                        start = Space.screen,
                        end = Space.screen,
                        top = Space.base,
                        bottom = Space.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    items(value.items, key = { it.id }) { message ->
                        MessageRow(
                            message = message,
                            onOpen = {
                                viewModel.read(message.id)
                                // By type first — a gap nudge opens duty scheduling, an hours
                                // reminder the facility's page — then by the destination the
                                // backend set. What lacks the facility it would need opens the
                                // owner's list, and a broadcast opens nothing: it is read here.
                                val target = NotificationTarget.of(
                                    message.type,
                                    message.destination,
                                    message.facilityId,
                                )
                                when (target) {
                                    is NotificationTarget.Facility -> onFacility(target.id)
                                    is NotificationTarget.DutyScheduling ->
                                        target.facilityId?.let { onDuty(it, target.date) } ?: onOwnerFacilities()
                                    is NotificationTarget.HoursConfirmation ->
                                        target.facilityId?.let(onManageFacility) ?: onOwnerFacilities()
                                    NotificationTarget.OwnerFacilities -> onOwnerFacilities()
                                    NotificationTarget.Invitations -> onInvitations()
                                    NotificationTarget.None -> Unit
                                }
                            },
                        )
                    }
                    if (value.hasMore || value.moreError != null) {
                        item(key = "more") {
                            LoadMoreRow(
                                loading = value.loadingMore,
                                onLoadMore = viewModel::loadMore,
                                error = value.moreError?.let { appErrorText(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One message, and whether it has been read.
 *
 * Read and unread were told apart by the tint of a bell and a tick the size of a full stop —
 * and a tick is what "read" looks like everywhere else, so the mark said the opposite of what
 * it meant. Unread now stands on the brand's soft green with its title in bold and a filled dot
 * at the end; read is a plain card. Three signals, none of them colour alone.
 *
 * Only a message with somewhere to go can be opened.
 */
@Composable
private fun MessageRow(message: InboxMessage, onOpen: () -> Unit) {
    val goes = message.destination != MessageDestination.NONE
    val unread = !message.isRead
    DirectoryOutlinedCard(highlighted = unread) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (goes) it.clickable(onClick = onOpen) else it }
                .padding(Space.base),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.touchTarget)
                    .clip(RoundedCornerShape(Radius.medium))
                    .background(
                        if (unread) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                DirectoryIcon(
                    icon = DirectoryIcons.bell,
                    contentDescription = null,
                    tint = if (unread) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Text(
                    text = message.titleAr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (message.bodyAr.isNotBlank()) {
                    Text(
                        text = message.bodyAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (unread) {
                // Read before the modifier: a semantics block is not composition, and this word
                // comes from the module's resources.
                val label = SavedCopy.UNREAD
                Box(
                    modifier = Modifier
                        .padding(top = Space.sm)
                        .size(UNREAD_DOT)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(MaterialTheme.colorScheme.primary)
                        .semantics { contentDescription = label },
                )
            }
        }
    }
}

/** Big enough to see beside a title, small enough not to be a button. */
private val UNREAD_DOT = 10.dp

/** A row that shows how many messages are waiting, for the account screen. */
@Composable
fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .let { it },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** The words of the saved facilities and the inbox, provisional until product copy is approved. */
object SavedCopy {
    val FAVORITES: String @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_favorites)
    val FAVORITES_ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_favorites_error)
    val FAVORITES_EMPTY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_favorites_empty)
    val FAVORITES_EMPTY_BODY: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_favorites_empty_body)
    val UNSAVE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_unsave)
    val NOTIFICATIONS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_notifications)
    val NOTIFICATIONS_ERROR: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_notifications_error)
    val NOTIFICATIONS_EMPTY: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_notifications_empty)
    val NOTIFICATIONS_EMPTY_BODY: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_notifications_empty_body)
    val READ_ALL: String @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_read_all)
    val UNREAD: String @Composable @ReadOnlyComposable get() = stringResource(R.string.saved_unread)
}
