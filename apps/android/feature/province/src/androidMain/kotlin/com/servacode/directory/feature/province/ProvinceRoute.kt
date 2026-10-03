package com.servacode.directory.feature.province

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltProvinceViewModel @Inject constructor(useCase: ProvinceUseCase) : ProvinceViewModel(useCase)

/** The province picker as the app's navigation opens it. */
@Composable
fun ProvinceRoute(
    onSelected: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: HiltProvinceViewModel = hiltViewModel(),
) {
    ProvinceScreen(viewModel, onSelected, onBack)
}
