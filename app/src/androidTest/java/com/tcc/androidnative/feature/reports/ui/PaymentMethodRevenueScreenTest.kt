package com.tcc.androidnative.feature.reports.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
                    onStartDateChange = {},
                    onEndDateChange = {},
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
