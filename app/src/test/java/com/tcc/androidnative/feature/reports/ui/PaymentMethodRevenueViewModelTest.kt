package com.tcc.androidnative.feature.reports.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.feature.payments.ui.PaymentMethod
import com.tcc.androidnative.feature.reports.data.PaymentMethodRevenueEntryModel
import com.tcc.androidnative.feature.reports.data.PaymentMethodRevenueReportModel
import com.tcc.androidnative.feature.reports.data.ReportPaymentScope
import com.tcc.androidnative.feature.reports.data.ReportsRepository
import com.tcc.androidnative.feature.reports.data.SyncMetadataModel
import com.tcc.androidnative.testutil.MainDispatcherRule
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodRevenueViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should require a confirmed period`() = runTest {
        val viewModel = PaymentMethodRevenueViewModel(FakePaymentMethodRevenueReportsRepository())

        viewModel.emitReport()
        runCurrent()

        assertEquals(R.string.feedback_report_period_invalid, viewModel.uiState.value.transientMessage?.textResId)
        assertEquals(MessageTone.ERROR, viewModel.uiState.value.transientMessage?.tone)
    }

    @Test
    fun `should validate one month period limit`() = runTest {
        val viewModel = PaymentMethodRevenueViewModel(FakePaymentMethodRevenueReportsRepository())
        viewModel.onPeriodSelected(
            startDate = LocalDate.of(2026, 4, 1),
            endDate = LocalDate.of(2026, 5, 3)
        )

        viewModel.emitReport()
        runCurrent()

        assertEquals(R.string.feedback_report_period_limit, viewModel.uiState.value.transientMessage?.textResId)
        assertEquals(MessageTone.WARNING, viewModel.uiState.value.transientMessage?.tone)
    }

    @Test
    fun `should normalize selected period order`() = runTest {
        val viewModel = PaymentMethodRevenueViewModel(FakePaymentMethodRevenueReportsRepository())

        viewModel.onPeriodSelected(
            startDate = LocalDate.of(2026, 5, 9),
            endDate = LocalDate.of(2026, 5, 2)
        )

        assertEquals("02/05/2026 - 09/05/2026", viewModel.uiState.value.periodInput)
        assertEquals(LocalDate.of(2026, 5, 2), viewModel.uiState.value.selectedStartDate)
        assertEquals(LocalDate.of(2026, 5, 9), viewModel.uiState.value.selectedEndDate)
    }

    @Test
    fun `should load report and move to report step`() = runTest {
        val viewModel = PaymentMethodRevenueViewModel(FakePaymentMethodRevenueReportsRepository())
        viewModel.onPeriodSelected(
            startDate = LocalDate.of(2026, 4, 1),
            endDate = LocalDate.of(2026, 4, 2)
        )

        viewModel.emitReport()
        runCurrent()

        assertEquals(PaymentMethodRevenueScreenStep.REPORT, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(4, viewModel.uiState.value.report?.entries?.size)
        assertTrue(viewModel.uiState.value.staleDataWarning)
    }
}

private class FakePaymentMethodRevenueReportsRepository : ReportsRepository {
    override suspend fun cashFlow(
        startDate: LocalDate,
        endDate: LocalDate,
        paymentScope: ReportPaymentScope
    ) = throw UnsupportedOperationException()

    override suspend fun revenue(
        startDate: LocalDate,
        endDate: LocalDate,
        paymentScope: ReportPaymentScope
    ) = throw UnsupportedOperationException()

    override suspend fun paymentMethodRevenue(
        startDate: LocalDate,
        endDate: LocalDate
    ): PaymentMethodRevenueReportModel {
        return PaymentMethodRevenueReportModel(
            entries = listOf(
                PaymentMethodRevenueEntryModel(PaymentMethod.DINHEIRO, BigDecimal("10.00")),
                PaymentMethodRevenueEntryModel(PaymentMethod.PIX, BigDecimal("20.00")),
                PaymentMethodRevenueEntryModel(PaymentMethod.DEBITO, BigDecimal("0.00")),
                PaymentMethodRevenueEntryModel(PaymentMethod.CREDITO, BigDecimal("30.00"))
            ),
            startDate = startDate,
            endDate = endDate,
            syncMetadata = SyncMetadataModel(
                dataUpToDate = false,
                lastSyncAt = null,
                reauthRequired = false
            )
        )
    }
}
