package com.servacode.directory.connected

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.network.DutyShiftInput
import com.servacode.directory.core.network.OkHttpRealtimeStream
import com.servacode.directory.core.network.OwnerFacilityDraftInput
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.RealtimeConfig
import com.servacode.directory.core.network.RealtimeSignal
import com.servacode.directory.core.network.TemporaryClosureInput
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO

/** One owner session for the whole class: owner logins count against a per-account throttle. */
private object Owner {
    val device: Device by lazy { Device().signIn(Accounts.OWNER_PHONE, Accounts.OWNER_PASSWORD) }
}

class OwnerConnectedTest {
    private val device = Owner.device
    private val owner = device.owner

    private fun raqqaId(): String = runBlocking { device.public.provinces().first { it.nameEn == "Raqqa" }.id }

    private fun jpeg(): ByteArray {
        val image = BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        check(ImageIO.write(image, "jpg", out))
        return out.toByteArray()
    }

    private fun appError(block: suspend () -> Any?): AppError =
        runBlocking { (runCatching { block() }.exceptionOrNull() as AppException).error }

    @Test fun `onboarding offers what the backend opened and asks for no evidence it has not configured`() {
        val config = runBlocking { owner.ownerConfig(raqqaId()) }

        val pharmacy = config.categories.single()
        assertEquals("PHARMACY", pharmacy.specialization)
        // LAUNCH_POLICY_PENDING: no pharmacy verification requirement is configured yet.
        assertTrue(pharmacy.verificationRequirements.isEmpty())
        assertTrue(pharmacy.capabilities.supportsOwnerOnboarding)
    }

    @Test fun `a selection the backend has not opened is refused by the backend`() {
        val error = appError {
            owner.createFacility(OwnerFacilityDraftInput(raqqaId(), UUID.randomUUID().toString(), "e2e-m-مرفوضة"))
        }

        assertEquals(AppError.Kind.VALIDATION, error.kind)
    }

    @Test fun `a draft is built step by step, uploaded to, and submitted`() = runBlocking {
        val raqqa = raqqaId()
        val pharmacy = owner.ownerConfig(raqqa).categories.single().category.id

        val draft = owner.createFacility(OwnerFacilityDraftInput(raqqa, pharmacy, "e2e-m-مسودة جديدة"))
        assertEquals(OwnerFacilityStatus.DRAFT, draft.summary.status)
        val id = draft.summary.id

        // A patch sends the one field it names; the name must survive it.
        val patched = owner.patchFacility(id, OwnerFacilityPatch(phone = "+963933111222"))
        assertEquals("+963933111222", patched.phone)
        assertEquals("e2e-m-مسودة جديدة", patched.summary.nameAr)

        val located = owner.updateLocation(id, latitude = 35.95, longitude = 39.01)
        assertEquals(35.95, located.latitude!!, 1e-6)

        val hours = owner.replaceHours(
            id,
            listOf(
                BusinessHour(0, "08:00", "12:00", 0),
                BusinessHour(0, "16:00", "22:00", 1),
                BusinessHour(4, "20:00", "02:00", 0),
            ),
        )
        assertEquals(listOf(0 to 0, 0 to 1, 4 to 0), hours.map { it.weekday to it.sequence }.sortedWith(compareBy({ it.first }, { it.second })))

        val image = owner.uploadImage(id, OwnerUploadPayload("upload.jpg", "image/jpeg", jpeg()))
        assertEquals(64, image.width)
        assertTrue(image.url.startsWith("http"))
        assertFalse(image.url.contains("private"))
        assertEquals(listOf(image.id), owner.images(id).map { it.id })

        val notAnImage = appError {
            owner.uploadImage(id, OwnerUploadPayload("upload.jpg", "image/jpeg", "not an image".toByteArray()))
        }
        assertEquals(AppError.Kind.VALIDATION, notAnImage.kind)
        val oversized = appError {
            owner.uploadImage(id, OwnerUploadPayload("upload.jpg", "image/jpeg", ByteArray(10 * 1024 * 1024 + 1)))
        }
        assertEquals(AppError.Kind.VALIDATION, oversized.kind)
        val unknownRequirement = appError {
            owner.uploadEvidence(id, UUID.randomUUID().toString(), OwnerUploadPayload("e.jpg", "image/jpeg", jpeg()))
        }
        assertTrue(unknownRequirement.kind in setOf(AppError.Kind.VALIDATION, AppError.Kind.NOT_FOUND))

        val day = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        val closure = owner.createTemporaryClosure(id, TemporaryClosureInput(now + day, now + 2 * day, "جرد"))
        assertEquals(listOf(closure.id), owner.temporaryClosures(id).map { it.id })
        owner.deleteTemporaryClosure(id, closure.id)
        assertTrue(owner.temporaryClosures(id).isEmpty())

        val shift = owner.createDuty(id, DutyShiftInput(now + day, now + day + 8 * 60 * 60 * 1000L))
        assertEquals(listOf(shift.id), owner.duty(id).map { it.id })
        owner.deleteDuty(id, shift.id)

        val submission = owner.submitFacility(id)
        assertEquals("SUBMITTED", submission.status)
        assertEquals(OwnerFacilityStatus.SUBMITTED, owner.facility(id).summary.status)
        assertNotNull(owner.facilities().firstOrNull { it.id == id })
    }

    @Test fun `a change the owner makes reaches a listening device as an event, and only as a signal`() = runBlocking {
        val raqqa = raqqaId()
        val broadcast = owner.facilities().single { it.nameAr.contains("للبث") }
        val stream = OkHttpRealtimeStream(
            OkHttpClient(),
            RealtimeConfig(socketUrl(), allowCleartext = true),
            MemoryAccess(),
        )
        val connected = CompletableDeferred<Unit>()

        val event = withTimeout(30_000) {
            val listening = launch(Dispatchers.IO) {
                stream.events(raqqa)
                    .onEach { if (it is RealtimeSignal.Connected) connected.complete(Unit) }
                    .filterIsInstance<RealtimeSignal.Event>()
                    .first { it.value.resourceId == broadcast.id }
                    .let { found.complete(it) }
            }
            connected.await()
            // Give the subscription a moment to be registered before the change.
            kotlinx.coroutines.delay(500)
            val now = System.currentTimeMillis()
            val closure = owner.createTemporaryClosure(
                broadcast.id,
                TemporaryClosureInput(now - 60_000, now + 60 * 60 * 1000L, "بث"),
            )
            val received = found.await()
            owner.deleteTemporaryClosure(broadcast.id, closure.id)
            listening.cancel()
            received
        }

        assertEquals("province", event.value.scope.type)
        assertEquals(raqqa, event.value.scope.id)
        assertTrue(event.value.name.startsWith("public."))
        // The payload names what changed; it carries no facility state to trust.
        assertNull(event.value.resourceId?.takeIf { it != broadcast.id })
    }

    private val found = CompletableDeferred<RealtimeSignal.Event>()
}
