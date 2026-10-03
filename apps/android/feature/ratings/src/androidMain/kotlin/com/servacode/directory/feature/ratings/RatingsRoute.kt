package com.servacode.directory.feature.ratings

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltRatingsViewModel @Inject constructor(ratings: RatingsUseCase) : RatingsViewModel(ratings)

/** Screen 09 as the app's navigation opens it. */
@Composable
fun RatingsRoute(
    onBack: () -> Unit,
    viewModel: HiltRatingsViewModel = hiltViewModel(),
) {
    RatingsScreen(viewModel, onBack)
}
