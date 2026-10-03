package com.servacode.directory.ios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ShellNavigationTest {
    private class Probe : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    private object Probes : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return Probe() as T
        }
    }

    private fun ShellNavigation.probe(place: ShellPlace): Probe =
        ViewModelProvider.create(store(place), Probes)[Probe::class]

    @Test
    fun `a place left is let go and the one under it keeps its state`() {
        val navigation = ShellNavigation()
        navigation.open(ShellPlace.Search)
        val search = navigation.probe(ShellPlace.Search)
        navigation.open(ShellPlace.Facility("f-1"))
        val facility = navigation.probe(ShellPlace.Facility("f-1"))

        navigation.back()

        assertEquals(ShellPlace.Search, navigation.current)
        assertTrue(facility.cleared)
        assertSame(search, navigation.probe(ShellPlace.Search))
    }

    @Test
    fun `the home is never left and a new province starts again from it`() {
        val navigation = ShellNavigation()
        navigation.back()
        assertEquals(listOf<ShellPlace>(ShellPlace.Home), navigation.places)

        val home = navigation.probe(ShellPlace.Home)
        navigation.open(ShellPlace.Province)
        navigation.restart()

        assertEquals(listOf<ShellPlace>(ShellPlace.Home), navigation.places)
        assertTrue(home.cleared)
        assertNotSame(home, navigation.probe(ShellPlace.Home))
    }

    @Test
    fun `a tab opens afresh and its own place is never left`() {
        val navigation = ShellNavigation()
        navigation.open(ShellPlace.Search)
        val search = navigation.probe(ShellPlace.Search)

        navigation.tab(ShellPlace.Account)
        navigation.back()

        assertEquals(listOf<ShellPlace>(ShellPlace.Account), navigation.places)
        assertTrue(search.cleared)
    }

    @Test
    fun `a shared facility carries the site's link when the build has one`() {
        assertEquals("Pharmacy\nhttps://example.org/f/f-1", shareText("Pharmacy", "f-1", "example.org"))
        assertEquals("Pharmacy", shareText("Pharmacy", "f-1", " "))
    }
}
