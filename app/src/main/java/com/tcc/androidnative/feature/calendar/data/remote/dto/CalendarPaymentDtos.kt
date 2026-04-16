package com.tcc.androidnative.feature.calendar.data.remote.dto

import java.math.BigDecimal

enum class CalendarPaymentStatusDto {
    NONE,
    PARTIAL,
    PAID
}

data class CalendarPaymentSummaryDto(
    val paidAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val status: CalendarPaymentStatusDto
)

data class CalendarPaymentEntryRequestDto(
    val paymentType: String,
    val amount: BigDecimal,
    val valueTotal: Boolean
)

data class CalendarPaymentsUpsertRequestDto(
    val payments: List<CalendarPaymentEntryRequestDto>
)

data class CalendarPaymentEntryResponseDto(
    val id: Long,
    val paymentType: String,
    val amount: BigDecimal,
    val valueTotal: Boolean,
    val paidAt: String?
)

data class CalendarPaymentsResponseDto(
    val eventId: Long,
    val payments: List<CalendarPaymentEntryResponseDto>
)
