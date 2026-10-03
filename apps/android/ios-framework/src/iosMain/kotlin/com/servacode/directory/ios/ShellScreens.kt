package com.servacode.directory.ios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.feature.facility.FacilityReportViewModel
import com.servacode.directory.feature.facility.FacilityScreen
import com.servacode.directory.feature.facility.FacilityViewModel
import com.servacode.directory.feature.home.HomeExtrasViewModel
import com.servacode.directory.feature.home.HomeScreen
import com.servacode.directory.feature.home.HomeViewModel
import com.servacode.directory.feature.province.ProvinceScreen
import com.servacode.directory.feature.province.ProvinceViewModel
import com.servacode.directory.feature.search.SearchScreen
import com.servacode.directory.feature.search.SearchViewModel

/**
 * The iPhone app's screens: Android's own, shared, on the shared design system (DECISIONS 094
 * to 097), with the shell's navigation until the app's places are shared too. What a shared
 * screen opens that has not moved yet (the notices, the emergency numbers, signing in) opens
 * nothing for now (ROADMAP ٨).
 */
@Composable
internal fun ShellApp(graph: ShellGraph) {
    DirectoryTheme {
        val preferences by graph.preferences.values.collectAsState(initial = null)
        val navigation = remember { ShellNavigation() }
        if ((preferences ?: return@DirectoryTheme).selectedProvinceId == null) {
            ShellProvince(graph, onChosen = navigation::restart)
            return@DirectoryTheme
        }
        val place = navigation.current
        // Each place's view models live in its own store, let go when the place is left.
        val owner = remember(place) {
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = navigation.store(place)
            }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
            when (place) {
                ShellPlace.Home -> ShellHome(graph, navigation)
                ShellPlace.Province -> ShellProvince(graph, onChosen = navigation::restart, onBack = navigation::back)
                ShellPlace.Search -> ShellSearch(graph, navigation)
                is ShellPlace.Facility -> ShellFacility(graph, place.id, onBack = navigation::back)
            }
        }
    }
}

@Composable
private fun ShellProvince(graph: ShellGraph, onChosen: () -> Unit, onBack: (() -> Unit)? = null) {
    val model = viewModel { ProvinceViewModel(graph.provinces) }
    ProvinceScreen(model, onSelected = onChosen, onBack = onBack)
}

@Composable
private fun ShellHome(graph: ShellGraph, navigation: ShellNavigation) {
    val model = viewModel { HomeViewModel(graph.home, graph.homeAds, graph.invalidations, graph.analytics) }
    val extras = viewModel { HomeExtrasViewModel(graph.recentlyViewed, graph.preferences, graph.network) }
    HomeScreen(
        model,
        extras,
        onProvince = { navigation.open(ShellPlace.Province) },
        onSearch = { navigation.open(ShellPlace.Search) },
        onFacility = { navigation.open(ShellPlace.Facility(it)) },
        onNotifications = {},
    )
}

@Composable
private fun ShellSearch(graph: ShellGraph, navigation: ShellNavigation) {
    val model = viewModel { SearchViewModel(graph.search, graph.analytics) }
    SearchScreen(model, onFacility = { navigation.open(ShellPlace.Facility(it)) }, onBack = navigation::back)
}

/** Screen 08. Its actions go to the phone's own apps (IosActions). */
@Composable
private fun ShellFacility(graph: ShellGraph, id: String, onBack: () -> Unit) {
    val model = viewModel {
        FacilityViewModel(id, graph.facility, graph.invalidations, graph.session, graph.recordVisit, graph.analytics)
    }
    val report = viewModel { FacilityReportViewModel(graph.reportFacility) }
    val screen = LocalUIViewController.current
    FacilityScreen(
        model,
        report,
        onDirections = IosActions::directions,
        onSignIn = {},
        onCall = IosActions::call,
        onWhatsApp = IosActions::open,
        onShare = { name -> IosActions.share(screen, shareText(name, id, graph.appLinkHost)) },
        onBack = onBack,
    )
}

/**
 * What is shared for a facility, as Android shares it: its name, and the site's own address for
 * it, which opens the app where it is installed. A build without the site's host shares the name.
 */
internal fun shareText(name: String, id: String, appLinkHost: String): String =
    if (appLinkHost.isBlank()) name else "$name\nhttps://$appLinkHost/f/$id"
