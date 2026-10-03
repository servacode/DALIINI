package com.servacode.directory

import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether this build may still run against the backend it is talking to.
 *
 * Asked once, at startup, before anybody signs in — a build that writes bad data or trusts
 * something it should not must be stopped before it does either, not after it has been used.
 *
 * **A failure is not an answer.** No network, a backend that is down, a reply we cannot read:
 * all leave the verdict [Verdict.Unknown], and nothing is blocked. A platform that cannot be
 * reached must not look like a platform that refuses you — the cost of letting an old build run
 * for one more session is small, and the cost of locking out every phone because a request
 * timed out is not.
 */
@Singleton
class VersionCheck @Inject constructor(
    private val api: PublicApiBoundary,
) {
    sealed interface Verdict {
        /** Not asked yet, or asked and not answered. Nothing is blocked. */
        data object Unknown : Verdict

        /** The backend accepts this build. */
        data object Allowed : Verdict

        /**
         * Too old to go on. [notice] is the backend's own words when it sent any, and
         * [storeUrl] is where to get a newer build — null when nobody configured one, and the
         * screen then shows no button rather than a button that does nothing.
         */
        data class TooOld(val notice: String?, val storeUrl: String?) : Verdict

        /**
         * Accepted, and a newer build exists ([latestVersionCode]) at [storeUrl]. Offered, never
         * imposed: this build goes on working whatever the answer. Only reported when there is a
         * store to send the reader to, since an offer they cannot act on is only noise.
         */
        data class Newer(val latestVersionCode: Int, val notice: String?, val storeUrl: String) : Verdict
    }

    private val state = MutableStateFlow<Verdict>(Verdict.Unknown)
    val verdict: StateFlow<Verdict> = state.asStateFlow()

    suspend fun refresh(versionCode: Int) {
        val release = runCatching { api.appRelease() }.getOrNull() ?: return
        state.value = when {
            release.blocks(versionCode) -> Verdict.TooOld(
                notice = release.noticeAr.ifBlank { null },
                storeUrl = release.storeUrl.ifBlank { null },
            )
            release.supersedes(versionCode) && release.storeUrl.isNotBlank() -> Verdict.Newer(
                latestVersionCode = release.latestVersionCode,
                notice = release.noticeAr.ifBlank { null },
                storeUrl = release.storeUrl,
            )
            else -> Verdict.Allowed
        }
    }
}
