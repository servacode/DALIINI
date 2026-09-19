package com.servacode.directory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.MapNavigation
import com.servacode.directory.feature.auth.LoginScreen
import com.servacode.directory.feature.auth.RecoveryScreen
import com.servacode.directory.feature.auth.RegisterScreen
import kotlinx.coroutines.flow.StateFlow
import com.servacode.directory.feature.account.AccountScreen
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

    NavHost(navController = navController, startDestination = DirectoryRoute.Bootstrap) {
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
                onCategory = { navController.navigate(DirectoryRoute.Directory(it)) },
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onMap = { navController.navigate(DirectoryRoute.Map()) },
                onAccount = { navController.navigate(DirectoryRoute.Account) },
            )
        }
        composable<DirectoryRoute.ProvincePicker> {
            ProvinceScreen(
                onSelected = {
                    navController.navigate(DirectoryRoute.Home) {
                        popUpTo<DirectoryRoute.Home> { inclusive = true }
                    }
                },
            )
        }
        composable<DirectoryRoute.Search> {
            SearchScreen(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
            )
        }
        composable<DirectoryRoute.Directory> {
            DirectoryScreen(
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
                onProvince = { navController.navigate(DirectoryRoute.ProvincePicker) },
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
                    navController.navigate(DirectoryRoute.BuiltInNavigation(latitude, longitude))
                },
                onRatings = { navController.navigate(DirectoryRoute.MyRatings) },
                onSignIn = { navController.navigate(DirectoryRoute.Login) },
            )
        }
        composable<DirectoryRoute.Map> {
            MapScreen(
                styleUrl = BuildConfig.MAP_STYLE_URL,
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
            )
        }
        composable<DirectoryRoute.Login> {
            LoginScreen(
                onSignedIn = { navController.popBackStack() },
                onRegister = { navController.navigate(DirectoryRoute.Register) },
                onRecovery = { navController.navigate(DirectoryRoute.Recovery) },
            )
        }
        composable<DirectoryRoute.Register> {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(DirectoryRoute.Account) {
                        popUpTo<DirectoryRoute.Home>()
                    }
                },
            )
        }
        composable<DirectoryRoute.Recovery> {
            RecoveryScreen(onDone = { navController.popBackStack() })
        }
        composable<DirectoryRoute.MyRatings> {
            RatingsScreen()
        }

        composable<DirectoryRoute.MyFacilities> {
            MyFacilitiesScreen(
                onAdd = { navController.navigate(DirectoryRoute.Onboarding()) },
                onManage = { navController.navigate(DirectoryRoute.ManageFacility(it)) },
                onDuty = { navController.navigate(DirectoryRoute.Duty(it)) },
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
            )
        }
        composable<DirectoryRoute.ManageFacility> {
            ManageFacilityScreen(
                onEdit = { navController.navigate(DirectoryRoute.Onboarding(it)) },
                onDuty = { navController.navigate(DirectoryRoute.Duty(it)) },
            )
        }
        composable<DirectoryRoute.Duty> {
            DutyScreen()
        }
    }
}
