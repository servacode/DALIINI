package com.servacode.directory.core.network

/**
 * Which realtime events make which screen fetch again.
 *
 * An event is only a signal. Its payload names what changed, never the new state, so the
 * answer to every event is a REST fetch — never an edit of local data from the event.
 */
object RealtimeInvalidation {
    private val facilityEvents = setOf(
        "public.facility.changed",
        "public.facility.availability_changed",
        "public.duty.changed",
    )

    /** Home, directory and map lists of the province the user is browsing. */
    fun refreshesProvinceLists(event: RealtimeEnvelope, provinceId: String?): Boolean =
        event.scope.type == "province" &&
            event.name.startsWith("public.") &&
            provinceId != null &&
            event.scope.id == provinceId

    /** The detail screen of one facility. */
    fun refreshesFacility(event: RealtimeEnvelope, facilityId: String): Boolean =
        event.scope.type == "province" && event.name in facilityEvents && event.resourceId == facilityId

    /** The signed-in owner's own facilities and applications. */
    fun refreshesOwnerState(event: RealtimeEnvelope): Boolean =
        event.scope.type == "user" &&
            (event.name == "user.facility.changed" || event.name == "user.application.changed")
}
