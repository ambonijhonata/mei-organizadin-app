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
    fun `should validate invalid start date`() = runTest {
        val viewModel = PaymentMethodRevenueViewModel(FakePaymentMethodRevenueReportsRepository())
        viewModel.onStartDateChange("99")
        viewModel.onEndDateChange("02/04/2026")

        viewModel.emitReport()
        runCurrent()

        assertEquals(R.string.feedback_report_start_date_invalid, viewModel.uiState.value.transientMessage?.textResId)
        assertEquals(MessageTone.ERROR, viewModel.uiState.value.transientMessage?.tone)
    }

    @Test
    fun `should validate invalid end date`() = runTest {
        val viewModel = PaymentMethodRevenueViewModel(FakePaymentMethodRevenueReportsRepository())
        viewModel.onStartDateChange("01/04/2026")
        viewModel.onEndDateChange("99")

        viewModel.emitReport()
        runCurrent()

        assertEquals(R.string.feedback_report_end_date_invalid, viewModel.uiState.value.transientMessage?.textResId)
    }

    @Test
    fun `should validate start date before end date`() = runTest {
        val viewModel = PaymentMethodRevenueViewModel(FakePaymentMethodRevenueReportsRepository())
        viewModel.onStartDateChange("03/04/2026")
        viewModel.onEndDateChange("02/04/2026")

        viewModel.emitReport()
        runCurrent()

        assertEquals(R.string.feedback_report_start_before_end, viewModel.uiState.value.transientMessage?.textResId)
    }

    @Test
    fun `should load report and move to report step`() = runTest {
        val viewModel = PaymentMethodRevenueViewModel(FakePaymentMethodRevenueReportsRepository())
        viewModel.onStartDateChange("01/04/2026")
        viewModel.onEndDateChange("02/04/2026")

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
