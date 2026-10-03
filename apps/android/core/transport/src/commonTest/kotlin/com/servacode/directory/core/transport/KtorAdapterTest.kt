package com.servacode.directory.core.transport

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.EmergencyScope
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.model.FacilityTag
import com.servacode.directory.core.model.GeoPoint
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.DirectorySort
import com.servacode.directory.core.network.DutyShiftInput
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.api.ApiEnvironment
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val PROVINCE = "11111111-1111-4111-8111-111111111111"
private const val PHARMACY = "22222222-2222-4222-8222-222222222222"
private const val FACILITY = "33333333-3333-4333-8333-333333333333"
private const val REQUIREMENT = "44444444-4444-4444-8444-444444444444"

/** A verification requirement is keyed by the model's integer (INT-068). */
private const val REQUIREMENT_ID = "7"

/**
 * The multiplatform client, the Ktor adapters and the mappers together, against canned
 * responses in the shape the backend's contract describes: the port of Android's
 * `GeneratedAdapterTest`. Each test checks both directions: what the app decodes and what it
 * actually put on the wire.
 */
class KtorAdapterTest {
    private val backend = FakeBackend()
    private val wiring = Wiring(backend)
    private val publicApi = KtorPublicApi(wiring.clients)
    private val ownerApi = KtorOwnerApi(wiring.clients)

    private fun respond(body: String, code: Int = 200) = backend.enqueue(code, body)

    private suspend fun taken(index: Int = 0): Recorded = backend.request(index)

    private suspend inline fun <T> failure(crossinline block: suspend () -> T): AppError =
        assertFailsWith<AppException> { block() }.error

    private fun compact(id: String, state: String) = """
        {"id":"$id","nameAr":"صيدلية","nameEn":null,"slug":"صيدلية",
         "category":{"id":"$PHARMACY","nameAr":"صيدلية","nameEn":"Pharmacy"},
         "city":null,"distanceMeters":120.5,"ratingAverage":4.5,"ratingCount":2,
         "isFavorite":false,"imageUrl":"https://cdn.example.test/shop.jpg",
         "lastVerifiedAt":null,"updatedAt":"2026-09-19T10:00:00Z",
         "availability":{"state":"$state","nextOpenAt":null,
                         "isOpenNow":true,"isOnDutyToday":false}}
    """

    @Test fun `provinces come from the backend unchanged`() = runTest {
        respond(
            """{"items":[{"id":"$PROVINCE","code":"raqqa","nameAr":"الرقة","nameEn":"Raqqa",
            "mapCenter":{"latitude":35.9528,"longitude":39.0085}}]}""",
        )

        val provinces = publicApi.provinces()

        assertEquals(
            listOf(Province(PROVINCE, "الرقة", "Raqqa", GeoPoint(35.9528, 39.0085), code = "raqqa")),
            provinces,
        )
        val request = taken()
        assertEquals("GET", request.method)
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
        val url = taken().url
        assertEquals("/api/v1/owner/config/", url.encodedPath)
        assertEquals(PROVINCE, url.parameters["provinceId"])
    }

    @Test fun `category capabilities decide what the screens offer`() = runTest {
        respond(
            """{"items":[{"id":"$PHARMACY","slug":"pharmacy","nameAr":"صيدلية","nameEn":"Pharmacy","iconKey":"pharmacy",
            "group":{"id":"$PROVINCE","nameAr":"صحة"},
            "capabilities":{"hours":true,"photos":true,"ratings":true,"duty":true,"specialtyFilter":false,
            "serviceFilter":false,"temporaryClosure":true,"ownerOnboarding":true}}]}""",
        )

        val category = publicApi.categories(PROVINCE).single()

        val capabilities = assertNotNull(category.capabilities)
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
        val firstRequest = taken(0).url
        assertEquals("/api/v1/public/facilities/", firstRequest.encodedPath)
        assertEquals(PROVINCE, firstRequest.parameters["provinceId"])
        assertEquals(PHARMACY, firstRequest.parameters["categoryId"])
        assertEquals("true", firstRequest.parameters["dutyNow"])
        assertNull(firstRequest.parameters["openNow"])
        assertNull(firstRequest.parameters["cursor"])
        // The token went back exactly as it came, special characters included.
        assertEquals("cD0x+/=", taken(1).url.parameters["cursor"])
    }

    @Test fun `open now and on duty today are sent as separate parameters and combine`() = runTest {
        respond("""{"items":[${compact(FACILITY, "DUTY")}],"nextCursor":null,"hasMore":false}""")

        val page = publicApi.directory(
            DirectoryQuery(
                provinceId = PROVINCE,
                openNow = true,
                dutyToday = true,
                sort = DirectorySort.NEAREST,
                latitude = 35.95,
                longitude = 39.01,
            ),
        )

        // The row says both things at once, which the single legacy state cannot express.
        val facility = page.items.single()
        assertTrue(facility.isOpenNow)
        assertFalse(facility.isOnDutyToday)
        assertEquals("https://cdn.example.test/shop.jpg", facility.imageUrl)
        val url = taken().url
        assertEquals("true", url.parameters["openNow"])
        assertEquals("true", url.parameters["dutyToday"])
        assertEquals("nearest", url.parameters["sort"])
        // Never confused with a shift that happens to be running at this second.
        assertNull(url.parameters["dutyNow"])
    }

    @Test fun `ordering by name still measures the distance`() = runTest {
        respond("""{"items":[${compact(FACILITY, "OPEN")}],"nextCursor":null,"hasMore":false}""")

        publicApi.directory(
            DirectoryQuery(
                provinceId = PROVINCE,
                sort = DirectorySort.NAME,
                latitude = 35.95,
                longitude = 39.01,
            ),
        )

        val url = taken().url
        assertEquals("name", url.parameters["sort"])
        // Four decimal places, always: a position leaves the phone at about ten metres so that
        // no log or proxy downstream can hold more (Coordinates.kt).
        assertEquals("35.9500", url.parameters["latitude"])
        assertEquals("39.0100", url.parameters["longitude"])
    }

    @Test fun `a category's choices keep the operators' order with their integer keys as string ids`() = runTest {
        respond(
            """{"specialties":[{"id":7,"nameAr":"قلبية"},{"id":3,"nameAr":"أطفال"}],
            "services":[{"id":12,"nameAr":"قياس ضغط"}]}""",
        )

        val tags = publicApi.categoryTags(PHARMACY)

        assertEquals(
            CategoryTags(
                specialties = listOf(FacilityTag("7", "قلبية"), FacilityTag("3", "أطفال")),
                services = listOf(FacilityTag("12", "قياس ضغط")),
            ),
            tags,
        )
        assertEquals("/api/v1/public/categories/$PHARMACY/tags/", taken().url.encodedPath)
    }

    @Test fun `a category with nothing to choose from has empty lists`() = runTest {
        respond("""{"specialties":[],"services":[]}""")

        assertTrue(publicApi.categoryTags(PHARMACY).isEmpty)
    }

    @Test fun `a specialty and a service narrow a directory page as the integers they are keyed by`() = runTest {
        respond("""{"items":[],"nextCursor":null,"hasMore":false}""")
        respond("""{"items":[],"nextCursor":null,"hasMore":false}""")

        publicApi.directory(DirectoryQuery(PROVINCE, PHARMACY, specialtyId = "3", serviceTagId = "12"))
        publicApi.directory(DirectoryQuery(PROVINCE, PHARMACY))

        val narrowed = taken(0).url
        assertEquals("3", narrowed.parameters["specialtyId"])
        assertEquals("12", narrowed.parameters["serviceTagId"])
        // The contract's name for it, not the older alias.
        assertNull(narrowed.parameters["serviceId"])
        val whole = taken(1).url
        assertNull(whole.parameters["specialtyId"])
        assertNull(whole.parameters["serviceTagId"])
    }

    @Test fun `a tag id that is not a number never reaches the network`() = runTest {
        val error = failure { publicApi.directory(DirectoryQuery(PROVINCE, PHARMACY, specialtyId = "x")) }

        assertEquals(AppError.Kind.UNEXPECTED, error.kind)
        assertTrue(backend.requests().isEmpty())
    }

    @Test fun `an invalid cursor is a validation error on the cursor field`() = runTest {
        respond(
            """{"code":"VALIDATION_ERROR","message":"Invalid input.",
            "details":{"cursor":["The cursor is not valid. Request the first page again."]},"requestId":"r"}""",
            code = 400,
        )

        val error = failure { publicApi.directory(DirectoryQuery(PROVINCE, PHARMACY), "garbage") }

        assertEquals(AppError.Kind.VALIDATION, error.kind)
        assertTrue("cursor" in error.fieldErrors)
    }

    @Test fun `detail hours are ordered by weekday then sequence and availability is the backend's`() = runTest {
        respond(
            """{"id":"$FACILITY","nameAr":"صيدلية","nameEn":null,"slug":"صيدلية",
            "category":{"id":"$PHARMACY","nameAr":"صيدلية","nameEn":null},"city":{"id":"$PROVINCE","nameAr":"الرقة"},
            "distanceMeters":null,"ratingAverage":null,"ratingCount":0,"isFavorite":true,"imageUrl":null,
            "lastVerifiedAt":"2026-09-18T10:00:00Z","updatedAt":"2026-09-19T10:00:00Z","whatsapp":"+963933000000",
            "availability":{"state":"CLOSED","nextOpenAt":"2026-09-20T08:00:00+03:00",
                            "isOpenNow":false,"isOnDutyToday":true},
            "descriptionAr":null,"descriptionEn":null,"phone":"+963900000000","addressAr":"شارع","addressEn":null,
            "neighborhood":null,"location":{"latitude":35.95,"longitude":39.01},
            "images":[{"id":"$REQUIREMENT","url":"https://cdn.example.test/a.jpg"}],
            "specialties":[{"id":3,"nameAr":"قلبية"}],"services":[{"id":12,"nameAr":"قياس ضغط"}],
            "hours":[
              {"id":"$PROVINCE","weekday":1,"opensAt":"16:00:00","closesAt":"22:00:00","sequence":1},
              {"id":"$PHARMACY","weekday":0,"opensAt":"09:00:00","closesAt":"13:00:00","sequence":0},
              {"id":"$FACILITY","weekday":1,"opensAt":"09:00:00","closesAt":"13:00:00","sequence":0}]}""",
        )

        val detail = publicApi.facility(FACILITY)

        assertEquals(listOf(0 to 0, 1 to 0, 1 to 1), detail.hours.map { it.weekday to it.sequence })
        // The backend's own text, as the JVM client passed it through.
        assertEquals(BusinessHour(1, "16:00:00", "22:00:00", 1), detail.hours.last())
        assertEquals(AvailabilityState.CLOSED, detail.summary.availability)
        assertEquals(1_789_880_400_000L, detail.summary.nextOpenAtEpochMillis)
        assertEquals(listOf("https://cdn.example.test/a.jpg"), detail.imageUrls)
        assertEquals(listOf("قلبية"), detail.specialties)
        assertEquals(listOf("قياس ضغط"), detail.services)
        assertEquals("الرقة", detail.summary.cityNameAr)
        assertEquals("+963933000000", detail.whatsapp)
        assertEquals(1_789_725_600_000L, detail.lastVerifiedAtEpochMillis)
        assertEquals(1_789_812_000_000L, detail.updatedAtEpochMillis)
        assertEquals("/api/v1/public/facilities/$FACILITY/", taken().url.encodedPath)
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
        assertEquals("/api/v1/public/home/", url.encodedPath)
        assertEquals(PROVINCE, url.parameters["provinceId"])
        // No location without permission: the home is province-wide.
        assertNull(url.parameters["latitude"])
    }

    @Test fun `a value the client was not generated for fails as unexpected and not as a wrong state`() = runTest {
        respond("""{"items":[${compact(FACILITY, "HALF_OPEN")}],"nextCursor":null,"hasMore":false}""")

        val error = failure { publicApi.directory(DirectoryQuery(PROVINCE, PHARMACY)) }

        assertEquals(AppError.Kind.UNEXPECTED, error.kind)
    }

    @Test fun `a patch sends only the fields it names`() = runTest {
        respond(ownerDetail())

        ownerApi.patchFacility(FACILITY, OwnerFacilityPatch(phone = "+963900000001"))

        val request = taken()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/owner/facilities/$FACILITY/", request.url.encodedPath)
        assertEquals("""{"phone":"+963900000001"}""", request.text)
    }

    @Test fun `specialties and services travel as the integer ids they are keyed by`() = runTest {
        respond(ownerDetail())

        val detail = ownerApi.patchFacility(
            FACILITY,
            OwnerFacilityPatch(specialtyIds = listOf("3"), serviceTagIds = listOf("12")),
        )

        assertEquals("""{"specialtyIds":[3],"serviceTagIds":[12]}""", taken().text)
        assertEquals(listOf("3"), detail.specialtyIds)
        assertEquals(listOf("12"), detail.serviceTagIds)
    }

    @Test fun `an empty WhatsApp is sent because blank is how it is cleared`() = runTest {
        respond(ownerDetail())

        val detail = ownerApi.patchFacility(FACILITY, OwnerFacilityPatch(whatsapp = ""))

        assertEquals("""{"whatsapp":""}""", taken().text)
        assertEquals("+963933000000", detail.whatsapp)
    }

    @Test fun `a refused WhatsApp names its field`() = runTest {
        respond(
            """{"code":"VALIDATION_ERROR","message":"Invalid input.",
            "details":{"whatsapp":["Enter a valid Syrian mobile number."]},"requestId":"r"}""",
            code = 400,
        )

        val error = failure { ownerApi.patchFacility(FACILITY, OwnerFacilityPatch(whatsapp = "123")) }

        assertEquals(AppError.Kind.VALIDATION, error.kind)
        assertTrue("whatsapp" in error.fieldErrors)
    }

    @Test fun `insights are the owner's three counts over the window`() = runTest {
        respond(
            """{"facilityId":"$FACILITY","windowDays":30,"since":"2026-08-20T10:00:00Z",
            "views":12,"calls":3,"directions":5}""",
        )

        val insights = ownerApi.insights(FACILITY)

        assertEquals("/api/v1/owner/facilities/$FACILITY/insights/", taken().url.encodedPath)
        assertEquals(listOf(12, 3, 5), listOf(insights.views, insights.calls, insights.directions))
        assertEquals(30, insights.windowDays)
    }

    @Test fun `a report goes to the facility with its reason and note`() = runTest {
        respond(
            """{"id":"$FACILITY","status":"OPEN","createdAt":"2026-09-19T10:00:00Z"}""",
            code = 201,
        )

        publicApi.reportFacility(FACILITY, FacilityReportReason.WRONG_HOURS, "  يفتح مساءً  ")

        val request = taken()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/facilities/$FACILITY/reports/", request.url.encodedPath)
        assertEquals("""{"reason":"WRONG_HOURS","note":"يفتح مساءً"}""", request.text)
    }

    @Test fun `evidence goes as a real file part with the requirement id as a bare form field`() = runTest {
        respond("""{"id":"$FACILITY","requirementId":$REQUIREMENT_ID}""", code = 201)
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

        val evidence = ownerApi.uploadEvidence(
            FACILITY,
            REQUIREMENT_ID,
            OwnerUploadPayload(fileName = "upload.jpg", mediaType = "image/jpeg", bytes = jpeg),
        )

        assertEquals(REQUIREMENT_ID, evidence.requirementId)
        assertEquals(FACILITY, evidence.id)
        val request = taken()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/owner/facilities/$FACILITY/evidence/", request.url.encodedPath)
        assertTrue(request.contentType.orEmpty().startsWith("multipart/form-data"), request.contentType)
        val body = request.latin1
        // A form field holding the bare value, not a JSON part. Ktor writes no Content-Type for
        // a form field where OkHttp wrote text/plain; the value is the same.
        val field = body.substringAfter("name=requirementId").substringBefore("--")
        assertTrue(field.endsWith("\r\n\r\n$REQUIREMENT_ID\r\n"), field)
        val file = body.substringAfter("name=file")
        assertTrue(file.startsWith("; filename=\"upload.jpg\"\r\n"), file)
        assertTrue(file.contains("Content-Type: image/jpeg\r\n"), file)
        assertTrue(file.contains("Content-Length: 4\r\n"), file)
        assertTrue(file.contains("\r\n\r\n" + jpeg.joinToString("") { (it.toInt() and 0xFF).toChar().toString() }))
    }

    @Test fun `a claim's evidence goes the same way`() = runTest {
        respond(
            """{"id":"$REQUIREMENT","requirementId":$REQUIREMENT_ID,"createdAt":"2026-09-19T10:00:00Z"}""",
            code = 201,
        )

        val evidence = ownerApi.uploadClaimEvidence(
            FACILITY,
            REQUIREMENT_ID,
            OwnerUploadPayload(fileName = "upload.pdf", mediaType = "application/pdf", bytes = byteArrayOf(1, 2)),
        )

        assertEquals(REQUIREMENT_ID, evidence.requirementId)
        assertEquals(1_789_812_000_000L, evidence.createdAtEpochMillis)
        val request = taken()
        assertEquals("/api/v1/owner/claims/$FACILITY/evidence/", request.url.encodedPath)
        val body = request.latin1
        assertTrue(body.substringAfter("name=requirementId").substringBefore("--").endsWith("\r\n\r\n7\r\n"))
        assertTrue(body.contains("name=file; filename=\"upload.pdf\"\r\n"))
        assertTrue(body.contains("Content-Type: application/pdf\r\n"))
    }

    @Test fun `the account picture is a file part on a PUT`() = runTest {
        respond(PROFILE)

        val profile = publicApi.updateProfileImage(
            OwnerUploadPayload(fileName = "upload.png", mediaType = "image/png", bytes = byteArrayOf(9)),
        )

        assertEquals("https://cdn.example.test/me.png", profile.imageUrl)
        val request = taken()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/account/profile/image/", request.url.encodedPath)
        assertTrue(request.latin1.contains("name=file; filename=\"upload.png\"\r\n"))
        assertTrue(request.latin1.contains("Content-Type: image/png\r\n"))
    }

    @Test fun `a file name is quoted as OkHttp quotes it and a media type it cannot read is left out`() = runTest {
        respond("""{"id":"$FACILITY","url":"https://cdn.example.test/a.jpg","sortOrder":0,"width":1,"height":1}""")

        ownerApi.uploadImage(
            FACILITY,
            OwnerUploadPayload(fileName = "a\"b\r\nc.jpg", mediaType = "not a type", bytes = byteArrayOf(1)),
        )

        val request = taken()
        assertEquals("/api/v1/owner/facilities/$FACILITY/images/", request.url.encodedPath)
        val file = request.latin1.substringAfter("name=file")
        assertTrue(file.startsWith("; filename=\"a%22b%0D%0Ac.jpg\"\r\n"), file)
        assertFalse(file.substringBefore("\r\n\r\n").contains("Content-Type"), file)
    }

    @Test fun `a duty shift is created without an id and in UTC`() = runTest {
        respond(
            """{"id":"$FACILITY","startsAt":"2026-09-20T00:00:00Z","endsAt":"2026-09-20T08:00:00Z"}""",
            code = 201,
        )

        val shift = ownerApi.createDuty(FACILITY, DutyShiftInput(1_789_862_400_000L, 1_789_891_200_000L))

        assertEquals(FACILITY, shift.id)
        assertEquals(1_789_862_400_000L, shift.startsAtEpochMillis)
        val request = taken()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/owner/facilities/$FACILITY/duty/", request.url.encodedPath)
        // The JVM client wrote "2026-09-20T00:00Z"; the same instant, with its seconds.
        assertEquals("""{"startsAt":"2026-09-20T00:00:00Z","endsAt":"2026-09-20T08:00:00Z"}""", request.text)
    }

    @Test fun `a closure the backend stored without a reason arrives with none`() = runTest {
        respond(
            """{"items":[
            {"id":"$FACILITY","startsAt":"2026-09-20T05:00:00Z","endsAt":"2026-09-20T13:00:00Z","reason":""},
            {"id":"$FACILITY","startsAt":"2026-09-20T05:00:00Z","endsAt":"2026-09-20T13:00:00Z","reason":"   "},
            {"id":"$FACILITY","startsAt":"2026-09-20T05:00:00Z","endsAt":"2026-09-20T13:00:00Z","reason":null},
            {"id":"$FACILITY","startsAt":"2026-09-20T05:00:00Z","endsAt":"2026-09-20T13:00:00Z","reason":"صيانة"}]}""",
        )

        val reasons = ownerApi.temporaryClosures(FACILITY).map { it.reason }

        assertEquals(listOf(null, null, null, "صيانة"), reasons)
    }

    @Test fun `owner facilities map status and a missing required action`() = runTest {
        respond(
            """{"items":[{"id":"$FACILITY","nameAr":"صيدلية","category":{"id":"$PHARMACY","nameAr":"صيدلية"},
            "province":{"id":"$PROVINCE","nameAr":"الرقة"},"status":"SUBMITTED",
            "lastUpdate":"2026-09-19T10:00:00Z","requiredAction":null,
            "capabilities":{"hours":true,"photos":true,"ratings":true,"duty":true,"specialtyFilter":false,
            "serviceFilter":false,"temporaryClosure":true,"ownerOnboarding":true}},
            {"id":"$REQUIREMENT","nameAr":"أخرى","category":{"id":"$PHARMACY","nameAr":"صيدلية"},
            "province":{"id":"$PROVINCE","nameAr":"الرقة"},"status":"DRAFT",
            "lastUpdate":"2026-09-19T10:00:00Z","requiredAction":"COMPLETE_AND_SUBMIT",
            "capabilities":{"hours":true,"photos":true,"ratings":true,"duty":false,"specialtyFilter":true,
            "serviceFilter":false,"temporaryClosure":false,"ownerOnboarding":true}}]}""",
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

    @Test fun `the evidence form is whatever the backend configured including nothing`() = runTest {
        respond(
            """{"province":{"id":"$PROVINCE","nameAr":"الرقة"},"categories":[{
            "category":{"id":"$PHARMACY","nameAr":"صيدلية","nameEn":null,"iconKey":null,"specialization":"PHARMACY"},
            "capabilities":{"hours":true,"photos":true,"ratings":true,"duty":true,"specialtyFilter":false,
            "serviceFilter":false,"temporaryClosure":true,"ownerOnboarding":true},"verificationRequirements":[],
            "specialties":[],"services":[]}]}""",
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
            "minFiles":1,"maxFiles":2}],"specialties":[{"id":3,"nameAr":"قلبية"}],
            "services":[{"id":12,"nameAr":"قياس ضغط"}]}]}""",
        )

        val requirement = ownerApi.ownerConfig(PROVINCE).categories.single().verificationRequirements.single()

        assertEquals(REQUIREMENT_ID, requirement.id)
        assertEquals(1, requirement.minFiles)
        assertEquals(2, requirement.maxFiles)
    }

    @Test fun `each category in the owner configuration carries what owners may pick for it`() = runTest {
        respond(
            """{"province":{"id":"$PROVINCE","nameAr":"الرقة"},"categories":[{
            "category":{"id":"$PHARMACY","nameAr":"عيادات","nameEn":null,"iconKey":null,
            "specialization":"MEDICAL_CLINIC"},
            "capabilities":{"hours":true,"photos":true,"ratings":true,"duty":false,"specialtyFilter":true,
            "serviceFilter":true,"temporaryClosure":true,"ownerOnboarding":true},"verificationRequirements":[],
            "specialties":[{"id":3,"nameAr":"قلبية"},{"id":4,"nameAr":"أطفال"}],
            "services":[{"id":12,"nameAr":"قياس ضغط"}]}]}""",
        )

        val tags = ownerApi.ownerConfig(PROVINCE).categories.single().tags

        assertEquals(listOf(FacilityTag("3", "قلبية"), FacilityTag("4", "أطفال")), tags.specialties)
        assertEquals(listOf(FacilityTag("12", "قياس ضغط")), tags.services)
    }

    @Test fun `a facility with no specialty and no service has empty lists for the page to leave out`() = runTest {
        respond(
            """{"id":"$FACILITY","nameAr":"صيدلية","nameEn":null,"slug":"صيدلية",
            "category":{"id":"$PHARMACY","nameAr":"صيدلية","nameEn":null},"city":null,
            "distanceMeters":null,"ratingAverage":null,"ratingCount":0,"isFavorite":false,"imageUrl":null,
            "lastVerifiedAt":null,"updatedAt":"2026-09-19T10:00:00Z","whatsapp":null,
            "availability":{"state":"OPEN","nextOpenAt":null,"isOpenNow":true,"isOnDutyToday":false},
            "descriptionAr":null,"descriptionEn":null,"phone":null,"addressAr":null,"addressEn":null,
            "neighborhood":null,"location":null,"images":[],"specialties":[],"services":[],"hours":[]}""",
        )

        val detail = publicApi.facility(FACILITY)

        assertTrue(detail.specialties.isEmpty())
        assertTrue(detail.services.isEmpty())
    }

    @Test fun `a push token is registered for android and unregistered by value`() = runTest {
        backend.enqueue(204)
        backend.enqueue(204)
        val push = KtorPushRegistration(wiring.clients)

        push.registerAndroidToken("fcm-token-1")
        push.deactivateAndroidToken("fcm-token-1")

        val register = taken(0)
        assertEquals("PUT", register.method)
        assertEquals("/api/v1/account/push-token/", register.url.encodedPath)
        assertEquals("""{"platform":"ANDROID","token":"fcm-token-1"}""", register.text)
        val unregister = taken(1)
        assertEquals("POST", unregister.method)
        assertEquals("/api/v1/account/push-token/unregister/", unregister.url.encodedPath)
        assertEquals("""{"token":"fcm-token-1"}""", unregister.text)
    }

    @Test fun `emergency numbers come in the backend's order and dialable`() = runTest {
        respond(
            """{"items":[
            {"id":"$REQUIREMENT","scope":"PROVINCE","provinceId":"$PROVINCE","labelAr":"مشفى الرقة الوطني",
             "phone":"022 123 456","kind":"HOSPITAL","sortOrder":5},
            {"id":"$FACILITY","scope":"NATIONAL","provinceId":null,"labelAr":"الإسعاف","phone":"110",
             "kind":"AMBULANCE","sortOrder":1}]}""",
        )

        val numbers = publicApi.emergencyNumbers(PROVINCE)

        assertEquals(listOf("الإسعاف", "مشفى الرقة الوطني"), numbers.map { it.nameAr })
        assertEquals(listOf("110", "022123456"), numbers.map { it.number })
        assertEquals(listOf(EmergencyScope.NATIONAL, EmergencyScope.PROVINCE), numbers.map { it.scope })
        assertEquals(PROVINCE, numbers[1].provinceId)
        val url = taken().url
        assertEquals("/api/v1/emergency-numbers/", url.encodedPath)
        assertEquals(PROVINCE, url.parameters["provinceId"])
    }

    @Test fun `the duty roster asks for its days and keeps each shift`() = runTest {
        respond(
            """{"provinceId":"$PROVINCE","days":[{"date":"2026-09-29","items":[${compact(FACILITY, "DUTY")}],
            "shifts":[{"facilityId":"$FACILITY","startsAt":"2026-09-29T20:00:00+03:00",
                       "endsAt":"2026-09-30T08:00:00+03:00"}]}]}""",
        )

        val days = publicApi.dutyRoster(PROVINCE, "2026-09-29", days = 9)

        assertEquals("2026-09-29", days.single().date)
        assertEquals(FACILITY, days.single().facilities.single().id)
        val shift = days.single().shifts.single()
        assertEquals(FACILITY, shift.facilityId)
        assertEquals("2026-09-29 20:00", DamascusTime.format(shift.startsAtEpochMillis))
        assertEquals("2026-09-30 08:00", DamascusTime.format(shift.endsAtEpochMillis))
        val url = taken().url
        assertEquals("/api/v1/public/duty/", url.encodedPath)
        assertEquals("2026-09-29", url.parameters["date"])
        // The backend serves one to seven days; more is not asked for.
        assertEquals("7", url.parameters["days"])
    }

    @Test fun `confirming the hours posts once and reads both times back`() = runTest {
        respond(
            """{"facilityId":"$FACILITY","hoursConfirmedAt":"2026-09-29T10:00:00Z",
            "infoConfirmedAt":"2026-09-29T10:00:00Z"}""",
        )

        val confirmed = ownerApi.confirmHours(FACILITY)

        assertEquals(1_790_676_000_000L, confirmed.hoursConfirmedAtEpochMillis)
        assertEquals(confirmed.hoursConfirmedAtEpochMillis, confirmed.infoConfirmedAtEpochMillis)
        val request = taken()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/owner/facilities/$FACILITY/confirm-hours/", request.url.encodedPath)
    }

    @Test fun `a category without hours is refused by code`() = runTest {
        respond(
            """{"code":"HOURS_NOT_SUPPORTED","message":"x","details":{},"requestId":"r"}""",
            code = 409,
        )

        val error = failure { ownerApi.confirmHours(FACILITY) }

        assertEquals(AppError.Kind.CONFLICT, error.kind)
        assertEquals("HOURS_NOT_SUPPORTED", error.code)
    }

    @Test fun `a no-content answer is success`() = runTest {
        backend.enqueue(204)

        publicApi.deleteRating(FACILITY)

        val request = taken()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/facilities/$FACILITY/rating/", request.url.encodedPath)
    }

    @Test fun `a malformed id never reaches the network`() = runTest {
        val error = failure { publicApi.facility("not-a-uuid") }

        assertEquals(AppError.Kind.UNEXPECTED, error.kind)
        assertTrue(backend.requests().isEmpty())
    }

    @Test fun `an id goes out as the UUID's own text as UUID fromString wrote it`() = runTest {
        respond(PROFILE)

        publicApi.updateProfile(provinceId = "ABCDEF00-1111-4111-8111-111111111111")

        assertEquals("""{"provinceId":"abcdef00-1111-4111-8111-111111111111"}""", taken().text)
    }

    @Test fun `hours are sent as times of day and read back as the backend writes them`() = runTest {
        respond(
            """{"items":[
            {"id":"$PROVINCE","weekday":0,"opensAt":"08:00:00","closesAt":"12:00:00","sequence":0},
            {"id":"$PHARMACY","weekday":0,"opensAt":"16:00:00","closesAt":"02:30:00","sequence":1}]}""",
        )

        val hours = ownerApi.replaceHours(
            FACILITY,
            listOf(BusinessHour(0, "08:00", "12:00", 0), BusinessHour(0, "16:00:00", "02:30", 1)),
        )

        assertEquals(
            listOf(BusinessHour(0, "08:00:00", "12:00:00", 0), BusinessHour(0, "16:00:00", "02:30:00", 1)),
            hours,
        )
        val request = taken()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/owner/facilities/$FACILITY/hours/", request.url.encodedPath)
        // A zero sequence is the model's default and, as from the JVM client, is not written.
        assertEquals(
            """[{"weekday":0,"opensAt":"08:00","closesAt":"12:00"},""" +
                """{"weekday":0,"opensAt":"16:00","closesAt":"02:30","sequence":1}]""",
            request.text,
        )
    }

    @Test fun `a time of day that is not one never reaches the network`() = runTest {
        val error = failure { ownerApi.replaceHours(FACILITY, listOf(BusinessHour(0, "9am", "13:00"))) }

        assertEquals(AppError.Kind.UNEXPECTED, error.kind)
        assertTrue(backend.requests().isEmpty())
    }

    @Test fun `discovery goes out without the user's token and account calls with it`() = runTest {
        wiring.accessTokens.set("token-1")
        respond("""{"items":[]}""")
        respond(PROFILE)

        publicApi.provinces()
        publicApi.profile()

        assertNull(taken(0).headers[AUTHORIZATION])
        assertEquals("Bearer token-1", taken(1).headers[AUTHORIZATION])
        assertEquals("/api/v1/account/profile/", taken(1).url.encodedPath)
    }

    @Test fun `a build without an address fails each call typed and not at construction`() = runTest {
        val unconfigured = Wiring(backend, environment = ApiEnvironment("https://<ROOT_DOMAIN>/"))
        val publicApi = KtorPublicApi(unconfigured.clients)
        val ownerApi = KtorOwnerApi(unconfigured.clients)
        val authApi = KtorAuthApi(unconfigured.clients, deviceName = "test")

        for (error in listOf(
            failure { publicApi.provinces() },
            failure { publicApi.provinces() },
            failure { ownerApi.facilities() },
            failure { authApi.login("+963900000001", "secret") },
        )) {
            assertEquals(AppError.Kind.UNEXPECTED, error.kind)
            assertEquals(TransportErrors.CLIENT_NOT_CONFIGURED, error.code)
        }
        assertTrue(backend.requests().isEmpty())
    }

    private fun ownerDetail() = """
        {"id":"$FACILITY","nameAr":"صيدلية","category":{"id":"$PHARMACY","nameAr":"صيدلية"},
         "province":{"id":"$PROVINCE","nameAr":"الرقة"},"status":"DRAFT","lastUpdate":"2026-09-19T10:00:00Z",
         "requiredAction":"COMPLETE_AND_SUBMIT",
         "capabilities":{"hours":true,"photos":true,"ratings":true,"duty":true,"specialtyFilter":false,
         "serviceFilter":false,"temporaryClosure":true,"ownerOnboarding":true},"nameEn":null,"descriptionAr":null,
         "descriptionEn":null,
         "phone":"+963900000001","whatsapp":"+963933000000","addressAr":null,"addressEn":null,
         "cityId":null,"neighborhoodId":null,
         "location":null,"specialtyIds":[3],"serviceTagIds":[12],"evidence":[],"hours":[],"application":null}
    """

    private companion object {
        const val PROFILE = """{"id":"$FACILITY","displayName":"مالك","phone":"+963900000001",""" +
            """"provinceId":null,"phoneVerifiedAt":null,"address":"",""" +
            """"profileImageUrl":"https://cdn.example.test/me.png"}"""
        const val AUTHORIZATION = "Authorization"
    }
}
