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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tcc.androidnative.feature.auth.ui.LoginRoute
import com.tcc.androidnative.feature.calendar.ui.CalendarHomeScreen
import com.tcc.androidnative.feature.calendar.ui.CalendarHomeViewModel
import com.tcc.androidnative.feature.calendar.data.CalendarPaymentStatus
import com.tcc.androidnative.feature.clients.ui.ClientsScreen
import com.tcc.androidnative.feature.payments.ui.PaymentsScreen
import com.tcc.androidnative.feature.reports.ui.CashFlowScreen
import com.tcc.androidnative.feature.reports.ui.RevenueScreen
import com.tcc.androidnative.feature.services.ui.ServicesScreen
import com.tcc.androidnative.feature.settings.ui.SettingsScreen
import com.tcc.androidnative.ui.theme.DrawerMenuIconBlue

@Composable
fun AppNavHost() {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val initialSetupViewModel: InitialSetupViewModel = hiltViewModel()
    val calendarHomeViewModel: CalendarHomeViewModel = hiltViewModel()
    val session = sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val startDestination = resolveStartDestination(hasSession = session.value != null)
    var requiresInitialSetup by rememberSaveable { mutableStateOf(false) }
    var pendingCalendarRefresh by rememberSaveable { mutableStateOf(false) }

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
            pendingCalendarRefresh = false
            navController.navigate(AppDestination.Login.route) {
                launchSingleTop = true
            }
        } else if (session.value != null) {
            pendingCalendarRefresh = true
        }
    }

    LaunchedEffect(currentRoute, pendingCalendarRefresh) {
        if (shouldRefreshCalendarAfterAuth(currentRoute, pendingCalendarRefresh)) {
            calendarHomeViewModel.refreshSelectedDate()
            pendingCalendarRefresh = false
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
                    viewModel = calendarHomeViewModel,
                    onReauthenticateRequested = sessionViewModel::logout,
                    onAppointmentClick = { item ->
                        navController.navigate(
                            AppDestination.Payments.createRoute(
                                eventId = item.eventId,
                                totalServiceValue = item.serviceTotalValue,
                                preloadPayments = item.paymentStatus != CalendarPaymentStatus.NONE
                            )
                        ) {
                            launchSingleTop = true
                        }
                    }
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
                    onSaveSuccess = {
                        requiresInitialSetup = false
                        navigateTo(resolveSettingsSaveDestination())
                    },
                    onNavigateHome = {
                        navigateTo(AppDestination.Home.route)
                    }
                )
            }
        }
        composable(
            route = AppDestination.Payments.route,
            arguments = listOf(
                navArgument(AppDestination.Payments.ARG_EVENT_ID) {
                    type = NavType.LongType
                },
                navArgument(AppDestination.Payments.ARG_TOTAL_SERVICE_VALUE) {
                    type = NavType.StringType
                    defaultValue = "0"
                },
                navArgument(AppDestination.Payments.ARG_PRELOAD_PAYMENTS) {
                    type = NavType.BoolType
                    defaultValue = true
                }
            )
        ) {
            AppShellScaffold(
                currentRoute = AppDestination.Payments.route,
                onNavigate = { route ->
                    if (route == AppDestination.Home.route) {
                        if (!navController.popBackStack()) {
                            navigateTo(AppDestination.Home.route)
                        }
                    } else {
                        navigateTo(route)
                    }
                },
                onLogout = sessionViewModel::logout,
                topBarTitle = "MEI ORGANIZADINHO",
                topBarTitleColor = DrawerMenuIconBlue,
                topBarIconColor = DrawerMenuIconBlue
            ) {
                PaymentsScreen(
                    onSaveSuccess = {
                        calendarHomeViewModel.refreshSelectedDate()
                        navController.popBackStack()
                    },
                    onCancelClick = { navController.popBackStack() }
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

internal fun resolveSettingsSaveDestination(): String {
    return AppDestination.Home.route
}

internal fun shouldRefreshCalendarAfterAuth(
    currentRoute: String?,
    pendingCalendarRefresh: Boolean
): Boolean {
    return pendingCalendarRefresh && currentRoute == AppDestination.Home.route
}
