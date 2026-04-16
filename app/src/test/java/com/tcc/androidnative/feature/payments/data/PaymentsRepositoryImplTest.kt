package com.tcc.androidnative.feature.payments.data

import com.tcc.androidnative.feature.calendar.data.remote.CalendarApi
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentEntryResponseDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentsResponseDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentsUpsertRequestDto
import com.tcc.androidnative.feature.payments.ui.PaymentEntryUiState
import com.tcc.androidnative.feature.payments.ui.PaymentMethod
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentsRepositoryImplTest {
    @Test
    fun `savePayments should send payment composition to api`() = runBlocking {
        val fakeApi = FakeCalendarApi()
        val repository = PaymentsRepositoryImpl(fakeApi)

        val response = repository.savePayments(
            eventId = 42L,
            payments = listOf(
                PaymentEntryUiState(
                    id = 1L,
                    method = PaymentMethod.PIX,
                    amountInput = "50.00",
                    isValueTotal = false
                ),
                PaymentEntryUiState(
                    id = 2L,
                    method = PaymentMethod.DEBITO,
                    amountInput = "25.50",
                    isValueTotal = true
                )
            )
        )

        assertEquals(42L, response.eventId)
        assertTrue(fakeApi.lastRequest != null)
        assertEquals("PIX", fakeApi.lastRequest?.payments?.first()?.paymentType)
        assertEquals(BigDecimal("25.50"), fakeApi.lastRequest?.payments?.last()?.amount)
        assertTrue(fakeApi.lastRequest?.payments?.last()?.valueTotal == true)
    }

    @Test
    fun `loadPayments should map api response to ui entries`() = runBlocking {
        val fakeApi = FakeCalendarApi(
            paymentsResponse = CalendarPaymentsResponseDto(
                eventId = 42L,
                payments = listOf(
                    CalendarPaymentEntryResponseDto(
                        id = 7L,
                        paymentType = "PIX",
                        amount = BigDecimal("50.00"),
                        valueTotal = false,
                        paidAt = "2026-01-01T10:00:00Z"
                    )
                )
            )
        )
        val repository = PaymentsRepositoryImpl(fakeApi)

        val loaded = repository.loadPayments(eventId = 42L)

        assertEquals(1, loaded.size)
        assertEquals(7L, loaded.first().id)
        assertEquals(PaymentMethod.PIX, loaded.first().method)
        assertEquals("50", loaded.first().amountInput)
        assertTrue(!loaded.first().isValueTotal)
    }
}

private class FakeCalendarApi : CalendarApi {
    constructor(
        paymentsResponse: CalendarPaymentsResponseDto? = null
    ) {
        this.paymentsResponse = paymentsResponse
    }

    var lastRequest: CalendarPaymentsUpsertRequestDto? = null
    private var paymentsResponse: CalendarPaymentsResponseDto? = null

    override suspend fun sync(startDate: String?) = error("not needed")

    override suspend fun listEvents(
        eventStart: String?,
        eventEnd: String?,
        page: Int,
        size: Int
    ) = error("not needed")

    override suspend fun status() = error("not needed")

    override suspend fun getPayments(eventId: Long): CalendarPaymentsResponseDto {
        return paymentsResponse ?: error("not needed")
    }

    override suspend fun upsertPayments(
        eventId: Long,
        body: CalendarPaymentsUpsertRequestDto
    ): CalendarPaymentsResponseDto {
        lastRequest = body
        return CalendarPaymentsResponseDto(
            eventId = eventId,
            payments = body.payments.mapIndexed { index, payment ->
                CalendarPaymentEntryResponseDto(
                    id = index.toLong() + 1L,
                    paymentType = payment.paymentType,
                    amount = payment.amount,
                    valueTotal = payment.valueTotal,
                    paidAt = "2026-01-01T10:00:00Z"
                )
            }
        )
    }
}
