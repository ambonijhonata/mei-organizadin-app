package com.tcc.androidnative.feature.services.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.core.network.BackendApiException
import com.tcc.androidnative.core.network.BackendErrorDetails
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.util.CurrencyFormats
import com.tcc.androidnative.feature.services.data.DeleteServiceOutcome
import com.tcc.androidnative.feature.services.data.ServiceModel
import com.tcc.androidnative.feature.services.data.ServicesPage
import com.tcc.androidnative.feature.services.data.ServicesRepository
import com.tcc.androidnative.testutil.MainDispatcherRule
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServicesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit form should require service description`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.submitForm()
        runCurrent()

        val formState = viewModel.uiState.value.formState
        assertEquals(R.string.feedback_service_description_required, formState.fieldErrors["description"]?.textResId)
        assertTrue(viewModel.uiState.value.transientMessages.isEmpty())

        viewModel.updateForm(description = "Design")
        assertTrue(viewModel.uiState.value.formState.fieldErrors.isEmpty())
    }

    @Test
    fun `submit form should require positive value`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(description = "Design", valueInput = "0")
        viewModel.submitForm()
        runCurrent()

        val formState = viewModel.uiState.value.formState
        assertEquals(R.string.feedback_service_value_positive_required, formState.fieldErrors["valueInput"]?.textResId)
        assertTrue(viewModel.uiState.value.transientMessages.isEmpty())
    }

    @Test
    fun `duplicate service description should show popup banner and keep dialog open`() = runTest {
        val repository = FakeServicesRepository(
            createException = BackendApiException(
                BackendErrorDetails(message = "Service with this description already exists")
            )
        )
        val viewModel = ServicesViewModel(repository)
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(description = "Corte", valueInput = "R$ 50,00")
        viewModel.submitForm()
        runCurrent()

        val formState = viewModel.uiState.value.formState
        assertEquals("Corte já cadastrado", formState.bannerMessage?.text)
        assertTrue(formState.fieldErrors.isEmpty())
        assertTrue(viewModel.uiState.value.formMode != null)
    }

    @Test
    fun `duplicate service description should use submitted description when backend message is unavailable`() = runTest {
        val repository = FakeServicesRepository(
            createException = BackendApiException(
                BackendErrorDetails(message = null)
            )
        )
        val viewModel = ServicesViewModel(repository)
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(description = "Corte", valueInput = "R$ 50,00")
        viewModel.submitForm()
        runCurrent()

        val formState = viewModel.uiState.value.formState
        assertEquals("Corte já cadastrado", formState.bannerMessage?.text)
        assertTrue(formState.fieldErrors.isEmpty())
    }

    @Test
    fun `backend duplicate service message should be normalized when already localized`() = runTest {
        val repository = FakeServicesRepository(
            createException = BackendApiException(
                BackendErrorDetails(message = "Corte Já cadastrado.")
            )
        )
        val viewModel = ServicesViewModel(repository)
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(description = "Corte", valueInput = "R$ 50,00")
        viewModel.submitForm()
        runCurrent()

        assertEquals("Corte já cadastrado", viewModel.uiState.value.formState.bannerMessage?.text)
    }

    @Test
    fun `bulk delete with mixed outcomes should show success and warning messages`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository(bulkDeleteResult = 1 to 1))
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.toggleRowSelection(2L, true)
        viewModel.deleteSelection()
        runCurrent()

        val messages = viewModel.uiState.value.transientMessages
        assertEquals(2, messages.size)
        assertEquals(R.string.feedback_service_bulk_delete_success, messages[0].textResId)
        assertEquals(listOf("1"), messages[0].textArgs)
        assertEquals(MessageTone.SUCCESS, messages[0].tone)
        assertEquals(MessageDurations.MEDIUM_5S, messages[0].durationMillis)
        assertEquals(R.string.feedback_service_bulk_delete_warning, messages[1].textResId)
        assertEquals(listOf("1"), messages[1].textArgs)
        assertEquals(MessageTone.WARNING, messages[1].tone)
        assertEquals(MessageDurations.LONG_8S, messages[1].durationMillis)
    }

    @Test
    fun `bulk delete with only deletions should show only success message`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository(bulkDeleteResult = 2 to 0))
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.toggleRowSelection(2L, true)
        viewModel.deleteSelection()
        runCurrent()

        val messages = viewModel.uiState.value.transientMessages
        assertEquals(1, messages.size)
        assertEquals(R.string.feedback_service_bulk_delete_success, messages.single().textResId)
        assertEquals(listOf("2"), messages.single().textArgs)
    }

    @Test
    fun `bulk delete with only linked records should show only warning message`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository(bulkDeleteResult = 0 to 2))
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.toggleRowSelection(2L, true)
        viewModel.deleteSelection()
        runCurrent()

        val messages = viewModel.uiState.value.transientMessages
        assertEquals(1, messages.size)
        assertEquals(R.string.feedback_service_bulk_delete_warning, messages.single().textResId)
        assertEquals(listOf("2"), messages.single().textArgs)
    }

    @Test
    fun `single delete with linked appointments should show warning message`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository(deleteOutcome = DeleteServiceOutcome.HAS_LINK))
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.deleteSelection()
        runCurrent()

        val message = viewModel.uiState.value.transientMessages.single()
        assertEquals(R.string.feedback_service_delete_link_warning, message.textResId)
        assertEquals(MessageTone.WARNING, message.tone)
        assertEquals(MessageDurations.LONG_8S, message.durationMillis)
    }

    @Test
    fun `filter should run only after typing pause`() = runTest {
        val repository = FakeServicesRepository()
        val viewModel = ServicesViewModel(repository)
        advanceUntilIdle()
        repository.resetListTracking()

        viewModel.onFilterDescriptionChange("D")
        runCurrent()
        advanceTimeBy(250)
        viewModel.onFilterDescriptionChange("De")
        runCurrent()
        advanceTimeBy(250)
        viewModel.onFilterDescriptionChange("Des")
        runCurrent()
        advanceTimeBy(499)
        runCurrent()
        assertEquals(0, repository.listCallCount)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, repository.listCallCount)
        assertEquals("Des", repository.lastListDescription)
    }

    @Test
    fun `manual filter should apply immediately and cancel pending debounce`() = runTest {
        val repository = FakeServicesRepository()
        val viewModel = ServicesViewModel(repository)
        advanceUntilIdle()
        repository.resetListTracking()

        viewModel.onFilterDescriptionChange("Design")
        runCurrent()
        assertEquals(0, repository.listCallCount)

        viewModel.applyFilter()
        runCurrent()
        assertEquals(1, repository.listCallCount)
        assertEquals("Design", repository.lastListDescription)

        advanceTimeBy(600)
        runCurrent()
        assertEquals(1, repository.listCallCount)
    }

    @Test
    fun `value input should apply deterministic currency mask without drift`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        listOf(
            "5" to "R$\u00a00,05",
            "0" to "R$\u00a00,50",
            "0" to "R$\u00a05,00",
            "0" to "R$\u00a050,00"
        ).forEach { (typedDigit, expectedValue) ->
            val currentValue = viewModel.uiState.value.formState.valueInput
            viewModel.updateForm(valueInput = currentValue + typedDigit)
            assertEquals(expectedValue, viewModel.uiState.value.formState.valueInput)
        }
    }

    @Test
    fun `open edit form should preload value with currency mask`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository())
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.openEditForm()
        runCurrent()

        assertEquals("R$\u00a050,00", viewModel.uiState.value.formState.valueInput)
    }

    @Test
    fun `single delete success should use medium duration`() = runTest {
        val viewModel = ServicesViewModel(FakeServicesRepository(deleteOutcome = DeleteServiceOutcome.DELETED))
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.deleteSelection()
        runCurrent()

        val message = viewModel.uiState.value.transientMessages.single()
        assertEquals(R.string.feedback_service_delete_success, message.textResId)
        assertEquals(MessageDurations.MEDIUM_5S, message.durationMillis)
        assertTrue(message.tone == MessageTone.SUCCESS)
    }
}

private class FakeServicesRepository(
    private val deleteOutcome: DeleteServiceOutcome = DeleteServiceOutcome.DELETED,
    private val bulkDeleteResult: Pair<Int, Int> = 0 to 0,
    private val createException: Throwable? = null,
    private val updateException: Throwable? = null
) : ServicesRepository {
    var listCallCount: Int = 0
        private set
    var lastListDescription: String? = null
        private set

    override suspend fun list(description: String?, uiPageIndex: Int, pageSize: Int): ServicesPage {
        listCallCount += 1
        lastListDescription = description
        return ServicesPage(
            items = listOf(
                ServiceModel(id = 1L, description = "Design", value = BigDecimal("50.00")),
                ServiceModel(id = 2L, description = "Henna", value = BigDecimal("25.00"))
            ),
            pageIndex = 1,
            totalPages = 1,
            totalItems = 2
        )
    }

    override suspend fun getById(id: Long): ServiceModel {
        return ServiceModel(id = id, description = "Design", value = BigDecimal("50.00"))
    }

    override suspend fun create(description: String, valueInput: String): ServiceModel {
        createException?.let { throw it }
        return ServiceModel(id = 99L, description = description, value = CurrencyFormats.parseUiValue(valueInput))
    }

    override suspend fun update(id: Long, description: String, valueInput: String): ServiceModel {
        updateException?.let { throw it }
        return ServiceModel(id = id, description = description, value = CurrencyFormats.parseUiValue(valueInput))
    }

    override suspend fun delete(id: Long): DeleteServiceOutcome = deleteOutcome

    override suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int> = bulkDeleteResult

    fun resetListTracking() {
        listCallCount = 0
        lastListDescription = null
    }
}
