package com.servacode.directory.feature.home

import com.servacode.directory.core.location.LocationFix
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.DirectorySort

/**
 * The four chips above the list, and what they mean together.
 *
 * They are not four alternatives. "Nearest" orders, while "open now" and "on duty today"
 * narrow, and a reader looking for somewhere to go tonight wants all three at once: the closest
 * pharmacies that are on tonight's roster and whose doors are open. Making them exclusive would
 * answer only the last one tapped.
 *
 * Nothing is decided here beyond which flags to send. The filtering, the ordering and the
 * paging all stay with the backend, so a filtered Home is the same list as every other list in
 * the app.
 */
enum class HomeChip {
    /** Orders by distance. Needs a position, and is offered only when there is one. */
    NEAREST,

    /** Open at this moment. Says nothing about duty. */
    OPEN_NOW,

    /**
     * On today's duty roster. Offered only for a category whose facilities keep one, and
     * deliberately not the same question as being open right now.
     */
    DUTY_TODAY,
}

data class HomeFilters(
    val nearest: Boolean = false,
    val openNow: Boolean = false,
    val dutyToday: Boolean = false,
    /**
     * The one specialty the list is narrowed to, by its id; null is every specialty. One at a
     * time, because the backend narrows by one.
     */
    val specialtyId: String? = null,
    /** The one service the list is narrowed to; null is every service. */
    val serviceTagId: String? = null,
) {
    /**
     * Nothing narrowed and nothing reordered: the whole province, as the backend lists it.
     *
     * This is the state Home opens in, and it is why there is no "all" chip. "All" was never a
     * filter — it was the absence of the other three — so drawing it meant a chip that could
     * only ever undo the rest, costing a quarter of the row to say what an empty row already
     * says. Turning the last chip off is how one gets back here.
     */
    val isAll: Boolean get() = !nearest && !openNow && !dutyToday && !hasTags

    /** A specialty or a service narrows the list. */
    val hasTags: Boolean get() = specialtyId != null || serviceTagId != null

    /**
     * Narrow to [id], or let it go when it is the one already chosen. Null is «كل التخصصات»:
     * every specialty.
     */
    fun chooseSpecialty(id: String?): HomeFilters = copy(specialtyId = id?.takeIf { it != specialtyId })

    /** The same for a service; null is «كل الخدمات». */
    fun chooseService(id: String?): HomeFilters = copy(serviceTagId = id?.takeIf { it != serviceTagId })

    /** «امسح التصفية»: every specialty and every service again. The other chips stay. */
    fun clearTags(): HomeFilters = copy(specialtyId = null, serviceTagId = null)

    /**
     * Drop a specialty or a service the category does not offer now. A choice retired since the
     * row was drawn would narrow the list to nothing, and by a chip nobody can see any more.
     */
    fun withinTags(offered: CategoryTags): HomeFilters = copy(
        specialtyId = specialtyId?.takeIf { id -> offered.specialties.any { it.id == id } },
        serviceTagId = serviceTagId?.takeIf { id -> offered.services.any { it.id == id } },
    )

    fun isOn(chip: HomeChip): Boolean = when (chip) {
        HomeChip.NEAREST -> nearest
        HomeChip.OPEN_NOW -> openNow
        HomeChip.DUTY_TODAY -> dutyToday
    }

    /** Each chip turns just itself on or off; they narrow together. */
    fun toggle(chip: HomeChip): HomeFilters = when (chip) {
        HomeChip.NEAREST -> copy(nearest = !nearest)
        HomeChip.OPEN_NOW -> copy(openNow = !openNow)
        HomeChip.DUTY_TODAY -> copy(dutyToday = !dutyToday)
    }

    /**
     * Drop what the current situation cannot honour.
     *
     * A remembered "nearest" must not survive the position being lost, and "on duty today"
     * must not survive a move to a category that keeps no roster — otherwise the list would be
     * narrowed by a chip the user can no longer see. A specialty or a service goes the same way
     * with a category that does not filter by it.
     */
    fun withinReach(hasLocation: Boolean, category: Category?): HomeFilters = copy(
        nearest = nearest && hasLocation,
        dutyToday = dutyToday && HomeChip.DUTY_TODAY.isOffered(hasLocation, category),
        specialtyId = specialtyId?.takeIf { category?.capabilities?.supportsSpecialtyFilter == true },
        serviceTagId = serviceTagId?.takeIf { category?.capabilities?.supportsServiceFilter == true },
    )

    /**
     * The query these chips mean.
     *
     * Coordinates go whenever they are known, whatever is selected, because they are what
     * produces the distance on a row — the ordering is a separate question, and `sort` is what
     * answers it.
     */
    fun query(provinceId: String, categoryId: String?, fix: LocationFix?): DirectoryQuery =
        DirectoryQuery(
            provinceId = provinceId,
            categoryId = categoryId,
            openNow = openNow,
            dutyToday = dutyToday,
            sort = if (nearest) DirectorySort.NEAREST else DirectorySort.NAME,
            latitude = fix?.latitude,
            longitude = fix?.longitude,
            specialtyId = specialtyId,
            serviceTagId = serviceTagId,
        )
}

/**
 * The specialties and services [category] lets its list be narrowed by: what it offers, and only
 * where its own capabilities say it filters by them. The rows follow the backend's flags rather
 * than a name, so a category that starts filtering by service needs no change here.
 */
fun offeredTags(category: Category?, tags: CategoryTags): CategoryTags = CategoryTags(
    specialties = if (category?.capabilities?.supportsSpecialtyFilter == true) tags.specialties else emptyList(),
    services = if (category?.capabilities?.supportsServiceFilter == true) tags.services else emptyList(),
)

/** Whether [category] filters by either at all; without one there is nothing to ask the backend for. */
fun filtersByTags(category: Category?): Boolean = category?.capabilities?.let {
    it.supportsSpecialtyFilter || it.supportsServiceFilter
} == true

/**
 * Whether a chip is offered at all.
 *
 * A chip that cannot do anything is worse than a missing one: it invites a tap and then either
 * lies or does nothing. Duty follows the category's own capability, which the backend declares,
 * so a category that gains or loses a roster needs no change here.
 */
fun HomeChip.isOffered(hasLocation: Boolean, category: Category?): Boolean = when (this) {
    HomeChip.OPEN_NOW -> true
    HomeChip.NEAREST -> hasLocation
    HomeChip.DUTY_TODAY -> category?.capabilities?.supportsDuty == true
}

/** The chips to draw, in the order they are read. */
fun homeChips(hasLocation: Boolean, category: Category?): List<HomeChip> =
    HomeChip.entries.filter { it.isOffered(hasLocation, category) }
