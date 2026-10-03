package com.servacode.directory.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.servacode.directory.core.analytics.AnonymousId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The pseudonymous id a device sends with a product measurement.
 *
 * It is a random UUID, made the first time anything is measured and kept after that. Three things
 * it deliberately is not:
 *
 *  - **Not derived from the device.** Not the advertising id, not the Android id, not the IMEI,
 *    not a hash of anything the hardware knows. A random value cannot be joined to another app's
 *    data about the same phone, which is the whole reason those identifiers are avoided.
 *  - **Not tied to an account.** Signing in does not change it and does not attach it to anybody;
 *    the events it labels are posted without the access token.
 *  - **Not permanent.** It lives in the app's own storage, so clearing the app's data makes a new
 *    one — which is what somebody clearing their data means by it.
 *
 * It answers "how many people", and that is all it is for.
 */
@Singleton
class StoredAnonymousId @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnonymousId {

    override suspend fun current(): String {
        val existing = context.directoryDataStore.data.first()[ANONYMOUS_ID]
        if (existing != null) return existing

        val created = UUID.randomUUID().toString()
        // `edit` is atomic, so two callers racing on first launch agree on one value: whichever
        // wrote first wins and the other reads it back rather than overwriting it.
        var settled = created
        context.directoryDataStore.edit { prefs ->
            val now = prefs[ANONYMOUS_ID]
            if (now == null) prefs[ANONYMOUS_ID] = created else settled = now
        }
        return settled
    }

    private companion object {
        val ANONYMOUS_ID = stringPreferencesKey("analytics_anonymous_id")
    }
}
