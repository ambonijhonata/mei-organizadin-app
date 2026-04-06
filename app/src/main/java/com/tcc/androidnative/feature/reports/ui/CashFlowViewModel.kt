package com.tcc.androidnative.feature.reports.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.reports.data.CashFlowEntryModel
import com.tcc.androidnative.feature.reports.data.CashFlowReportModel
import com.tcc.androidnative.feature.reports.data.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CashFlowScreenStep {
    FORM,
    REPORT,
    DETAIL
}

data class CashFlowUiState(
    val startDateInput: String = DateFormats.toUiDate(LocalDate.now(ZoneOffset.UTC).minusDays(1)),
    val endDateInput: String = DateFormats.toUiDate(LocalDate.now(ZoneOffset.UTC)),
    val step: CashFlowScreenStep = CashFlowScreenStep.FORM,
    val isLoading: Boolean = false,
    val report: CashFlowReportModel? = null,
    val selectedDetail: CashFlowEntryModel? = null,
    val staleDataWarning: Boolean = false,
    val transientMessage: TransientMessage? = null
)

@HiltViewModel
class CashFlowViewModel @Inject constructor(
    private val repository: ReportsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CashFlowUiState())
    val uiState: StateFlow<CashFlowUiState> = _uiState.asStateFlow()

    fun onStartDateChange(value: String) {
        _uiState.update { it.copy(startDateInput = value) }
    }

    fun onEndDateChange(value: String) {
        _uiState.update { it.copy(endDateInput = value) }
    }

    fun emitReport() {
        val start = runCatching { DateFormats.parseUiDate(_uiState.value.startDateInput) }.getOrNull()
        if (start == null) {
            showMessage(R.string.feedback_report_start_date_invalid, MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        val end = runCatching { DateFormats.parseUiDate(_uiState.value.endDateInput) }.getOrNull()
        if (end == null) {
            showMessage(R.string.feedback_report_end_date_invalid, MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        if (start.isAfter(end)) {
            showMessage(R.string.feedback_report_start_before_end, MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        val rangeDaysInclusive = ChronoUnit.DAYS.between(start, end) + 1
        if (rangeDaysInclusive > 7) {
            showMessage(R.string.feedback_cash_flow_period_limit, MessageTone.WARNING, MessageDurations.SHORT_3S)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedDetail = null) }
            runCatching { repository.cashFlow(startDate = start, endDate = end) }
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            report = report,
                            step = CashFlowScreenStep.REPORT,
                            staleDataWarning = !report.syncMetadata.dataUpToDate
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            report = null,
                            step = CashFlowScreenStep.FORM,
                            staleDataWarning = false
                        )
                    }
                    showMessage(
                        textResId = R.string.feedback_report_generate_error,
                        tone = MessageTone.ERROR,
                        duration = MessageDurations.SHORT_3S
                    )
                }
        }
    }

    fun openDetail(entry: CashFlowEntryModel) {
        _uiState.update { it.copy(selectedDetail = entry, step = CashFlowScreenStep.DETAIL) }
    }

    fun backToReport() {
        _uiState.update { it.copy(selectedDetail = null, step = CashFlowScreenStep.REPORT) }
    }

    fun backToForm() {
        _uiState.update { it.copy(step = CashFlowScreenStep.FORM, selectedDetail = null) }
    }

    private fun showMessage(
        @StringRes textResId: Int,
        tone: MessageTone,
        duration: Long,
        textArgs: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val message = TransientMessage(
                textResId = textResId,
                textArgs = textArgs,
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
