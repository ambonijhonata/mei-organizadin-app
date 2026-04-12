package com.tcc.androidnative.navigation

import androidx.lifecycle.ViewModel
import com.tcc.androidnative.feature.settings.data.CalendarSyncSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InitialSetupViewModel @Inject constructor(
    private val calendarSyncSettingsStore: CalendarSyncSettingsStore
) : ViewModel() {
    fun requiresInitialSetup(): Boolean {
        return !calendarSyncSettingsStore.getSettings().initialSetupCompleted
    }
}

