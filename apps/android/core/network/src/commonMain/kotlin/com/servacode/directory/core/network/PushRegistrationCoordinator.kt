package com.servacode.directory.core.network

import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.inject.Singleton
import kotlinx.datetime.LocalDate
import kotlin.concurrent.Volatile

/**
 * Keeps the backend told which push token reaches this device.
 *
 * The provider hands a token at start-up and whenever it rotates one; the backend needs it only
 * for a signed-in session, and ties it to that session, so a token that arrives while signed
 * out is held here and registered when a session starts. Ending a session needs no call from
 * the app: the backend stops pushing to that session's tokens itself.
 */
@Singleton
class PushRegistrationCoordinator @Inject constructor(
    private val boundary: PushRegistrationBoundary,
    private val session: SessionCoordinator,
) {
    // Only read and replaced whole, so a volatile field is enough.
    @Volatile private var latest: String? = null

    suspend fun onTokenAvailable(token: String) {
        val normalized = token.trim()
        require(normalized.isNotBlank() && normalized.length <= 4096) { "Invalid push token." }
        latest = normalized
        if (session.state.value == SessionState.SIGNED_IN) boundary.registerAndroidToken(normalized)
    }

    /** Called when a session starts: the device's current token now belongs to it. */
    suspend fun onSignedIn() {
        latest?.let { boundary.registerAndroidToken(it) }
    }

    /** The user turned notifications off for this device. */
    suspend fun unregister() {
        latest?.let { boundary.deactivateAndroidToken(it) }
    }
}

/**
 * What a push may carry: identifiers, never content.
 *
 * `notificationId` and `type`, as the backend sends today. For a duty-gap nudge
 * (`duty.gap_nudge`) the day and the place it is about are also accepted if the push carries them
 * — `gapDate` or `date` ("YYYY-MM-DD"), `provinceId`, `facilityId` — so the notice can open the
 * roster on that day; without them it opens the owner's facilities. Any other key (a title, a body,
 * a phone number) makes the whole payload refused: the rule that content arrives over REST, not in
 * the push, holds for new types too.
 * Parsing is tolerant within that: a malformed date or id is dropped, not the push.
 */
data class PushMessageData(
    val notificationId: String?,
    val type: String,
    val date: LocalDate? = null,
    val provinceId: String? = null,
    val facilityId: String? = null,
) {
    val isDutyGap: Boolean
        get() = type.equals(DUTY_GAP, ignoreCase = true) ||
            type.equals(com.servacode.directory.core.model.NotificationTypes.DUTY_GAP_NUDGE, ignoreCase = true)

    companion object {
        const val DUTY_GAP = "DUTY_GAP"
        private val ALLOWED = setOf("notificationId", "type", "date", "gapDate", "provinceId", "facilityId")
        private val IDENTIFIER = Regex("^[0-9A-Za-z-]{1,64}$")

        fun from(data: Map<String, String>): PushMessageData? {
            if (data.keys.any { it !in ALLOWED }) return null
            val type = data["type"]?.trim().orEmpty()
            if (type.isBlank()) return null
            val notificationId = data["notificationId"]?.trim()?.takeIf { it.isNotEmpty() }
            // A notice from the inbox needs its id; a nudge is about a date, and may come without.
            val nudge = type.equals(DUTY_GAP, ignoreCase = true) ||
                type.equals(com.servacode.directory.core.model.NotificationTypes.DUTY_GAP_NUDGE, ignoreCase = true)
            if (notificationId == null && !nudge) return null
            return PushMessageData(
                notificationId = notificationId,
                type = type,
                date = (data["gapDate"] ?: data["date"])?.trim()?.take(10)?.let {
                    runCatching { LocalDate.parse(it) }.getOrNull()
                },
                provinceId = data["provinceId"]?.trim()?.takeIf { IDENTIFIER.matches(it) },
                facilityId = data["facilityId"]?.trim()?.takeIf { IDENTIFIER.matches(it) },
            )
        }
    }
}
