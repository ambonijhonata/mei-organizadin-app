package com.tcc.androidnative.feature.settings.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.settings.data.CalendarSyncSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsNavigationTarget {
    HOME
}

data class SettingsUiState(
    val useStartDateFilter: Boolean = false,
    val startDate: LocalDate = LocalDate.now(),
    val startDateInput: String = DateFormats.toUiDate(LocalDate.now()),
    @StringRes val startDateInputErrorResId: Int? = null,
    val considerPaidAppointmentsOnlyInReports: Boolean = false,
    val isStartDateTooltipVisible: Boolean = false,
    val requiresFirstAccessSave: Boolean = false,
    val transientMessage: TransientMessage? = null,
    val pendingNavigationTarget: SettingsNavigationTarget? = null
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
        _uiState.update {
            it.copy(
                useStartDateFilter = enabled,
                startDateInputErrorResId = if (enabled) it.startDateInputErrorResId else null
            )
        }
    }

    fun onStartDateSelected(startDate: LocalDate) {
        _uiState.update {
            it.copy(
                startDate = startDate,
                startDateInput = DateFormats.toUiDate(startDate),
                startDateInputErrorResId = null
            )
        }
    }

    fun onStartDateInputChanged(input: String) {
        val parsedDate = runCatching { DateFormats.parseUiDate(input) }.getOrNull()
        _uiState.update { state ->
            when {
                input.isBlank() -> {
                    state.copy(
                        startDateInput = input,
                        startDateInputErrorResId = R.string.settings_start_date_invalid
                    )
                }

                parsedDate != null -> {
                    state.copy(
                        startDate = parsedDate,
                        startDateInput = input,
                        startDateInputErrorResId = null
                    )
                }

                else -> {
                    state.copy(
                        startDateInput = input,
                        startDateInputErrorResId = R.string.settings_start_date_invalid
                    )
                }
            }
        }
    }

    fun onConsiderPaidAppointmentsOnlyInReportsChanged(enabled: Boolean) {
        _uiState.update { it.copy(considerPaidAppointmentsOnlyInReports = enabled) }
    }

    fun showStartDateTooltip() {
        _uiState.update { it.copy(isStartDateTooltipVisible = true) }
    }

    fun hideStartDateTooltip() {
        _uiState.update { it.copy(isStartDateTooltipVisible = false) }
    }

    fun save() {
        val currentState = _uiState.value
        val validatedStartDate = if (currentState.useStartDateFilter) {
            runCatching { DateFormats.parseUiDate(currentState.startDateInput) }.getOrNull()
        } else {
            currentState.startDate
        }

        if (currentState.useStartDateFilter && validatedStartDate == null) {
            _uiState.update {
                it.copy(
                    startDateInputErrorResId = R.string.settings_start_date_invalid,
                    isStartDateTooltipVisible = false,
                    pendingNavigationTarget = null
                )
            }
            return
        }

        val startDateToSave = validatedStartDate ?: currentState.startDate
        runCatching {
            calendarSyncSettingsStore.saveSettings(
                useStartDateFilter = currentState.useStartDateFilter,
                startDate = startDateToSave,
                considerPaidAppointmentsOnlyInReports = currentState.considerPaidAppointmentsOnlyInReports
            )
        }.onSuccess {
            _uiState.update {
                it.copy(
                    startDate = startDateToSave,
                    startDateInput = DateFormats.toUiDate(startDateToSave),
                    startDateInputErrorResId = null,
                    requiresFirstAccessSave = false,
                    isStartDateTooltipVisible = false,
                    pendingNavigationTarget = SettingsNavigationTarget.HOME
                )
            }
            showMessage(
                textResId = R.string.feedback_settings_save_success,
                tone = MessageTone.SUCCESS,
                duration = MessageDurations.SHORT_3S
            )
        }.onFailure {
            _uiState.update {
                it.copy(
                    isStartDateTooltipVisible = false,
                    pendingNavigationTarget = null
                )
            }
            showMessage(
                textResId = R.string.feedback_settings_save_error,
                tone = MessageTone.ERROR,
                duration = MessageDurations.SHORT_3S
            )
        }
    }

    fun cancel(): Boolean {
        if (_uiState.value.requiresFirstAccessSave) {
            return false
        }
        reloadFromStore()
        return true
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(pendingNavigationTarget = null) }
    }

    private fun reloadFromStore() {
        val settings = calendarSyncSettingsStore.getSettings()
        _uiState.value = SettingsUiState(
            useStartDateFilter = settings.useStartDateFilter,
            startDate = settings.startDate,
            startDateInput = DateFormats.toUiDate(settings.startDate),
            startDateInputErrorResId = null,
            considerPaidAppointmentsOnlyInReports = settings.considerPaidAppointmentsOnlyInReports,
            isStartDateTooltipVisible = false,
            requiresFirstAccessSave = !settings.initialSetupCompleted
        )
    }

    private fun showMessage(
        @StringRes textResId: Int,
        tone: MessageTone,
        duration: Long
    ) {
        viewModelScope.launch {
            val message = TransientMessage(
                textResId = textResId,
                tone = tone,
                durationMillis = duration
            )
            _uiState.update { it.copy(transientMessage = message) }
            delay(duration)
            _uiState.update { state ->
                if (state.transientMessage == message) state.copy(transientMessage = null) else state
            }
        }
    }
}
