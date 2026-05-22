package com.tcc.androidnative.feature.appointments.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.feature.clients.data.ClientModel
import com.tcc.androidnative.feature.clients.data.ClientsPage
import com.tcc.androidnative.feature.clients.data.ClientsRepository
import com.tcc.androidnative.feature.clients.data.DeleteClientOutcome
import com.tcc.androidnative.feature.services.data.DeleteServiceOutcome
import com.tcc.androidnative.feature.services.data.ServiceModel
import com.tcc.androidnative.feature.services.data.ServicesPage
import com.tcc.androidnative.feature.services.data.ServicesRepository
import com.tcc.androidnative.testutil.MainDispatcherRule
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init should load client and service options`() = runTest {
        val clientsRepository = FakeClientsRepository()
        val servicesRepository = FakeServicesRepository()
        val viewModel = AppointmentFormViewModel(
            clientsRepository = clientsRepository,
            servicesRepository = servicesRepository
        )

        advanceUntilIdle()

        assertEquals(25, viewModel.uiState.value.clients.size)
        assertEquals("Cliente 1", viewModel.uiState.value.clients.first().name)
        assertEquals("Cliente 25", viewModel.uiState.value.clients.last().name)
        assertEquals(listOf(ClientListCall(name = null, pageIndex = 1, itemsPerPage = 25)), clientsRepository.listCalls)
        assertEquals(25, viewModel.uiState.value.services.size)
        assertEquals("Servico 1", viewModel.uiState.value.services.first().description)
        assertEquals("Servico 25", viewModel.uiState.value.services.last().description)
        assertEquals(listOf(ServiceListCall(description = null, pageIndex = 1, pageSize = 25)), servicesRepository.listCalls)
        assertFalse(viewModel.uiState.value.isLoadingOptions)
    }

    @Test
    fun `client selector should append next page when list reaches the end`() = runTest {
        val clientsRepository = FakeClientsRepository()
        val viewModel = AppointmentFormViewModel(clientsRepository, FakeServicesRepository())
        advanceUntilIdle()

        viewModel.onClientOptionRendered(23)
        advanceUntilIdle()

        assertEquals(50, viewModel.uiState.value.clients.size)
        assertEquals("Cliente 50", viewModel.uiState.value.clients.last().name)
        assertEquals(
            listOf(
                ClientListCall(name = null, pageIndex = 1, itemsPerPage = 25),
                ClientListCall(name = null, pageIndex = 2, itemsPerPage = 25)
            ),
            clientsRepository.listCalls
        )
    }

    @Test
    fun `client selector should filter by typed name with debounce`() = runTest {
        val clientsRepository = FakeClientsRepository()
        val viewModel = AppointmentFormViewModel(clientsRepository, FakeServicesRepository())
        advanceUntilIdle()

        viewModel.selectClient(1)
        viewModel.onClientFilterNameChange("Cliente 5")
        advanceTimeBy(499)

        assertEquals(1, clientsRepository.listCalls.size)
        assertEquals(null, viewModel.uiState.value.selectedClientId)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals("Cliente 5", viewModel.uiState.value.clientFilterName)
        assertEquals(listOf("Cliente 5", "Cliente 50", "Cliente 51"), viewModel.uiState.value.clients.take(3).map { it.name })
        assertEquals(ClientListCall(name = "Cliente 5", pageIndex = 1, itemsPerPage = 25), clientsRepository.listCalls.last())
    }

    @Test
    fun `client selector click should clear filter and list all clients`() = runTest {
        val clientsRepository = FakeClientsRepository()
        val viewModel = AppointmentFormViewModel(clientsRepository, FakeServicesRepository())
        advanceUntilIdle()

        viewModel.onClientFilterNameChange("Cliente 5")
        advanceTimeBy(500)
        advanceUntilIdle()

        assertEquals("Cliente 5", viewModel.uiState.value.clientFilterName)
        assertEquals(ClientListCall(name = "Cliente 5", pageIndex = 1, itemsPerPage = 25), clientsRepository.listCalls.last())

        viewModel.onClientSelectorClick()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.clientFilterName)
        assertEquals(25, viewModel.uiState.value.clients.size)
        assertEquals("Cliente 1", viewModel.uiState.value.clients.first().name)
        assertEquals("Cliente 25", viewModel.uiState.value.clients.last().name)
        assertEquals(ClientListCall(name = null, pageIndex = 1, itemsPerPage = 25), clientsRepository.listCalls.last())
    }

    @Test
    fun `toggle service should preserve other selected services`() = runTest {
        val viewModel = AppointmentFormViewModel(FakeClientsRepository(), FakeServicesRepository())
        advanceUntilIdle()

        viewModel.toggleService(1)
        viewModel.toggleService(2)
        viewModel.toggleService(1)

        assertEquals(setOf(2L), viewModel.uiState.value.selectedServiceIds)
    }

    @Test
    fun `service selector should append next page when list reaches the end`() = runTest {
        val servicesRepository = FakeServicesRepository()
        val viewModel = AppointmentFormViewModel(FakeClientsRepository(), servicesRepository)
        advanceUntilIdle()

        viewModel.onServiceOptionRendered(23)
        advanceUntilIdle()

        assertEquals(50, viewModel.uiState.value.services.size)
        assertEquals("Servico 50", viewModel.uiState.value.services.last().description)
        assertEquals(
            listOf(
                ServiceListCall(description = null, pageIndex = 1, pageSize = 25),
                ServiceListCall(description = null, pageIndex = 2, pageSize = 25)
            ),
            servicesRepository.listCalls
        )
    }

    @Test
    fun `service selector should filter by typed description with debounce`() = runTest {
        val servicesRepository = FakeServicesRepository()
        val viewModel = AppointmentFormViewModel(FakeClientsRepository(), servicesRepository)
        advanceUntilIdle()

        viewModel.onServiceFilterDescriptionChange("Servico 5")
        advanceTimeBy(499)

        assertEquals(1, servicesRepository.listCalls.size)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals("Servico 5", viewModel.uiState.value.serviceFilterDescription)
        assertEquals(listOf("Servico 5", "Servico 50", "Servico 51"), viewModel.uiState.value.services.take(3).map { it.description })
        assertEquals(ServiceListCall(description = "Servico 5", pageIndex = 1, pageSize = 25), servicesRepository.listCalls.last())
    }

    @Test
    fun `service selector click should clear filter and list all services`() = runTest {
        val servicesRepository = FakeServicesRepository()
        val viewModel = AppointmentFormViewModel(FakeClientsRepository(), servicesRepository)
        advanceUntilIdle()

        viewModel.onServiceFilterDescriptionChange("Servico 5")
        advanceTimeBy(500)
        advanceUntilIdle()

        assertEquals("Servico 5", viewModel.uiState.value.serviceFilterDescription)
        assertEquals(ServiceListCall(description = "Servico 5", pageIndex = 1, pageSize = 25), servicesRepository.listCalls.last())

        viewModel.onServiceSelectorClick()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.serviceFilterDescription)
        assertEquals(25, viewModel.uiState.value.services.size)
        assertEquals("Servico 1", viewModel.uiState.value.services.first().description)
        assertEquals("Servico 25", viewModel.uiState.value.services.last().description)
        assertEquals(ServiceListCall(description = null, pageIndex = 1, pageSize = 25), servicesRepository.listCalls.last())
    }

    @Test
    fun `save local only should validate required fields and not persist`() = runTest {
        val clientsRepository = FakeClientsRepository()
        val servicesRepository = FakeServicesRepository()
        val viewModel = AppointmentFormViewModel(clientsRepository, servicesRepository)
        advanceUntilIdle()

        viewModel.saveLocalOnly()

        assertEquals(R.string.appointment_required_feedback, viewModel.uiState.value.feedbackMessage?.textResId)

        viewModel.selectClient(1)
        viewModel.updateStartTime("08:00")
        viewModel.updateEndTime("09:00")
        viewModel.toggleService(1)
        viewModel.saveLocalOnly()

        assertEquals(R.string.appointment_local_save_feedback, viewModel.uiState.value.feedbackMessage?.textResId)
        assertEquals(0, clientsRepository.createCalls)
        assertEquals(0, clientsRepository.updateCalls)
        assertEquals(0, servicesRepository.createCalls)
        assertEquals(0, servicesRepository.updateCalls)
    }
}

private data class ClientListCall(
    val name: String?,
    val pageIndex: Int,
    val itemsPerPage: Int
)

private data class ServiceListCall(
    val description: String?,
    val pageIndex: Int,
    val pageSize: Int
)

private class FakeClientsRepository : ClientsRepository {
    var createCalls = 0
    var updateCalls = 0
    val listCalls = mutableListOf<ClientListCall>()

    private val allItems = (1L..60L).map { id ->
        ClientModel(id, "Cliente $id", null, null, null, null)
    }

    override suspend fun list(name: String?, pageIndex: Int, itemsPerPage: Int): ClientsPage {
        listCalls += ClientListCall(name = name, pageIndex = pageIndex, itemsPerPage = itemsPerPage)
        val filteredItems = name?.let { filter ->
            allItems.filter { it.name.contains(filter, ignoreCase = true) }
        } ?: allItems
        val fromIndex = ((pageIndex - 1) * itemsPerPage).coerceAtLeast(0)
        val toIndex = (fromIndex + itemsPerPage).coerceAtMost(filteredItems.size)
        val items = if (fromIndex < filteredItems.size) filteredItems.subList(fromIndex, toIndex) else emptyList()
        return ClientsPage(
            items = items,
            pageIndex = pageIndex,
            totalPages = (filteredItems.size + itemsPerPage - 1) / itemsPerPage,
            totalItems = filteredItems.size.toLong()
        )
    }

    override suspend fun getById(id: Long): ClientModel {
        return ClientModel(id, "Cliente $id", null, null, null, null)
    }

    override suspend fun create(model: ClientModel): ClientModel {
        createCalls++
        return model
    }

    override suspend fun update(id: Long, model: ClientModel): ClientModel {
        updateCalls++
        return model.copy(id = id)
    }

    override suspend fun delete(id: Long): DeleteClientOutcome = DeleteClientOutcome.DELETED

    override suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int> = ids.size to 0
}

private class FakeServicesRepository : ServicesRepository {
    var createCalls = 0
    var updateCalls = 0
    val listCalls = mutableListOf<ServiceListCall>()

    private val allItems = (1L..60L).map { id ->
        ServiceModel(id, "Servico $id", BigDecimal("10.00"))
    }

    override suspend fun list(description: String?, uiPageIndex: Int, pageSize: Int): ServicesPage {
        listCalls += ServiceListCall(description = description, pageIndex = uiPageIndex, pageSize = pageSize)
        val filteredItems = description?.let { filter ->
            allItems.filter { it.description.contains(filter, ignoreCase = true) }
        } ?: allItems
        val fromIndex = ((uiPageIndex - 1) * pageSize).coerceAtLeast(0)
        val toIndex = (fromIndex + pageSize).coerceAtMost(filteredItems.size)
        val items = if (fromIndex < filteredItems.size) filteredItems.subList(fromIndex, toIndex) else emptyList()
        return ServicesPage(
            items = items,
            pageIndex = uiPageIndex,
            totalPages = (filteredItems.size + pageSize - 1) / pageSize,
            totalItems = filteredItems.size
        )
    }

    override suspend fun getById(id: Long): ServiceModel {
        return ServiceModel(id, "Servico $id", BigDecimal.TEN)
    }

    override suspend fun create(description: String, valueInput: String): ServiceModel {
        createCalls++
        return ServiceModel(10, description, BigDecimal.TEN)
    }

    override suspend fun update(id: Long, description: String, valueInput: String): ServiceModel {
        updateCalls++
        return ServiceModel(id, description, BigDecimal.TEN)
    }

    override suspend fun delete(id: Long): DeleteServiceOutcome = DeleteServiceOutcome.DELETED

    override suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int> = ids.size to 0
}
