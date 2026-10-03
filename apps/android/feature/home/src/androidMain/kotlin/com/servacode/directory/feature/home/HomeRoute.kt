package com.servacode.directory.feature.home

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.servacode.directory.core.analytics.AnalyticsTracker
import com.servacode.directory.core.database.RecentlyViewedStore
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.network.NetworkMonitor
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltHomeViewModel @Inject constructor(
    loadHome: HomeUseCase,
    loadAds: HomeAdsUseCase,
    invalidations: RealtimeInvalidationBus,
    analytics: AnalyticsTracker,
) : HomeViewModel(loadHome, loadAds, invalidations, analytics)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltHomeExtrasViewModel @Inject constructor(
    recentlyViewed: RecentlyViewedStore,
    preferences: DirectoryPreferencesStore,
    network: NetworkMonitor,
) : HomeExtrasViewModel(recentlyViewed, preferences, network)

/** Screen 04 as the app's navigation opens it. */
@Composable
fun HomeRoute(
    onProvince: () -> Unit,
    onSearch: () -> Unit,
    onFacility: (String) -> Unit,
    onNotifications: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    onEmergencyNumbers: () -> Unit = {},
    viewModel: HiltHomeViewModel = hiltViewModel(),
    extras: HiltHomeExtrasViewModel = hiltViewModel(),
) {
    HomeScreen(viewModel, extras, onProvince, onSearch, onFacility, onNotifications, bottomBar, onEmergencyNumbers)
}
