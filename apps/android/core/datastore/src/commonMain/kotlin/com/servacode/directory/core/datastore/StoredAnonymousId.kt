package com.servacode.directory.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.servacode.directory.core.analytics.AnonymousId
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The pseudonymous id a device sends with a product measurement.
 *
 * It is a random UUID, made the first time anything is measured and kept after that. Three things
 * it deliberately is not:
 *
 *  - **Not derived from the device.** Not the advertising id, not the Android id, not the IMEI,
 *    not the iPhone's identifier for vendor, not a hash of anything the hardware knows. A random
 *    value cannot be joined to another app's data about the same phone, which is the whole
 *    reason those identifiers are avoided.
 *  - **Not tied to an account.** Signing in does not change it and does not attach it to anybody;
 *    the events it labels are posted without the access token.
 *  - **Not permanent.** It lives in the app's own storage, so clearing the app's data makes a new
 *    one — which is what somebody clearing their data means by it.
 *
 * It answers "how many people", and that is all it is for.
 */
@Singleton
class StoredAnonymousId @Inject constructor(
    file: DirectoryDataStore,
) : AnonymousId {
    private val store = file.store

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun current(): String {
        val existing = store.data.first()[ANONYMOUS_ID]
        if (existing != null) return existing

        // Lower-case and hyphenated, as java.util.UUID wrote it before this was shared.
        val created = Uuid.random().toString()
        // `edit` is atomic, so two callers racing on first launch agree on one value: whichever
        // wrote first wins and the other reads it back rather than overwriting it.
        var settled = created
        store.edit { prefs ->
            val now = prefs[ANONYMOUS_ID]
            if (now == null) prefs[ANONYMOUS_ID] = created else settled = now
        }
        return settled
    }

    private companion object {
        val ANONYMOUS_ID = stringPreferencesKey("analytics_anonymous_id")
    }
}
