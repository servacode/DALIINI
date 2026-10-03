package com.servacode.directory.feature.facility

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.servacode.directory.core.analytics.AnalyticsTracker
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it: the facility from the route. */
@HiltViewModel
class HiltFacilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    facility: FacilityUseCase,
    invalidations: RealtimeInvalidationBus,
    session: SessionCoordinator,
    recordVisit: RecordVisitUseCase,
    analytics: AnalyticsTracker,
) : FacilityViewModel(
    savedStateHandle.toRoute<DirectoryRoute.FacilityDetailRoute>().id,
    facility,
    invalidations,
    session,
    recordVisit,
    analytics,
)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltFacilityReportViewModel @Inject constructor(
    report: ReportFacilityUseCase,
) : FacilityReportViewModel(report)

/** Screen 08 as the app's navigation opens it. */
@Composable
fun FacilityRoute(
    onDirections: (Double, Double) -> Unit,
    onSignIn: () -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onShare: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HiltFacilityViewModel = hiltViewModel(),
    reportViewModel: HiltFacilityReportViewModel = hiltViewModel(),
) {
    FacilityScreen(viewModel, reportViewModel, onDirections, onSignIn, onCall, onWhatsApp, onShare, onBack)
}
