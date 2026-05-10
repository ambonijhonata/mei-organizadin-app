package com.tcc.androidnative.feature.reports.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.tcc.androidnative.feature.reports.data.CashFlowEntryModel
import com.tcc.androidnative.feature.reports.data.CashFlowReportModel
import com.tcc.androidnative.feature.reports.data.CashFlowServiceModel
import com.tcc.androidnative.feature.reports.data.SyncMetadataModel
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class CashFlowScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cashFlowDetail_should_render_quantity_column_and_values() {
        val detailDate = LocalDate.of(2026, 5, 7)
        val detailEntry = CashFlowEntryModel(
            date = detailDate,
            total = BigDecimal("71.00"),
            services = listOf(
                CashFlowServiceModel(
                    name = "Sobrancelha",
                    quantity = 2,
                    total = BigDecimal("48.00")
                ),
                CashFlowServiceModel(
                    name = "Buco",
                    quantity = 1,
                    total = BigDecimal("23.00")
                )
            )
        )

        composeRule.setContent {
            AndroidNativeTheme {
                CashFlowScreenContent(
                    uiState = CashFlowUiState(
                        step = CashFlowScreenStep.DETAIL,
                        report = CashFlowReportModel(
                            entries = listOf(detailEntry),
                            startDate = detailDate,
                            endDate = detailDate,
                            syncMetadata = SyncMetadataModel(
                                dataUpToDate = true,
                                lastSyncAt = null,
                                reauthRequired = false
                            )
                        ),
                        selectedDetail = detailEntry
                    ),
                    onPeriodSelected = { _, _ -> },
                    onEmit = {},
                    onBackToForm = {},
                    onOpenDetail = {},
                    onBackToReport = {}
                )
            }
        }

        composeRule.onNodeWithText("Qtd.").assertIsDisplayed()
        composeRule.onNodeWithText("07/05/26").assertIsDisplayed()
        composeRule.onNodeWithText("Sobrancelha").assertIsDisplayed()
        composeRule.onNodeWithText("Buco").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("R$\u00a048,00").assertIsDisplayed()
        composeRule.onNodeWithText("R$\u00a023,00").assertIsDisplayed()
    }

    @Test
    fun cashFlowReport_should_keep_total_visible_on_compact_width() {
        val reportDate = LocalDate.of(2026, 5, 7)

        composeRule.setContent {
            AndroidNativeTheme {
                Box(modifier = Modifier.requiredWidth(320.dp)) {
                    CashFlowScreenContent(
                        uiState = CashFlowUiState(
                            step = CashFlowScreenStep.REPORT,
                            report = CashFlowReportModel(
                                entries = listOf(
                                    CashFlowEntryModel(
                                        date = reportDate,
                                        total = BigDecimal("1234.56"),
                                        services = listOf(
                                            CashFlowServiceModel(
                                                name = "Design de sobrancelha com henna",
                                                quantity = 1,
                                                total = BigDecimal("1234.56")
                                            )
                                        )
                                    )
                                ),
                                startDate = reportDate,
                                endDate = reportDate,
                                syncMetadata = SyncMetadataModel(
                                    dataUpToDate = true,
                                    lastSyncAt = null,
                                    reauthRequired = false
                                )
                            )
                        ),
                        onPeriodSelected = { _, _ -> },
                        onEmit = {},
                        onBackToForm = {},
                        onOpenDetail = {},
                        onBackToReport = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("07/05/2026").assertIsDisplayed()
        composeRule.onNodeWithText("R$\u00a01.234,56").assertIsDisplayed()
        composeRule.onNodeWithText("Design de sobrancelha com henna").assertIsDisplayed()
    }

    @Test
    fun cashFlowDetail_should_switch_to_compact_layout_on_compact_width() {
        val detailDate = LocalDate.of(2026, 5, 7)
        val detailEntry = CashFlowEntryModel(
            date = detailDate,
            total = BigDecimal("71.00"),
            services = listOf(
                CashFlowServiceModel(
                    name = "Design de sobrancelha com henna",
                    quantity = 2,
                    total = BigDecimal("48.00")
                )
            )
        )

        composeRule.setContent {
            AndroidNativeTheme {
                Box(modifier = Modifier.requiredWidth(320.dp)) {
                    CashFlowScreenContent(
                        uiState = CashFlowUiState(
                            step = CashFlowScreenStep.DETAIL,
                            report = CashFlowReportModel(
                                entries = listOf(detailEntry),
                                startDate = detailDate,
                                endDate = detailDate,
                                syncMetadata = SyncMetadataModel(
                                    dataUpToDate = true,
                                    lastSyncAt = null,
                                    reauthRequired = false
                                )
                            ),
                            selectedDetail = detailEntry
                        ),
                        onPeriodSelected = { _, _ -> },
                        onEmit = {},
                        onBackToForm = {},
                        onOpenDetail = {},
                        onBackToReport = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("cashflow-detail-compact-header").assertIsDisplayed()
        composeRule.onNodeWithText("Qtd.: 2").assertIsDisplayed()
        composeRule.onNodeWithText("R$\u00a048,00").assertIsDisplayed()
    }

    @Test
    fun cashFlowForm_should_render_single_period_field() {
        composeRule.setContent {
            AndroidNativeTheme {
                CashFlowScreenContent(
                    uiState = CashFlowUiState(),
                    onPeriodSelected = { _, _ -> },
                    onEmit = {},
                    onBackToForm = {},
                    onOpenDetail = {},
                    onBackToReport = {}
                )
            }
        }

        composeRule.onNodeWithText("Periodo (dd/MM/yyyy)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Periodo do filtro de fluxo de caixa").assertIsDisplayed()
        composeRule.onAllNodesWithText("Data inicial (dd/MM/yyyy)").assertCountEquals(0)
        composeRule.onAllNodesWithText("Data final (dd/MM/yyyy)").assertCountEquals(0)
    }

    @Test
    fun cashFlowForm_should_open_period_picker_from_calendar_icon() {
        composeRule.setContent {
            AndroidNativeTheme {
                CashFlowScreenContent(
                    uiState = CashFlowUiState(),
                    onPeriodSelected = { _, _ -> },
                    onEmit = {},
                    onBackToForm = {},
                    onOpenDetail = {},
                    onBackToReport = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Abrir calendario do periodo do fluxo de caixa")
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("cashflow-period-picker").assertIsDisplayed()
        composeRule.onNodeWithTag("cashflow-period-confirm").assertIsNotEnabled()
    }

    @Test
    fun cashFlowPeriodPicker_should_disable_confirm_when_only_one_date_is_selected() {
        composeRule.setContent {
            AndroidNativeTheme {
                CashFlowPeriodPickerDialog(
                    initialSelectedStartDate = LocalDate.of(2026, 5, 9),
                    initialSelectedEndDate = null,
                    onDismiss = {},
                    onConfirm = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("cashflow-period-confirm").assertIsNotEnabled()
    }

    @Test
    fun cashFlowPeriodPicker_should_enable_confirm_for_valid_one_month_range() {
        composeRule.setContent {
            AndroidNativeTheme {
                CashFlowPeriodPickerDialog(
                    initialSelectedStartDate = LocalDate.of(2026, 5, 9),
                    initialSelectedEndDate = LocalDate.of(2026, 6, 9),
                    onDismiss = {},
                    onConfirm = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("cashflow-period-confirm").assertIsEnabled()
    }

    @Test
    fun cashFlowPeriodPicker_should_disable_confirm_for_range_greater_than_one_month() {
        composeRule.setContent {
            AndroidNativeTheme {
                CashFlowPeriodPickerDialog(
                    initialSelectedStartDate = LocalDate.of(2026, 5, 9),
                    initialSelectedEndDate = LocalDate.of(2026, 6, 10),
                    onDismiss = {},
                    onConfirm = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("cashflow-period-confirm").assertIsNotEnabled()
    }
}
