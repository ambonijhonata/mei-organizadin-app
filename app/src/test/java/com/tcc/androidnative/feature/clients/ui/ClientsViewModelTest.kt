package com.tcc.androidnative.feature.clients.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.feature.clients.data.ClientModel
import com.tcc.androidnative.feature.clients.data.ClientsPage
import com.tcc.androidnative.feature.clients.data.ClientsRepository
import com.tcc.androidnative.feature.clients.data.DeleteClientOutcome
import com.tcc.androidnative.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_client_name_required, message?.textResId)
        assertEquals(MessageTone.ERROR, message?.tone)
        assertEquals(MessageDurations.SHORT_3S, message?.durationMillis)
    }

    @Test
    fun `bulk delete with linked records should show warning template from resources`() = runTest {
        val viewModel = ClientsViewModel(
            FakeClientsRepository(
                listItems = listOf(
                    ClientModel(id = 1L, name = "Ana", cpf = null, dateOfBirth = null, email = null, phone = null),
                    ClientModel(id = 2L, name = "Bia", cpf = null, dateOfBirth = null, email = null, phone = null)
                ),
                bulkDeleteResult = 1 to 1
            )
        )
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.toggleRowSelection(2L, true)
        viewModel.deleteSelection()
        runCurrent()

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_client_bulk_delete_warning, message?.textResId)
        assertEquals(listOf("1"), message?.textArgs)
        assertTrue(message?.tone == MessageTone.WARNING)
    }

    @Test
    fun `single delete with linked appointments should show warning message`() = runTest {
        val viewModel = ClientsViewModel(
            FakeClientsRepository(
                listItems = listOf(
                    ClientModel(id = 1L, name = "Ana", cpf = null, dateOfBirth = null, email = null, phone = null)
                ),
                deleteOutcome = DeleteClientOutcome.HAS_LINK
            )
        )
        advanceUntilIdle()

        viewModel.toggleRowSelection(1L, true)
        viewModel.deleteSelection()
        runCurrent()

        val message = viewModel.uiState.value.transientMessage
        assertEquals(R.string.feedback_client_delete_link_warning, message?.textResId)
        assertEquals(MessageTone.WARNING, message?.tone)
    }
}

private class FakeClientsRepository(
    private val listItems: List<ClientModel> = emptyList(),
    private val bulkDeleteResult: Pair<Int, Int> = 0 to 0,
    private val deleteOutcome: DeleteClientOutcome = DeleteClientOutcome.DELETED
) : ClientsRepository {
    override suspend fun list(name: String?, pageIndex: Int, itemsPerPage: Int): ClientsPage {
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

    override suspend fun create(model: ClientModel): ClientModel = model.copy(id = 99L)

    override suspend fun update(id: Long, model: ClientModel): ClientModel = model.copy(id = id)

    override suspend fun delete(id: Long): DeleteClientOutcome = deleteOutcome

    override suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int> = bulkDeleteResult
}
