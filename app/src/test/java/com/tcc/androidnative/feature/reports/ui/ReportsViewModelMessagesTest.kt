package com.tcc.androidnative.feature.reports.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.feature.reports.data.CashFlowEntryModel
import com.tcc.androidnative.feature.reports.data.CashFlowReportModel
import com.tcc.androidnative.feature.reports.data.ReportPaymentScope
import com.tcc.androidnative.feature.reports.data.ReportsRepository
import com.tcc.androidnative.feature.reports.data.RevenueReportModel
import com.tcc.androidnative.feature.reports.data.SyncMetadataModel
import com.tcc.androidnative.feature.settings.data.CalendarSyncSettings
import com.tcc.androidnative.feature.settings.data.CalendarSyncSettingsStore
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
        val viewModel = CashFlowViewModel(
            repository = FakeReportsRepository(),
            calendarSyncSettingsStore = FakeCalendarSyncSettingsStore()
        )
        viewModel.onStartDateChange("01/04/2026")
        viewModel.onEndDateChange("03/05/2026")

        viewModel.emitReport()
        runCurrent()

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_cash_flow_period_limit, message?.textResId)
        assertEquals(MessageTone.WARNING, message?.tone)
    }

    @Test
    fun `cash flow should allow period up to one month`() = runTest {
        val repository = FakeReportsRepository()
        val viewModel = CashFlowViewModel(
            repository = repository,
            calendarSyncSettingsStore = FakeCalendarSyncSettingsStore()
        )
        viewModel.onStartDateChange("01/04/2026")
        viewModel.onEndDateChange("01/05/2026")

        viewModel.emitReport()
        runCurrent()

        assertEquals(null, viewModel.uiState.value.transientMessage)
        assertEquals(ReportPaymentScope.ALL, repository.lastCashFlowScope)
    }

    @Test
    fun `revenue should show generic report generation error on failure`() = runTest {
        val viewModel = RevenueViewModel(
            repository = FakeReportsRepository(shouldFail = true),
            calendarSyncSettingsStore = FakeCalendarSyncSettingsStore()
        )
        viewModel.onStartDateChange("01/04/2026")
        viewModel.onEndDateChange("02/04/2026")

        viewModel.emitReport()
        runCurrent()

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_report_generate_error, message?.textResId)
        assertEquals(MessageTone.ERROR, message?.tone)
    }

    @Test
    fun `revenue should use paid only scope when setting is enabled`() = runTest {
        val repository = FakeReportsRepository()
        val viewModel = RevenueViewModel(
            repository = repository,
            calendarSyncSettingsStore = FakeCalendarSyncSettingsStore(
                considerPaidOnlyInReports = true
            )
        )
        viewModel.onStartDateChange("01/04/2026")
        viewModel.onEndDateChange("02/04/2026")

        viewModel.emitReport()
        runCurrent()

        assertEquals(ReportPaymentScope.PAID_ONLY, repository.lastRevenueScope)
    }

    @Test
    fun `cash flow should use all scope when setting is disabled`() = runTest {
        val repository = FakeReportsRepository()
        val viewModel = CashFlowViewModel(
            repository = repository,
            calendarSyncSettingsStore = FakeCalendarSyncSettingsStore(
                considerPaidOnlyInReports = false
            )
        )
        viewModel.onStartDateChange("01/04/2026")
        viewModel.onEndDateChange("02/04/2026")

        viewModel.emitReport()
        runCurrent()

        assertEquals(ReportPaymentScope.ALL, repository.lastCashFlowScope)
    }

    @Test
    fun `cash flow should sanitize and mask date inputs`() = runTest {
        val viewModel = CashFlowViewModel(
            repository = FakeReportsRepository(),
            calendarSyncSettingsStore = FakeCalendarSyncSettingsStore()
        )

        viewModel.onStartDateChange("2a/6b-1_2@2#0$2%6")
        viewModel.onEndDateChange("26122026123")

        assertEquals("26/12/2026", viewModel.uiState.value.startDateInput)
        assertEquals("26/12/2026", viewModel.uiState.value.endDateInput)
    }

    @Test
    fun `revenue should sanitize and mask date inputs`() = runTest {
        val viewModel = RevenueViewModel(
            repository = FakeReportsRepository(),
            calendarSyncSettingsStore = FakeCalendarSyncSettingsStore()
        )

        viewModel.onStartDateChange("26-12-2026")
        viewModel.onEndDateChange("2612202")

        assertEquals("26/12/2026", viewModel.uiState.value.startDateInput)
        assertEquals("26/12/202", viewModel.uiState.value.endDateInput)
    }
}

private class FakeReportsRepository(
    private val shouldFail: Boolean = false
) : ReportsRepository {
    var lastCashFlowScope: ReportPaymentScope? = null
    var lastRevenueScope: ReportPaymentScope? = null

    override suspend fun cashFlow(
        startDate: LocalDate,
        endDate: LocalDate,
        paymentScope: ReportPaymentScope
    ): CashFlowReportModel {
        lastCashFlowScope = paymentScope
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

    override suspend fun revenue(
        startDate: LocalDate,
        endDate: LocalDate,
        paymentScope: ReportPaymentScope
    ): RevenueReportModel {
        lastRevenueScope = paymentScope
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

private class FakeCalendarSyncSettingsStore(
    considerPaidOnlyInReports: Boolean = false
) : CalendarSyncSettingsStore {
    private val settings = CalendarSyncSettings(
        useStartDateFilter = false,
        startDate = LocalDate.of(2026, 1, 1),
        considerPaidAppointmentsOnlyInReports = considerPaidOnlyInReports,
        initialSetupCompleted = true
    )

    override fun getSettings(): CalendarSyncSettings = settings

    override fun saveSettings(
        useStartDateFilter: Boolean,
        startDate: LocalDate,
        considerPaidAppointmentsOnlyInReports: Boolean
    ) {
    }
}
