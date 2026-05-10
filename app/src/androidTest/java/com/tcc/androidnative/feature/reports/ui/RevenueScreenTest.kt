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
import com.tcc.androidnative.feature.reports.data.RevenueReportModel
import com.tcc.androidnative.feature.reports.data.SyncMetadataModel
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class RevenueScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun form_step_should_render_single_period_field() {
        composeRule.setContent {
            AndroidNativeTheme {
                RevenueScreenContent(
                    uiState = RevenueUiState(),
                    onPeriodSelected = { _, _ -> },
                    onEmit = {},
                    onBackToForm = {}
                )
            }
        }

        composeRule.onNodeWithText("Periodo (dd/MM/yyyy)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Periodo do filtro de faturamento").assertIsDisplayed()
        composeRule.onAllNodesWithText("Data inicial (dd/MM/yyyy)").assertCountEquals(0)
        composeRule.onAllNodesWithText("Data final (dd/MM/yyyy)").assertCountEquals(0)
    }

    @Test
    fun form_step_should_open_period_picker_from_calendar_icon() {
        composeRule.setContent {
            AndroidNativeTheme {
                RevenueScreenContent(
                    uiState = RevenueUiState(),
                    onPeriodSelected = { _, _ -> },
                    onEmit = {},
                    onBackToForm = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Abrir calendario do periodo do faturamento")
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("revenue-period-picker").assertIsDisplayed()
        composeRule.onNodeWithTag("revenue-period-confirm").assertIsNotEnabled()
    }

    @Test
    fun report_step_should_render_selected_period_and_total() {
        composeRule.setContent {
            AndroidNativeTheme {
                RevenueScreenContent(
                    uiState = RevenueUiState(
                        step = RevenueScreenStep.REPORT,
                        report = RevenueReportModel(
                            totalRevenue = BigDecimal("580.55"),
                            startDate = LocalDate.of(2026, 4, 1),
                            endDate = LocalDate.of(2026, 4, 30),
                            syncMetadata = SyncMetadataModel(
                                dataUpToDate = true,
                                lastSyncAt = null,
                                reauthRequired = false
                            )
                        )
                    ),
                    onPeriodSelected = { _, _ -> },
                    onEmit = {},
                    onBackToForm = {}
                )
            }
        }

        composeRule.onNodeWithText("Relatório de faturamento").assertIsDisplayed()
        composeRule.onNodeWithText("01/04/2026 - 30/04/2026").assertIsDisplayed()
        composeRule.onNodeWithText("R$\u00a0580,55").assertIsDisplayed()
    }
}
