package com.servacode.directory

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.feature.bootstrap.BootstrapScreen

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
            Text("الدليل")
        }
    }
}
