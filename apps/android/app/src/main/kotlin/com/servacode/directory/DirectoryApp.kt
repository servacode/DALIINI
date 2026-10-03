package com.servacode.directory

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.designsystem.DirectoryBottomBar
import com.servacode.directory.core.designsystem.DirectoryDestination
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryWords
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.DeepLinkTarget
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.core.model.MapNavigation
import com.servacode.directory.core.model.NotificationTarget
import com.servacode.directory.feature.account.AccountRoute
import com.servacode.directory.feature.account.FavoritesRoute
import com.servacode.directory.feature.account.NotificationsRoute
import com.servacode.directory.feature.account.PasswordChangeRoute
import com.servacode.directory.feature.account.PhoneChangeRoute
import com.servacode.directory.feature.account.ProfileEditRoute
import com.servacode.directory.feature.account.RecentlyViewedRoute
import com.servacode.directory.feature.auth.LoginRoute
import com.servacode.directory.feature.auth.RecoveryRoute
import com.servacode.directory.feature.auth.RegisterRoute
import com.servacode.directory.feature.bootstrap.BootstrapScreen
import com.servacode.directory.feature.bootstrap.LocationPermissionScreen
import com.servacode.directory.feature.bootstrap.StartDestination
import com.servacode.directory.feature.bootstrap.WelcomeScreen
import com.servacode.directory.feature.duty.DutyRoute
import com.servacode.directory.feature.duty.DutyRosterRoute
import com.servacode.directory.feature.facility.FacilityRoute
import com.servacode.directory.feature.home.HomeRoute
import com.servacode.directory.feature.map.MapScreen
import com.servacode.directory.feature.navigation.BuiltInNavigationScreen
import com.servacode.directory.feature.onboarding.OnboardingScreen
import com.servacode.directory.feature.owner.ClaimScreen
import com.servacode.directory.feature.owner.ClaimSearchScreen
import com.servacode.directory.feature.owner.InvitationsScreen
import com.servacode.directory.feature.owner.ManageFacilityScreen
import com.servacode.directory.feature.owner.MyFacilitiesScreen
import com.servacode.directory.feature.owner.OwnerPresenceViewModel
import com.servacode.directory.feature.province.ProvinceRoute
import com.servacode.directory.feature.ratings.RatingsRoute
import com.servacode.directory.feature.search.SearchRoute
import com.servacode.directory.feature.settings.HelpScreen
import com.servacode.directory.feature.settings.EmergencyNumbersScreen
import com.servacode.directory.feature.settings.LegalPageScreen
import com.servacode.directory.feature.settings.SettingsScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun DirectoryApp(
    sessionState: StateFlow<SessionState>,
    /** App Links, tapped notices and the widget, as the activity receives them. */
    entries: Flow<AppEntry> = emptyFlow(),
    entryViewModel: EntryViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val session by sessionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Where the app was asked to go, kept until there is somewhere to go from: the start screen
    // and the first run finish first, so a link that launched the app lands on top of Home.
    var pending by remember { mutableStateOf<AppEntry?>(null) }
    LaunchedEffect(entries) { entries.collect { pending = it } }
    val current by navController.currentBackStackEntryAsState()
    LaunchedEffect(pending, current?.destination) {
        val entry = pending ?: return@LaunchedEffect
        val destination = current?.destination ?: return@LaunchedEffect
        val starting = destination.hasRoute<DirectoryRoute.Bootstrap>() ||
            destination.hasRoute<DirectoryRoute.Welcome>() ||
            destination.hasRoute<DirectoryRoute.LocationPermission>()
        if (starting) return@LaunchedEffect
        pending = null
        when (entry) {
            is AppEntry.Link -> when (val target = entry.target) {
                is DeepLinkTarget.Facility ->
                    navController.navigate(DirectoryRoute.FacilityDetailRoute(target.id))
                DeepLinkTarget.DutyNow ->
                    navController.navigate(DirectoryRoute.DutyNow) { launchSingleTop = true }
                // The province the link names becomes the reader's, as if picked; Home opens on it.
                is DeepLinkTarget.Province -> scope.launch {
                    entryViewModel.selectProvince(target.code)
                    navController.navigate(DirectoryRoute.Home) {
                        popUpTo<DirectoryRoute.Home> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            is AppEntry.Notice -> {
                val route: DirectoryRoute = when (val target = entry.target) {
                    is NotificationTarget.Facility -> DirectoryRoute.FacilityDetailRoute(target.id)
                    is NotificationTarget.DutyScheduling ->
                        target.facilityId?.let { DirectoryRoute.Duty(it, target.date) } ?: DirectoryRoute.MyFacilities
                    is NotificationTarget.HoursConfirmation ->
                        target.facilityId?.let(DirectoryRoute::ManageFacility) ?: DirectoryRoute.MyFacilities
                    NotificationTarget.OwnerFacilities -> DirectoryRoute.MyFacilities
                    NotificationTarget.Invitations -> DirectoryRoute.Invitations
                    // The notice's own words are in the inbox; a push carries none.
                    NotificationTarget.None -> DirectoryRoute.Notifications
                }
                // Each of these is the account's own; signed out, signing in comes first.
                val needsAccount = route is DirectoryRoute.Duty || route is DirectoryRoute.ManageFacility ||
                    route == DirectoryRoute.MyFacilities || route == DirectoryRoute.Notifications ||
                    route == DirectoryRoute.Invitations
                val signedIn = session == SessionState.SIGNED_IN
                navController.navigate(if (needsAccount && !signedIn) DirectoryRoute.Login else route)
            }
        }
    }

    // When the session ends — signed out elsewhere, revoked, or its refresh refused — a screen
    // that shows the user's own data gives way to sign-in instead of failing on every request.
    LaunchedEffect(session) {
        if (session != SessionState.SIGNED_OUT) return@LaunchedEffect
        val destination = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect
        val private = destination.hasRoute<DirectoryRoute.MyRatings>() ||
            destination.hasRoute<DirectoryRoute.MyFacilities>() ||
            destination.hasRoute<DirectoryRoute.Onboarding>() ||
            destination.hasRoute<DirectoryRoute.ManageFacility>() ||
            destination.hasRoute<DirectoryRoute.Invitations>() ||
            destination.hasRoute<DirectoryRoute.ClaimFacility>() ||
            destination.hasRoute<DirectoryRoute.Claim>() ||
            destination.hasRoute<DirectoryRoute.Duty>()
        if (private) {
            navController.navigate(DirectoryRoute.Login) {
                popUpTo<DirectoryRoute.Home>()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = DirectoryRoute.Bootstrap,
        // One screen at a time, and no animation between them.
        //
        // Leaving these out does not mean no animation: navigation-compose fills them with a
        // long cross-fade of its own, and a cross-fade is the one shape of motion that reads as
        // a fault. Nothing moves during it — both screens sit still at full size while one
        // becomes transparent — so the eye sees the page it just left frozen over the page it
        // is returning to, which is exactly the "it took a screenshot" complaint. Measured on
        // an A52, the outgoing screen was still a visible ghost more than a second after Back.
        //
        // The answer is to draw one screen, not to pick a different animation: with None the
        // two are never composited together and there is nothing to mistake for a still image.
        // Predictive back is untouched; the system gesture still runs, this only says the app
        // does not cross-fade its own content while it does.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<DirectoryRoute.Bootstrap> {
            BootstrapScreen(
                onReady = { start ->
                    val destination = when (start) {
                        StartDestination.WELCOME -> DirectoryRoute.Welcome
                        StartDestination.HOME -> DirectoryRoute.Home
                    }
                    navController.navigate(destination) {
                        popUpTo<DirectoryRoute.Bootstrap> { inclusive = true }
                    }
                },
            )
        }
        composable<DirectoryRoute.Welcome> {
            WelcomeScreen(onContinue = { navController.navigate(DirectoryRoute.LocationPermission) })
        }
        composable<DirectoryRoute.LocationPermission> {
            LocationPermissionScreen(
                onDone = {
                    navController.navigate(DirectoryRoute.Home) {
                        // The first run is over; back from Home leaves the app, as it always did.
                        popUpTo<DirectoryRoute.Welcome> { inclusive = true }
                    }
                },
            )
        }
        composable<DirectoryRoute.Home> {
            HomeRoute(
                onProvince = { navController.navigate(DirectoryRoute.ProvincePicker) },
                onSearch = { navController.navigate(DirectoryRoute.Search) },
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onNotifications = { navController.navigate(DirectoryRoute.Notifications) },
                bottomBar = { DirectoryTabs(DirectoryTab.HOME, navController) },
                onEmergencyNumbers = { navController.navigate(DirectoryRoute.EmergencyNumbers) },
            )
        }
        composable<DirectoryRoute.ProvincePicker> {
            ProvinceRoute(
                onSelected = {
                    navController.navigate(DirectoryRoute.Home) {
                        popUpTo<DirectoryRoute.Home> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Search> {
            SearchRoute(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.FacilityDetailRoute> { backStackEntry ->
            val facilityId = backStackEntry.toRoute<DirectoryRoute.FacilityDetailRoute>().id
            FacilityRoute(
                onDirections = { latitude, longitude ->
                    // The way there is shown before it is followed; starting is the user's own
                    // decision, on the next screen.
                    navController.navigate(
                        DirectoryRoute.BuiltInNavigation(latitude, longitude),
                    )
                },
                onSignIn = { navController.navigate(DirectoryRoute.Login) },
                // The dialer opens with the number the backend published; the call is the user's.
                onCall = { phone ->
                    context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
                },
                // A wa.me link built from the facility's own WhatsApp number. With WhatsApp
                // installed it opens there; without it, its own page opens in the browser and
                // says so. A phone with nothing to open an https link does nothing.
                onWhatsApp = { link ->
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
                    } catch (_: ActivityNotFoundException) {
                        // Nothing can show it; the tap is a no-op rather than a crash.
                    }
                },
                // The site's own address for this facility, which is also the App Link that
                // opens this screen again on a phone that has the app. Whoever it is sent to
                // can read it either way, which is the point of sharing it at all.
                onShare = { name ->
                    val link = "https://${BuildConfig.APP_LINK_HOST}/f/$facilityId"
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, name)
                        putExtra(Intent.EXTRA_TEXT, "$name\n$link")
                    }
                    try {
                        context.startActivity(Intent.createChooser(send, null))
                    } catch (_: ActivityNotFoundException) {
                        // A phone with nothing to share to; the tap is a no-op rather than a crash.
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Map> {
            MapScreen(
                styleUrl = BuildConfig.MAP_STYLE_URL,
                bottomBar = { DirectoryTabs(DirectoryTab.MAP, navController) },
                onRoute = { id, latitude, longitude ->
                    navController.navigate(DirectoryRoute.BuiltInNavigation(latitude, longitude))
                },
                onFacility = { id ->
                    val below = navController.previousBackStackEntry
                        ?.takeIf { it.destination.hasRoute<DirectoryRoute.FacilityDetailRoute>() }
                        ?.toRoute<DirectoryRoute.FacilityDetailRoute>()
                    if (MapNavigation.returnsToDetail(below, id)) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(DirectoryRoute.FacilityDetailRoute(id)) { launchSingleTop = true }
                    }
                },
            )
        }
        composable<DirectoryRoute.BuiltInNavigation> { backStackEntry ->
            val route = backStackEntry.toRoute<DirectoryRoute.BuiltInNavigation>()
            BuiltInNavigationScreen(
                styleUrl = BuildConfig.MAP_STYLE_URL,
                destination = com.servacode.directory.core.maps.MapPoint(
                    route.latitude,
                    route.longitude,
                ),
                onClose = { navController.popBackStack() },
                // A trip driven by made-up readings, so that guidance and its voice can be
                // watched without a car. The gate is here and nowhere else: a release build is
                // not given the function, so the control does not exist in it.
                onSimulate = if (BuildConfig.DEBUG) {
                    { profile ->
                        // A new entry, deliberately. The screen being left is this same
                        // destination, and `simulated` is read once, when the view model is
                        // built (NavigationViewModel). Asking to stay on a single top entry
                        // hands the new arguments to the model that already exists and never
                        // reads them again, so the demonstration was requested and nothing
                        // happened: no banner, no movement, no voice. Pushing an entry builds
                        // the model that reads `simulated = true`, and Back returns to the
                        // real trip.
                        navController.navigate(
                            DirectoryRoute.BuiltInNavigation(
                                latitude = route.latitude,
                                longitude = route.longitude,
                                profile = profile.name,
                                simulated = true,
                            ),
                        )
                    }
                } else {
                    null
                },
            )
        }
        composable<DirectoryRoute.Account> {
            // Signed out, the tab is signing in — not a profile with nothing in it. There is no
            // account to show, so the page that makes one is the page the tab opens.
            if (session != SessionState.SIGNED_IN) {
                LoginRoute(
                    onSignedIn = { },
                    onRegister = { navController.navigate(DirectoryRoute.Register) },
                    onRecovery = { navController.navigate(DirectoryRoute.Recovery) },
                    // The root of a tab has nothing behind it.
                    onBack = null,
                    bottomBar = { DirectoryTabs(DirectoryTab.ACCOUNT, navController) },
                )
                return@composable
            }
            AccountRoute(
                onFacilities = { navController.navigate(DirectoryRoute.MyFacilities) },
                // Joining is adding the first facility; there is nothing else to join.
                onJoinAsOwner = { navController.navigate(DirectoryRoute.Onboarding()) },
                onAccountDeleted = {
                    navController.navigate(DirectoryRoute.Home) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                // The root of a tab has nothing behind it, signed in as well as signed out.
                onBack = null,
                onEditProfile = { navController.navigate(DirectoryRoute.EditProfile) },
                onFavorites = { navController.navigate(DirectoryRoute.Favorites) },
                onNotifications = { navController.navigate(DirectoryRoute.Notifications) },
                onSettings = { navController.navigate(DirectoryRoute.Settings) },
                onHelp = { navController.navigate(DirectoryRoute.Help) },
                bottomBar = { DirectoryTabs(DirectoryTab.ACCOUNT, navController) },
                onRecentlyViewed = { navController.navigate(DirectoryRoute.RecentlyViewed) },
                onMyRatings = { navController.navigate(DirectoryRoute.MyRatings) },
                onInvitations = { navController.navigate(DirectoryRoute.Invitations) },
                onClaim = { navController.navigate(DirectoryRoute.ClaimFacility) },
            )
        }
        composable<DirectoryRoute.Login> {
            LoginRoute(
                onSignedIn = { navController.popBackStack() },
                onRegister = { navController.navigate(DirectoryRoute.Register) },
                onRecovery = { navController.navigate(DirectoryRoute.Recovery) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Register> {
            RegisterRoute(
                onRegistered = {
                    navController.navigate(DirectoryRoute.Account) {
                        popUpTo<DirectoryRoute.Home>()
                    }
                },
                onBack = { navController.popBackStack() },
                // Signing in is the page this one was opened from.
                onSignIn = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Recovery> {
            RecoveryRoute(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Favorites> {
            FavoritesRoute(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Notifications> {
            NotificationsRoute(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onOwnerFacilities = { navController.navigate(DirectoryRoute.MyFacilities) },
                onDuty = { id, date -> navController.navigate(DirectoryRoute.Duty(id, date)) },
                onManageFacility = { navController.navigate(DirectoryRoute.ManageFacility(it)) },
                onInvitations = { navController.navigate(DirectoryRoute.Invitations) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.EditProfile> {
            ProfileEditRoute(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                onChangePhone = { navController.navigate(DirectoryRoute.ChangePhone) },
            )
        }
        composable<DirectoryRoute.ChangePhone> {
            PhoneChangeRoute(
                // Every session ended, this one included: the app goes back to signing in, and
                // the number it signs in with is now the new one.
                onChanged = {
                    navController.navigate(DirectoryRoute.Login) {
                        popUpTo<DirectoryRoute.Home>()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.ChangePassword> {
            PasswordChangeRoute(
                // Every session ended, this one included: the app goes back to signing in.
                onChanged = {
                    navController.navigate(DirectoryRoute.Login) {
                        popUpTo<DirectoryRoute.Home>()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Settings> {
            SettingsScreen(
                onChangePassword = { navController.navigate(DirectoryRoute.ChangePassword) },
                onChangePhone = { navController.navigate(DirectoryRoute.ChangePhone) },
                onBack = { navController.popBackStack() },
                signedIn = session == SessionState.SIGNED_IN,
                onEmergencyNumbers = { navController.navigate(DirectoryRoute.EmergencyNumbers) },
            )
        }
        composable<DirectoryRoute.EmergencyNumbers> {
            EmergencyNumbersScreen(onBack = { navController.popBackStack() })
        }
        composable<DirectoryRoute.RecentlyViewed> {
            RecentlyViewedRoute(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Help> {
            HelpScreen(
                onPage = { key -> navController.navigate(DirectoryRoute.LegalPageRoute(key.name)) },
                onBack = { navController.popBackStack() },
                appVersion = BuildConfig.VERSION_NAME,
            )
        }
        composable<DirectoryRoute.LegalPageRoute> { backStackEntry ->
            val key = backStackEntry.toRoute<DirectoryRoute.LegalPageRoute>().key
            LegalPageScreen(
                key = runCatching { LegalPageKey.valueOf(key) }.getOrDefault(LegalPageKey.ABOUT),
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.MyRatings> {
            RatingsRoute(onBack = { navController.popBackStack() })
        }

        composable<DirectoryRoute.MyFacilities> {
            MyFacilitiesScreen(
                onAdd = { navController.navigate(DirectoryRoute.Onboarding()) },
                onManage = { navController.navigate(DirectoryRoute.ManageFacility(it)) },
                onDuty = { navController.navigate(DirectoryRoute.Duty(it)) },
                // A place in the bar, so back leads nowhere the bar does not already go.
                onBack = null,
                bottomBar = { DirectoryTabs(DirectoryTab.FACILITIES, navController) },
                onClaim = { navController.navigate(DirectoryRoute.ClaimFacility) },
                onOpenClaim = { navController.navigate(DirectoryRoute.Claim(it)) },
            )
        }
        composable<DirectoryRoute.ClaimFacility> {
            ClaimSearchScreen(
                // The search is behind the claim it started: back from the claim is the list.
                onClaim = {
                    navController.navigate(DirectoryRoute.Claim(it)) {
                        popUpTo<DirectoryRoute.ClaimFacility> { inclusive = true }
                    }
                },
                onAdd = {
                    navController.navigate(DirectoryRoute.Onboarding()) {
                        popUpTo<DirectoryRoute.ClaimFacility> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Claim> {
            ClaimScreen(
                onWithdrawn = { navController.popBackStack() },
                onReopened = {
                    navController.navigate(DirectoryRoute.Claim(it)) {
                        popUpTo<DirectoryRoute.Claim> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Onboarding> {
            OnboardingScreen(
                styleUrl = BuildConfig.MAP_STYLE_URL,
                onChooseProvince = { navController.navigate(DirectoryRoute.ProvincePicker) },
                onDone = {
                    navController.navigate(DirectoryRoute.MyFacilities) {
                        popUpTo<DirectoryRoute.MyFacilities> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.ManageFacility> {
            ManageFacilityScreen(
                onEdit = { navController.navigate(DirectoryRoute.Onboarding(it)) },
                onDuty = { navController.navigate(DirectoryRoute.Duty(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Invitations> {
            InvitationsScreen(
                // Joined, the facility is the account's to manage; the invitations are behind it.
                onJoined = {
                    navController.navigate(DirectoryRoute.ManageFacility(it)) {
                        popUpTo<DirectoryRoute.Invitations> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Duty> {
            DutyRoute(onBack = { navController.popBackStack() })
        }
        // Who is on duty in the province today, tomorrow or this week: the site's /duty.
        composable<DirectoryRoute.DutyNow> {
            DutyRosterRoute(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onProvince = { navController.navigate(DirectoryRoute.ProvincePicker) },
                onBack = {
                    // Opened from a link, there may be nothing behind it; Home is.
                    if (!navController.popBackStack()) navController.navigate(DirectoryRoute.Home)
                },
            )
        }
    }
}

/** The app's few main places, as the bar along the bottom carries them. */
private enum class DirectoryTab { HOME, MAP, FACILITIES, ACCOUNT }

/**
 * The bottom bar.
 *
 * Three places for most people. A fourth appears for an account that has facilities of its own:
 * someone who manages a pharmacy opens its hours, its duty roster and its photographs far more
 * often than they browse the directory, and for them those facilities are a place rather than a
 * page inside a menu. For everyone else it is not drawn, because an empty tab is a promise the
 * app cannot keep.
 */
@Composable
private fun DirectoryTabs(
    current: DirectoryTab,
    navController: NavHostController,
    presence: OwnerPresenceViewModel = hiltViewModel(),
) {
    val ownsFacility by presence.ownsFacility.collectAsStateWithLifecycle()
    DirectoryBottomBar(
        listOfNotNull(
            DirectoryDestination(
                label = DirectoryWords.TAB_HOME,
                icon = DirectoryIcons.home,
                selected = current == DirectoryTab.HOME,
            ) {
                if (current != DirectoryTab.HOME) {
                    navController.navigate(DirectoryRoute.Home) {
                        popUpTo<DirectoryRoute.Home> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            DirectoryDestination(
                label = DirectoryWords.TAB_MAP,
                icon = DirectoryIcons.map,
                selected = current == DirectoryTab.MAP,
            ) {
                if (current != DirectoryTab.MAP) {
                    // One map at most, as everywhere else that opens it.
                    navController.navigate(DirectoryRoute.Map()) {
                        popUpTo<DirectoryRoute.Map> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            // Only for those who have something to manage.
            if (!ownsFacility) {
                null
            } else {
                DirectoryDestination(
                    label = DirectoryWords.TAB_FACILITIES,
                    icon = DirectoryIcons.hospital,
                    selected = current == DirectoryTab.FACILITIES,
                ) {
                    if (current != DirectoryTab.FACILITIES) {
                        navController.navigate(DirectoryRoute.MyFacilities) {
                            popUpTo<DirectoryRoute.MyFacilities> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            },
            DirectoryDestination(
                label = DirectoryWords.TAB_ACCOUNT,
                icon = DirectoryIcons.person,
                selected = current == DirectoryTab.ACCOUNT,
            ) {
                if (current != DirectoryTab.ACCOUNT) {
                    navController.navigate(DirectoryRoute.Account) {
                        popUpTo<DirectoryRoute.Account> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
        ),
    )
}
