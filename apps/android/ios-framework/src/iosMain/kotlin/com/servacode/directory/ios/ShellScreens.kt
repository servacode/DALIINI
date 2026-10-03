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
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.designsystem.DirectoryBottomBar
import com.servacode.directory.core.designsystem.DirectoryDestination
import com.servacode.directory.core.designsystem.DirectoryGlyph
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.core.designsystem.DirectoryWords
import com.servacode.directory.feature.account.AccountScreen
import com.servacode.directory.feature.account.AccountViewModel
import com.servacode.directory.feature.account.FavoritesScreen
import com.servacode.directory.feature.account.FavoritesViewModel
import com.servacode.directory.feature.account.InboxViewModel
import com.servacode.directory.feature.account.NotificationsScreen
import com.servacode.directory.feature.account.PhoneChangeScreen
import com.servacode.directory.feature.account.PhoneChangeViewModel
import com.servacode.directory.feature.account.ProfileEditScreen
import com.servacode.directory.feature.account.ProfileEditViewModel
import com.servacode.directory.feature.account.RecentlyViewedScreen
import com.servacode.directory.feature.account.RecentlyViewedViewModel
import com.servacode.directory.feature.auth.LoginScreen
import com.servacode.directory.feature.auth.LoginViewModel
import com.servacode.directory.feature.auth.RecoveryScreen
import com.servacode.directory.feature.auth.RecoveryViewModel
import com.servacode.directory.feature.auth.RegisterScreen
import com.servacode.directory.feature.auth.RegisterViewModel
import com.servacode.directory.feature.duty.DutyScreen
import com.servacode.directory.feature.duty.DutyViewModel
import com.servacode.directory.feature.duty.LoadDutyUseCase
import com.servacode.directory.feature.duty.ManageDutyUseCase
import com.servacode.directory.feature.facility.FacilityReportViewModel
import com.servacode.directory.feature.facility.FacilityScreen
import com.servacode.directory.feature.facility.FacilityViewModel
import com.servacode.directory.feature.home.HomeExtrasViewModel
import com.servacode.directory.feature.home.HomeScreen
import com.servacode.directory.feature.home.HomeViewModel
import com.servacode.directory.feature.owner.ClaimScreen
import com.servacode.directory.feature.owner.ClaimSearchScreen
import com.servacode.directory.feature.owner.ClaimSearchViewModel
import com.servacode.directory.feature.owner.ClaimViewModel
import com.servacode.directory.feature.owner.FacilityTagsViewModel
import com.servacode.directory.feature.owner.HoursConfirmationViewModel
import com.servacode.directory.feature.owner.InvitationsScreen
import com.servacode.directory.feature.owner.InvitationsViewModel
import com.servacode.directory.feature.owner.ManageFacilityScreen
import com.servacode.directory.feature.owner.ManageFacilityViewModel
import com.servacode.directory.feature.owner.MyFacilitiesScreen
import com.servacode.directory.feature.owner.MyFacilitiesViewModel
import com.servacode.directory.feature.owner.OwnerInsightsViewModel
import com.servacode.directory.feature.owner.OwnerPresenceViewModel
import com.servacode.directory.feature.province.ProvinceScreen
import com.servacode.directory.feature.province.ProvinceViewModel
import com.servacode.directory.feature.ratings.RatingsScreen
import com.servacode.directory.feature.ratings.RatingsViewModel
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
                is ShellPlace.Facility -> ShellFacility(graph, place.id, navigation)
                else -> ShellAccountPlace(graph, place, navigation)
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
        onNotifications = { navigation.open(ShellPlace.Notifications) },
        bottomBar = { ShellTabs(graph, ShellPlace.Home, navigation) },
    )
}

@Composable
private fun ShellSearch(graph: ShellGraph, navigation: ShellNavigation) {
    val model = viewModel { SearchViewModel(graph.search, graph.analytics) }
    SearchScreen(model, onFacility = { navigation.open(ShellPlace.Facility(it)) }, onBack = navigation::back)
}

/** Screen 08. Its actions go to the phone's own apps (IosActions). */
@Composable
private fun ShellFacility(graph: ShellGraph, id: String, navigation: ShellNavigation) {
    val model = viewModel {
        FacilityViewModel(id, graph.facility, graph.invalidations, graph.session, graph.recordVisit, graph.analytics)
    }
    val report = viewModel { FacilityReportViewModel(graph.reportFacility) }
    val screen = LocalUIViewController.current
    FacilityScreen(
        model,
        report,
        onDirections = IosActions::directions,
        onSignIn = { navigation.open(ShellPlace.Login) },
        onCall = IosActions::call,
        onWhatsApp = IosActions::open,
        onShare = { name -> IosActions.share(screen, shareText(name, id, graph.appLinkHost)) },
        onBack = navigation::back,
    )
}

/**
 * The bar along the bottom: the tabs whose screens are shared, the owner's only for an account
 * with a facility to manage, as on Android.
 */
@Composable
private fun ShellTabs(graph: ShellGraph, current: ShellPlace, navigation: ShellNavigation) {
    val presence = viewModel { OwnerPresenceViewModel(graph.ownerFacilities, graph.session) }
    val ownsFacility by presence.ownsFacility.collectAsState()
    fun tab(place: ShellPlace, label: String, icon: DirectoryGlyph) =
        DirectoryDestination(label, icon, current == place) { if (current != place) navigation.tab(place) }
    DirectoryBottomBar(
        listOfNotNull(
            tab(ShellPlace.Home, DirectoryWords.TAB_HOME, DirectoryIcons.home),
            if (ownsFacility) {
                tab(ShellPlace.MyFacilities, DirectoryWords.TAB_FACILITIES, DirectoryIcons.hospital)
            } else {
                null
            },
            tab(ShellPlace.Account, DirectoryWords.TAB_ACCOUNT, DirectoryIcons.person),
        ),
    )
}

/**
 * The account's places (DECISION-098), opened as Android's navigation opens them. What they open
 * that has not moved yet (an owner's facilities, settings, help) opens nothing for now.
 */
@Composable
private fun ShellAccountPlace(graph: ShellGraph, place: ShellPlace, navigation: ShellNavigation) {
    val session by graph.session.state.collectAsState()
    val openFacility: (String) -> Unit = { navigation.open(ShellPlace.Facility(it)) }
    when (place) {
        // Signed out, the tab is signing in; there is no account to show.
        ShellPlace.Account -> if (session != SessionState.SIGNED_IN) {
            LoginScreen(
                viewModel { LoginViewModel(graph.auth) },
                onSignedIn = {},
                onRegister = { navigation.open(ShellPlace.Register) },
                onRecovery = { navigation.open(ShellPlace.Recovery) },
                bottomBar = { ShellTabs(graph, ShellPlace.Account, navigation) },
            )
        } else {
            AccountScreen(
                viewModel { AccountViewModel(graph.account, graph.deleteAccount) },
                onFacilities = { navigation.open(ShellPlace.MyFacilities) },
                onJoinAsOwner = {},
                onAccountDeleted = navigation::restart,
                onEditProfile = { navigation.open(ShellPlace.EditProfile) },
                onFavorites = { navigation.open(ShellPlace.Favorites) },
                onNotifications = { navigation.open(ShellPlace.Notifications) },
                onSettings = {},
                onHelp = {},
                bottomBar = { ShellTabs(graph, ShellPlace.Account, navigation) },
                onRecentlyViewed = { navigation.open(ShellPlace.RecentlyViewed) },
                onMyRatings = { navigation.open(ShellPlace.Ratings) },
                onInvitations = { navigation.open(ShellPlace.Invitations) },
                onClaim = { navigation.open(ShellPlace.ClaimSearch) },
            )
        }
        ShellPlace.Login -> LoginScreen(
            viewModel { LoginViewModel(graph.auth) },
            onSignedIn = navigation::back,
            onRegister = { navigation.open(ShellPlace.Register) },
            onRecovery = { navigation.open(ShellPlace.Recovery) },
            onBack = navigation::back,
        )
        ShellPlace.Register -> RegisterScreen(
            viewModel { RegisterViewModel(graph.auth, graph.publicApi, graph.preferences, graph.location) },
            onRegistered = { navigation.tab(ShellPlace.Account) },
            onBack = navigation::back,
        )
        ShellPlace.Recovery -> RecoveryScreen(
            viewModel { RecoveryViewModel(graph.auth) },
            onDone = navigation::back,
            onBack = navigation::back,
        )
        ShellPlace.Favorites -> FavoritesScreen(
            viewModel { FavoritesViewModel(graph.saved) },
            onFacility = openFacility,
            onBack = navigation::back,
        )
        ShellPlace.Notifications -> NotificationsScreen(
            viewModel { InboxViewModel(graph.saved) },
            onFacility = openFacility,
            onOwnerFacilities = { navigation.open(ShellPlace.MyFacilities) },
            onDuty = { id, date -> navigation.open(ShellPlace.Duty(id, date)) },
            onManageFacility = { navigation.open(ShellPlace.ManageFacility(it)) },
            onInvitations = { navigation.open(ShellPlace.Invitations) },
            onBack = navigation::back,
        )
        ShellPlace.EditProfile -> ProfileEditScreen(
            viewModel { ProfileEditViewModel(graph.account) },
            viewModel { AccountViewModel(graph.account, graph.deleteAccount) },
            onDone = navigation::back,
            onBack = navigation::back,
            onChangePhone = { navigation.open(ShellPlace.ChangePhone) },
        )
        // Every session ended, this one included: the account tab is signing in again.
        ShellPlace.ChangePhone -> PhoneChangeScreen(
            viewModel { PhoneChangeViewModel(graph.account) },
            onChanged = { navigation.tab(ShellPlace.Account) },
            onBack = navigation::back,
        )
        ShellPlace.RecentlyViewed -> RecentlyViewedScreen(
            viewModel { RecentlyViewedViewModel(graph.recentlyViewed) },
            onFacility = openFacility,
            onBack = navigation::back,
        )
        ShellPlace.Ratings -> RatingsScreen(viewModel { RatingsViewModel(graph.ratings) }, onBack = navigation::back)
        is ShellPlace.Duty -> DutyScreen(
            viewModel {
                DutyViewModel(place.facilityId, place.date, LoadDutyUseCase(graph.duty), ManageDutyUseCase(graph.duty))
            },
            onBack = navigation::back,
        )
        else -> ShellOwnerPlace(graph, place, navigation)
    }
}

/**
 * The owner's places (DECISION-099). Adding a facility and editing its details are onboarding,
 * which moves with the map, and open nothing on the iPhone for now.
 */
@Composable
private fun ShellOwnerPlace(graph: ShellGraph, place: ShellPlace, navigation: ShellNavigation) {
    when (place) {
        ShellPlace.MyFacilities -> MyFacilitiesScreen(
            viewModel { MyFacilitiesViewModel(graph.ownerFacilities, graph.claims, graph.invalidations) },
            onAdd = {},
            onManage = { navigation.open(ShellPlace.ManageFacility(it)) },
            onDuty = { navigation.open(ShellPlace.Duty(it, null)) },
            onBack = if (navigation.places.size > 1) navigation::back else null,
            bottomBar = { ShellTabs(graph, ShellPlace.MyFacilities, navigation) },
            onClaim = { navigation.open(ShellPlace.ClaimSearch) },
            onOpenClaim = { navigation.open(ShellPlace.Claim(it)) },
        )
        is ShellPlace.ManageFacility -> ManageFacilityScreen(
            viewModel {
                ManageFacilityViewModel(place.id, graph.loadManaged, graph.manageFacility, graph.invalidations)
            },
            viewModel { OwnerInsightsViewModel(graph.insights) },
            viewModel { HoursConfirmationViewModel(graph.confirmHours) },
            viewModel { FacilityTagsViewModel(graph.tagChoices, graph.manageFacility, graph.network) },
            onEdit = {},
            onDuty = { navigation.open(ShellPlace.Duty(it, null)) },
            onBack = navigation::back,
        )
        // Joined, the facility is the account's to manage; the invitations are behind it.
        ShellPlace.Invitations -> InvitationsScreen(
            viewModel { InvitationsViewModel(graph.receivedInvitations) },
            onJoined = { navigation.replace(ShellPlace.ManageFacility(it)) },
            onBack = navigation::back,
        )
        ShellPlace.ClaimSearch -> ClaimSearchScreen(
            viewModel { ClaimSearchViewModel(graph.claims) },
            onClaim = { navigation.replace(ShellPlace.Claim(it)) },
            onAdd = {},
            onBack = navigation::back,
        )
        is ShellPlace.Claim -> ClaimScreen(
            viewModel { ClaimViewModel(place.id, graph.claims) },
            onWithdrawn = navigation::back,
            onReopened = { navigation.replace(ShellPlace.Claim(it)) },
            onBack = navigation::back,
        )
        else -> Unit
    }
}

/**
 * What is shared for a facility, as Android shares it: its name, and the site's own address for
 * it, which opens the app where it is installed. A build without the site's host shares the name.
 */
internal fun shareText(name: String, id: String, appLinkHost: String): String =
    if (appLinkHost.isBlank()) name else "$name\nhttps://$appLinkHost/f/$id"
