package com.tcc.androidnative.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tcc.androidnative.R
import com.tcc.androidnative.ui.theme.DrawerChevronGray
import com.tcc.androidnative.ui.theme.DrawerMenuIconBlue
import com.tcc.androidnative.ui.theme.DrawerMenuSelectedBackground
import com.tcc.androidnative.ui.theme.DrawerMenuTextGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShellScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    topBarTitle: String = "MEI ORGANIZADINHO",
    topBarTitleColor: Color = DrawerMenuIconBlue,
    topBarIconColor: Color = DrawerMenuIconBlue,
    isNavigationLocked: Boolean = false,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val homeLabel = stringResource(R.string.drawer_menu_home)
    val cadastrosLabel = stringResource(R.string.drawer_menu_registers)
    val clientsLabel = stringResource(R.string.drawer_menu_clients)
    val servicesLabel = stringResource(R.string.drawer_menu_services)
    val relatoriosLabel = stringResource(R.string.drawer_menu_reports)
    val cashFlowLabel = stringResource(R.string.drawer_menu_cash_flow)
    val revenueLabel = stringResource(R.string.drawer_menu_revenue)
    val settingsLabel = stringResource(R.string.drawer_menu_settings)
    val logoutLabel = stringResource(R.string.drawer_menu_logout)
    val drawerTitle = stringResource(R.string.drawer_app_title)
    val drawerParentItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = DrawerMenuSelectedBackground,
        selectedTextColor = DrawerMenuIconBlue,
        selectedIconColor = DrawerMenuIconBlue,
        unselectedContainerColor = Color.Transparent,
        unselectedTextColor = DrawerMenuTextGray,
        unselectedIconColor = DrawerMenuIconBlue
    )
    val drawerChildItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = DrawerMenuSelectedBackground,
        selectedTextColor = DrawerMenuIconBlue,
        selectedIconColor = DrawerMenuIconBlue,
        unselectedContainerColor = Color.Transparent,
        unselectedTextColor = DrawerChevronGray,
        unselectedIconColor = DrawerChevronGray
    )

    val cadastrosChildRoutes = setOf(AppDestination.Clients.route, AppDestination.Services.route)
    val relatoriosChildRoutes = setOf(AppDestination.CashFlow.route, AppDestination.Revenue.route)

    var cadastrosExpanded by rememberSaveable { mutableStateOf(false) }
    var relatoriosExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        cadastrosExpanded = currentRoute in cadastrosChildRoutes
        relatoriosExpanded = currentRoute in relatoriosChildRoutes
    }

    fun navigateOrClose(route: String) {
        if (isNavigationLocked && route != AppDestination.Settings.route) {
            return
        }
        scope.launch {
            drawerState.close()
            if (currentRoute != route) {
                onNavigate(route)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = drawerTitle,
                            color = DrawerMenuIconBlue
                        )
                        IconButton(
                            enabled = !isNavigationLocked,
                            onClick = { navigateOrClose(AppDestination.Home.route) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = homeLabel,
                                tint = DrawerMenuIconBlue
                            )
                        }
                    }

                    NavigationDrawerItem(
                        label = { Text(homeLabel) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = null
                            )
                        },
                        selected = currentRoute == AppDestination.Home.route,
                        onClick = { navigateOrClose(AppDestination.Home.route) },
                        colors = drawerParentItemColors
                    )

                    val expandCadastrosDescription =
                        stringResource(R.string.drawer_expand_submenu, cadastrosLabel)
                    val collapseCadastrosDescription =
                        stringResource(R.string.drawer_collapse_submenu, cadastrosLabel)

                    NavigationDrawerItem(
                        modifier = Modifier.semantics {
                            contentDescription = if (cadastrosExpanded) {
                                collapseCadastrosDescription
                            } else {
                                expandCadastrosDescription
                            }
                        },
                        label = { Text(cadastrosLabel) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null
                            )
                        },
                        selected = false,
                        onClick = {
                            if (!isNavigationLocked) {
                                cadastrosExpanded = !cadastrosExpanded
                            }
                        },
                        badge = {
                            Icon(
                                imageVector = if (cadastrosExpanded) {
                                    Icons.Default.ExpandMore
                                } else {
                                    Icons.Default.ChevronRight
                                },
                                contentDescription = null,
                                tint = DrawerChevronGray
                            )
                        },
                        colors = drawerParentItemColors
                    )
                    if (cadastrosExpanded) {
                        NavigationDrawerItem(
                            modifier = Modifier.padding(start = 24.dp),
                            label = { Text(clientsLabel) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null
                                )
                            },
                            selected = currentRoute == AppDestination.Clients.route,
                            onClick = { navigateOrClose(AppDestination.Clients.route) },
                            colors = drawerChildItemColors
                        )
                        NavigationDrawerItem(
                            modifier = Modifier.padding(start = 24.dp),
                            label = { Text(servicesLabel) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCut,
                                    contentDescription = null
                                )
                            },
                            selected = currentRoute == AppDestination.Services.route,
                            onClick = { navigateOrClose(AppDestination.Services.route) },
                            colors = drawerChildItemColors
                        )
                    }

                    val expandRelatoriosDescription =
                        stringResource(R.string.drawer_expand_submenu, relatoriosLabel)
                    val collapseRelatoriosDescription =
                        stringResource(R.string.drawer_collapse_submenu, relatoriosLabel)

                    NavigationDrawerItem(
                        modifier = Modifier.semantics {
                            contentDescription = if (relatoriosExpanded) {
                                collapseRelatoriosDescription
                            } else {
                                expandRelatoriosDescription
                            }
                        },
                        label = { Text(relatoriosLabel) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.BarChart,
                                contentDescription = null
                            )
                        },
                        selected = false,
                        onClick = {
                            if (!isNavigationLocked) {
                                relatoriosExpanded = !relatoriosExpanded
                            }
                        },
                        badge = {
                            Icon(
                                imageVector = if (relatoriosExpanded) {
                                    Icons.Default.ExpandMore
                                } else {
                                    Icons.Default.ChevronRight
                                },
                                contentDescription = null,
                                tint = DrawerChevronGray
                            )
                        },
                        colors = drawerParentItemColors
                    )
                    if (relatoriosExpanded) {
                        NavigationDrawerItem(
                            modifier = Modifier.padding(start = 24.dp),
                            label = { Text(cashFlowLabel) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                    contentDescription = null
                                )
                            },
                            selected = currentRoute == AppDestination.CashFlow.route,
                            onClick = { navigateOrClose(AppDestination.CashFlow.route) },
                            colors = drawerChildItemColors
                        )
                        NavigationDrawerItem(
                            modifier = Modifier.padding(start = 24.dp),
                            label = { Text(revenueLabel) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    contentDescription = null
                                )
                            },
                            selected = currentRoute == AppDestination.Revenue.route,
                            onClick = { navigateOrClose(AppDestination.Revenue.route) },
                            colors = drawerChildItemColors
                        )
                    }

                    NavigationDrawerItem(
                        label = { Text(settingsLabel) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null
                            )
                        },
                        selected = currentRoute == AppDestination.Settings.route,
                        onClick = { navigateOrClose(AppDestination.Settings.route) },
                        colors = drawerParentItemColors
                    )

                    NavigationDrawerItem(
                        label = { Text(logoutLabel) },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null
                            )
                        },
                        selected = false,
                        onClick = {
                            if (!isNavigationLocked) {
                                scope.launch { drawerState.close() }
                                onLogout()
                            }
                        },
                        colors = drawerParentItemColors
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.semantics { contentDescription = "Abrir menu hamburguer" },
                            enabled = !isNavigationLocked,
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(
                            modifier = Modifier.semantics { contentDescription = "Ir para tela inicial" },
                            enabled = !isNavigationLocked,
                            onClick = { navigateOrClose(AppDestination.Home.route) }
                        ) {
                            Icon(Icons.Outlined.Home, contentDescription = "Home")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        titleContentColor = topBarTitleColor,
                        navigationIconContentColor = topBarIconColor,
                        actionIconContentColor = topBarIconColor
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                content()
            }
        }
    }
}
