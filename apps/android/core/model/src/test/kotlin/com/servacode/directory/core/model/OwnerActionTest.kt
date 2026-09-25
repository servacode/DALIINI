package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Every action the contract lists is one this app knows.
 *
 * A code that falls through to [OwnerAction.UNKNOWN] leaves an owner holding a facility that
 * needs something, without being told what — which is the one thing this table exists to prevent.
 * The words are in the design system's resources, so this asserts the choice and not the wording.
 */
class OwnerActionTest {
    @Test fun `every documented action is known`() {
        listOf(
            "REVIEW_REJECTION",
            "COMPLETE_AND_SUBMIT",
            "REVERIFY_AND_SUBMIT",
            "WAIT_FOR_REVIEW",
            "CONTACT_SUPPORT",
        ).forEach { code ->
            assertEquals(code, OwnerAction.valueOf(code), ownerAction(code))
            assertNotEquals(code, OwnerAction.UNKNOWN, ownerAction(code))
        }
    }

    @Test fun `a code from a newer backend is unknown, not shown as itself`() {
        assertEquals(OwnerAction.UNKNOWN, ownerAction("SOMETHING_NEW"))
    }
}
