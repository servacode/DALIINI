package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerFacilitySummary

/**
 * Which owner controls a facility gets.
 *
 * Owner facility responses do not carry their category's capabilities, so they are read from
 * the owner configuration of the facility's province. When the category is not there, the
 * control is hidden: showing duty for a category that does not support it is worse than
 * hiding it for one that does, and the backend refuses it either way.
 */
object OwnerCapabilities {
    fun supportsDuty(facility: OwnerFacilitySummary, config: OwnerConfig?): Boolean =
        config?.categories?.firstOrNull { it.category.id == facility.category.id }
            ?.capabilities?.supportsDuty == true

    fun supportsTemporaryClosure(facility: OwnerFacilitySummary, config: OwnerConfig?): Boolean =
        config?.categories?.firstOrNull { it.category.id == facility.category.id }
            ?.capabilities?.supportsTemporaryClosure == true
}

/** One of the owner's facilities with the controls it supports. */
data class OwnedFacility(
    val summary: OwnerFacilitySummary,
    val supportsDuty: Boolean,
)
