package com.tcc.androidnative.feature.payments.ui

import com.tcc.androidnative.core.util.CurrencyFormats
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentsFormReducerTest {
    @Test
    fun `should keep only selected entry when total value is checked`() {
        var state = PaymentsFormReducer.initial(
            eventId = 2L,
            totalServiceValue = BigDecimal("65.00")
        )
        state = PaymentsFormReducer.addPayment(state)
        val secondId = state.payments[1].id

        state = PaymentsFormReducer.updateValueTotal(
            state = state,
            paymentId = secondId,
            checked = true
        )

        assertEquals(1, state.payments.size)
        assertEquals(secondId, state.payments.first().id)
        assertTrue(state.payments.first().isValueTotal)
        assertEquals(0, state.totalPaid.compareTo(BigDecimal("65.00")))
        assertFalse(state.canAddPayment)
    }

    @Test
    fun `should suggest remaining amount when adding a payment entry`() {
        var state = PaymentsFormReducer.initial(
            eventId = 3L,
            totalServiceValue = BigDecimal("100.00")
        )

        state = PaymentsFormReducer.updateAmount(
            state = state,
            paymentId = state.payments.first().id,
            amountInput = "R$ 40,00"
        )
        state = PaymentsFormReducer.addPayment(state)

        val newEntry = state.payments.last()
        assertEquals(CurrencyFormats.formatForUi(BigDecimal("60.00")), newEntry.amountInput)
        assertEquals(PaymentMethod.PIX, newEntry.method)
        assertEquals(0, state.totalPaid.compareTo(BigDecimal("100.00")))
        assertFalse(state.canAddPayment)
    }

    @Test
    fun `should disable add button when total is reached and enable when total goes below target`() {
        var state = PaymentsFormReducer.initial(
            eventId = 4L,
            totalServiceValue = BigDecimal("100.00")
        )
        val firstId = state.payments.first().id

        state = PaymentsFormReducer.updateAmount(state, firstId, "R$ 20,00")
        assertTrue(state.canAddPayment)

        state = PaymentsFormReducer.addPayment(state)
        val secondId = state.payments.last().id
        assertFalse(state.canAddPayment)

        state = PaymentsFormReducer.updateAmount(state, secondId, "R$ 70,00")
        assertTrue(state.canAddPayment)
        assertEquals(0, state.totalPaid.compareTo(BigDecimal("90.00")))
    }

    @Test
    fun `should block amount composition above total service value`() {
        var state = PaymentsFormReducer.initial(
            eventId = 5L,
            totalServiceValue = BigDecimal("100.00")
        )
        val firstId = state.payments.first().id

        state = PaymentsFormReducer.updateAmount(state, firstId, "R$ 80,00")
        state = PaymentsFormReducer.addPayment(state)
        val secondId = state.payments.last().id

        val afterInvalid = PaymentsFormReducer.updateAmount(state, secondId, "R$ 30,00")

        assertEquals(CurrencyFormats.formatForUi(BigDecimal("20.00")), afterInvalid.payments.last().amountInput)
        assertEquals(0, afterInvalid.totalPaid.compareTo(BigDecimal("100.00")))
    }

    @Test
    fun `loaded payments should prefill entries and totals by method`() {
        val state = PaymentsFormReducer.withLoadedPayments(
            eventId = 6L,
            totalServiceValue = BigDecimal("150.00"),
            payments = listOf(
                PaymentEntryUiState(
                    id = 10L,
                    method = PaymentMethod.PIX,
                    amountInput = "R$ 50,00",
                    isValueTotal = false
                ),
                PaymentEntryUiState(
                    id = 11L,
                    method = PaymentMethod.DEBITO,
                    amountInput = "R$ 100,00",
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

    @Test
    fun `unchecking total value should keep entry editable and allow adding when value goes below total`() {
        var state = PaymentsFormReducer.initial(
            eventId = 7L,
            totalServiceValue = BigDecimal("120.00")
        )
        val firstId = state.payments.first().id

        state = PaymentsFormReducer.updateValueTotal(state, firstId, true)
        state = PaymentsFormReducer.updateValueTotal(state, firstId, false)
        state = PaymentsFormReducer.updateAmount(state, firstId, "R$ 40,00")

        assertEquals(null, state.totalValueOwnerPaymentId)
        assertTrue(state.canAddPayment)
        assertEquals(0, state.totalPaid.compareTo(BigDecimal("40.00")))
    }

    @Test
    fun `new payment should use a method not selected by other entries`() {
        var state = PaymentsFormReducer.initial(
            eventId = 8L,
            totalServiceValue = BigDecimal("100.00")
        )
        val firstId = state.payments.first().id
        state = PaymentsFormReducer.updateMethod(state, firstId, PaymentMethod.DINHEIRO)
        state = PaymentsFormReducer.updateAmount(state, firstId, "R$ 40,00")

        state = PaymentsFormReducer.addPayment(state)

        assertEquals(PaymentMethod.PIX, state.payments.last().method)
    }
}
