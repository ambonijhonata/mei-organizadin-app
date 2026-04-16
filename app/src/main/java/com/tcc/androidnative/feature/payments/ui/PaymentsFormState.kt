package com.tcc.androidnative.feature.payments.ui

import java.math.BigDecimal

enum class PaymentMethod {
    DINHEIRO,
    PIX,
    CREDITO,
    DEBITO
}

data class PaymentEntryUiState(
    val id: Long,
    val method: PaymentMethod = PaymentMethod.DINHEIRO,
    val amountInput: String = "",
    val isValueTotal: Boolean = false
)

data class PaymentMethodTotalUiState(
    val method: PaymentMethod,
    val total: BigDecimal
)

data class PaymentsUiState(
    val eventId: Long = 0L,
    val totalServiceValue: BigDecimal = BigDecimal.ZERO,
    val payments: List<PaymentEntryUiState> = listOf(PaymentEntryUiState(id = 1L)),
    val totalPaid: BigDecimal = BigDecimal.ZERO,
    val paymentTypeTotals: List<PaymentMethodTotalUiState> = emptyList(),
    val canAddPayment: Boolean = true,
    val totalValueOwnerPaymentId: Long? = null,
    val isLoadingPayments: Boolean = false,
    val isSaving: Boolean = false,
    val loadErrorMessage: String? = null,
    val saveErrorMessage: String? = null
)

object PaymentsFormReducer {
    private const val MAX_PAYMENT_ENTRIES = 4

    fun initial(
        eventId: Long,
        totalServiceValue: BigDecimal
    ): PaymentsUiState {
        return recalculate(
            PaymentsUiState(
                eventId = eventId,
                totalServiceValue = totalServiceValue
            )
        )
    }

    fun withLoadedPayments(
        eventId: Long,
        totalServiceValue: BigDecimal,
        payments: List<PaymentEntryUiState>
    ): PaymentsUiState {
        val normalizedPayments = if (payments.isEmpty()) {
            listOf(PaymentEntryUiState(id = 1L))
        } else {
            payments
        }
        return recalculate(
            PaymentsUiState(
                eventId = eventId,
                totalServiceValue = totalServiceValue,
                payments = normalizedPayments
            )
        )
    }

    fun addPayment(state: PaymentsUiState): PaymentsUiState {
        if (!state.canAddPayment) return state
        val nextId = nextPaymentId(state.payments)
        return recalculate(
            state.copy(
                payments = state.payments + PaymentEntryUiState(id = nextId)
            )
        )
    }

    fun removePayment(state: PaymentsUiState, paymentId: Long): PaymentsUiState {
        val filtered = state.payments.filterNot { it.id == paymentId }
        val normalized = if (filtered.isEmpty()) {
            listOf(PaymentEntryUiState(id = nextPaymentId(state.payments)))
        } else {
            filtered
        }
        return recalculate(state.copy(payments = normalized))
    }

    fun updateMethod(
        state: PaymentsUiState,
        paymentId: Long,
        method: PaymentMethod
    ): PaymentsUiState {
        return recalculate(
            state.copy(
                payments = state.payments.map { entry ->
                    if (entry.id == paymentId) entry.copy(method = method) else entry
                }
            )
        )
    }

    fun updateAmount(
        state: PaymentsUiState,
        paymentId: Long,
        amountInput: String
    ): PaymentsUiState {
        val targetEntry = state.payments.firstOrNull { it.id == paymentId } ?: return state
        if (targetEntry.isValueTotal) return state
        if (!isValidAmountInput(amountInput)) return state

        val updatedPayments = state.payments.map { entry ->
            if (entry.id == paymentId) entry.copy(amountInput = amountInput) else entry
        }
        val updatedState = state.copy(payments = updatedPayments)
        val hasTotalValueSelection = updatedPayments.any { it.isValueTotal }
        if (!hasTotalValueSelection && calculatePartialTotal(updatedPayments) > state.totalServiceValue) {
            return state
        }

        return recalculate(updatedState)
    }

    fun updateValueTotal(
        state: PaymentsUiState,
        paymentId: Long,
        checked: Boolean
    ): PaymentsUiState {
        val updatedPayments = if (checked) {
            state.payments.map { entry ->
                if (entry.id == paymentId) {
                    entry.copy(
                        isValueTotal = true,
                        amountInput = toAmountInput(state.totalServiceValue)
                    )
                } else {
                    entry.copy(isValueTotal = false)
                }
            }
        } else {
            state.payments.map { entry ->
                if (entry.id == paymentId) entry.copy(isValueTotal = false) else entry
            }
        }

        return recalculate(state.copy(payments = updatedPayments))
    }

    private fun recalculate(state: PaymentsUiState): PaymentsUiState {
        val totalOwner = state.payments.firstOrNull { it.isValueTotal }?.id
        val totalPaid = if (totalOwner != null) {
            state.totalServiceValue
        } else {
            calculatePartialTotal(state.payments)
        }
        val canAdd = state.payments.size < MAX_PAYMENT_ENTRIES && totalOwner == null
        val paymentTypeTotals = calculatePaymentTypeTotals(state.payments)
        return state.copy(
            totalPaid = totalPaid,
            canAddPayment = canAdd,
            totalValueOwnerPaymentId = totalOwner,
            paymentTypeTotals = paymentTypeTotals
        )
    }

    private fun calculatePartialTotal(payments: List<PaymentEntryUiState>): BigDecimal {
        return payments.fold(BigDecimal.ZERO) { acc, entry ->
            acc + (parseAmount(entry.amountInput) ?: BigDecimal.ZERO)
        }
    }

    private fun calculatePaymentTypeTotals(
        payments: List<PaymentEntryUiState>
    ): List<PaymentMethodTotalUiState> {
        return PaymentMethod.entries.mapNotNull { method ->
            val total = payments.fold(BigDecimal.ZERO) { acc, entry ->
                if (entry.method == method) {
                    acc + (parseAmount(entry.amountInput) ?: BigDecimal.ZERO)
                } else {
                    acc
                }
            }

            if (total.compareTo(BigDecimal.ZERO) > 0) {
                PaymentMethodTotalUiState(method = method, total = total)
            } else {
                null
            }
        }
    }

    private fun nextPaymentId(payments: List<PaymentEntryUiState>): Long {
        return (payments.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    private fun isValidAmountInput(input: String): Boolean {
        if (input.isBlank()) return true
        val parsed = parseAmount(input) ?: return false
        return parsed >= BigDecimal.ZERO
    }

    private fun parseAmount(input: String): BigDecimal? {
        val normalized = input.replace(",", ".").trim()
        if (normalized.isBlank()) return null
        return normalized.toBigDecimalOrNull()
    }

    private fun toAmountInput(value: BigDecimal): String {
        return value.stripTrailingZeros().toPlainString()
    }
}
