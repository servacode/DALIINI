package com.servacode.directory.ios

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModelStore

/** A place in the iPhone app: the screens shared so far (DECISIONS 095 and 096). */
internal sealed interface ShellPlace {
    data object Home : ShellPlace
    data object Province : ShellPlace
    data object Search : ShellPlace
    data class Facility(val id: String) : ShellPlace
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

    /** Leaves the current place; the home is never left. */
    fun back() {
        if (stack.size > 1) stores.remove(stack.removeAt(stack.lastIndex))?.clear()
    }

    /** Back to a new home, as after choosing another province: every place's state is let go. */
    fun restart() {
        stores.values.forEach { it.clear() }
        stores.clear()
        stack.clear()
        stack += ShellPlace.Home
    }

    fun store(place: ShellPlace): ViewModelStore = stores.getOrPut(place) { ViewModelStore() }
}
