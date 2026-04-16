package com.tcc.androidnative.feature.settings.data

import java.time.LocalDate

data class CalendarSyncSettings(
    val useStartDateFilter: Boolean,
    val startDate: LocalDate,
    val considerPaidAppointmentsOnlyInReports: Boolean = false,
    val initialSetupCompleted: Boolean
)

interface CalendarSyncSettingsStore {
    fun getSettings(): CalendarSyncSettings
    fun saveSettings(
        useStartDateFilter: Boolean,
        startDate: LocalDate,
        considerPaidAppointmentsOnlyInReports: Boolean
    )
}
