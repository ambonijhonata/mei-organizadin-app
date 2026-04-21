package com.tcc.androidnative.feature.payments.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.feature.payments.data.PaymentsRepository
import com.tcc.androidnative.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val paymentsRepository: PaymentsRepository
) : ViewModel() {
    private val eventId: Long = savedStateHandle[AppDestination.Payments.ARG_EVENT_ID] ?: 0L
    private val totalServiceValue: BigDecimal = parseRouteTotalServiceValue(
        savedStateHandle[AppDestination.Payments.ARG_TOTAL_SERVICE_VALUE]
    )
    private val preloadPayments: Boolean =
        savedStateHandle[AppDestination.Payments.ARG_PRELOAD_PAYMENTS] ?: true

    private val _saveEvents = MutableSharedFlow<PaymentSaveEvent>(extraBufferCapacity = 1)
    val saveEvents: SharedFlow<PaymentSaveEvent> = _saveEvents.asSharedFlow()

    private val _uiState = MutableStateFlow(
        PaymentsFormReducer.initial(
            eventId = eventId,
            totalServiceValue = totalServiceValue
        ).copy(isLoadingPayments = preloadPayments)
    )
    val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    init {
        if (preloadPayments) {
            viewModelScope.launch {
                loadPayments()
            }
        }
    }

    fun onAddPaymentClick() {
        _uiState.update { state -> PaymentsFormReducer.addPayment(state) }
    }

    fun onRemovePaymentClick(paymentId: Long) {
        _uiState.update { state -> PaymentsFormReducer.removePayment(state, paymentId) }
    }

    fun onPaymentMethodChanged(paymentId: Long, method: PaymentMethod) {
        _uiState.update { state -> PaymentsFormReducer.updateMethod(state, paymentId, method) }
    }

    fun onPaymentAmountChanged(paymentId: Long, value: String) {
        _uiState.update { state -> PaymentsFormReducer.updateAmount(state, paymentId, value) }
    }

    fun onPaymentValueTotalChanged(paymentId: Long, checked: Boolean) {
        _uiState.update { state -> PaymentsFormReducer.updateValueTotal(state, paymentId, checked) }
    }

    private suspend fun loadPayments() {
        runCatching {
            paymentsRepository.loadPayments(eventId)
        }.onSuccess { loadedPayments ->
            _uiState.update {
                val hydratedState = if (loadedPayments.isEmpty()) {
                    PaymentsFormReducer.initial(
                        eventId = eventId,
                        totalServiceValue = totalServiceValue
                    )
                } else {
                    PaymentsFormReducer.withLoadedPayments(
                        eventId = eventId,
                        totalServiceValue = totalServiceValue,
                        payments = loadedPayments
                    )
                }
                hydratedState.copy(
                    isLoadingPayments = false,
                    loadErrorMessage = null,
                    isSaving = false
                )
            }
        }.onFailure { error ->
            _uiState.update {
                if (error.isUnauthorizedPreloadFailure()) {
                    PaymentsFormReducer.initial(
                        eventId = eventId,
                        totalServiceValue = totalServiceValue
                    ).copy(
                        isLoadingPayments = false,
                        loadErrorMessage = null,
                        isSaving = false
                    )
                } else {
                    it.copy(
                        isLoadingPayments = false,
                        loadErrorMessage = error.message ?: "Falha ao carregar pagamentos."
                    )
                }
            }
        }
    }

    fun savePayments() {
        val currentState = _uiState.value
        if (currentState.isSaving) return
        if (currentState.hasPaymentWithoutAmount()) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveErrorMessage = null,
                    saveErrorMessageResId = R.string.payments_value_required_feedback
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    saveErrorMessage = null,
                    saveErrorMessageResId = null
                )
            }

            runCatching {
                paymentsRepository.savePayments(
                    eventId = currentState.eventId,
                    payments = currentState.payments
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = null,
                        saveErrorMessageResId = null
                    )
                }
                _saveEvents.tryEmit(PaymentSaveEvent.Saved)
            }.onFailure { error ->
                val saveErrorResId = error.toSaveErrorMessageResId()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveErrorMessage = if (saveErrorResId != null) {
                            null
                        } else {
                            error.message ?: "Falha ao salvar pagamentos."
                        },
                        saveErrorMessageResId = saveErrorResId
                    )
                }
            }
        }
    }

    private fun parseRouteTotalServiceValue(value: String?): BigDecimal {
        val normalized = value?.replace(",", ".")?.trim()
        return normalized?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun Throwable.isUnauthorizedPreloadFailure(): Boolean {
        return this is HttpException && code() == 401
    }

    private fun Throwable.toSaveErrorMessageResId(): Int? {
        return if (this is HttpException && code() == 422) {
            R.string.payments_value_required_feedback
        } else {
            null
        }
    }

    private fun PaymentsUiState.hasPaymentWithoutAmount(): Boolean {
        return payments.any { entry -> entry.amountInput.isBlank() }
    }
}

sealed interface PaymentSaveEvent {
    data object Saved : PaymentSaveEvent
}
