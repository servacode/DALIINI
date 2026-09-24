package com.servacode.directory

import android.content.Intent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.designsystem.DirectoryBottomBar
import com.servacode.directory.core.designsystem.DirectoryDestination
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.MapNavigation
import com.servacode.directory.feature.auth.LoginScreen
import com.servacode.directory.feature.auth.RecoveryScreen
import com.servacode.directory.feature.auth.RegisterScreen
import kotlinx.coroutines.flow.StateFlow
import com.servacode.directory.core.model.LegalPageKey
import com.servacode.directory.feature.account.AccountScreen
import com.servacode.directory.feature.account.FavoritesScreen
import com.servacode.directory.feature.account.NotificationsScreen
import com.servacode.directory.feature.account.PasswordChangeScreen
import com.servacode.directory.feature.account.ProfileEditScreen
import com.servacode.directory.feature.settings.HelpScreen
import com.servacode.directory.feature.settings.LegalPageScreen
import com.servacode.directory.feature.settings.SettingsScreen
import com.servacode.directory.feature.bootstrap.BootstrapScreen
import com.servacode.directory.feature.bootstrap.LocationPermissionScreen
import com.servacode.directory.feature.bootstrap.StartDestination
import com.servacode.directory.feature.bootstrap.WelcomeScreen
import com.servacode.directory.feature.directory.DirectoryScreen
import com.servacode.directory.feature.owner.MyFacilitiesScreen
import com.servacode.directory.feature.owner.ManageFacilityScreen
import com.servacode.directory.feature.onboarding.OnboardingScreen
import com.servacode.directory.feature.duty.DutyScreen
import com.servacode.directory.feature.facility.FacilityScreen
import com.servacode.directory.feature.home.HomeScreen
import com.servacode.directory.feature.map.MapScreen
import com.servacode.directory.feature.navigation.BuiltInNavigationScreen
import com.servacode.directory.feature.province.ProvinceScreen
import com.servacode.directory.feature.ratings.RatingsScreen
import com.servacode.directory.feature.search.SearchScreen

@Composable
fun DirectoryApp(sessionState: StateFlow<SessionState>) {
    val navController = rememberNavController()
    val session by sessionState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // When the session ends — signed out elsewhere, revoked, or its refresh refused — a screen
    // that shows the user's own data gives way to sign-in instead of failing on every request.
    LaunchedEffect(session) {
        if (session != SessionState.SIGNED_OUT) return@LaunchedEffect
        val destination = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect
        val private = destination.hasRoute<DirectoryRoute.MyRatings>() ||
            destination.hasRoute<DirectoryRoute.MyFacilities>() ||
            destination.hasRoute<DirectoryRoute.Onboarding>() ||
            destination.hasRoute<DirectoryRoute.ManageFacility>() ||
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
            HomeScreen(
                onProvince = { navController.navigate(DirectoryRoute.ProvincePicker) },
                onSearch = { navController.navigate(DirectoryRoute.Search) },
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onNotifications = { navController.navigate(DirectoryRoute.Notifications) },
                bottomBar = { DirectoryTabs(DirectoryTab.HOME, navController) },
            )
        }
        composable<DirectoryRoute.ProvincePicker> {
            ProvinceScreen(
                onSelected = {
                    navController.navigate(DirectoryRoute.Home) {
                        popUpTo<DirectoryRoute.Home> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Search> {
            SearchScreen(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Directory> {
            DirectoryScreen(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onProvince = { navController.navigate(DirectoryRoute.ProvincePicker) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.FacilityDetailRoute> { backStackEntry ->
            val facilityId = backStackEntry.toRoute<DirectoryRoute.FacilityDetailRoute>().id
            FacilityScreen(
                onMap = {
                    // One map at most: any map already on the stack gives way to this one.
                    navController.navigate(MapNavigation.mapFor(facilityId)) {
                        popUpTo<DirectoryRoute.Map> { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onDirections = { latitude, longitude ->
                    // The way there is shown before it is followed; starting is the user's own
                    // decision, on the next screen.
                    navController.navigate(
                        DirectoryRoute.BuiltInNavigation(latitude, longitude),
                    )
                },
                onRatings = { navController.navigate(DirectoryRoute.MyRatings) },
                onSignIn = { navController.navigate(DirectoryRoute.Login) },
                // The dialer opens with the number the backend published; the call is the user's.
                onCall = { phone ->
                    context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
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
                        navController.navigate(
                            DirectoryRoute.BuiltInNavigation(
                                latitude = route.latitude,
                                longitude = route.longitude,
                                profile = profile.name,
                                simulated = true,
                            ),
                        ) { launchSingleTop = true }
                    }
                } else {
                    null
                },
            )
        }
        composable<DirectoryRoute.Account> {
            AccountScreen(
                onRatings = { navController.navigate(DirectoryRoute.MyRatings) },
                onFacilities = { navController.navigate(DirectoryRoute.MyFacilities) },
                onAccountDeleted = {
                    navController.navigate(DirectoryRoute.Home) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onSignIn = { navController.navigate(DirectoryRoute.Login) },
                onRegister = { navController.navigate(DirectoryRoute.Register) },
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(DirectoryRoute.EditProfile) },
                onChangePassword = { navController.navigate(DirectoryRoute.ChangePassword) },
                onFavorites = { navController.navigate(DirectoryRoute.Favorites) },
                onNotifications = { navController.navigate(DirectoryRoute.Notifications) },
                onSettings = { navController.navigate(DirectoryRoute.Settings) },
                onHelp = { navController.navigate(DirectoryRoute.Help) },
                bottomBar = { DirectoryTabs(DirectoryTab.ACCOUNT, navController) },
            )
        }
        composable<DirectoryRoute.Login> {
            LoginScreen(
                onSignedIn = { navController.popBackStack() },
                onRegister = { navController.navigate(DirectoryRoute.Register) },
                onRecovery = { navController.navigate(DirectoryRoute.Recovery) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Register> {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(DirectoryRoute.Account) {
                        popUpTo<DirectoryRoute.Home>()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Recovery> {
            RecoveryScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Favorites> {
            FavoritesScreen(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.Notifications> {
            NotificationsScreen(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onOwnerFacilities = { navController.navigate(DirectoryRoute.MyFacilities) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.EditProfile> {
            ProfileEditScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DirectoryRoute.ChangePassword> {
            PasswordChangeScreen(
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
                onNotifications = { navController.navigate(DirectoryRoute.Notifications) },
                onHelp = { navController.navigate(DirectoryRoute.Help) },
                onBack = { navController.popBackStack() },
                appVersion = BuildConfig.VERSION_NAME,
                signedIn = session == SessionState.SIGNED_IN,
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
            RatingsScreen(onBack = { navController.popBackStack() })
        }

        composable<DirectoryRoute.MyFacilities> {
            MyFacilitiesScreen(
                onAdd = { navController.navigate(DirectoryRoute.Onboarding()) },
                onManage = { navController.navigate(DirectoryRoute.ManageFacility(it)) },
                onDuty = { navController.navigate(DirectoryRoute.Duty(it)) },
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
        composable<DirectoryRoute.Duty> {
            DutyScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** The app's few main places, as the bar along the bottom carries them. */
private enum class DirectoryTab { HOME, MAP, ACCOUNT }

/**
 * The bottom bar.
 *
 * It holds the three places the app actually has. The references show a fourth, saved
 * facilities, and nothing in the API stores them, so it is not drawn rather than drawn dead.
 */
@Composable
private fun DirectoryTabs(current: DirectoryTab, navController: NavHostController) {
    DirectoryBottomBar(
        listOf(
            DirectoryDestination(
                label = "الرئيسية",
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
                label = "الخريطة",
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
            DirectoryDestination(
                label = "حسابي",
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
