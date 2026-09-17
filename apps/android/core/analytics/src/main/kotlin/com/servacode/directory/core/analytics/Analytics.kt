package com.servacode.directory.core.analytics

sealed interface AnalyticsEvent {
    val properties: Map<String, String>

    data class HomeViewed(val provinceId: String) : AnalyticsEvent {
        override val properties = mapOf("provinceId" to provinceId)
    }

    data class SearchSubmitted(val queryLength: Int, val provinceId: String) : AnalyticsEvent {
        override val properties = mapOf(
            "queryLength" to queryLength.toString(),
            "provinceId" to provinceId,
        )
    }

    data class FacilityViewed(val facilityId: String) : AnalyticsEvent {
        override val properties = mapOf("facilityId" to facilityId)
    }
}

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}
