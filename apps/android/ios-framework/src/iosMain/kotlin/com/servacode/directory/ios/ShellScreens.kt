package com.servacode.directory.ios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.feature.home.HomeExtrasViewModel
import com.servacode.directory.feature.home.HomeScreen
import com.servacode.directory.feature.home.HomeViewModel
import com.servacode.directory.feature.province.ProvinceScreen
import com.servacode.directory.feature.province.ProvinceViewModel
import com.servacode.directory.feature.search.SearchScreen
import com.servacode.directory.feature.search.SearchViewModel

/** Where the shell is: the places that are shared screens so far (DECISION-095). */
internal enum class ShellPlace { HOME, PROVINCE, SEARCH }

/**
 * The iPhone app's screens: Android's own, shared, on the shared design system (DECISIONS 094
 * and 095), with a navigation of the shell's until the app's places are shared too. What the
 * home opens that has not moved yet (a facility, the notices, the emergency numbers) opens
 * nothing for now (ROADMAP ٨).
 */
@Composable
internal fun ShellApp(graph: ShellGraph) {
    DirectoryTheme {
        val preferences by graph.preferences.values.collectAsState(initial = null)
        val provinceId = (preferences ?: return@DirectoryTheme).selectedProvinceId
        var place by remember { mutableStateOf(ShellPlace.HOME) }
        when {
            provinceId == null -> ShellProvince(graph, onChosen = { place = ShellPlace.HOME })
            place == ShellPlace.PROVINCE -> ShellProvince(
                graph,
                onChosen = { place = ShellPlace.HOME },
                onBack = { place = ShellPlace.HOME },
            )
            place == ShellPlace.SEARCH -> ShellSearch(graph, onBack = { place = ShellPlace.HOME })
            else -> ShellHome(
                graph,
                provinceId,
                onProvince = { place = ShellPlace.PROVINCE },
                onSearch = { place = ShellPlace.SEARCH },
            )
        }
    }
}

/** The shared province picker, its view model kept for as long as the app runs. */
@Composable
private fun ShellProvince(graph: ShellGraph, onChosen: () -> Unit, onBack: (() -> Unit)? = null) {
    val model = viewModel { ProvinceViewModel(graph.provinces) }
    ProvinceScreen(model, onSelected = onChosen, onBack = onBack)
}

/**
 * The shared home. Its view model reads the province when it loads, so each province has its
 * own, as Android's navigation makes a new one when the province changes.
 */
@Composable
private fun ShellHome(graph: ShellGraph, provinceId: String, onProvince: () -> Unit, onSearch: () -> Unit) {
    val model = viewModel(key = "home:$provinceId") {
        HomeViewModel(graph.home, graph.homeAds, graph.invalidations, graph.analytics)
    }
    val extras = viewModel { HomeExtrasViewModel(graph.recentlyViewed, graph.preferences, graph.network) }
    HomeScreen(
        model,
        extras,
        onProvince = onProvince,
        onSearch = onSearch,
        onFacility = {},
        onNotifications = {},
    )
}

/** The shared search. Until the app's places are shared it is one for the run, last query kept. */
@Composable
private fun ShellSearch(graph: ShellGraph, onBack: () -> Unit) {
    val model = viewModel { SearchViewModel(graph.search, graph.analytics) }
    SearchScreen(model, onFacility = {}, onBack = onBack)
}
