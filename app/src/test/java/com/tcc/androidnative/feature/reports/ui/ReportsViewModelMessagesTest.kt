package com.tcc.androidnative.feature.reports.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.feature.reports.data.CashFlowEntryModel
import com.tcc.androidnative.feature.reports.data.CashFlowReportModel
import com.tcc.androidnative.feature.reports.data.ReportsRepository
import com.tcc.androidnative.feature.reports.data.RevenueReportModel
import com.tcc.androidnative.feature.reports.data.SyncMetadataModel
import com.tcc.androidnative.testutil.MainDispatcherRule
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelMessagesTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cash flow should validate period limit with prototype message`() = runTest {
        val viewModel = CashFlowViewModel(FakeReportsRepository())
        viewModel.onStartDateChange("01/04/2026")
        viewModel.onEndDateChange("10/04/2026")

        viewModel.emitReport()
        runCurrent()

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_cash_flow_period_limit, message?.textResId)
        assertEquals(MessageTone.WARNING, message?.tone)
    }

    @Test
    fun `revenue should show generic report generation error on failure`() = runTest {
        val viewModel = RevenueViewModel(FakeReportsRepository(shouldFail = true))
        viewModel.onStartDateChange("01/04/2026")
        viewModel.onEndDateChange("02/04/2026")

        viewModel.emitReport()
        runCurrent()

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_report_generate_error, message?.textResId)
        assertEquals(MessageTone.ERROR, message?.tone)
    }
}

private class FakeReportsRepository(
    private val shouldFail: Boolean = false
) : ReportsRepository {
    override suspend fun cashFlow(startDate: LocalDate, endDate: LocalDate): CashFlowReportModel {
        if (shouldFail) throw IllegalStateException("cash flow failure")
        return CashFlowReportModel(
            entries = listOf(
                CashFlowEntryModel(
                    date = startDate,
                    total = BigDecimal("10.00"),
                    services = emptyList()
                )
            ),
            startDate = startDate,
            endDate = endDate,
            syncMetadata = SyncMetadataModel(
                dataUpToDate = true,
                lastSyncAt = null,
                reauthRequired = false
            )
        )
    }

    override suspend fun revenue(startDate: LocalDate, endDate: LocalDate): RevenueReportModel {
        if (shouldFail) throw IllegalStateException("revenue failure")
        return RevenueReportModel(
            totalRevenue = BigDecimal("100.00"),
            startDate = startDate,
            endDate = endDate,
            syncMetadata = SyncMetadataModel(
                dataUpToDate = true,
                lastSyncAt = null,
                reauthRequired = false
            )
        )
    }
}

