package com.servacode.directory.feature.owner

import com.servacode.directory.core.model.OwnerFacilitySummary

/**
 * Which owner controls a facility gets, read from the capabilities the backend serves with
 * the facility itself (INT-056). The backend still refuses anything the category does not
 * support; hiding the control only spares the owner the refusal.
 */
object OwnerCapabilities {
    fun supportsDuty(facility: OwnerFacilitySummary): Boolean = facility.capabilities.supportsDuty

    fun supportsTemporaryClosure(facility: OwnerFacilitySummary): Boolean =
        facility.capabilities.supportsTemporaryClosure
}
