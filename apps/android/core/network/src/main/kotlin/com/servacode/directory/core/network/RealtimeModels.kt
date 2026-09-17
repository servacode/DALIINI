package com.servacode.directory.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RealtimeScope(
    val type: String,
    val id: String,
)

@Serializable
data class RealtimeEnvelope(
    val version: Int,
    val name: String,
    val scope: RealtimeScope,
    @SerialName("resourceId") val resourceId: String? = null,
    @SerialName("occurredAt") val occurredAt: String,
) {
    init {
        require(version == 1) { "Unsupported realtime event version." }
        require(name in RealtimeEventCatalog.allowed) { "Unsupported realtime event name." }
        require(scope.type in setOf("province", "user", "admin")) { "Unsupported realtime scope." }
        require(scope.id.isNotBlank()) { "Realtime scope id is required." }
        require(occurredAt.isNotBlank()) { "Realtime occurrence time is required." }
    }
}

object RealtimeEventCatalog {
    val allowed = setOf(
        "public.province.configuration_changed",
        "public.facility.changed",
        "public.facility.availability_changed",
        "public.duty.changed",
        "user.application.changed",
        "user.facility.changed",
        "admin.review_queue.changed",
        "admin.system.changed",
    )
}

sealed interface RealtimeSignal {
    data object Connected : RealtimeSignal
    data class Event(val value: RealtimeEnvelope) : RealtimeSignal
}
