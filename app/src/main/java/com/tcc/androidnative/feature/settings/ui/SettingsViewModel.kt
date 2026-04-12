package com.tcc.androidnative.feature.settings.ui

import androidx.lifecycle.ViewModel
import com.tcc.androidnative.feature.settings.data.CalendarSyncSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val useStartDateFilter: Boolean = false,
    val startDate: LocalDate = LocalDate.now(),
    val isTooltipVisible: Boolean = false,
    val requiresFirstAccessSave: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val calendarSyncSettingsStore: CalendarSyncSettingsStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        reloadFromStore()
    }

    fun onUseStartDateFilterChanged(enabled: Boolean) {
        _uiState.update { it.copy(useStartDateFilter = enabled) }
    }

    fun onStartDateSelected(startDate: LocalDate) {
        _uiState.update { it.copy(startDate = startDate) }
    }

    fun showTooltip() {
        _uiState.update { it.copy(isTooltipVisible = true) }
    }

    fun hideTooltip() {
        _uiState.update { it.copy(isTooltipVisible = false) }
    }

    fun save(): Boolean {
        val currentState = _uiState.value
        val firstAccessSave = currentState.requiresFirstAccessSave
        calendarSyncSettingsStore.saveSettings(
            useStartDateFilter = currentState.useStartDateFilter,
            startDate = currentState.startDate
        )
        _uiState.update {
            it.copy(
                requiresFirstAccessSave = false,
                isTooltipVisible = false
            )
        }
        return firstAccessSave
    }

    fun cancel(): Boolean {
        if (_uiState.value.requiresFirstAccessSave) {
            return false
        }
        reloadFromStore()
        return true
    }

    private fun reloadFromStore() {
        val settings = calendarSyncSettingsStore.getSettings()
        _uiState.value = SettingsUiState(
            useStartDateFilter = settings.useStartDateFilter,
            startDate = settings.startDate,
            isTooltipVisible = false,
            requiresFirstAccessSave = !settings.initialSetupCompleted
        )
    }
}

