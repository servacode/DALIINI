package com.servacode.directory.feature.duty

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.servacode.directory.core.model.DirectoryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it: the facility and the nudged night from the route. */
@HiltViewModel
class HiltDutyViewModel private constructor(
    route: DirectoryRoute.Duty,
    load: LoadDutyUseCase,
    manage: ManageDutyUseCase,
) : DutyViewModel(route.id, route.date, load, manage) {
    @Inject constructor(
        savedStateHandle: SavedStateHandle,
        load: LoadDutyUseCase,
        manage: ManageDutyUseCase,
    ) : this(savedStateHandle.toRoute<DirectoryRoute.Duty>(), load, manage)
}

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltDutyRosterViewModel @Inject constructor(
    repository: DutyRosterRepository,
) : DutyRosterViewModel(repository)

/** The owner's duty shifts as the app's navigation opens them. */
@Composable
fun DutyRoute(
    onBack: () -> Unit,
    viewModel: HiltDutyViewModel = hiltViewModel(),
) {
    DutyScreen(viewModel, onBack)
}

/** «المناوبات» as the app's navigation opens it. */
@Composable
fun DutyRosterRoute(
    onFacility: (String) -> Unit,
    onProvince: () -> Unit,
    onBack: () -> Unit,
    viewModel: HiltDutyRosterViewModel = hiltViewModel(),
) {
    DutyRosterScreen(viewModel, onFacility, onProvince, onBack)
}
