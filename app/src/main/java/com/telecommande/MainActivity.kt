package com.telecommande

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.telecommande.data.AppSettings
import com.telecommande.ui.remote.RemoteControlScreen
import com.telecommande.ui.screens.TvManagementScreen
import com.telecommande.ui.theme.TelecommandeTheme
import com.telecommande.ui.viewmodels.AppUiState
import com.telecommande.ui.viewmodels.RemoteViewModel
import com.telecommande.ui.viewmodels.TvManagementViewModel

sealed class Screen(val route: String) {
    object RemoteControl : Screen("remote_control")
    object TvManagement : Screen("tv_management")
}

class MainActivity : ComponentActivity() {

    private lateinit var appSettings: AppSettings

    private val remoteViewModel: RemoteViewModel by viewModels()
    private val tvManagementViewModel: TvManagementViewModel by viewModels {
        TvManagementViewModel.provideFactory(
            application = application,
            appSettings = appSettings,
            remoteViewModel = remoteViewModel
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSettings = AppSettings(application)

        setContent {
            TelecommandeTheme {
                val navController = rememberNavController()

                val activeTv by remoteViewModel.currentTargetTvInfo.collectAsState()
                val pairedTvs by tvManagementViewModel.pairedTvs.collectAsState()
                val uiStateRemote by remoteViewModel.uiState.collectAsState()

                LaunchedEffect(activeTv, pairedTvs, uiStateRemote) {
                    val isNoTvConfigured = activeTv == null && pairedTvs.isEmpty()
                    val isRemoteNoTvState = uiStateRemote == AppUiState.NO_TV_CONFIGURED

                    Log.d("MainActivity", "Initial Nav Check: activeTv: $activeTv, pairedTvs empty: ${pairedTvs.isEmpty()}, isNoTvConfigured: $isNoTvConfigured, isRemoteNoTvState: $isRemoteNoTvState")

                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    Log.d("MainActivity", "Current route: $currentRoute")

                    if (isNoTvConfigured && currentRoute != Screen.TvManagement.route) {
                        Log.d("MainActivity", "Navigating to TvManagement as initial or forced.")
                        navController.navigate(Screen.TvManagement.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else if (!isNoTvConfigured && currentRoute != Screen.RemoteControl.route && currentRoute != Screen.TvManagement.route) {
                        Log.d("MainActivity", "Navigating to RemoteControl as default.")
                        navController.navigate(Screen.RemoteControl.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                val startDestination = if (activeTv == null && pairedTvs.isEmpty()) {
                    Log.d("MainActivity", "StartDestination: TvManagement")
                    Screen.TvManagement.route
                } else {
                    Log.d("MainActivity", "StartDestination: RemoteControl")
                    Screen.RemoteControl.route
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Screen.RemoteControl.route) {
                        RemoteControlScreen(
                            viewModel = remoteViewModel,
                            onNavigateToTvManagement = {
                                navController.navigate(Screen.TvManagement.route)
                            }
                        )
                    }
                    composable(Screen.TvManagement.route) {
                        TvManagementScreen(
                            viewModel = tvManagementViewModel,
                            onNavigateBack = {
                                if (remoteViewModel.currentTargetTvInfo.value != null) {
                                    navController.navigate(Screen.RemoteControl.route) {
                                        popUpTo(Screen.TvManagement.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}