package com.tcc.androidnative.feature.payments.ui

import androidx.lifecycle.SavedStateHandle
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentEntryResponseDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentsResponseDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentsUpsertRequestDto
import com.tcc.androidnative.feature.payments.data.PaymentsRepository
import com.tcc.androidnative.navigation.AppDestination
import com.tcc.androidnative.testutil.MainDispatcherRule
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `savePayments should call repository and emit success event`() = runTest {
        val fakeRepository = FakePaymentsRepository()
        val viewModel = PaymentsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AppDestination.Payments.ARG_EVENT_ID to 77L,
                    AppDestination.Payments.ARG_TOTAL_SERVICE_VALUE to "50.00"
                )
            ),
            paymentsRepository = fakeRepository
        )

        viewModel.onPaymentAmountChanged(1L, "50")
        viewModel.onPaymentValueTotalChanged(1L, true)
        viewModel.savePayments()
        advanceUntilIdle()

        assertEquals(77L, fakeRepository.lastEventId)
        assertTrue(fakeRepository.lastRequest != null)
        assertTrue(viewModel.uiState.value.isSaving.not())
    }

    @Test
    fun `loadPayments should prefill state when api returns existing composition`() = runTest {
        val fakeRepository = FakePaymentsRepository(
            paymentsToLoad = listOf(
                PaymentEntryUiState(
                    id = 20L,
                    method = PaymentMethod.CREDITO,
                    amountInput = "80",
                    isValueTotal = false
                ),
                PaymentEntryUiState(
                    id = 21L,
                    method = PaymentMethod.DEBITO,
                    amountInput = "20",
                    isValueTotal = false
                )
            )
        )

        val viewModel = PaymentsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AppDestination.Payments.ARG_EVENT_ID to 88L,
                    AppDestination.Payments.ARG_TOTAL_SERVICE_VALUE to "100.00"
                )
            ),
            paymentsRepository = fakeRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.payments.size)
        assertEquals(PaymentMethod.CREDITO, state.payments.first().method)
        assertEquals(0, state.totalPaid.compareTo(BigDecimal("100.00")))
        assertEquals(2, state.paymentTypeTotals.size)
        assertTrue(state.isLoadingPayments.not())
    }

    @Test
    fun `loadPayments should fall back to empty form when preload returns unauthorized`() = runTest {
        val fakeRepository = FakePaymentsRepository(
            loadFailure = unauthorizedHttpException()
        )

        val viewModel = PaymentsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AppDestination.Payments.ARG_EVENT_ID to 88L,
                    AppDestination.Payments.ARG_TOTAL_SERVICE_VALUE to "100.00"
                )
            ),
            paymentsRepository = fakeRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.payments.size)
        assertTrue(state.loadErrorMessage == null)
        assertTrue(state.isLoadingPayments.not())
    }
}

private class FakePaymentsRepository : PaymentsRepository {
    constructor(
        paymentsToLoad: List<PaymentEntryUiState> = emptyList(),
        loadFailure: Throwable? = null
    ) {
        this.paymentsToLoad = paymentsToLoad
        this.loadFailure = loadFailure
    }

    var lastEventId: Long? = null
    var lastRequest: CalendarPaymentsUpsertRequestDto? = null
    private var paymentsToLoad: List<PaymentEntryUiState> = emptyList()
    private var loadFailure: Throwable? = null

    override suspend fun loadPayments(eventId: Long): List<PaymentEntryUiState> {
        lastEventId = eventId
        loadFailure?.let { throw it }
        return paymentsToLoad
    }

    override suspend fun savePayments(
        eventId: Long,
        payments: List<PaymentEntryUiState>
    ): CalendarPaymentsResponseDto {
        lastEventId = eventId
        lastRequest = CalendarPaymentsUpsertRequestDto(
            payments = payments.map {
                com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentEntryRequestDto(
                    paymentType = it.method.name,
                    amount = BigDecimal(it.amountInput),
                    valueTotal = it.isValueTotal
                )
            }
        )
        return CalendarPaymentsResponseDto(
            eventId = eventId,
            payments = emptyList()
        )
    }
}

private fun unauthorizedHttpException(): HttpException {
    val body = """{"code":"UNAUTHORIZED","message":"Authentication required"}"""
        .toResponseBody("application/json".toMediaType())
    return HttpException(Response.error<CalendarPaymentsResponseDto>(401, body))
}
