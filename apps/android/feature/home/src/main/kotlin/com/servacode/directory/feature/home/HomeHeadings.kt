package com.servacode.directory.feature.home

import com.servacode.directory.core.model.FacilitySummary

/** The titles of the home sections that depend on what the backend answered. */
object HomeHeadings {
    /**
     * "الأقرب إليك" only when the list is ordered by the user's location. The backend returns a
     * distance for every row exactly when it ordered by one, and leaves unplaceable facilities
     * out; without a location the rows carry no distance and the list is province-wide.
     */
    fun nearby(items: List<FacilitySummary>): String =
        if (items.isNotEmpty() && items.all { it.distanceMeters != null }) "الأقرب إليك" else "المنشآت"
}
