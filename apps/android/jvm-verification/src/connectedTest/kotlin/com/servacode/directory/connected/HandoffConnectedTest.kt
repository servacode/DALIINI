package com.servacode.directory.connected

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.OwnerFacilityDraftInput
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The Android -> Admin -> Android hand-off, in the two halves the app plays.
 *
 * `scripts/e2e-android.sh` runs this class twice around the real Admin: first with
 * `HANDOFF_PHASE=submit`, then, after an operator has opened the evidence and approved in a
 * browser (`apps/admin/tests/e2e/handoff.spec.ts`), with `HANDOFF_PHASE=public`. In the
 * connected suite proper the phase is unset and both are skipped: each half means nothing
 * without the other.
 */
class HandoffConnectedTest {
    private val phase = System.getenv("HANDOFF_PHASE").orEmpty()

    @Test fun `the owner submits a facility with its photo and the evidence the backend asks for`() = runBlocking {
        assumeTrue("runs as the first half of the hand-off", phase == "submit")
        val device = Device().signIn(Accounts.OWNER_PHONE, Accounts.OWNER_PASSWORD)
        val owner = device.owner
        val raqqa = device.public.provinces().first { it.nameEn == "Raqqa" }.id

        // What to upload is the backend's to say: the app asks, and answers each requirement.
        val pharmacy = owner.ownerConfig(raqqa).categories.single()
        val requirement = pharmacy.verificationRequirements.single()
        assertTrue(requirement.required)

        val id = owner.createFacility(OwnerFacilityDraftInput(raqqa, pharmacy.category.id, NAME)).summary.id
        owner.patchFacility(id, OwnerFacilityPatch(phone = "+963933444555", addressAr = "شارع التسليم"))
        owner.updateLocation(id, latitude = 35.9612, longitude = 39.0117)
        owner.replaceHours(id, (0..6).map { BusinessHour(it, "00:00", "23:59", 0) })
        owner.uploadImage(id, OwnerUploadPayload("shopfront.jpg", "image/jpeg", jpeg(320, 240)))

        // Without the evidence, the backend refuses the submission.
        val early = runCatching { owner.submitFacility(id) }.exceptionOrNull() as AppException
        assertEquals(AppError.Kind.VALIDATION, early.error.kind)
        assertEquals(OwnerFacilityStatus.DRAFT, owner.facility(id).summary.status)

        repeat(requirement.minFiles) {
            owner.uploadEvidence(id, requirement.id, OwnerUploadPayload("licence.jpg", "image/jpeg", jpeg(200, 150)))
        }
        val submission = owner.submitFacility(id)

        assertEquals("SUBMITTED", submission.status)
        val submitted = owner.facility(id)
        assertEquals(OwnerFacilityStatus.SUBMITTED, submitted.summary.status)
        assertEquals(List(requirement.minFiles) { requirement.id }, submitted.evidence.map { it.requirementId })
    }

    @Test fun `after the operator approves,
        the owner sees it and anyone finds the facility with its photo`() = runBlocking {
        assumeTrue("runs as the second half of the hand-off", phase == "public")

        // The owner's app re-fetches and sees the operator's decision.
        val owner = Device().signIn(Accounts.OWNER_PHONE, Accounts.OWNER_PASSWORD).owner
        val mine = owner.facilities().single { it.nameAr == NAME }
        assertEquals(OwnerFacilityStatus.ACTIVE, mine.status)
        assertEquals("APPROVED", owner.facility(mine.id).application?.status)

        // Anyone, signed in or not, finds it in public discovery.
        val public = Device().public
        val raqqa = public.provinces().first { it.nameEn == "Raqqa" }.id
        val pharmacy = public.categories(raqqa).single().id
        val rows = mutableListOf<FacilitySummary>()
        var cursor: String? = null
        do {
            val page = public.directory(DirectoryQuery(raqqa, pharmacy), cursor)
            rows += page.items
            cursor = page.nextCursor
        } while (cursor != null)
        val listed = rows.single { it.nameAr == NAME }
        assertEquals(mine.id, listed.id)

        // Its photo is public media: a permanent address, not a signed one, readable by anyone.
        val detail = public.facility(listed.id)
        val url = detail.imageUrls.single()
        assertTrue(url, url.contains("/directory-public/facilities/${listed.id}/public/"))
        for (marker in listOf("X-Amz-", "Signature", "Expires", "directory-private", "/evidence/")) {
            assertFalse("$marker in $url", url.contains(marker))
        }
        assertEquals(url, public.facility(listed.id).imageUrls.single())
        val photo = fetchAnonymously(url)
        assertEquals(200, photo.status)
        assertEquals("image/jpeg", photo.contentType)
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), photo.bytes.copyOfRange(0, 3))
    }

    private companion object {
        /** The Admin half finds the application by this name. */
        const val NAME = "e2e-m-تسليم للمراجعة"
    }
}
