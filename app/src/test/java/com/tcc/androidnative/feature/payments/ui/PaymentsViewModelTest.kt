package com.tcc.androidnative.feature.payments.ui

import androidx.lifecycle.SavedStateHandle
import com.tcc.androidnative.R
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentEntryResponseDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentsResponseDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentsUpsertRequestDto
import com.tcc.androidnative.core.util.CurrencyFormats
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
                    AppDestination.Payments.ARG_TOTAL_SERVICE_VALUE to "50.00",
                    AppDestination.Payments.ARG_PRELOAD_PAYMENTS to false
                )
            ),
            paymentsRepository = fakeRepository
        )

        viewModel.onPaymentAmountChanged(1L, "R$ 50,00")
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
                    amountInput = "R$ 80,00",
                    isValueTotal = false
                ),
                PaymentEntryUiState(
                    id = 21L,
                    method = PaymentMethod.DEBITO,
                    amountInput = "R$ 20,00",
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

    @Test
    fun `savePayments should block save and show required amount message when amount is blank`() = runTest {
        val fakeRepository = FakePaymentsRepository()
        val viewModel = PaymentsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AppDestination.Payments.ARG_EVENT_ID to 77L,
                    AppDestination.Payments.ARG_TOTAL_SERVICE_VALUE to "50.00",
                    AppDestination.Payments.ARG_PRELOAD_PAYMENTS to false
                )
            ),
            paymentsRepository = fakeRepository
        )

        viewModel.savePayments()
        advanceUntilIdle()

        assertTrue(fakeRepository.lastRequest == null)
        assertTrue(viewModel.uiState.value.isSaving.not())
        assertEquals(
            R.string.payments_value_required_feedback,
            viewModel.uiState.value.saveErrorMessageResId
        )
    }

    @Test
    fun `savePayments should map backend 422 error to required amount message`() = runTest {
        val fakeRepository = FakePaymentsRepository(
            saveFailure = unprocessableEntityHttpException()
        )
        val viewModel = PaymentsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AppDestination.Payments.ARG_EVENT_ID to 77L,
                    AppDestination.Payments.ARG_TOTAL_SERVICE_VALUE to "50.00"
                )
            ),
            paymentsRepository = fakeRepository
        )

        viewModel.onPaymentAmountChanged(1L, "R$ 10,00")
        viewModel.savePayments()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaving.not())
        assertEquals(
            R.string.payments_value_required_feedback,
            viewModel.uiState.value.saveErrorMessageResId
        )
        assertTrue(viewModel.uiState.value.saveErrorMessage == null)
    }

    @Test
    fun `savePayments should allow clearing loaded payments and send empty request`() = runTest {
        val fakeRepository = FakePaymentsRepository(
            paymentsToLoad = listOf(
                PaymentEntryUiState(
                    id = 20L,
                    method = PaymentMethod.CREDITO,
                    amountInput = "R$ 80,00",
                    isValueTotal = false
                ),
                PaymentEntryUiState(
                    id = 21L,
                    method = PaymentMethod.DEBITO,
                    amountInput = "R$ 20,00",
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

        viewModel.onRemovePaymentClick(20L)
        viewModel.onRemovePaymentClick(21L)
        viewModel.savePayments()
        advanceUntilIdle()

        assertEquals(88L, fakeRepository.lastEventId)
        assertTrue(fakeRepository.lastRequest != null)
        assertTrue(fakeRepository.lastRequest?.payments?.isEmpty() == true)
        assertTrue(viewModel.uiState.value.saveErrorMessageResId == null)
        assertTrue(viewModel.uiState.value.isSaving.not())
    }

    @Test
    fun `savePayments should still block placeholder when preload finds no persisted payments`() = runTest {
        val fakeRepository = FakePaymentsRepository(
            paymentsToLoad = emptyList()
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

        viewModel.savePayments()
        advanceUntilIdle()

        assertTrue(fakeRepository.lastRequest == null)
        assertEquals(
            R.string.payments_value_required_feedback,
            viewModel.uiState.value.saveErrorMessageResId
        )
    }
}

private class FakePaymentsRepository : PaymentsRepository {
    constructor(
        paymentsToLoad: List<PaymentEntryUiState> = emptyList(),
        loadFailure: Throwable? = null,
        saveFailure: Throwable? = null
    ) {
        this.paymentsToLoad = paymentsToLoad
        this.loadFailure = loadFailure
        this.saveFailure = saveFailure
    }

    var lastEventId: Long? = null
    var lastRequest: CalendarPaymentsUpsertRequestDto? = null
    private var paymentsToLoad: List<PaymentEntryUiState> = emptyList()
    private var loadFailure: Throwable? = null
    private var saveFailure: Throwable? = null

    override suspend fun loadPayments(eventId: Long): List<PaymentEntryUiState> {
        lastEventId = eventId
        loadFailure?.let { throw it }
        return paymentsToLoad
    }

    override suspend fun savePayments(
        eventId: Long,
        payments: List<PaymentEntryUiState>
    ): CalendarPaymentsResponseDto {
        saveFailure?.let { throw it }
        lastEventId = eventId
        lastRequest = CalendarPaymentsUpsertRequestDto(
            payments = payments.map {
                com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarPaymentEntryRequestDto(
                    paymentType = it.method.name,
                    amount = CurrencyFormats.parseUiValue(it.amountInput),
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

private fun unprocessableEntityHttpException(): HttpException {
    val body = """{"code":"VALIDATION_ERROR","message":"Valor do pagamento é obrigatório"}"""
        .toResponseBody("application/json".toMediaType())
    return HttpException(Response.error<CalendarPaymentsResponseDto>(422, body))
}
