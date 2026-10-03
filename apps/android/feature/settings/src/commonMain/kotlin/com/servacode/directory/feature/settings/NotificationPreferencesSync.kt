package com.servacode.directory.feature.settings

import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.datastore.NotificationPreferences
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.inject.Singleton
import com.servacode.directory.core.model.NotificationSwitches
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The notice switches of a signed-in account, kept by the backend (DECISION-077).
 *
 * The backend decides what it pushes, so the account's choice is the record and the device
 * holds a copy: the copy is what Settings draws, and what a push is checked against when it
 * arrives (a push sent before a change still lands after it). Signed out, there is no account
 * to keep anything, and the device's own copy is all there is.
 */
@Singleton
class NotificationPreferencesSync @Inject constructor(
    private val api: PublicApiBoundary,
    private val store: DirectoryPreferencesStore,
) {
    /** One change at a time, so a second tap compares against the first one's outcome. */
    private val lock = Mutex()

    /** The account's choices replace the device's: after signing in, and when Settings opens. */
    suspend fun pull(): Result<NotificationPreferences> = lock.withLock {
        runCatching {
            val account = api.notificationSwitches().toDevice()
            store.setNotificationPreferences(account)
            account
        }
    }

    /**
     * One change: kept on the device at once, so the switch moves under the finger, then sent.
     * A refusal puts the device back as it was and is returned for the screen to say so.
     */
    suspend fun change(
        edit: NotificationPreferences.() -> NotificationPreferences,
    ): Result<NotificationPreferences> = lock.withLock {
        val before = store.values.first().notifications
        val next = before.edit()
        if (next == before) return@withLock Result.success(before)
        store.setNotificationPreferences(next)
        runCatching {
            api.updateNotificationSwitches(
                dutyReminders = next.dutyReminders.takeIf { it != before.dutyReminders },
                provinceNews = next.provinceNews.takeIf { it != before.provinceNews },
                applicationStatus = next.applicationStatus.takeIf { it != before.applicationStatus },
            ).toDevice()
        }.onSuccess { store.setNotificationPreferences(it) }
            .onFailure { store.setNotificationPreferences(before) }
    }

    private fun NotificationSwitches.toDevice() = NotificationPreferences(
        dutyReminders = dutyReminders,
        provinceNews = provinceNews,
        applicationStatus = applicationStatus,
    )
}
