package com.servacode.directory.feature.account

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.InboxPage
import com.servacode.directory.core.model.Page
import com.servacode.directory.core.network.PublicApiBoundary

/**
 * What belongs to the account itself: the facilities it saved and the messages it was sent.
 *
 * Neither is cached on the device. Both are server-side state that follows the account to any
 * phone it signs in on, and a stale local copy of "what I saved" would be worse than a moment's
 * wait — it would be wrong on the second device.
 */
class SavedRepository @Inject constructor(
    private val api: PublicApiBoundary,
) {
    suspend fun favorites(cursor: String? = null): Result<Page<FacilitySummary>> =
        runCatching { api.favorites(cursor) }

    /** Returns the state the backend now holds, which is what the screen shows. */
    suspend fun save(facilityId: String): Result<Boolean> = runCatching { api.addFavorite(facilityId) }

    suspend fun unsave(facilityId: String): Result<Boolean> =
        runCatching { api.removeFavorite(facilityId) }

    suspend fun inbox(cursor: String? = null): Result<InboxPage> = runCatching { api.inbox(cursor) }

    suspend fun unreadCount(): Result<Int> = runCatching { api.unreadMessageCount() }

    suspend fun markRead(messageId: String): Result<Int> =
        runCatching { api.markMessageRead(messageId) }

    suspend fun markAllRead(): Result<Unit> = runCatching { api.markAllMessagesRead() }
}
