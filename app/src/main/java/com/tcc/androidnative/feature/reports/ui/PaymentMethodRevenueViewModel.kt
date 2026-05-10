package com.tcc.androidnative.feature.reports.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.reports.data.PaymentMethodRevenueReportModel
import com.tcc.androidnative.feature.reports.data.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PaymentMethodRevenueScreenStep {
    FORM,
    REPORT
}

data class PaymentMethodRevenueUiState(
    val periodInput: String = "",
    val selectedStartDate: LocalDate? = null,
    val selectedEndDate: LocalDate? = null,
    val step: PaymentMethodRevenueScreenStep = PaymentMethodRevenueScreenStep.FORM,
    val isLoading: Boolean = false,
    val report: PaymentMethodRevenueReportModel? = null,
    val staleDataWarning: Boolean = false,
    val transientMessage: TransientMessage? = null
)

@HiltViewModel
class PaymentMethodRevenueViewModel @Inject constructor(
    private val repository: ReportsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentMethodRevenueUiState())
    val uiState: StateFlow<PaymentMethodRevenueUiState> = _uiState.asStateFlow()

    fun onPeriodSelected(startDate: LocalDate, endDate: LocalDate) {
        val normalizedStart = minOf(startDate, endDate)
        val normalizedEnd = maxOf(startDate, endDate)
        _uiState.update {
            it.copy(
                periodInput = formatPeriod(normalizedStart, normalizedEnd),
                selectedStartDate = normalizedStart,
                selectedEndDate = normalizedEnd
            )
        }
    }

    fun emitReport() {
        val start = _uiState.value.selectedStartDate
        val end = _uiState.value.selectedEndDate
        if (start == null || end == null) {
            showMessage(R.string.feedback_report_period_invalid, MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        if (end.isAfter(start.plusMonths(1))) {
            showMessage(R.string.feedback_report_period_limit, MessageTone.WARNING, MessageDurations.SHORT_3S)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                repository.paymentMethodRevenue(
                    startDate = start,
                    endDate = end
                )
            }
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            report = report,
                            step = PaymentMethodRevenueScreenStep.REPORT,
                            staleDataWarning = !report.syncMetadata.dataUpToDate
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            report = null,
                            step = PaymentMethodRevenueScreenStep.FORM,
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

    private fun formatPeriod(startDate: LocalDate, endDate: LocalDate): String {
        return "${DateFormats.toUiDate(startDate)} - ${DateFormats.toUiDate(endDate)}"
    }

    fun backToForm() {
        _uiState.update { it.copy(step = PaymentMethodRevenueScreenStep.FORM) }
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
