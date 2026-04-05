package com.tcc.androidnative.navigation

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")
    data object Home : AppDestination("home")
    data object Clients : AppDestination("clients")
    data object Services : AppDestination("services")
    data object CashFlow : AppDestination("cashflow")
    data object Revenue : AppDestination("revenue")
}

