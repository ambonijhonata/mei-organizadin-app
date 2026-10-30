package com.tcc.androidnative.feature.reports.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tcc.androidnative.feature.payments.ui.PaymentMethod
import com.tcc.androidnative.feature.reports.data.PaymentMethodRevenueEntryModel
import com.tcc.androidnative.feature.reports.data.PaymentMethodRevenueReportModel
import com.tcc.androidnative.feature.reports.data.SyncMetadataModel
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class PaymentMethodRevenueScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun form_step_should_render_single_period_field() {
        composeRule.setContent {
            AndroidNativeTheme {
                PaymentMethodRevenueScreenContent(
                    uiState = PaymentMethodRevenueUiState(),
                    onPeriodSelected = { _, _ -> },
                    onEmit = {},
                    onBackToForm = {}
                )
            }
        }

        composeRule.onNodeWithText("Periodo (dd/MM/yyyy)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Periodo do filtro de faturamento por método de pagamento"
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Data inicial (dd/MM/yyyy)").assertCountEquals(0)
        composeRule.onAllNodesWithText("Data final (dd/MM/yyyy)").assertCountEquals(0)
    }

    @Test
    fun form_step_should_open_period_picker_from_calendar_icon() {
        composeRule.setContent {
            AndroidNativeTheme {
                PaymentMethodRevenueScreenContent(
                    uiState = PaymentMethodRevenueUiState(),
                    onPeriodSelected = { _, _ -> },
                    onEmit = {},
                    onBackToForm = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Abrir calendario do periodo do faturamento por método de pagamento")
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("payment-method-period-picker").assertIsDisplayed()
        composeRule.onNodeWithTag("payment-method-period-confirm").assertIsNotEnabled()
    }

    @Test
    fun report_step_should_render_period_and_payment_method_rows() {
        composeRule.setContent {
            AndroidNativeTheme {
                PaymentMethodRevenueScreenContent(
                    uiState = PaymentMethodRevenueUiState(
                        step = PaymentMethodRevenueScreenStep.REPORT,
                        report = PaymentMethodRevenueReportModel(
                            entries = listOf(
                                PaymentMethodRevenueEntryModel(PaymentMethod.DINHEIRO, BigDecimal("456.12")),
                                PaymentMethodRevenueEntryModel(PaymentMethod.PIX, BigDecimal("123.56")),
                                PaymentMethodRevenueEntryModel(PaymentMethod.DEBITO, BigDecimal("789.56")),
                                PaymentMethodRevenueEntryModel(PaymentMethod.CREDITO, BigDecimal("741.56"))
                            ),
                            startDate = LocalDate.of(2026, 4, 1),
                            endDate = LocalDate.of(2026, 4, 30),
                            syncMetadata = SyncMetadataModel(
                                dataUpToDate = false,
                                lastSyncAt = null,
                                reauthRequired = false
                            )
                        ),
                        staleDataWarning = true
                    ),
                    onPeriodSelected = { _, _ -> },
                    onEmit = {},
                    onBackToForm = {}
                )
            }
        }

        composeRule.onNodeWithText("Faturamento por método de pagamento").assertIsDisplayed()
        composeRule.onNodeWithText("01/04/2026 - 30/04/2026").assertIsDisplayed()
        composeRule.onNodeWithText("Dinheiro").assertIsDisplayed()
        composeRule.onNodeWithText("PIX").assertIsDisplayed()
        composeRule.onNodeWithText("Débito").assertIsDisplayed()
        composeRule.onNodeWithText("Crédito").assertIsDisplayed()
        composeRule.onNodeWithText("Dados desatualizados").assertIsDisplayed()
    }
}
