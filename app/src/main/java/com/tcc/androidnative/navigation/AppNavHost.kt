package com.tcc.androidnative.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyListState
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
import com.tcc.androidnative.feature.onboarding.ui.OnboardingStep1Screen
import com.tcc.androidnative.feature.onboarding.ui.OnboardingStep2Screen
import com.tcc.androidnative.feature.onboarding.ui.OnboardingStep4Screen
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
    val hasSession = session.value != null
    val startDestination = resolveStartDestination(
        hasSession = hasSession,
        shouldShowOnboarding = hasSession && initialSetupViewModel.shouldShowOnboarding()
    )
    var pendingCalendarRefresh by rememberSaveable { mutableStateOf(false) }
    val calendarListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var allowCalendarInitialAutoFocus by rememberSaveable { mutableStateOf(true) }

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
                    val authenticatedRoute = resolvePostLoginDestination(
                        shouldShowOnboarding = initialSetupViewModel.shouldShowOnboarding()
                    )
                    navController.navigate(authenticatedRoute) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(AppDestination.Onboarding1.route) {
            OnboardingStep1Screen(
                onCloseClick = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Onboarding1.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNextClick = {
                    navController.navigate(AppDestination.Onboarding2.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(AppDestination.Onboarding2.route) {
            OnboardingStep2Screen(
                onCloseClick = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Onboarding1.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    navController.navigate(AppDestination.Onboarding1.route) {
                        launchSingleTop = true
                    }
                },
                onNextClick = {
                    navController.navigate(AppDestination.Onboarding4.route) {
                        launchSingleTop = true
                    }
                },
                onAddServiceClick = {
                    navController.navigate(AppDestination.ServicesOnboarding.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(AppDestination.Onboarding4.route) {
            OnboardingStep4Screen(
                onCloseClick = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Onboarding1.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    navController.navigate(AppDestination.Onboarding2.route) {
                        launchSingleTop = true
                    }
                },
                onConcludeClick = {
                    initialSetupViewModel.completeOnboarding()
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Onboarding1.route) { inclusive = true }
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
                    listState = calendarListState,
                    allowInitialAutoFocus = allowCalendarInitialAutoFocus,
                    onReauthenticateRequested = sessionViewModel::logout,
                    onAppointmentClick = { item ->
                        allowCalendarInitialAutoFocus = false
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
            ) {
                ServicesScreen(
                    showContinueOnboarding = !initialSetupViewModel.hasCompletedOnboarding(),
                    onContinueOnboardingClick = {
                        navController.navigate(AppDestination.Onboarding4.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        composable(AppDestination.ServicesOnboarding.route) {
            AppShellScaffold(
                currentRoute = AppDestination.Services.route,
                onNavigate = ::navigateTo,
                onLogout = sessionViewModel::logout,
                topBarTitle = "MEI ORGANIZADINHO",
                topBarTitleColor = DrawerMenuIconBlue,
                topBarIconColor = DrawerMenuIconBlue
            ) {
                ServicesScreen(
                    showContinueOnboarding = true,
                    onContinueOnboardingClick = {
                        navController.navigate(AppDestination.Onboarding4.route) {
                            launchSingleTop = true
                        }
                    },
                    openCreateFormOnLaunch = true
                )
            }
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
                topBarIconColor = DrawerMenuIconBlue
            ) {
                SettingsScreen(
                    onSaveSuccess = {
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

internal fun resolveStartDestination(
    hasSession: Boolean,
    shouldShowOnboarding: Boolean
): String {
    if (!hasSession) {
        return AppDestination.Login.route
    }
    return if (shouldShowOnboarding) AppDestination.Onboarding1.route else AppDestination.Home.route
}

internal fun resolvePostLoginDestination(shouldShowOnboarding: Boolean): String {
    return if (shouldShowOnboarding) AppDestination.Onboarding1.route else AppDestination.Home.route
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
