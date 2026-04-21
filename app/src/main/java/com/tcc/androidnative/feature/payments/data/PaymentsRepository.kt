package com.tcc.androidnative.feature.payments.data

import com.tcc.androidnative.feature.calendar.data.remote.CalendarApi
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentEntryRequestDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentsResponseDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentsUpsertRequestDto
import com.tcc.androidnative.core.util.CurrencyFormats
import com.tcc.androidnative.feature.payments.ui.PaymentEntryUiState
import com.tcc.androidnative.feature.payments.ui.PaymentMethod
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

interface PaymentsRepository {
    suspend fun loadPayments(eventId: Long): List<PaymentEntryUiState>

    suspend fun savePayments(
        eventId: Long,
        payments: List<PaymentEntryUiState>
    ): CalendarPaymentsResponseDto
}

@Singleton
class PaymentsRepositoryImpl @Inject constructor(
    private val calendarApi: CalendarApi
) : PaymentsRepository {
    override suspend fun loadPayments(eventId: Long): List<PaymentEntryUiState> {
        return calendarApi.getPayments(eventId = eventId)
            .payments
            .map { entry ->
                PaymentEntryUiState(
                    id = entry.id,
                    method = PaymentMethod.valueOf(entry.paymentType.uppercase()),
                    amountInput = CurrencyFormats.formatForUi(entry.amount),
                    isValueTotal = entry.valueTotal
                )
            }
    }

    override suspend fun savePayments(
        eventId: Long,
        payments: List<PaymentEntryUiState>
    ): CalendarPaymentsResponseDto {
        val request = CalendarPaymentsUpsertRequestDto(
            payments = payments.map { entry ->
                CalendarPaymentEntryRequestDto(
                    paymentType = entry.method.name,
                    amount = parseAmount(entry.amountInput),
                    valueTotal = entry.isValueTotal
                )
            }
        )
        return calendarApi.upsertPayments(eventId = eventId, body = request)
    }

    private fun parseAmount(input: String): BigDecimal {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return BigDecimal.ZERO
        val legacyNumericInput = LEGACY_NUMERIC_INPUT_REGEX.matchEntire(trimmed) != null
        return if (legacyNumericInput) {
            trimmed.replace(",", ".").toBigDecimalOrNull() ?: BigDecimal.ZERO
        } else {
            CurrencyFormats.parseUiValue(trimmed)
        }
    }

    private companion object {
        private val LEGACY_NUMERIC_INPUT_REGEX = Regex("""^\d+([.,]\d{0,2})?$""")
    }
}
