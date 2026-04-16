package com.tcc.androidnative.feature.payments.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test

class PaymentsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun payments_should_show_only_supported_method_options() {
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(
                    PaymentsFormReducer.initial(
                        eventId = 1L,
                        totalServiceValue = BigDecimal("100.00")
                    )
                )
                PaymentsContent(
                    uiState = state,
                    onAddPaymentClick = { state = PaymentsFormReducer.addPayment(state) },
                    onRemovePaymentClick = { paymentId -> state = PaymentsFormReducer.removePayment(state, paymentId) },
                    onPaymentMethodChanged = { paymentId, method ->
                        state = PaymentsFormReducer.updateMethod(state, paymentId, method)
                    },
                    onPaymentAmountChanged = { paymentId, value ->
                        state = PaymentsFormReducer.updateAmount(state, paymentId, value)
                    },
                    onPaymentValueTotalChanged = { paymentId, checked ->
                        state = PaymentsFormReducer.updateValueTotal(state, paymentId, checked)
                    },
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("payment_method_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PIX").assertIsDisplayed()
        composeRule.onNodeWithText("Crédito").assertIsDisplayed()
        composeRule.onNodeWithText("Débito").assertIsDisplayed()
    }

    @Test
    fun payments_should_disable_value_field_and_add_button_when_total_value_is_checked() {
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(
                    PaymentsFormReducer.initial(
                        eventId = 1L,
                        totalServiceValue = BigDecimal("100.00")
                    )
                )
                PaymentsContent(
                    uiState = state,
                    onAddPaymentClick = { state = PaymentsFormReducer.addPayment(state) },
                    onRemovePaymentClick = { paymentId -> state = PaymentsFormReducer.removePayment(state, paymentId) },
                    onPaymentMethodChanged = { paymentId, method ->
                        state = PaymentsFormReducer.updateMethod(state, paymentId, method)
                    },
                    onPaymentAmountChanged = { paymentId, value ->
                        state = PaymentsFormReducer.updateAmount(state, paymentId, value)
                    },
                    onPaymentValueTotalChanged = { paymentId, checked ->
                        state = PaymentsFormReducer.updateValueTotal(state, paymentId, checked)
                    },
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("payment_total_checkbox_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payment_amount_1").assertIsNotEnabled()
        composeRule.onNodeWithTag("add_payment_button").assertIsNotEnabled()
    }

    @Test
    fun payments_should_limit_to_four_entries() {
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(
                    PaymentsFormReducer.initial(
                        eventId = 1L,
                        totalServiceValue = BigDecimal("100.00")
                    )
                )
                PaymentsContent(
                    uiState = state,
                    onAddPaymentClick = { state = PaymentsFormReducer.addPayment(state) },
                    onRemovePaymentClick = { paymentId -> state = PaymentsFormReducer.removePayment(state, paymentId) },
                    onPaymentMethodChanged = { paymentId, method ->
                        state = PaymentsFormReducer.updateMethod(state, paymentId, method)
                    },
                    onPaymentAmountChanged = { paymentId, value ->
                        state = PaymentsFormReducer.updateAmount(state, paymentId, value)
                    },
                    onPaymentValueTotalChanged = { paymentId, checked ->
                        state = PaymentsFormReducer.updateValueTotal(state, paymentId, checked)
                    },
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("add_payment_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_payment_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_payment_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_payment_button").assertIsNotEnabled()
        composeRule.onAllNodesWithText("Valor *").assertCountEquals(4)
    }

    @Test
    fun payments_should_block_value_input_that_exceeds_total_service_value() {
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(
                    PaymentsFormReducer.addPayment(
                        PaymentsFormReducer.initial(
                            eventId = 1L,
                            totalServiceValue = BigDecimal("100.00")
                        )
                    )
                )
                PaymentsContent(
                    uiState = state,
                    onAddPaymentClick = { state = PaymentsFormReducer.addPayment(state) },
                    onRemovePaymentClick = { paymentId -> state = PaymentsFormReducer.removePayment(state, paymentId) },
                    onPaymentMethodChanged = { paymentId, method ->
                        state = PaymentsFormReducer.updateMethod(state, paymentId, method)
                    },
                    onPaymentAmountChanged = { paymentId, value ->
                        state = PaymentsFormReducer.updateAmount(state, paymentId, value)
                    },
                    onPaymentValueTotalChanged = { paymentId, checked ->
                        state = PaymentsFormReducer.updateValueTotal(state, paymentId, checked)
                    },
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payment_amount_1").performTextInput("80")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payment_amount_2").performTextInput("30")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Total pago:").assertIsDisplayed()
        composeRule.onNodeWithText("R$\u00a080,00").assertIsDisplayed()
        composeRule.onNodeWithTag("add_payment_button").assertIsEnabled()
    }

    @Test
    fun payments_should_render_totals_by_payment_type_when_loaded() {
        composeRule.setContent {
            AndroidNativeTheme {
                PaymentsContent(
                    uiState = PaymentsFormReducer.withLoadedPayments(
                        eventId = 1L,
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
                    ),
                    onAddPaymentClick = {},
                    onRemovePaymentClick = {},
                    onPaymentMethodChanged = { _, _ -> },
                    onPaymentAmountChanged = { _, _ -> },
                    onPaymentValueTotalChanged = { _, _ -> },
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Totais por metodo:").assertIsDisplayed()
        composeRule.onNodeWithText("PIX").assertIsDisplayed()
        composeRule.onNodeWithText("Débito").assertIsDisplayed()
        composeRule.onNodeWithText("R$\u00a050,00").assertIsDisplayed()
        composeRule.onNodeWithText("R$\u00a0100,00").assertIsDisplayed()
    }
}
