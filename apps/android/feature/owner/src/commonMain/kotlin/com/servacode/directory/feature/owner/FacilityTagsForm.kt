package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.FacilityTag
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.network.OwnerFacilityPatch

/**
 * «التخصصات والخدمات» on the management screen: what the category lets owners pick, what the
 * facility carries on the backend, and what the owner has ticked since.
 *
 * Saving sends only the list that changed. A list replaces the whole set on the backend and a
 * missing one leaves it alone, so ticking a service never rewrites the specialties, and an owner
 * who changed nothing sends nothing.
 */
data class FacilityTagsForm(
    val choices: CategoryTags,
    val savedSpecialties: Set<String>,
    val savedServices: Set<String>,
    val specialties: Set<String> = savedSpecialties,
    val services: Set<String> = savedServices,
) {
    val specialtiesChanged: Boolean get() = specialties != savedSpecialties
    val servicesChanged: Boolean get() = services != savedServices
    val changed: Boolean get() = specialtiesChanged || servicesChanged

    fun toggleSpecialty(id: String): FacilityTagsForm = copy(specialties = specialties.toggled(id))

    fun toggleService(id: String): FacilityTagsForm = copy(services = services.toggled(id))

    /**
     * The patch for what changed, or null when nothing did.
     *
     * Each list holds only ids the category offers, in its own order: anything else the backend
     * refuses. An empty list is sent as it is, because that is how every choice is cleared.
     */
    fun patch(): OwnerFacilityPatch? = if (!changed) {
        null
    } else {
        OwnerFacilityPatch(
            specialtyIds = if (specialtiesChanged) choices.specialties.picked(specialties) else null,
            serviceTagIds = if (servicesChanged) choices.services.picked(services) else null,
        )
    }

    /**
     * The same form once [facility] has been read again: what the backend holds now is what is
     * saved. A list the owner has not touched follows it; one they are still editing keeps their
     * ticks.
     */
    fun rebase(facility: OwnerFacilityDetail): FacilityTagsForm {
        val specialtiesNow = facility.specialtyIds.toSet()
        val servicesNow = facility.serviceTagIds.toSet()
        return copy(
            savedSpecialties = specialtiesNow,
            savedServices = servicesNow,
            specialties = if (specialtiesChanged) specialties else specialtiesNow,
            services = if (servicesChanged) services else servicesNow,
        )
    }

    /**
     * The same ticks against a fresh list of choices. A tick the owner added on a choice that is
     * no longer offered goes; what the facility already carries is left for the backend to judge.
     */
    fun withChoices(fresh: CategoryTags): FacilityTagsForm = copy(
        choices = fresh,
        specialties = specialties.filterTo(mutableSetOf()) { it in savedSpecialties || fresh.specialties.offers(it) },
        services = services.filterTo(mutableSetOf()) { it in savedServices || fresh.services.offers(it) },
    )

    companion object {
        /** The form for [facility], with what it carries already ticked. */
        fun of(facility: OwnerFacilityDetail, choices: CategoryTags) = FacilityTagsForm(
            choices = choices,
            savedSpecialties = facility.specialtyIds.toSet(),
            savedServices = facility.serviceTagIds.toSet(),
        )
    }
}

/**
 * Why a save did not go through, as the section says it.
 *
 * A refusal that names `specialtyIds` or `serviceTagIds` means a choice was retired or moved after
 * the list was read: the owner is told that and offered a fresh list, rather than a general
 * "check what you entered". Anything else is the error's own sentence.
 */
sealed interface FacilityTagsFailure {
    data object ChoicesOutdated : FacilityTagsFailure

    data class Failed(val error: AppError) : FacilityTagsFailure

    companion object {
        private val FIELDS = setOf("specialtyIds", "serviceTagIds")

        fun of(error: AppError): FacilityTagsFailure =
            if (error.kind == AppError.Kind.VALIDATION && error.fieldErrors.keys.any { it in FIELDS }) {
                ChoicesOutdated
            } else {
                Failed(error)
            }
    }
}

private fun Set<String>.toggled(id: String): Set<String> = if (id in this) this - id else this + id

private fun List<FacilityTag>.offers(id: String): Boolean = any { it.id == id }

private fun List<FacilityTag>.picked(ids: Set<String>): List<String> = filter { it.id in ids }.map { it.id }
