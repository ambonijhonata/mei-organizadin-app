package com.tcc.androidnative.feature.reports.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.reports.data.ReportsRepository
import com.tcc.androidnative.feature.reports.data.RevenueReportModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RevenueScreenStep {
    FORM,
    REPORT
}

data class RevenueUiState(
    val startDateInput: String = DateFormats.toUiDate(LocalDate.now(ZoneOffset.UTC).minusMonths(1)),
    val endDateInput: String = DateFormats.toUiDate(LocalDate.now(ZoneOffset.UTC)),
    val step: RevenueScreenStep = RevenueScreenStep.FORM,
    val isLoading: Boolean = false,
    val report: RevenueReportModel? = null,
    val staleDataWarning: Boolean = false,
    val transientMessage: TransientMessage? = null
)

@HiltViewModel
class RevenueViewModel @Inject constructor(
    private val repository: ReportsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RevenueUiState())
    val uiState: StateFlow<RevenueUiState> = _uiState.asStateFlow()

    fun onStartDateChange(value: String) {
        _uiState.update { it.copy(startDateInput = value) }
    }

    fun onEndDateChange(value: String) {
        _uiState.update { it.copy(endDateInput = value) }
    }

    fun emitReport() {
        val start = runCatching { DateFormats.parseUiDate(_uiState.value.startDateInput) }.getOrNull()
        if (start == null) {
            showMessage("Data inicial invalida", MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        val end = runCatching { DateFormats.parseUiDate(_uiState.value.endDateInput) }.getOrNull()
        if (end == null) {
            showMessage("Data final invalida", MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        if (start.isAfter(end)) {
            showMessage("Data inicial deve ser menor que a data final", MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { repository.revenue(startDate = start, endDate = end) }
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            report = report,
                            step = RevenueScreenStep.REPORT,
                            staleDataWarning = !report.syncMetadata.dataUpToDate
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            report = null,
                            step = RevenueScreenStep.FORM,
                            staleDataWarning = false
                        )
                    }
                    showMessage(
                        text = "Erro ao gerar faturamento",
                        tone = MessageTone.ERROR,
                        duration = MessageDurations.MEDIUM_5S
                    )
                }
        }
    }

    fun backToForm() {
        _uiState.update { it.copy(step = RevenueScreenStep.FORM) }
    }

    private fun showMessage(text: String, tone: MessageTone, duration: Long) {
        viewModelScope.launch {
            val message = TransientMessage(text = text, tone = tone, durationMillis = duration)
            _uiState.update { it.copy(transientMessage = message) }
            delay(duration)
            _uiState.update { state ->
                if (state.transientMessage == message) state.copy(transientMessage = null) else state
            }
        }
    }
}
