package com.tcc.androidnative.navigation

import java.math.BigDecimal

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")
    data object Home : AppDestination("home")
    data object Onboarding1 : AppDestination("onboarding1")
    data object Onboarding2 : AppDestination("onboarding2")
    data object Onboarding4 : AppDestination("onboarding4")
    data object Clients : AppDestination("clients")
    data object Services : AppDestination("services")
    data object ServicesOnboarding : AppDestination("services_onboarding")
    data object CashFlow : AppDestination("cashflow")
    data object Revenue : AppDestination("revenue")
    data object PaymentMethodRevenue : AppDestination("payment-method-revenue")
    data object Settings : AppDestination("settings")
    data object Payments : AppDestination(
        "payments/{eventId}?totalServiceValue={totalServiceValue}&preloadPayments={preloadPayments}"
    ) {
        const val ROUTE_BASE = "payments"
        const val ARG_EVENT_ID = "eventId"
        const val ARG_TOTAL_SERVICE_VALUE = "totalServiceValue"
        const val ARG_PRELOAD_PAYMENTS = "preloadPayments"

        fun createRoute(
            eventId: Long,
            totalServiceValue: BigDecimal,
            preloadPayments: Boolean = true
        ): String {
            return "$ROUTE_BASE/$eventId?$ARG_TOTAL_SERVICE_VALUE=${totalServiceValue.toPlainString()}&$ARG_PRELOAD_PAYMENTS=$preloadPayments"
        }
    }
}
