package com.servacode.directory.feature.settings

import com.servacode.directory.core.model.LegalPage
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.network.PublicApiBoundary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The platform's published pages: who it is, what it does with the user's data, the terms, how
 * to use it, and the common questions.
 *
 * They are fetched rather than written into the app, because they change for legal and product
 * reasons long after an APK is signed. What has been read once is kept in memory for the life
 * of the process, so moving between pages and back does not ask twice, and a page that was read
 * before still opens when the connection has gone.
 */
@Singleton
class LegalRepository @Inject constructor(
    private val api: PublicApiBoundary,
) {
    private val read = mutableMapOf<LegalPageKey, LegalPage>()
    private var listed: List<LegalPage>? = null

    suspend fun pages(): Result<List<LegalPage>> {
        listed?.let { return Result.success(it) }
        return runCatching { api.legalPages() }.onSuccess { listed = it }
    }

    suspend fun page(key: LegalPageKey): Result<LegalPage> {
        read[key]?.let { return Result.success(it) }
        return runCatching { api.legalPage(key) }.onSuccess { read[key] = it }
    }

    /** What was read before, for a screen that opens with no connection. */
    fun cached(key: LegalPageKey): LegalPage? = read[key]
}
