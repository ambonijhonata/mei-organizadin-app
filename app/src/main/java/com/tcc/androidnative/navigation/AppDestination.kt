package com.tcc.androidnative.navigation

import java.math.BigDecimal
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")
    data object Home : AppDestination("home")
    data object Onboarding1 : AppDestination("onboarding1")
    data object Onboarding2 : AppDestination("onboarding2")
    data object Onboarding4 : AppDestination("onboarding4")
    data object Clients : AppDestination("clients")
    data object Services : AppDestination("services")
    data object AppointmentForm : AppDestination(
        "appointment-form?date={date}&startTime={startTime}&endTime={endTime}"
    ) {
        const val ROUTE_BASE = "appointment-form"
        const val ARG_DATE = "date"
        const val ARG_START_TIME = "startTime"
        const val ARG_END_TIME = "endTime"

        fun createRoute(
            date: LocalDate? = null,
            startTime: LocalTime? = null,
            endTime: LocalTime? = null
        ): String {
            if (date == null && startTime == null && endTime == null) {
                return ROUTE_BASE
            }

            val queryParams = buildList {
                date?.let { add("$ARG_DATE=${encodeQueryValue(it.toString())}") }
                startTime?.let { add("$ARG_START_TIME=${encodeQueryValue(it.toString())}") }
                endTime?.let { add("$ARG_END_TIME=${encodeQueryValue(it.toString())}") }
            }

            return if (queryParams.isEmpty()) {
                ROUTE_BASE
            } else {
                "$ROUTE_BASE?${queryParams.joinToString("&")}"
            }
        }
    }
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

private fun encodeQueryValue(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8)
}
