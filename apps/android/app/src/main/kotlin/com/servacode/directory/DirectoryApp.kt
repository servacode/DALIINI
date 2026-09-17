package com.servacode.directory

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.feature.account.AccountScreen
import com.servacode.directory.feature.bootstrap.BootstrapScreen
import com.servacode.directory.feature.directory.DirectoryScreen
import com.servacode.directory.feature.facility.FacilityScreen
import com.servacode.directory.feature.home.HomeScreen
import com.servacode.directory.feature.map.MapScreen
import com.servacode.directory.feature.province.ProvinceScreen
import com.servacode.directory.feature.ratings.RatingsScreen
import com.servacode.directory.feature.search.SearchScreen

@Composable
fun DirectoryApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = DirectoryRoute.Bootstrap) {
        composable<DirectoryRoute.Bootstrap> {
            BootstrapScreen(
                onReady = {
                    navController.navigate(DirectoryRoute.Home) {
                        popUpTo<DirectoryRoute.Bootstrap> { inclusive = true }
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
                onMap = { navController.navigate(DirectoryRoute.Map) },
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
        composable<DirectoryRoute.FacilityDetailRoute> {
            FacilityScreen(
                onMap = { navController.navigate(DirectoryRoute.Map) },
                onRatings = { navController.navigate(DirectoryRoute.MyRatings) },
            )
        }
        composable<DirectoryRoute.Map> {
            MapScreen(
                styleUrl = BuildConfig.MAP_STYLE_URL,
                onFacility = { navController.navigate(DirectoryRoute.FacilityDetailRoute(it)) },
            )
        }
        composable<DirectoryRoute.Account> {
            AccountScreen(
                onRatings = { navController.navigate(DirectoryRoute.MyRatings) },
            )
        }
        composable<DirectoryRoute.MyRatings> {
            RatingsScreen()
        }
    }
}
