package com.servacode.directory.core.analytics

/**
 * What the product measures, and the only thing it is allowed to measure.
 *
 * Every event here exists in the backend's registry (`analytics/registry.py`) under the same
 * name, with the same keys. That is not a coincidence to be maintained by hand: the server
 * rejects an unknown name outright, and rejects a known name carrying a key it did not declare,
 * so a drift between these two lists is a 400 and not a silent column of wrong data.
 *
 * What is deliberately absent matters as much as what is here. No coordinates, no phone numbers,
 * no names, no search text — only the *length* of a query, which answers "did they type
 * something or tap a chip" without recording what anybody looked for. The server refuses the
 * forbidden keys too, so this is a floor rather than an honour system.
 */
sealed interface AnalyticsEvent {
    /** The registry name. The server matches on this exactly. */
    val name: String

    /** The declared keys for that name, and nothing else. */
    val properties: Map<String, String>

    /** The app was brought to the foreground. */
    data class AppOpen(val platform: String, val appVersion: String) : AnalyticsEvent {
        override val name = "app_open"
        override val properties = mapOf("platform" to platform, "appVersion" to appVersion)
    }

    data class HomeView(val provinceId: String) : AnalyticsEvent {
        override val name = "home_view"
        override val properties = mapOf("provinceId" to provinceId)
    }

    data class ProvinceSelected(val provinceId: String) : AnalyticsEvent {
        override val name = "province_selected"
        override val properties = mapOf("provinceId" to provinceId)
    }

    data class CategoryOpen(val provinceId: String, val categoryId: String) : AnalyticsEvent {
        override val name = "category_open"
        override val properties = mapOf("provinceId" to provinceId, "categoryId" to categoryId)
    }

    /**
     * How long what they typed was, never what it was. A zero-result search is its own event
     * because "people search and find nothing" is the one number that says what to add next.
     */
    data class SearchSubmitted(
        val queryLength: Int,
        val provinceId: String,
        val categoryId: String? = null,
    ) : AnalyticsEvent {
        override val name = "search_submitted"
        override val properties = buildMap {
            put("queryLength", queryLength.toString())
            put("provinceId", provinceId)
            categoryId?.let { put("categoryId", it) }
        }
    }

    data class SearchZeroResults(
        val queryLength: Int,
        val provinceId: String,
        val categoryId: String? = null,
    ) : AnalyticsEvent {
        override val name = "search_zero_results"
        override val properties = buildMap {
            put("queryLength", queryLength.toString())
            put("provinceId", provinceId)
            categoryId?.let { put("categoryId", it) }
        }
    }

    data class FacilityView(
        val facilityId: String,
        val provinceId: String? = null,
        val categoryId: String? = null,
    ) : AnalyticsEvent {
        override val name = "facility_view"
        override val properties = buildMap {
            put("facilityId", facilityId)
            provinceId?.let { put("provinceId", it) }
            categoryId?.let { put("categoryId", it) }
        }
    }

    data class MapOpen(val provinceId: String, val categoryId: String? = null) : AnalyticsEvent {
        override val name = "map_open"
        override val properties = buildMap {
            put("provinceId", provinceId)
            categoryId?.let { put("categoryId", it) }
        }
    }

    data class MarkerOpen(val facilityId: String) : AnalyticsEvent {
        override val name = "marker_open"
        override val properties = mapOf("facilityId" to facilityId)
    }

    /** That the dialer was opened, not that a call happened — the app cannot know the second. */
    data class PhoneTap(val facilityId: String) : AnalyticsEvent {
        override val name = "phone_tap"
        override val properties = mapOf("facilityId" to facilityId)
    }

    data class DirectionsStart(val facilityId: String, val routingProvider: String) : AnalyticsEvent {
        override val name = "directions_start"
        override val properties =
            mapOf("facilityId" to facilityId, "routingProvider" to routingProvider)
    }

    data class RatingSubmit(val facilityId: String, val stars: Int) : AnalyticsEvent {
        override val name = "rating_submit"
        override val properties = mapOf("facilityId" to facilityId, "stars" to stars.toString())
    }

    /**
     * A slide settled on screen. Counted once per advertisement per province on the home page,
     * so a slider cycling for a minute is one view and not twelve; the console divides clicks by
     * these to give each advertisement its rate.
     */
    data class AdImpression(val adId: String, val provinceId: String) : AnalyticsEvent {
        override val name = "ad_impression"
        override val properties = mapOf("adId" to adId, "provinceId" to provinceId)
    }

    /** A slide was pressed. [actionType] is the backend's name for where it leads (`FACILITY`…). */
    data class AdClick(val adId: String, val actionType: String) : AnalyticsEvent {
        override val name = "ad_click"
        override val properties = mapOf("adId" to adId, "actionType" to actionType)
    }
}

/**
 * Where events go.
 *
 * `track` returns nothing and never throws. A screen must be able to call it on any line without
 * wondering what happens if the network is down, and nothing a person is doing may ever wait on
 * a measurement of them doing it.
 */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/**
 * The tracker for builds that measure nothing: tests, previews, and any flavour that has not been
 * given a destination. It exists so a screen's call site is identical whether or not anything is
 * listening.
 */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}
