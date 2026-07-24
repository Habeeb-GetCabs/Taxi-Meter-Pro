package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeMeterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TripReceiptScreen
import com.example.ui.screens.TripSummaryScreen
import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.SettingsViewModel

@Composable
fun TaxiMeterNavGraph(
    meterViewModel: MeterViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val currencySymbol by settingsViewModel.currency.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }

        composable("home") {
            HomeMeterScreen(
                viewModel = meterViewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToReceipt = { tripId -> navController.navigate("receipt/$tripId") }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("receipt/{tripId}") { backStackEntry ->
            val tripIdStr = backStackEntry.arguments?.getString("tripId")
            val tripId = tripIdStr?.toIntOrNull() ?: 0

            TripReceiptScreen(
                viewModel = meterViewModel,
                tripId = tripId,
                currencySymbol = currencySymbol,
                onNavigateBack = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("summary/{tripId}") { backStackEntry ->
            val tripIdStr = backStackEntry.arguments?.getString("tripId")
            val tripId = tripIdStr?.toIntOrNull() ?: 0

            TripSummaryScreen(
                tripId = tripId,
                viewModel = meterViewModel,
                currencySymbol = currencySymbol,
                onNavigateBack = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
