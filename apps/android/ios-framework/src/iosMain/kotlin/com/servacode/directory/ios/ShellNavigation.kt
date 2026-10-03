package com.servacode.directory.ios

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModelStore

/** A place in the iPhone app: the screens shared so far (DECISIONS 095 to 099). */
internal sealed interface ShellPlace {
    data object Home : ShellPlace
    data object Province : ShellPlace
    data object Search : ShellPlace
    data class Facility(val id: String) : ShellPlace
    /** The account tab: signing in while signed out, the account once signed in. */
    data object Account : ShellPlace
    data object Login : ShellPlace
    data object Register : ShellPlace
    data object Recovery : ShellPlace
    data object Favorites : ShellPlace
    data object Notifications : ShellPlace
    data object EditProfile : ShellPlace
    data object ChangePhone : ShellPlace
    data object RecentlyViewed : ShellPlace
    data object Ratings : ShellPlace
    data class Duty(val facilityId: String, val date: String?) : ShellPlace
    /** The owner's tab, for an account that has a facility to manage. */
    data object MyFacilities : ShellPlace
    data class ManageFacility(val id: String) : ShellPlace
    data object Invitations : ShellPlace
    data object ClaimSearch : ShellPlace
    data class Claim(val id: String) : ShellPlace
}

/**
 * The shell's back stack, until the app's navigation is shared too. Each place on it keeps its
 * view models in a store of its own, cleared when the place is left, as Android's navigation
 * clears a destination's: search keeps its results while a facility opened from it is shown,
 * and a facility left is let go.
 */
internal class ShellNavigation {
    private val stack = mutableStateListOf<ShellPlace>(ShellPlace.Home)
    private val stores = mutableMapOf<ShellPlace, ViewModelStore>()

    val current: ShellPlace get() = stack.last()
    val places: List<ShellPlace> get() = stack.toList()

    fun open(place: ShellPlace) {
        if (place != current) stack += place
    }

    /** Leaves the current place; a tab's own place is never left. */
    fun back() {
        if (stack.size > 1) stores.remove(stack.removeAt(stack.lastIndex))?.clear()
    }

    /** Back to a new home, as after choosing another province: every place's state is let go. */
    fun restart() = tab(ShellPlace.Home)

    /** A tab of the bar along the bottom, opened afresh as Android's bar opens its places. */
    fun tab(root: ShellPlace) {
        stores.values.forEach { it.clear() }
        stores.clear()
        stack.clear()
        stack += root
    }

    /** Leaves the current place for [place], as Android pops a screen to open the next. */
    fun replace(place: ShellPlace) {
        back()
        open(place)
    }

    fun store(place: ShellPlace): ViewModelStore = stores.getOrPut(place) { ViewModelStore() }
}
