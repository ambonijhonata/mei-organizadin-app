package com.tcc.androidnative.feature.clients.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.core.network.BackendApiException
import com.tcc.androidnative.core.network.BackendErrorDetails
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.clients.data.ClientModel
import com.tcc.androidnative.feature.clients.data.ClientsPage
import com.tcc.androidnative.feature.clients.data.ClientsRepository
import com.tcc.androidnative.feature.clients.data.DeleteClientOutcome
import com.tcc.androidnative.testutil.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit form should show required-name validation message`() = runTest {
        val viewModel = ClientsViewModel(FakeClientsRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.submitForm()
        runCurrent()

        val formState = viewModel.uiState.value.formState
        assertEquals(R.string.feedback_client_name_required, formState.fieldErrors["name"]?.textResId)
        assertTrue(viewModel.uiState.value.transientMessages.isEmpty())

        viewModel.updateForm(name = "Cliente")
        assertTrue(viewModel.uiState.value.formState.fieldErrors.isEmpty())
    }

    @Test
    fun `submit form should show cpf validation message in popup`() = runTest {
        val viewModel = ClientsViewModel(FakeClientsRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(name = "Cliente", cpf = "123")
        viewModel.submitForm()
        runCurrent()

        val formState = viewModel.uiState.value.formState
        assertEquals(R.string.feedback_client_cpf_invalid, formState.fieldErrors["cpf"]?.textResId)
        assertTrue(viewModel.uiState.value.transientMessages.isEmpty())
    }

    @Test
    fun `date picker selection should populate birth date field`() = runTest {
        val viewModel = ClientsViewModel(FakeClientsRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.onBirthDatePicked(LocalDate.of(2000, 1, 2))

        assertEquals("02/01/2000", viewModel.uiState.value.formState.birthDate)
    }

    @Test
    fun `masked inputs should submit canonical cpf phone and date values`() = runTest {
        val repository = FakeClientsRepository()
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(
            name = "Cliente",
            cpf = "52998224725",
            birthDate = "01012000",
            phone = "11999999999"
        )
        assertEquals("529.982.247-25", viewModel.uiState.value.formState.cpf)
        assertEquals("01/01/2000", viewModel.uiState.value.formState.birthDate)
        assertEquals("(11) 99999-9999", viewModel.uiState.value.formState.phone)

        viewModel.submitForm()
        runCurrent()

        val created = repository.lastCreatedModel
        assertNotNull(created)
        assertEquals("52998224725", created?.cpf)
        assertEquals(LocalDate.of(2000, 1, 1), created?.dateOfBirth)
        assertEquals("11999999999", created?.phone)
    }

    @Test
    fun `masked inputs should keep expected formatting for provided regression examples`() = runTest {
        val viewModel = ClientsViewModel(FakeClientsRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(cpf = "12866634900")
        viewModel.updateForm(birthDate = "20011995")
        viewModel.updateForm(phone = "48940028922")

        val formState = viewModel.uiState.value.formState
        assertEquals("128.666.349-00", formState.cpf)
        assertEquals("20/01/1995", formState.birthDate)
        assertEquals("(48) 94002-8922", formState.phone)
    }

    @Test
    fun `masked inputs should be deterministic for deletion and paste like flows`() = runTest {
        val viewModel = ClientsViewModel(FakeClientsRepository())
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(phone = "48940028922")
        assertEquals("(48) 94002-8922", viewModel.uiState.value.formState.phone)

        viewModel.updateForm(phone = "4894002892")
        assertEquals("(48) 94002-892", viewModel.uiState.value.formState.phone)

        viewModel.updateForm(cpf = "128.666abc349-00")
        assertEquals("128.666.349-00", viewModel.uiState.value.formState.cpf)
    }

    @Test
    fun `duplicate client name should show popup banner and keep dialog open`() = runTest {
        val repository = FakeClientsRepository(
            createException = BackendApiException(
                BackendErrorDetails(message = "Maria Silva Ja cadastrado.")
            )
        )
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()

        viewModel.openCreateForm()
        viewModel.updateForm(name = "Maria Silva")
        viewModel.submitForm()
        runCurrent()

        val formState = viewModel.uiState.value.formState
        assertEquals("Maria Silva já cadastrado", formState.bannerMessage?.text)
        assertTrue(formState.fieldErrors.isEmpty())
        assertNotNull(viewModel.uiState.value.formMode)
    }

    @Test
    fun `bulk delete with mixed outcomes should show success and warning messages`() = runTest {
        val repository = FakeClientsRepository(bulkDeleteResult = 1 to 1)
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.toggleRowSelection(2L, true)
        viewModel.deleteSelection()
        runCurrent()

        val messages = viewModel.uiState.value.transientMessages
        assertEquals(2, messages.size)
        assertEquals(R.string.feedback_client_bulk_delete_success, messages[0].textResId)
        assertEquals(listOf("1"), messages[0].textArgs)
        assertEquals(MessageTone.SUCCESS, messages[0].tone)
        assertEquals(MessageDurations.MEDIUM_5S, messages[0].durationMillis)
        assertEquals(R.string.feedback_client_bulk_delete_warning, messages[1].textResId)
        assertEquals(listOf("1"), messages[1].textArgs)
        assertEquals(MessageTone.WARNING, messages[1].tone)
        assertEquals(MessageDurations.LONG_8S, messages[1].durationMillis)
    }

    @Test
    fun `bulk delete with only deletions should show only success message`() = runTest {
        val repository = FakeClientsRepository(bulkDeleteResult = 2 to 0)
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.toggleRowSelection(2L, true)
        viewModel.deleteSelection()
        runCurrent()

        val messages = viewModel.uiState.value.transientMessages
        assertEquals(1, messages.size)
        assertEquals(R.string.feedback_client_bulk_delete_success, messages.single().textResId)
        assertEquals(listOf("2"), messages.single().textArgs)
    }

    @Test
    fun `bulk delete with only linked records should show only warning message`() = runTest {
        val repository = FakeClientsRepository(bulkDeleteResult = 0 to 2)
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.toggleRowSelection(2L, true)
        viewModel.deleteSelection()
        runCurrent()

        val messages = viewModel.uiState.value.transientMessages
        assertEquals(1, messages.size)
        assertEquals(R.string.feedback_client_bulk_delete_warning, messages.single().textResId)
        assertEquals(listOf("2"), messages.single().textArgs)
    }

    @Test
    fun `single delete with linked appointments should show warning message`() = runTest {
        val viewModel = ClientsViewModel(
            FakeClientsRepository(deleteOutcome = DeleteClientOutcome.HAS_LINK)
        )
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.deleteSelection()
        runCurrent()

        val message = viewModel.uiState.value.transientMessages.single()
        assertEquals(R.string.feedback_client_delete_link_warning, message.textResId)
        assertEquals(MessageTone.WARNING, message.tone)
        assertEquals(MessageDurations.LONG_8S, message.durationMillis)
    }

    @Test
    fun `filter should run only after typing pause`() = runTest {
        val repository = FakeClientsRepository()
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()
        repository.resetListTracking()

        viewModel.onFilterNameChange("C")
        runCurrent()
        advanceTimeBy(300)
        viewModel.onFilterNameChange("Cl")
        runCurrent()
        advanceTimeBy(300)
        viewModel.onFilterNameChange("Cli")
        runCurrent()
        advanceTimeBy(499)
        runCurrent()

        assertEquals(0, repository.listCallCount)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(1, repository.listCallCount)
        assertEquals("Cli", repository.lastListName)
    }

    @Test
    fun `manual filter should apply immediately and cancel pending debounce`() = runTest {
        val repository = FakeClientsRepository()
        val viewModel = ClientsViewModel(repository)
        advanceUntilIdle()
        repository.resetListTracking()

        viewModel.onFilterNameChange("Cliente")
        runCurrent()
        assertEquals(0, repository.listCallCount)

        viewModel.applyFilter()
        runCurrent()

        assertEquals(1, repository.listCallCount)
        assertEquals("Cliente", repository.lastListName)

        advanceTimeBy(600)
        runCurrent()
        assertEquals(1, repository.listCallCount)
    }

    @Test
    fun `single delete success should use medium duration`() = runTest {
        val viewModel = ClientsViewModel(FakeClientsRepository(deleteOutcome = DeleteClientOutcome.DELETED))
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.deleteSelection()
        runCurrent()

        val message = viewModel.uiState.value.transientMessages.single()
        assertEquals(R.string.feedback_client_delete_success, message.textResId)
        assertEquals(MessageDurations.MEDIUM_5S, message.durationMillis)
        assertTrue(message.tone == MessageTone.SUCCESS)
    }
}

private class FakeClientsRepository(
    private val listItems: List<ClientModel> = listOf(
        ClientModel(id = 1L, name = "Ana", cpf = null, dateOfBirth = null, email = null, phone = null),
        ClientModel(id = 2L, name = "Bia", cpf = null, dateOfBirth = null, email = null, phone = null)
    ),
    private val bulkDeleteResult: Pair<Int, Int> = 0 to 0,
    private val deleteOutcome: DeleteClientOutcome = DeleteClientOutcome.DELETED,
    private val createException: Throwable? = null,
    private val updateException: Throwable? = null
) : ClientsRepository {
    var listCallCount: Int = 0
        private set
    var lastListName: String? = null
        private set
    var lastCreatedModel: ClientModel? = null
        private set
    var lastUpdatedModel: ClientModel? = null
        private set

    override suspend fun list(name: String?, pageIndex: Int, itemsPerPage: Int): ClientsPage {
        listCallCount += 1
        lastListName = name
        return ClientsPage(
            items = listItems,
            pageIndex = 1,
            totalPages = 1,
            totalItems = listItems.size.toLong()
        )
    }

    override suspend fun getById(id: Long): ClientModel {
        return listItems.firstOrNull { it.id == id } ?: ClientModel(
            id = id,
            name = "Cliente",
            cpf = null,
            dateOfBirth = null,
            email = null,
            phone = null
        )
    }

    override suspend fun create(model: ClientModel): ClientModel {
        lastCreatedModel = model
        createException?.let { throw it }
        return model.copy(id = 99L)
    }

    override suspend fun update(id: Long, model: ClientModel): ClientModel {
        lastUpdatedModel = model.copy(id = id)
        updateException?.let { throw it }
        return model.copy(id = id)
    }

    override suspend fun delete(id: Long): DeleteClientOutcome = deleteOutcome

    override suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int> = bulkDeleteResult

    fun resetListTracking() {
        listCallCount = 0
        lastListName = null
    }
}

