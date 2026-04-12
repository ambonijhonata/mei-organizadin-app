package com.tcc.androidnative.feature.settings.ui

import com.tcc.androidnative.feature.settings.data.CalendarSyncSettings
import com.tcc.androidnative.feature.settings.data.CalendarSyncSettingsStore
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun `first access should require save before leaving screen`() {
        val store = FakeCalendarSyncSettingsStore(
            CalendarSyncSettings(
                useStartDateFilter = false,
                startDate = LocalDate.of(2026, 4, 12),
                initialSetupCompleted = false
            )
        )
        val viewModel = SettingsViewModel(store)

        assertTrue(viewModel.uiState.value.requiresFirstAccessSave)
        assertFalse(viewModel.cancel())
    }

    @Test
    fun `save should mark first access as completed and allow leaving`() {
        val store = FakeCalendarSyncSettingsStore(
            CalendarSyncSettings(
                useStartDateFilter = false,
                startDate = LocalDate.of(2026, 4, 12),
                initialSetupCompleted = false
            )
        )
        val viewModel = SettingsViewModel(store)
        viewModel.onUseStartDateFilterChanged(true)
        viewModel.onStartDateSelected(LocalDate.of(2026, 4, 7))

        val firstAccessSave = viewModel.save()

        assertTrue(firstAccessSave)
        assertFalse(viewModel.uiState.value.requiresFirstAccessSave)
        assertTrue(viewModel.cancel())
        assertTrue(store.getSettings().initialSetupCompleted)
        assertTrue(store.getSettings().useStartDateFilter)
        assertEquals(LocalDate.of(2026, 4, 7), store.getSettings().startDate)
    }

    @Test
    fun `subsequent accesses should allow leaving without save`() {
        val store = FakeCalendarSyncSettingsStore(
            CalendarSyncSettings(
                useStartDateFilter = false,
                startDate = LocalDate.of(2026, 4, 12),
                initialSetupCompleted = true
            )
        )
        val viewModel = SettingsViewModel(store)

        assertFalse(viewModel.uiState.value.requiresFirstAccessSave)
        assertTrue(viewModel.cancel())
    }
}

private class FakeCalendarSyncSettingsStore(
    private var settings: CalendarSyncSettings
) : CalendarSyncSettingsStore {
    override fun getSettings(): CalendarSyncSettings = settings

    override fun saveSettings(useStartDateFilter: Boolean, startDate: LocalDate) {
        settings = settings.copy(
            useStartDateFilter = useStartDateFilter,
            startDate = startDate,
            initialSetupCompleted = true
        )
    }
}

