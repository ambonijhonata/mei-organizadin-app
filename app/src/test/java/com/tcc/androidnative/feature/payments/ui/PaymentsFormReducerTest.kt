package com.tcc.androidnative.feature.payments.ui

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentsFormReducerTest {
    @Test
    fun `should allow at most four payment entries`() {
        var state = PaymentsFormReducer.initial(
            eventId = 1L,
            totalServiceValue = BigDecimal("100.00")
        )

        state = PaymentsFormReducer.addPayment(state)
        state = PaymentsFormReducer.addPayment(state)
        state = PaymentsFormReducer.addPayment(state)
        val stateAfterFourth = state
        state = PaymentsFormReducer.addPayment(state)

        assertEquals(4, state.payments.size)
        assertFalse(state.canAddPayment)
        assertEquals(stateAfterFourth, state)
    }

    @Test
    fun `should enforce total value selection exclusivity`() {
        var state = PaymentsFormReducer.initial(
            eventId = 2L,
            totalServiceValue = BigDecimal("65.00")
        )
        state = PaymentsFormReducer.addPayment(state)

        val firstId = state.payments[0].id
        val secondId = state.payments[1].id
        state = PaymentsFormReducer.updateValueTotal(
            state = state,
            paymentId = secondId,
            checked = true
        )

        assertEquals(secondId, state.totalValueOwnerPaymentId)
        assertTrue(state.payments.first { it.id == secondId }.isValueTotal)
        assertFalse(state.payments.first { it.id == firstId }.isValueTotal)
        assertEquals(BigDecimal("65.00"), state.totalPaid)
        assertFalse(state.canAddPayment)
    }

    @Test
    fun `should block amount composition above total service value`() {
        var state = PaymentsFormReducer.initial(
            eventId = 3L,
            totalServiceValue = BigDecimal("100.00")
        )
        state = PaymentsFormReducer.addPayment(state)
        val firstId = state.payments[0].id
        val secondId = state.payments[1].id

        state = PaymentsFormReducer.updateAmount(state, firstId, "80")
        val afterValid = PaymentsFormReducer.updateAmount(state, secondId, "20")
        val afterInvalid = PaymentsFormReducer.updateAmount(afterValid, secondId, "30")

        assertEquals(0, afterValid.totalPaid.compareTo(BigDecimal("100.00")))
        assertEquals("20", afterInvalid.payments.first { it.id == secondId }.amountInput)
        assertEquals(0, afterInvalid.totalPaid.compareTo(BigDecimal("100.00")))
    }

    @Test
    fun `unchecking total value should return to partial composition mode`() {
        var state = PaymentsFormReducer.initial(
            eventId = 4L,
            totalServiceValue = BigDecimal("120.00")
        )
        val firstId = state.payments.first().id

        state = PaymentsFormReducer.updateValueTotal(state, firstId, true)
        state = PaymentsFormReducer.updateValueTotal(state, firstId, false)
        state = PaymentsFormReducer.updateAmount(state, firstId, "40")

        assertEquals(null, state.totalValueOwnerPaymentId)
        assertTrue(state.canAddPayment)
        assertEquals(0, state.totalPaid.compareTo(BigDecimal("40.00")))
    }

    @Test
    fun `loaded payments should prefill entries and totals by method`() {
        val state = PaymentsFormReducer.withLoadedPayments(
            eventId = 5L,
            totalServiceValue = BigDecimal("150.00"),
            payments = listOf(
                PaymentEntryUiState(
                    id = 10L,
                    method = PaymentMethod.PIX,
                    amountInput = "50",
                    isValueTotal = false
                ),
                PaymentEntryUiState(
                    id = 11L,
                    method = PaymentMethod.DEBITO,
                    amountInput = "100",
                    isValueTotal = false
                )
            )
        )

        assertEquals(2, state.payments.size)
        assertEquals(PaymentMethod.PIX, state.payments[0].method)
        assertEquals(0, state.totalPaid.compareTo(BigDecimal("150.00")))
        assertEquals(2, state.paymentTypeTotals.size)
        assertEquals(PaymentMethod.PIX, state.paymentTypeTotals[0].method)
        assertEquals(0, state.paymentTypeTotals[0].total.compareTo(BigDecimal("50")))
        assertEquals(PaymentMethod.DEBITO, state.paymentTypeTotals[1].method)
        assertEquals(0, state.paymentTypeTotals[1].total.compareTo(BigDecimal("100")))
    }
}
