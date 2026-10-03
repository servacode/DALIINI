package com.servacode.directory.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules that keep a measurement from harming what it measures. No Android, no network, no
 * clock: these run on the JVM in milliseconds, which is the point of having them separate from
 * the sender.
 */
class AnalyticsQueueTest {

    @Test
    fun `a success is forgotten`() {
        assertEquals(AnalyticsOutcome.SENT, outcomeFor(202))
        assertEquals(AnalyticsOutcome.SENT, outcomeFor(200))
    }

    @Test
    fun `a rejection the server will repeat is dropped rather than retried`() {
        // 400 is the app sending something the registry does not know. It will be wrong again
        // next time, so retrying it is an infinite loop against our own server.
        assertEquals(AnalyticsOutcome.DROP, outcomeFor(400))
        assertEquals(AnalyticsOutcome.DROP, outcomeFor(401))
        assertEquals(AnalyticsOutcome.DROP, outcomeFor(404))
    }

    @Test
    fun `being told to slow down is a reason to wait, not to give up`() {
        assertEquals(AnalyticsOutcome.RETRY, outcomeFor(429))
    }

    @Test
    fun `a server fault and a dead connection are both temporary`() {
        assertEquals(AnalyticsOutcome.RETRY, outcomeFor(500))
        assertEquals(AnalyticsOutcome.RETRY, outcomeFor(503))
        assertEquals(AnalyticsOutcome.RETRY, outcomeFor(null))
    }

    @Test
    fun `the queue never grows past its limit`() {
        var pending = emptyList<Int>()
        repeat(500) { pending = enqueueBounded(pending, it, limit = 50) }
        assertEquals(50, pending.size)
    }

    @Test
    fun `a full queue drops the oldest, not the newest`() {
        var pending = emptyList<Int>()
        repeat(60) { pending = enqueueBounded(pending, it, limit = 50) }
        // A product question is about what people are doing now, so the recent events are the
        // ones worth keeping.
        assertEquals(10, pending.first())
        assertEquals(59, pending.last())
    }

    @Test
    fun `a queue under the limit keeps everything in order`() {
        var pending = emptyList<String>()
        for (value in listOf("a", "b", "c")) pending = enqueueBounded(pending, value, limit = 50)
        assertEquals(listOf("a", "b", "c"), pending)
    }

    @Test
    fun `no event carries anything that identifies a person`() {
        val events = listOf(
            AnalyticsEvent.AppOpen("android", "1.0.0"),
            AnalyticsEvent.HomeView("p1"),
            AnalyticsEvent.ProvinceSelected("p1"),
            AnalyticsEvent.CategoryOpen("p1", "c1"),
            AnalyticsEvent.SearchSubmitted(7, "p1", "c1"),
            AnalyticsEvent.SearchZeroResults(7, "p1"),
            AnalyticsEvent.FacilityView("f1", "p1", "c1"),
            AnalyticsEvent.MapOpen("p1"),
            AnalyticsEvent.MarkerOpen("f1"),
            AnalyticsEvent.PhoneTap("f1"),
            AnalyticsEvent.DirectionsStart("f1", "valhalla"),
            AnalyticsEvent.RatingSubmit("f1", 5),
        )
        val forbidden = setOf(
            "latitude", "longitude", "lat", "lng", "coordinates", "location",
            "phone", "whatsapp", "token", "email", "name", "query", "text", "address",
        )
        for (event in events) {
            for (key in event.properties.keys) {
                assertTrue("${event.name} carries a forbidden key: $key", key !in forbidden)
            }
        }
    }

    @Test
    fun `a search records how long the words were and never the words`() {
        val event = AnalyticsEvent.SearchSubmitted(queryLength = 12, provinceId = "p1")
        assertEquals("12", event.properties["queryLength"])
        assertEquals(setOf("queryLength", "provinceId"), event.properties.keys)
    }

    @Test
    fun `optional properties are left out rather than sent empty`() {
        // The server checks keys against what the event declares; an empty string is a value it
        // would have to interpret, and a missing key is not.
        val event = AnalyticsEvent.FacilityView("f1")
        assertEquals(setOf("facilityId"), event.properties.keys)
    }

    @Test
    fun `every event name is one the backend registry declares`() {
        // Kept in step with analytics/registry.py by hand, and proven here: the server answers an
        // unknown name with a 400, which this list exists to make impossible to reach by accident.
        val registered = setOf(
            "app_open", "home_view", "province_selected", "location_permission_result",
            "category_open", "search_submitted", "search_zero_results", "facility_view",
            "map_open", "marker_open", "phone_tap", "directions_start", "rating_submit",
            "owner_draft_create", "owner_submit", "application_status_view",
            "ad_impression", "ad_click",
        )
        val used = listOf(
            AnalyticsEvent.AppOpen("android", "1"),
            AnalyticsEvent.HomeView("p"),
            AnalyticsEvent.ProvinceSelected("p"),
            AnalyticsEvent.CategoryOpen("p", "c"),
            AnalyticsEvent.SearchSubmitted(1, "p"),
            AnalyticsEvent.SearchZeroResults(1, "p"),
            AnalyticsEvent.FacilityView("f"),
            AnalyticsEvent.MapOpen("p"),
            AnalyticsEvent.MarkerOpen("f"),
            AnalyticsEvent.PhoneTap("f"),
            AnalyticsEvent.DirectionsStart("f", "valhalla"),
            AnalyticsEvent.RatingSubmit("f", 4),
            AnalyticsEvent.AdImpression("a", "p"),
            AnalyticsEvent.AdClick("a", "FACILITY"),
        )
        for (event in used) {
            assertTrue("${event.name} is not in the backend registry", event.name in registered)
        }
    }

    @Test
    fun `the no-op tracker swallows everything without complaint`() {
        NoOpAnalyticsTracker.track(AnalyticsEvent.HomeView("p1"))
        NoOpAnalyticsTracker.track(AnalyticsEvent.PhoneTap("f1"))
    }
}
