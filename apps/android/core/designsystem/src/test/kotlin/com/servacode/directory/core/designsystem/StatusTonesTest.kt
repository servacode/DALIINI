package com.servacode.directory.core.designsystem

import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.model.OwnerFacilityStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The app's tones are the shared vocabulary's: every state is checked against
 * `packages/design-tokens/vocabulary.json`, so a change there that the app has not followed
 * fails here instead of showing one colour in the app and another in the console.
 */
class StatusTonesTest {
    private val vocabulary: String = listOf(
        "../../../../packages/design-tokens/vocabulary.json",
        "../../packages/design-tokens/vocabulary.json",
        "packages/design-tokens/vocabulary.json",
    ).map(::File).first { it.isFile }.readText()

    /** The tone the file gives [key] inside [group], read without a JSON library. */
    private fun tone(group: String, key: String): StatusTone {
        val block = Regex("\"$group\"\\s*:\\s*\\{(.*?)\\n\\s*\\}", RegexOption.DOT_MATCHES_ALL)
            .find(vocabulary)?.groupValues?.get(1) ?: error("no group $group")
        val value = Regex("\"$key\"\\s*:\\s*\\{[^}]*\"tone\"\\s*:\\s*\"(\\w+)\"")
            .find(block)?.groupValues?.get(1) ?: error("no $group.$key")
        return StatusTone.valueOf(value.uppercase())
    }

    @Test fun `availability tones are the vocabulary's`() {
        AvailabilityState.entries.forEach { state ->
            assertEquals(state.name, tone("availability", state.name), StatusTones.availability(state))
        }
    }

    @Test fun `facility status tones are the vocabulary's`() {
        OwnerFacilityStatus.entries.forEach { status ->
            assertEquals(status.name, tone("facilityStatus", status.name), StatusTones.facilityStatus(status))
        }
    }

    @Test fun `report reason tones are the vocabulary's`() {
        FacilityReportReason.entries.forEach { reason ->
            assertEquals(reason.name, tone("reportReason", reason.name), StatusTones.reportReason(reason))
        }
    }
}
