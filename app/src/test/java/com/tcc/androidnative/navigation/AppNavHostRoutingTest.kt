package com.tcc.androidnative.navigation

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavHostRoutingTest {
    @Test
    fun `without session start destination should be login`() {
        val route = resolveStartDestination(hasSession = false)

        assertEquals(AppDestination.Login.route, route)
    }

    @Test
    fun `with restored session start destination should be home`() {
        val route = resolveStartDestination(hasSession = true)

        assertEquals(AppDestination.Home.route, route)
    }

    @Test
    fun `post login first access should navigate to settings`() {
        val route = resolvePostLoginDestination(requiresInitialSetup = true)

        assertEquals(AppDestination.Settings.route, route)
    }

    @Test
    fun `post login subsequent access should navigate to home`() {
        val route = resolvePostLoginDestination(requiresInitialSetup = false)

        assertEquals(AppDestination.Home.route, route)
    }

    @Test
    fun `successful settings save should navigate to home`() {
        val route = resolveSettingsSaveDestination()

        assertEquals(AppDestination.Home.route, route)
    }

    @Test
    fun `calendar refresh should trigger only when home is visible and refresh is pending`() {
        assertEquals(true, shouldRefreshCalendarAfterAuth(AppDestination.Home.route, true))
        assertEquals(false, shouldRefreshCalendarAfterAuth(AppDestination.Login.route, true))
        assertEquals(false, shouldRefreshCalendarAfterAuth(AppDestination.Home.route, false))
    }

    @Test
    fun `payments destination should create route with event id and total service value`() {
        val route = AppDestination.Payments.createRoute(
            eventId = 25L,
            totalServiceValue = BigDecimal("100.00")
        )

        assertEquals("payments/25?totalServiceValue=100.00&preloadPayments=true", route)
    }

    @Test
    fun `payments destination should create route with preload disabled`() {
        val route = AppDestination.Payments.createRoute(
            eventId = 25L,
            totalServiceValue = BigDecimal("100.00"),
            preloadPayments = false
        )

        assertEquals("payments/25?totalServiceValue=100.00&preloadPayments=false", route)
    }
}
