package com.servacode.directory.core.model

import kotlinx.serialization.Serializable

sealed interface DirectoryRoute {
    @Serializable data object Bootstrap : DirectoryRoute
    /** The first run: a welcome, then the location question, then Home. */
    @Serializable data object Welcome : DirectoryRoute
    @Serializable data object LocationPermission : DirectoryRoute
    @Serializable data object Home : DirectoryRoute
    @Serializable data object ProvincePicker : DirectoryRoute
    @Serializable data object Search : DirectoryRoute
    @Serializable data class FacilityDetailRoute(val id: String) : DirectoryRoute
    /** The public map; opened for one facility, it centres on it and shows it selected. */
    @Serializable data class Map(val focusFacilityId: String? = null) : DirectoryRoute
    /**
     * Live guidance to a point.
     *
     * [profile] is the travel mode the route was previewed in, so guidance follows the same
     * streets the preview drew rather than quietly reverting to driving. [simulated] drives the
     * trip from made-up readings instead of the receiver — a demonstration a debug build offers
     * so guidance can be watched without anyone getting into a car.
     */
    @Serializable data class BuiltInNavigation(
        val latitude: Double,
        val longitude: Double,
        val profile: String = "DRIVING",
        val simulated: Boolean = false,
    ) : DirectoryRoute
    @Serializable data object Login : DirectoryRoute
    @Serializable data object Register : DirectoryRoute
    @Serializable data object Recovery : DirectoryRoute
    @Serializable data object Account : DirectoryRoute
    @Serializable data object MyRatings : DirectoryRoute
    @Serializable data object MyFacilities : DirectoryRoute
    @Serializable data class Onboarding(val draftId: String? = null) : DirectoryRoute
    @Serializable data class ManageFacility(val id: String) : DirectoryRoute
    /**
     * An owner's duty roster. [date] ("YYYY-MM-DD") prefills a night shift on that day — a gap
     * nudge from the platform opens it this way.
     */
    @Serializable data class Duty(val id: String, val date: String? = null) : DirectoryRoute

    /** Who is on duty now, as the site's `/duty` link opens it: Home with the duty filter on. */
    @Serializable data object DutyNow : DirectoryRoute

    /** The country's and the province's emergency numbers. */
    @Serializable data object EmergencyNumbers : DirectoryRoute

    /** The facilities opened on this device, newest first. */
    @Serializable data object RecentlyViewed : DirectoryRoute
    @Serializable data object Settings : DirectoryRoute

    /** The account's own saved facilities and the messages the platform sent it. */
    @Serializable data object Favorites : DirectoryRoute
    @Serializable data object Notifications : DirectoryRoute

    /** Editing the account, and replacing its password. */
    @Serializable data object EditProfile : DirectoryRoute
    @Serializable data object ChangePassword : DirectoryRoute
    @Serializable data object ChangePhone : DirectoryRoute

    /** The platform's published pages: the list, and one of them. */
    @Serializable data object Help : DirectoryRoute
    @Serializable data class LegalPageRoute(val key: String) : DirectoryRoute
}
