package com.servacode.directory.core.model

import kotlinx.serialization.Serializable

sealed interface DirectoryRoute {
    @Serializable data object Bootstrap : DirectoryRoute
    @Serializable data object Home : DirectoryRoute
    @Serializable data object ProvincePicker : DirectoryRoute
    @Serializable data object Search : DirectoryRoute
    @Serializable data class Directory(val categoryId: String) : DirectoryRoute
    @Serializable data class FacilityDetailRoute(val id: String) : DirectoryRoute
    @Serializable data object Map : DirectoryRoute
    @Serializable data class BuiltInNavigation(val latitude: Double, val longitude: Double) : DirectoryRoute
    @Serializable data object Login : DirectoryRoute
    @Serializable data object Register : DirectoryRoute
    @Serializable data object Recovery : DirectoryRoute
    @Serializable data object Account : DirectoryRoute
    @Serializable data object MyRatings : DirectoryRoute
    @Serializable data object MyFacilities : DirectoryRoute
    @Serializable data class Onboarding(val draftId: String? = null) : DirectoryRoute
    @Serializable data class ManageFacility(val id: String) : DirectoryRoute
    @Serializable data class Duty(val id: String) : DirectoryRoute
    @Serializable data object Settings : DirectoryRoute
}
