package com.tcc.androidnative.navigation

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
}

