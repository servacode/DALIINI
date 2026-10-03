package com.servacode.directory.api.multiplatform

import com.servacode.directory.api.multiplatform.apis.PublicPlatformApi
import com.servacode.directory.api.multiplatform.infrastructure.ApiClient
import com.servacode.directory.api.multiplatform.models.AdminAnalyticsDay
import com.servacode.directory.api.multiplatform.models.OwnerHoursConfirmed
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

/**
 * The client generated for Kotlin Multiplatform, driven through Ktor's mock engine on every
 * platform it is compiled for (DECISION-090): the request it makes and the dates it reads.
 */
class GeneratedClientTest {
    @Test fun `the status request goes to the platform's own path and its answer is read`() = runTest {
        var path: String? = null
        val engine = MockEngine { request ->
            path = request.url.encodedPath
            respond(
                content = """{"maintenance":false,"messageAr":"","retryAfterSeconds":0}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = PublicPlatformApi(baseUrl = "https://api.example.test", httpClientEngine = engine)

        val response = api.publicPlatformStatusRetrieve()

        assertEquals("/api/v1/platform/status/", path)
        assertEquals(200, response.status)
        assertFalse(response.body().maintenance)
    }

    @Test fun `a moment with an offset and microseconds is the same instant in UTC`() {
        // Django writes the offset it was given and up to six decimals; both must survive.
        val parsed = ApiClient.JSON_DEFAULT.decodeFromString(
            OwnerHoursConfirmed.serializer(),
            """{"facilityId":"f-1","hoursConfirmedAt":"2026-09-28T10:00:00.123456+03:00",""" +
                """"infoConfirmedAt":"2026-09-28T07:00:00Z"}""",
        )

        assertEquals(Instant.parse("2026-09-28T07:00:00.123456Z"), parsed.hoursConfirmedAt)
        assertEquals(Instant.parse("2026-09-28T07:00:00Z"), parsed.infoConfirmedAt)
    }

    @Test fun `a calendar day is read as a date with no time and no zone`() {
        val parsed = ApiClient.JSON_DEFAULT.decodeFromString(
            AdminAnalyticsDay.serializer(),
            """{"date":"2026-09-30","searches":1,"zeroResultSearches":0,"facilityViews":2,""" +
                """"directionsRequests":0,"newUsers":0,"approvals":0,"reports":0}""",
        )

        assertEquals(LocalDate(2026, 9, 30), parsed.date)
    }
}
