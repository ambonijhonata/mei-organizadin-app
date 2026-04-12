package com.tcc.androidnative.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tcc.androidnative.feature.auth.ui.LoginRoute
import com.tcc.androidnative.feature.calendar.ui.CalendarHomeScreen
import com.tcc.androidnative.feature.clients.ui.ClientsScreen
import com.tcc.androidnative.feature.reports.ui.CashFlowScreen
import com.tcc.androidnative.feature.reports.ui.RevenueScreen
import com.tcc.androidnative.feature.services.ui.ServicesScreen
import com.tcc.androidnative.feature.settings.ui.SettingsScreen
import com.tcc.androidnative.ui.theme.DrawerMenuIconBlue

@Composable
fun AppNavHost() {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val initialSetupViewModel: InitialSetupViewModel = hiltViewModel()
    val session = sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val startDestination = resolveStartDestination(hasSession = session.value != null)
    var requiresInitialSetup by rememberSaveable { mutableStateOf(false) }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(session.value) {
        val currentRoute = navController.currentDestination?.route
        if (session.value == null && currentRoute != AppDestination.Login.route) {
            requiresInitialSetup = false
            navController.navigate(AppDestination.Login.route) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppDestination.Login.route) {
            LoginRoute(
                onLoginSuccess = {
                    val authenticatedRoute = if (initialSetupViewModel.requiresInitialSetup()) {
                        requiresInitialSetup = true
                        resolvePostLoginDestination(requiresInitialSetup = true)
                    } else {
                        requiresInitialSetup = false
                        resolvePostLoginDestination(requiresInitialSetup = false)
                    }
                    navController.navigate(authenticatedRoute) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(AppDestination.Home.route) {
            AppShellScaffold(
                currentRoute = AppDestination.Home.route,
                onNavigate = ::navigateTo,
                onLogout = sessionViewModel::logout,
                topBarTitle = "MEI ORGANIZADINHO",
                topBarTitleColor = DrawerMenuIconBlue,
                topBarIconColor = DrawerMenuIconBlue
            ) {
                CalendarHomeScreen(
                    onReauthenticateRequested = sessionViewModel::logout
                )
            }
        }
        composable(AppDestination.Clients.route) {
            AppShellScaffold(
                currentRoute = AppDestination.Clients.route,
                onNavigate = ::navigateTo,
                onLogout = sessionViewModel::logout,
                topBarTitle = "MEI ORGANIZADINHO",
                topBarTitleColor = DrawerMenuIconBlue,
                topBarIconColor = DrawerMenuIconBlue
            ) { ClientsScreen() }
        }
        composable(AppDestination.Services.route) {
            AppShellScaffold(
                currentRoute = AppDestination.Services.route,
                onNavigate = ::navigateTo,
                onLogout = sessionViewModel::logout,
                topBarTitle = "MEI ORGANIZADINHO",
                topBarTitleColor = DrawerMenuIconBlue,
                topBarIconColor = DrawerMenuIconBlue
            ) { ServicesScreen() }
        }
        composable(AppDestination.CashFlow.route) {
            AppShellScaffold(
                currentRoute = AppDestination.CashFlow.route,
                onNavigate = ::navigateTo,
                onLogout = sessionViewModel::logout,
                topBarTitle = "MEI ORGANIZADINHO",
                topBarTitleColor = DrawerMenuIconBlue,
                topBarIconColor = DrawerMenuIconBlue
            ) { CashFlowScreen() }
        }
        composable(AppDestination.Revenue.route) {
            AppShellScaffold(
                currentRoute = AppDestination.Revenue.route,
                onNavigate = ::navigateTo,
                onLogout = sessionViewModel::logout,
                topBarTitle = "MEI ORGANIZADINHO",
                topBarTitleColor = DrawerMenuIconBlue,
                topBarIconColor = DrawerMenuIconBlue
            ) { RevenueScreen() }
        }
        composable(AppDestination.Settings.route) {
            AppShellScaffold(
                currentRoute = AppDestination.Settings.route,
                onNavigate = ::navigateTo,
                onLogout = sessionViewModel::logout,
                topBarTitle = "MEI ORGANIZADINHO",
                topBarTitleColor = DrawerMenuIconBlue,
                topBarIconColor = DrawerMenuIconBlue,
                isNavigationLocked = requiresInitialSetup
            ) {
                SettingsScreen(
                    onInitialSetupCompleted = {
                        requiresInitialSetup = false
                        navigateTo(AppDestination.Home.route)
                    },
                    onNavigateHome = {
                        navigateTo(AppDestination.Home.route)
                    }
                )
            }
        }
    }
}

internal fun resolveStartDestination(hasSession: Boolean): String {
    return if (hasSession) AppDestination.Home.route else AppDestination.Login.route
}

internal fun resolvePostLoginDestination(requiresInitialSetup: Boolean): String {
    return if (requiresInitialSetup) AppDestination.Settings.route else AppDestination.Home.route
}
