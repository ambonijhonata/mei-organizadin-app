package com.tcc.androidnative.feature.settings.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.feature.settings.data.CalendarSyncSettings
import com.tcc.androidnative.feature.settings.data.CalendarSyncSettingsStore
import com.tcc.androidnative.testutil.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `first access should require save before leaving screen`() = runTest {
        val store = FakeCalendarSyncSettingsStore(
            CalendarSyncSettings(
                useStartDateFilter = false,
                startDate = LocalDate.of(2026, 4, 12),
                considerPaidAppointmentsOnlyInReports = false,
                initialSetupCompleted = false
            )
        )
        val viewModel = SettingsViewModel(store)

        assertTrue(viewModel.uiState.value.requiresFirstAccessSave)
        assertFalse(viewModel.cancel())
    }

    @Test
    fun `save should expose success feedback and navigation to home`() = runTest {
        val store = FakeCalendarSyncSettingsStore(
            CalendarSyncSettings(
                useStartDateFilter = false,
                startDate = LocalDate.of(2026, 4, 12),
                considerPaidAppointmentsOnlyInReports = false,
                initialSetupCompleted = false
            )
        )
        val viewModel = SettingsViewModel(store)
        viewModel.onUseStartDateFilterChanged(true)
        viewModel.onConsiderPaidAppointmentsOnlyInReportsChanged(true)
        viewModel.onStartDateSelected(LocalDate.of(2026, 4, 7))

        viewModel.save()
        runCurrent()

        assertFalse(viewModel.uiState.value.requiresFirstAccessSave)
        assertEquals(R.string.feedback_settings_save_success, viewModel.uiState.value.transientMessage?.textResId)
        assertEquals(MessageTone.SUCCESS, viewModel.uiState.value.transientMessage?.tone)
        assertEquals(SettingsNavigationTarget.HOME, viewModel.uiState.value.pendingNavigationTarget)
        assertTrue(viewModel.cancel())
        assertTrue(store.getSettings().initialSetupCompleted)
        assertTrue(store.getSettings().useStartDateFilter)
        assertTrue(store.getSettings().considerPaidAppointmentsOnlyInReports)
        assertEquals(LocalDate.of(2026, 4, 7), store.getSettings().startDate)

        viewModel.onNavigationHandled()
        assertNull(viewModel.uiState.value.pendingNavigationTarget)

        advanceUntilIdle()
        assertNull(viewModel.uiState.value.transientMessage)
    }

    @Test
    fun `valid manual date input should update canonical start date`() = runTest {
        val store = FakeCalendarSyncSettingsStore(
            CalendarSyncSettings(
                useStartDateFilter = true,
                startDate = LocalDate.of(2026, 4, 12),
                considerPaidAppointmentsOnlyInReports = false,
                initialSetupCompleted = true
            )
        )
        val viewModel = SettingsViewModel(store)

        viewModel.onStartDateInputChanged("15/04/2026")

        assertEquals(LocalDate.of(2026, 4, 15), viewModel.uiState.value.startDate)
        assertEquals("15/04/2026", viewModel.uiState.value.startDateInput)
        assertNull(viewModel.uiState.value.startDateInputErrorResId)
    }

    @Test
    fun `invalid manual date input should not replace canonical start date`() = runTest {
        val store = FakeCalendarSyncSettingsStore(
            CalendarSyncSettings(
                useStartDateFilter = true,
                startDate = LocalDate.of(2026, 4, 12),
                considerPaidAppointmentsOnlyInReports = false,
                initialSetupCompleted = true
            )
        )
        val viewModel = SettingsViewModel(store)

        viewModel.onStartDateInputChanged("99/99/9999")

        assertEquals(LocalDate.of(2026, 4, 12), viewModel.uiState.value.startDate)
        assertEquals("99/99/9999", viewModel.uiState.value.startDateInput)
        assertEquals(R.string.settings_start_date_invalid, viewModel.uiState.value.startDateInputErrorResId)
    }

    @Test
    fun `save should reject invalid manual date input`() = runTest {
        val store = FakeCalendarSyncSettingsStore(
            CalendarSyncSettings(
                useStartDateFilter = true,
                startDate = LocalDate.of(2026, 4, 12),
                considerPaidAppointmentsOnlyInReports = false,
                initialSetupCompleted = true
            )
        )
        val viewModel = SettingsViewModel(store)

        viewModel.onStartDateInputChanged("99/99/9999")
        viewModel.save()
        runCurrent()

        assertEquals(R.string.settings_start_date_invalid, viewModel.uiState.value.startDateInputErrorResId)
        assertNull(viewModel.uiState.value.pendingNavigationTarget)
        assertEquals(LocalDate.of(2026, 4, 12), store.getSettings().startDate)
    }

    @Test
    fun `save failure should keep first access lock and expose error feedback`() = runTest {
        val store = FakeCalendarSyncSettingsStore(
            settings = CalendarSyncSettings(
                useStartDateFilter = false,
                startDate = LocalDate.of(2026, 4, 12),
                considerPaidAppointmentsOnlyInReports = false,
                initialSetupCompleted = false
            ),
            shouldThrowOnSave = true
        )
        val viewModel = SettingsViewModel(store)

        viewModel.save()
        runCurrent()

        assertTrue(viewModel.uiState.value.requiresFirstAccessSave)
        assertEquals(R.string.feedback_settings_save_error, viewModel.uiState.value.transientMessage?.textResId)
        assertEquals(MessageTone.ERROR, viewModel.uiState.value.transientMessage?.tone)
        assertNull(viewModel.uiState.value.pendingNavigationTarget)
        assertFalse(store.getSettings().initialSetupCompleted)
    }

    @Test
    fun `subsequent accesses should allow leaving without save`() = runTest {
        val store = FakeCalendarSyncSettingsStore(
            CalendarSyncSettings(
                useStartDateFilter = false,
                startDate = LocalDate.of(2026, 4, 12),
                considerPaidAppointmentsOnlyInReports = false,
                initialSetupCompleted = true
            )
        )
        val viewModel = SettingsViewModel(store)

        assertFalse(viewModel.uiState.value.requiresFirstAccessSave)
        assertTrue(viewModel.cancel())
    }
}

private class FakeCalendarSyncSettingsStore(
    private var settings: CalendarSyncSettings,
    private val shouldThrowOnSave: Boolean = false
) : CalendarSyncSettingsStore {
    override fun getSettings(): CalendarSyncSettings = settings

    override fun saveSettings(
        useStartDateFilter: Boolean,
        startDate: LocalDate,
        considerPaidAppointmentsOnlyInReports: Boolean
    ) {
        if (shouldThrowOnSave) {
            error("save failed")
        }
        settings = settings.copy(
            useStartDateFilter = useStartDateFilter,
            startDate = startDate,
            considerPaidAppointmentsOnlyInReports = considerPaidAppointmentsOnlyInReports,
            initialSetupCompleted = true
        )
    }
}
