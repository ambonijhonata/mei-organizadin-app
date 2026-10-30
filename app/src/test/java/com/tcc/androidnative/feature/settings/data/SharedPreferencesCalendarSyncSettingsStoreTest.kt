package com.tcc.androidnative.feature.settings.data

import com.tcc.androidnative.testutil.FakeSharedPreferences
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesCalendarSyncSettingsStoreTest {
    @Test
    fun `defaults should use device local date with filter disabled and setup pending`() {
        val store = SharedPreferencesCalendarSyncSettingsStore(
            prefs = FakeSharedPreferences(),
            currentDateProvider = { LocalDate.of(2026, 4, 12) }
        )

        val settings = store.getSettings()

        assertFalse(settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 4, 12), settings.startDate)
        assertFalse(settings.considerPaidAppointmentsOnlyInReports)
        assertFalse(settings.initialSetupCompleted)
    }

    @Test
    fun `save should persist values and mark initial setup completed`() {
        val store = SharedPreferencesCalendarSyncSettingsStore(
            prefs = FakeSharedPreferences(),
            currentDateProvider = { LocalDate.of(2026, 4, 12) }
        )

        store.saveSettings(
            useStartDateFilter = true,
            startDate = LocalDate.of(2026, 4, 7),
            considerPaidAppointmentsOnlyInReports = true
        )

        val settings = store.getSettings()
        assertTrue(settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 4, 7), settings.startDate)
        assertTrue(settings.considerPaidAppointmentsOnlyInReports)
        assertTrue(settings.initialSetupCompleted)
    }

    @Test
    fun `settings should be isolated by authenticated user id`() {
        val prefs = FakeSharedPreferences()
        val user1Store = SharedPreferencesCalendarSyncSettingsStore(
            prefs = prefs,
            currentDateProvider = { LocalDate.of(2026, 4, 12) },
            currentUserIdProvider = { 1L }
        )
        val user2Store = SharedPreferencesCalendarSyncSettingsStore(
            prefs = prefs,
            currentDateProvider = { LocalDate.of(2026, 4, 12) },
            currentUserIdProvider = { 2L }
        )

        user1Store.saveSettings(
            useStartDateFilter = true,
            startDate = LocalDate.of(2026, 4, 7),
            considerPaidAppointmentsOnlyInReports = true
        )
        user2Store.saveSettings(
            useStartDateFilter = false,
            startDate = LocalDate.of(2026, 5, 1),
            considerPaidAppointmentsOnlyInReports = false
        )

        val user1Settings = user1Store.getSettings()
        val user2Settings = user2Store.getSettings()
        assertTrue(user1Settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 4, 7), user1Settings.startDate)
        assertTrue(user1Settings.considerPaidAppointmentsOnlyInReports)
        assertTrue(user1Settings.initialSetupCompleted)
        assertFalse(user2Settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 5, 1), user2Settings.startDate)
        assertFalse(user2Settings.considerPaidAppointmentsOnlyInReports)
        assertTrue(user2Settings.initialSetupCompleted)
    }

    @Test
    fun `anonymous fallback scope should remain stable when user id is unavailable`() {
        val prefs = FakeSharedPreferences()
        val anonymousStore = SharedPreferencesCalendarSyncSettingsStore(
            prefs = prefs,
            currentDateProvider = { LocalDate.of(2026, 4, 12) },
            currentUserIdProvider = { null }
        )

        anonymousStore.saveSettings(
            useStartDateFilter = true,
            startDate = LocalDate.of(2026, 4, 7),
            considerPaidAppointmentsOnlyInReports = true
        )

        val settings = anonymousStore.getSettings()
        assertTrue(settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 4, 7), settings.startDate)
        assertTrue(settings.considerPaidAppointmentsOnlyInReports)
        assertTrue(settings.initialSetupCompleted)
    }
}
