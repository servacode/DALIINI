package com.servacode.directory.core.network.api

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.GeoPoint
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.DutyShiftInput
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

private const val PROVINCE = "11111111-1111-4111-8111-111111111111"
private const val PHARMACY = "22222222-2222-4222-8222-222222222222"
private const val FACILITY = "33333333-3333-4333-8333-333333333333"
private const val REQUIREMENT = "44444444-4444-4444-8444-444444444444"
/** A verification requirement is keyed by the model's integer (INT-068). */
private const val REQUIREMENT_ID = "7"

/**
 * The generated client, the adapters and the mappers together, against canned responses in
 * the shape the backend's contract describes. Each test checks both directions: what the app
 * decodes and what it actually put on the wire.
 */
class GeneratedAdapterTest {
    private val server = MockWebServer()
    private lateinit var publicApi: GeneratedPublicApi
    private lateinit var ownerApi: GeneratedOwnerApi

    @Before fun start() {
        server.start()
        val client = GeneratedClient(
            ApiEnvironment(server.url("/").toString(), allowCleartext = true),
            OkHttpClient(),
        )
        publicApi = GeneratedPublicApi(client, client)
        ownerApi = GeneratedOwnerApi(client)
    }

    @After fun stop() = server.close()

    private fun respond(body: String, code: Int = 200) =
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build())

    private fun taken(): RecordedRequest = server.takeRequest()

    private fun compact(id: String, state: String) = """
        {"id":"$id","nameAr":"صيدلية","nameEn":null,
         "category":{"id":"$PHARMACY","nameAr":"صيدلية","nameEn":"Pharmacy"},
         "city":null,"distanceMeters":120.5,"ratingAverage":4.5,"ratingCount":2,
         "availability":{"state":"$state","nextOpenAt":null}}
    """

    @Test fun `provinces come from the backend unchanged`() = runTest {
        respond(
            """{"items":[{"id":"$PROVINCE","code":"raqqa","nameAr":"الرقة","nameEn":"Raqqa",
            "mapCenter":{"latitude":35.9528,"longitude":39.0085}}]}""",
        )

        val provinces = publicApi.provinces()

        assertEquals(listOf(Province(PROVINCE, "الرقة", "Raqqa", GeoPoint(35.9528, 39.0085))), provinces)
        val request = taken()
        assertEquals("/api/v1/public/provinces/", request.url.encodedPath)
    }

    @Test fun `a province without a map centre carries none`() = runTest {
        respond("""{"items":[{"id":"$PROVINCE","code":"raqqa","nameAr":"الرقة","nameEn":null,"mapCenter":null}]}""")

        assertNull(publicApi.provinces().single().mapCenter)
    }

    @Test fun `the owner config carries the province map centre`() = runTest {
        respond(
            """{"province":{"id":"$PROVINCE","nameAr":"الرقة","mapCenter":{"latitude":35.9528,"longitude":39.0085}},
            "categories":[]}""",
        )

        val province = ownerApi.ownerConfig(PROVINCE).province

        assertEquals(Province(PROVINCE, "الرقة", mapCenter = GeoPoint(35.9528, 39.0085)), province)
    }

    @Test fun `category capabilities decide what the screens offer`() = runTest {
        respond(
            """{"items":[{"id":"$PHARMACY","nameAr":"صيدلية","nameEn":"Pharmacy","iconKey":"pharmacy",
            "group":{"id":"$PROVINCE","nameAr":"صحة"},
            "capabilities":{"hours":true,"photos":true,"ratings":true,"duty":true,"specialtyFilter":false,
            "serviceFilter":false,"temporaryClosure":true,"ownerOnboarding":true}}]}""",
        )

        val category = publicApi.categories(PROVINCE).single()

        val capabilities = category.capabilities!!
        assertTrue(capabilities.supportsDuty)
        assertTrue(capabilities.supportsRatings)
        assertFalse(capabilities.supportsSpecialtyFilter)
        assertEquals("/api/v1/public/provinces/$PROVINCE/categories/", taken().url.encodedPath)
    }

    @Test fun `a directory page carries the cursor opaquely and sends filters only when set`() = runTest {
        respond("""{"items":[${compact(FACILITY, "DUTY")}],"nextCursor":"cD0x+/=","hasMore":true}""")
        respond("""{"items":[],"nextCursor":null,"hasMore":false}""")
        val query = DirectoryQuery(provinceId = PROVINCE, categoryId = PHARMACY, dutyNow = true)

        val first = publicApi.directory(query)
        val last = publicApi.directory(query, first.nextCursor)

        assertEquals(AvailabilityState.DUTY, first.items.single().availability)
        assertEquals("cD0x+/=", first.nextCursor)
        assertTrue(first.hasMore)
        assertFalse(last.hasMore)
        assertNull(last.nextCursor)
        val firstRequest = taken().url
        assertEquals("true", firstRequest.queryParameter("dutyNow"))
        assertNull(firstRequest.queryParameter("openNow"))
        assertNull(firstRequest.queryParameter("cursor"))
        // The token went back exactly as it came, special characters included.
        assertEquals("cD0x+/=", taken().url.queryParameter("cursor"))
    }

    @Test fun `an invalid cursor is a validation error on the cursor field`() = runTest {
        respond(
            """{"code":"VALIDATION_ERROR","message":"Invalid input.",
            "details":{"cursor":["The cursor is not valid. Request the first page again."]},"requestId":"r"}""",
            code = 400,
        )

        val error = runCatching { publicApi.directory(DirectoryQuery(PROVINCE, PHARMACY), "garbage") }
            .exceptionOrNull() as AppException

        assertEquals(AppError.Kind.VALIDATION, error.error.kind)
        assertTrue("cursor" in error.error.fieldErrors)
    }

    @Test fun `detail hours are ordered by weekday then sequence, and availability is the backend's`() = runTest {
        respond(
            """{"id":"$FACILITY","nameAr":"صيدلية","nameEn":null,
            "category":{"id":"$PHARMACY","nameAr":"صيدلية","nameEn":null},"city":{"id":"$PROVINCE","nameAr":"الرقة"},
            "distanceMeters":null,"ratingAverage":null,"ratingCount":0,
            "availability":{"state":"CLOSED","nextOpenAt":"2026-09-20T08:00:00+03:00"},
            "descriptionAr":null,"descriptionEn":null,"phone":"+963900000000","addressAr":"شارع","addressEn":null,
            "neighborhood":null,"location":{"latitude":35.95,"longitude":39.01},
            "images":[{"id":"$REQUIREMENT","url":"https://cdn.example.test/a.jpg"}],
            "specialties":[],"services":[{"id":"$PROVINCE","nameAr":"قياس ضغط"}],
            "hours":[
              {"id":"$PROVINCE","weekday":1,"opensAt":"16:00:00","closesAt":"22:00:00","sequence":1},
              {"id":"$PHARMACY","weekday":0,"opensAt":"09:00:00","closesAt":"13:00:00","sequence":0},
              {"id":"$FACILITY","weekday":1,"opensAt":"09:00:00","closesAt":"13:00:00","sequence":0}]}""",
        )

        val detail = publicApi.facility(FACILITY)

        assertEquals(listOf(0 to 0, 1 to 0, 1 to 1), detail.hours.map { it.weekday to it.sequence })
        assertEquals(AvailabilityState.CLOSED, detail.summary.availability)
        assertEquals(1_789_880_400_000L, detail.summary.nextOpenAtEpochMillis)
        assertEquals(listOf("https://cdn.example.test/a.jpg"), detail.imageUrls)
        assertEquals(listOf("قياس ضغط"), detail.services)
        assertEquals("الرقة", detail.summary.cityNameAr)
    }

    @Test fun `the home decodes advertisements with a free-form payload`() = runTest {
        respond(
            """{"ads":[{"id":"$FACILITY","imageUrl":"https://cdn.example.test/ad.jpg","titleAr":"عرض",
            "titleEn":null,"subtitleAr":null,"subtitleEn":null,
            "action":{"type":"FACILITY","payload":{"facilityId":"$FACILITY","nested":{"a":[1,2]}}},
            "slideDurationMs":4000}],
            "categories":[],"nearby":[${compact(FACILITY, "OPEN")}],"openNearby":[],"dutyNow":[],
            "serverTime":"2026-09-19T10:00:00Z"}""",
        )

        val home = publicApi.home(Province(PROVINCE, "الرقة"), latitude = null, longitude = null)

        assertEquals("عرض", home.ads.single().titleAr)
        assertEquals(AvailabilityState.OPEN, home.nearby.single().availability)
        assertEquals(1_789_812_000_000L, home.refreshedAtEpochMillis)
        val url = taken().url
        assertEquals(PROVINCE, url.queryParameter("provinceId"))
        // No location without permission: the home is province-wide.
        assertNull(url.queryParameter("latitude"))
    }

    @Test fun `a value the client was not generated for fails as unexpected, not as a wrong state`() = runTest {
        respond("""{"items":[${compact(FACILITY, "HALF_OPEN")}],"nextCursor":null,"hasMore":false}""")

        val error = runCatching { publicApi.directory(DirectoryQuery(PROVINCE, PHARMACY)) }
            .exceptionOrNull() as AppException

        assertEquals(AppError.Kind.UNEXPECTED, error.error.kind)
    }

    @Test fun `a patch sends only the fields it names`() = runTest {
        respond(ownerDetail())

        ownerApi.patchFacility(FACILITY, OwnerFacilityPatch(phone = "+963900000001"))

        val request = taken()
        assertEquals("PATCH", request.method)
        assertEquals("""{"phone":"+963900000001"}""", request.body!!.utf8())
    }

    @Test fun `evidence goes as a real file part with the requirement id as plain text`() = runTest {
        respond("""{"id":"$FACILITY","requirementId":$REQUIREMENT_ID}""", code = 201)
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

        val evidence = ownerApi.uploadEvidence(
            FACILITY,
            REQUIREMENT_ID,
            OwnerUploadPayload(fileName = "upload.jpg", mediaType = "image/jpeg", bytes = jpeg),
        )

        assertEquals(REQUIREMENT_ID, evidence.requirementId)
        val body = taken().body!!.string(Charsets.ISO_8859_1)
        // A text form field holding the bare value, not a JSON part.
        val field = body.substringAfter("name=\"requirementId\"").substringBefore("--")
        assertTrue(field, field.contains("Content-Type: text/plain"))
        assertTrue(field, field.endsWith("\r\n\r\n$REQUIREMENT_ID\r\n"))
        assertTrue(body.contains("name=\"file\"; filename=\"upload.jpg\""))
        assertTrue(body.contains("Content-Type: image/jpeg"))
    }

    @Test fun `a duty shift is created without an id and in UTC`() = runTest {
        respond(
            """{"id":"$FACILITY","startsAt":"2026-09-20T00:00:00Z","endsAt":"2026-09-20T08:00:00Z"}""",
            code = 201,
        )

        val shift = ownerApi.createDuty(FACILITY, DutyShiftInput(1_789_862_400_000L, 1_789_891_200_000L))

        assertEquals(FACILITY, shift.id)
        val body = taken().body!!.utf8()
        assertFalse(body.contains("\"id\""))
        assertTrue(body.contains("\"startsAt\":\"2026-09-20T00:00Z\"") || body.contains("2026-09-20T00:00:00Z"))
    }

    @Test fun `owner facilities map status and a missing required action`() = runTest {
        respond(
            """{"items":[{"id":"$FACILITY","nameAr":"صيدلية","category":{"id":"$PHARMACY","nameAr":"صيدلية"},
            "province":{"id":"$PROVINCE","nameAr":"الرقة"},"status":"SUBMITTED",
            "lastUpdate":"2026-09-19T10:00:00Z","requiredAction":null,"capabilities":{"hours":true,"photos":true,"ratings":true,"duty":true,"specialtyFilter":false,"serviceFilter":false,"temporaryClosure":true,"ownerOnboarding":true}},
            {"id":"$REQUIREMENT","nameAr":"أخرى","category":{"id":"$PHARMACY","nameAr":"صيدلية"},
            "province":{"id":"$PROVINCE","nameAr":"الرقة"},"status":"DRAFT",
            "lastUpdate":"2026-09-19T10:00:00Z","requiredAction":"COMPLETE_AND_SUBMIT","capabilities":{"hours":true,"photos":true,"ratings":true,"duty":false,"specialtyFilter":true,"serviceFilter":false,"temporaryClosure":false,"ownerOnboarding":true}}]}""",
        )

        val facilities = ownerApi.facilities()

        assertEquals(OwnerFacilityStatus.SUBMITTED, facilities[0].status)
        assertNull(facilities[0].requiredAction)
        assertEquals("COMPLETE_AND_SUBMIT", facilities[1].requiredAction)
        // Capabilities come with each facility (INT-056), not from a side lookup.
        assertTrue(facilities[0].capabilities.supportsDuty)
        assertFalse(facilities[1].capabilities.supportsDuty)
        assertFalse(facilities[1].capabilities.supportsTemporaryClosure)
    }

    @Test fun `the evidence form is whatever the backend configured, including nothing`() = runTest {
        respond(
            """{"province":{"id":"$PROVINCE","nameAr":"الرقة"},"categories":[{
            "category":{"id":"$PHARMACY","nameAr":"صيدلية","nameEn":null,"iconKey":null,"specialization":"PHARMACY"},
            "capabilities":{"hours":true,"photos":true,"ratings":true,"duty":true,"specialtyFilter":false,
            "serviceFilter":false,"temporaryClosure":true,"ownerOnboarding":true},"verificationRequirements":[]}]}""",
        )

        val config = ownerApi.ownerConfig(PROVINCE)

        val pharmacy = config.categories.single()
        assertEquals("PHARMACY", pharmacy.specialization)
        assertTrue(pharmacy.verificationRequirements.isEmpty())
        assertTrue(pharmacy.capabilities.supportsDuty)
        assertTrue(pharmacy.capabilities.supportsRatings)
    }

    @Test fun `a configured requirement arrives with the id the upload sends back`() = runTest {
        respond(
            """{"province":{"id":"$PROVINCE","nameAr":"الرقة"},"categories":[{
            "category":{"id":"$PHARMACY","nameAr":"صيدلية","nameEn":null,"iconKey":null,"specialization":"PHARMACY"},
            "capabilities":{"hours":true,"photos":true,"ratings":true,"duty":true,"specialtyFilter":false,
            "serviceFilter":false,"temporaryClosure":true,"ownerOnboarding":true},"verificationRequirements":[
            {"id":$REQUIREMENT_ID,"labelAr":"ترخيص","labelEn":null,"instructionsAr":null,"required":true,
            "minFiles":1,"maxFiles":2}]}]}""",
        )

        val requirement = ownerApi.ownerConfig(PROVINCE).categories.single().verificationRequirements.single()

        assertEquals(REQUIREMENT_ID, requirement.id)
        assertEquals(1, requirement.minFiles)
        assertEquals(2, requirement.maxFiles)
    }

    @Test fun `a push token is registered for android and unregistered by value`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        server.enqueue(MockResponse.Builder().code(204).build())
        val push = GeneratedPushRegistration(
            GeneratedClient(ApiEnvironment(server.url("/").toString(), allowCleartext = true), OkHttpClient()),
        )

        push.registerAndroidToken("fcm-token-1")
        push.deactivateAndroidToken("fcm-token-1")

        val register = taken()
        assertEquals("PUT", register.method)
        assertEquals("/api/v1/account/push-token/", register.url.encodedPath)
        assertEquals("""{"platform":"ANDROID","token":"fcm-token-1"}""", register.body!!.utf8())
        val unregister = taken()
        assertEquals("/api/v1/account/push-token/unregister/", unregister.url.encodedPath)
        assertEquals("""{"token":"fcm-token-1"}""", unregister.body!!.utf8())
    }

    @Test fun `a no-content answer is success`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())

        publicApi.deleteRating(FACILITY)

        assertEquals("DELETE", taken().method)
    }

    @Test fun `a malformed id never reaches the network`() = runTest {
        try {
            publicApi.facility("not-a-uuid")
            fail("expected an error")
        } catch (error: AppException) {
            assertEquals(AppError.Kind.UNEXPECTED, error.error.kind)
        }
        assertEquals(0, server.requestCount)
    }

    private fun ownerDetail() = """
        {"id":"$FACILITY","nameAr":"صيدلية","category":{"id":"$PHARMACY","nameAr":"صيدلية"},
         "province":{"id":"$PROVINCE","nameAr":"الرقة"},"status":"DRAFT","lastUpdate":"2026-09-19T10:00:00Z",
         "requiredAction":"COMPLETE_AND_SUBMIT","capabilities":{"hours":true,"photos":true,"ratings":true,"duty":true,"specialtyFilter":false,"serviceFilter":false,"temporaryClosure":true,"ownerOnboarding":true},"nameEn":null,"descriptionAr":null,"descriptionEn":null,
         "phone":"+963900000001","addressAr":null,"addressEn":null,"cityId":null,"neighborhoodId":null,
         "location":null,"specialtyIds":[],"serviceTagIds":[],"evidence":[],"hours":[],"application":null}
    """
}
