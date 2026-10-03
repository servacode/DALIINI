package com.servacode.directory.feature.owner

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.network.NetworkMonitor
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltOwnerPresenceViewModel @Inject constructor(
    facilities: LoadOwnerFacilitiesUseCase,
    session: SessionCoordinator,
) : OwnerPresenceViewModel(facilities, session)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltOwnerInsightsViewModel @Inject constructor(
    load: LoadOwnerInsightsUseCase,
) : OwnerInsightsViewModel(load)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltFacilityTagsViewModel @Inject constructor(
    choices: LoadTagChoicesUseCase,
    manage: ManageFacilityUseCase,
    network: NetworkMonitor,
) : FacilityTagsViewModel(choices, manage, network)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltMyFacilitiesViewModel @Inject constructor(
    load: LoadOwnerFacilitiesUseCase,
    claims: ClaimFacilityUseCase,
    invalidations: RealtimeInvalidationBus,
) : MyFacilitiesViewModel(load, claims, invalidations)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltManageFacilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    load: LoadManageFacilityUseCase,
    manage: ManageFacilityUseCase,
    invalidations: RealtimeInvalidationBus,
) : ManageFacilityViewModel(savedStateHandle.routeId(), load, manage, invalidations)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltInvitationsViewModel @Inject constructor(
    invitations: ReceivedInvitationsUseCase,
) : InvitationsViewModel(invitations)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltClaimSearchViewModel @Inject constructor(
    claims: ClaimFacilityUseCase,
) : ClaimSearchViewModel(claims)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltClaimViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    claims: ClaimFacilityUseCase,
) : ClaimViewModel(savedStateHandle.routeId(), claims)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltHoursConfirmationViewModel @Inject constructor(
    confirm: ConfirmHoursUseCase,
) : HoursConfirmationViewModel(confirm)

/**
 * The `id` of the route a screen was opened with: [DirectoryRoute.ManageFacility] or
 * [DirectoryRoute.Claim]. Type-safe navigation keeps each route argument in the saved state
 * under its property's name, which is what `toRoute` reads too; reading the one string directly
 * does the same without going through a Bundle.
 */
internal fun SavedStateHandle.routeId(): String =
    checkNotNull(get<String>("id")) { "The route carries no id." }

/** `MyFacilitiesScreen` as the app's navigation opens it. */
@Composable
fun MyFacilitiesRoute(
    onAdd: () -> Unit,
    onManage: (String) -> Unit,
    onDuty: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    onClaim: () -> Unit = {},
    onOpenClaim: (String) -> Unit = {},
    viewModel: HiltMyFacilitiesViewModel = hiltViewModel(),
) {
    MyFacilitiesScreen(
        viewModel = viewModel,
        onAdd = onAdd,
        onManage = onManage,
        onDuty = onDuty,
        onBack = onBack,
        bottomBar = bottomBar,
        onClaim = onClaim,
        onOpenClaim = onOpenClaim,
    )
}

/** `ManageFacilityScreen` as the app's navigation opens it. */
@Composable
fun ManageFacilityRoute(
    onEdit: (String) -> Unit,
    onDuty: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HiltManageFacilityViewModel = hiltViewModel(),
    insightsViewModel: HiltOwnerInsightsViewModel = hiltViewModel(),
    hoursViewModel: HiltHoursConfirmationViewModel = hiltViewModel(),
    tagsViewModel: HiltFacilityTagsViewModel = hiltViewModel(),
) {
    ManageFacilityScreen(
        viewModel = viewModel,
        insightsViewModel = insightsViewModel,
        hoursViewModel = hoursViewModel,
        tagsViewModel = tagsViewModel,
        onEdit = onEdit,
        onDuty = onDuty,
        onBack = onBack,
    )
}

/** `InvitationsScreen` as the app's navigation opens it. */
@Composable
fun InvitationsRoute(
    onJoined: (facilityId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: HiltInvitationsViewModel = hiltViewModel(),
) {
    InvitationsScreen(
        viewModel = viewModel,
        onJoined = onJoined,
        onBack = onBack,
    )
}

/** `ClaimSearchScreen` as the app's navigation opens it. */
@Composable
fun ClaimSearchRoute(
    onClaim: (claimId: String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    viewModel: HiltClaimSearchViewModel = hiltViewModel(),
) {
    ClaimSearchScreen(
        viewModel = viewModel,
        onClaim = onClaim,
        onAdd = onAdd,
        onBack = onBack,
    )
}

/** `ClaimScreen` as the app's navigation opens it. */
@Composable
fun ClaimRoute(
    onWithdrawn: () -> Unit,
    onReopened: (claimId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: HiltClaimViewModel = hiltViewModel(),
) {
    ClaimScreen(
        viewModel = viewModel,
        onWithdrawn = onWithdrawn,
        onReopened = onReopened,
        onBack = onBack,
    )
}
