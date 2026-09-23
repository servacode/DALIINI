package com.servacode.directory.feature.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.FacilityCard
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.LoadMoreRow
import com.servacode.directory.core.designsystem.Radius
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.model.InboxMessage
import com.servacode.directory.core.model.MessageDestination

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
                body = value.message,
                onRetry = viewModel::refresh,
            )
            is FavoritesUiState.Content -> if (value.items.isEmpty()) {
                DirectoryEmptyState(
                    title = SavedCopy.FAVORITES_EMPTY,
                    modifier = Modifier.padding(padding),
                    body = SavedCopy.FAVORITES_EMPTY_BODY,
                    icon = DirectoryIcons.star,
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
                                error = value.moreError,
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
                body = value.message,
                onRetry = viewModel::refresh,
            )
            is InboxUiState.Content -> if (value.items.isEmpty()) {
                DirectoryEmptyState(
                    title = SavedCopy.NOTIFICATIONS_EMPTY,
                    modifier = Modifier.padding(padding),
                    body = SavedCopy.NOTIFICATIONS_EMPTY_BODY,
                    icon = DirectoryIcons.bell,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = Space.xxl),
                ) {
                    items(value.items, key = { it.id }) { message ->
                        MessageRow(
                            message = message,
                            onOpen = {
                                viewModel.read(message.id)
                                when (message.destination) {
                                    MessageDestination.FACILITY ->
                                        message.facilityId?.let(onFacility)
                                    MessageDestination.OWNER_FACILITIES -> onOwnerFacilities()
                                    MessageDestination.NONE -> Unit
                                }
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Space.screen),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    if (value.hasMore || value.moreError != null) {
                        item(key = "more") {
                            LoadMoreRow(
                                loading = value.loadingMore,
                                onLoadMore = viewModel::loadMore,
                                error = value.moreError,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One message: unread ones carry a mark, and only a message with somewhere to go can be opened. */
@Composable
private fun MessageRow(message: InboxMessage, onOpen: () -> Unit) {
    val goes = message.destination != MessageDestination.NONE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (goes) it.clickable(onClick = onOpen) else it }
            .padding(horizontal = Space.screen, vertical = Space.base),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Box(
            modifier = Modifier.size(Sizes.touchTarget),
            contentAlignment = Alignment.Center,
        ) {
            DirectoryIcon(
                icon = DirectoryIcons.bell,
                contentDescription = null,
                tint = if (message.isRead) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.primary
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
        if (!message.isRead) {
            // A dot as well as the colour, so "unread" is never carried by colour alone.
            Box(
                modifier = Modifier
                    .padding(top = Space.md)
                    .size(Space.sm)
                    .clip(RoundedCornerShape(Radius.pill))
                    .let { it },
                contentAlignment = Alignment.Center,
            ) {
                DirectoryIcon(
                    icon = DirectoryIcons.check,
                    contentDescription = SavedCopy.UNREAD,
                    size = IconSize.small,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

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
    const val FAVORITES = "المفضلة"
    const val FAVORITES_ERROR = "تعذر تحميل المفضلة"
    const val FAVORITES_EMPTY = "لا توجد منشآت محفوظة"
    const val FAVORITES_EMPTY_BODY = "احفظ منشأة من صفحتها لتجدها هنا على أي جهاز."
    const val UNSAVE = "إزالة من المفضلة"
    const val NOTIFICATIONS = "الإشعارات"
    const val NOTIFICATIONS_ERROR = "تعذر تحميل الإشعارات"
    const val NOTIFICATIONS_EMPTY = "لا توجد إشعارات"
    const val NOTIFICATIONS_EMPTY_BODY = "سيصلك هنا كل ما ترسله المنصة إلى حسابك."
    const val READ_ALL = "تعليم الكل كمقروء"
    const val UNREAD = "غير مقروء"
}
