package com.servacode.directory.feature.home

import com.servacode.directory.core.location.LocationFix
import com.servacode.directory.core.model.Category
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
) {
    /**
     * Nothing narrowed and nothing reordered: the whole province, as the backend lists it.
     *
     * This is the state Home opens in, and it is why there is no "all" chip. "All" was never a
     * filter — it was the absence of the other three — so drawing it meant a chip that could
     * only ever undo the rest, costing a quarter of the row to say what an empty row already
     * says. Turning the last chip off is how one gets back here.
     */
    val isAll: Boolean get() = !nearest && !openNow && !dutyToday

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
     * narrowed by a chip the user can no longer see.
     */
    fun withinReach(hasLocation: Boolean, category: Category?): HomeFilters = copy(
        nearest = nearest && hasLocation,
        dutyToday = dutyToday && HomeChip.DUTY_TODAY.isOffered(hasLocation, category),
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
        )
}

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
