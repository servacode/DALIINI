package com.servacode.directory.feature.account

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.servacode.directory.core.database.RecentlyViewedStore
import com.servacode.directory.core.network.PublicApiBoundary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltRecentlyViewedViewModel @Inject constructor(
    store: RecentlyViewedStore,
) : RecentlyViewedViewModel(store)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltPhoneChangeViewModel @Inject constructor(
    account: AccountUseCase,
) : PhoneChangeViewModel(account)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltProfileEditViewModel @Inject constructor(
    account: AccountUseCase,
) : ProfileEditViewModel(account)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltPasswordChangeViewModel @Inject constructor(
    api: PublicApiBoundary,
) : PasswordChangeViewModel(api)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltFavoritesViewModel @Inject constructor(
    saved: SavedRepository,
) : FavoritesViewModel(saved)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltInboxViewModel @Inject constructor(
    saved: SavedRepository,
) : InboxViewModel(saved)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltAccountViewModel @Inject constructor(
    account: AccountUseCase,
    accountDeletion: DeleteAccountUseCase,
) : AccountViewModel(account, accountDeletion)

/** `RecentlyViewedScreen` as the app's navigation opens it. */
@Composable
fun RecentlyViewedRoute(
    onFacility: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HiltRecentlyViewedViewModel = hiltViewModel(),
) {
    RecentlyViewedScreen(
        viewModel = viewModel,
        onFacility = onFacility,
        onBack = onBack,
    )
}

/** `PhoneChangeScreen` as the app's navigation opens it. */
@Composable
fun PhoneChangeRoute(
    onChanged: () -> Unit,
    onBack: () -> Unit,
    viewModel: HiltPhoneChangeViewModel = hiltViewModel(),
) {
    PhoneChangeScreen(
        viewModel = viewModel,
        onChanged = onChanged,
        onBack = onBack,
    )
}

/** `ProfileEditScreen` as the app's navigation opens it. */
@Composable
fun ProfileEditRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    onChangePhone: () -> Unit,
    viewModel: HiltProfileEditViewModel = hiltViewModel(),
    accountViewModel: HiltAccountViewModel = hiltViewModel(),
) {
    ProfileEditScreen(
        viewModel = viewModel,
        accountViewModel = accountViewModel,
        onDone = onDone,
        onBack = onBack,
        onChangePhone = onChangePhone,
    )
}

/** `PasswordChangeScreen` as the app's navigation opens it. */
@Composable
fun PasswordChangeRoute(
    onChanged: () -> Unit,
    onBack: () -> Unit,
    viewModel: HiltPasswordChangeViewModel = hiltViewModel(),
) {
    PasswordChangeScreen(
        viewModel = viewModel,
        onChanged = onChanged,
        onBack = onBack,
    )
}

/** `FavoritesScreen` as the app's navigation opens it. */
@Composable
fun FavoritesRoute(
    onFacility: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HiltFavoritesViewModel = hiltViewModel(),
) {
    FavoritesScreen(
        viewModel = viewModel,
        onFacility = onFacility,
        onBack = onBack,
    )
}

/** `NotificationsScreen` as the app's navigation opens it. */
@Composable
fun NotificationsRoute(
    onFacility: (String) -> Unit,
    onOwnerFacilities: () -> Unit,
    onDuty: (facilityId: String, date: String?) -> Unit,
    onManageFacility: (String) -> Unit,
    onInvitations: () -> Unit,
    onBack: () -> Unit,
    viewModel: HiltInboxViewModel = hiltViewModel(),
) {
    NotificationsScreen(
        viewModel = viewModel,
        onFacility = onFacility,
        onOwnerFacilities = onOwnerFacilities,
        onDuty = onDuty,
        onManageFacility = onManageFacility,
        onInvitations = onInvitations,
        onBack = onBack,
    )
}

/** `AccountScreen` as the app's navigation opens it. */
@Composable
fun AccountRoute(
    onFacilities: () -> Unit,
    onJoinAsOwner: () -> Unit,
    onAccountDeleted: () -> Unit,
    onBack: (() -> Unit)? = null,
    onEditProfile: () -> Unit,
    onFavorites: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    onRecentlyViewed: () -> Unit = {},
    onMyRatings: () -> Unit = {},
    onInvitations: () -> Unit = {},
    onClaim: () -> Unit = {},
    viewModel: HiltAccountViewModel = hiltViewModel(),
) {
    AccountScreen(
        viewModel = viewModel,
        onFacilities = onFacilities,
        onJoinAsOwner = onJoinAsOwner,
        onAccountDeleted = onAccountDeleted,
        onBack = onBack,
        onEditProfile = onEditProfile,
        onFavorites = onFavorites,
        onNotifications = onNotifications,
        onSettings = onSettings,
        onHelp = onHelp,
        bottomBar = bottomBar,
        onRecentlyViewed = onRecentlyViewed,
        onMyRatings = onMyRatings,
        onInvitations = onInvitations,
        onClaim = onClaim,
    )
}
