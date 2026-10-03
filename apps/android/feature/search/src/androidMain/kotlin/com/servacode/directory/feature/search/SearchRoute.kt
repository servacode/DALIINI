package com.servacode.directory.feature.search

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.servacode.directory.core.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltSearchViewModel @Inject constructor(
    search: SearchUseCase,
    analytics: AnalyticsTracker,
) : SearchViewModel(search, analytics)

/** Screen 06 as the app's navigation opens it. */
@Composable
fun SearchRoute(
    onFacility: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HiltSearchViewModel = hiltViewModel(),
) {
    SearchScreen(viewModel, onFacility, onBack)
}
