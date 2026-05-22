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
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
        assertEquals(25, viewModel.uiState.value.clientSeedOptions.size)
        assertEquals("Cliente 1", viewModel.uiState.value.clients.first().name)
        assertEquals("Cliente 25", viewModel.uiState.value.clients.last().name)
        assertEquals(listOf(ClientListCall(name = null, pageIndex = 1, itemsPerPage = 25)), clientsRepository.listCalls)
        assertEquals(25, viewModel.uiState.value.services.size)
        assertEquals(25, viewModel.uiState.value.serviceSeedOptions.size)
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
    fun `client selector should filter from cached items immediately and request remote in parallel`() = runTest {
        val clientsRepository = FakeClientsRepository(delayByName = mapOf("Cliente 2" to 1_000L))
        val viewModel = AppointmentFormViewModel(clientsRepository, FakeServicesRepository())
        advanceUntilIdle()

        viewModel.selectClient(1)
        viewModel.onClientFilterNameChange("Cliente 2")

        assertEquals(1L, viewModel.uiState.value.selectedClientId)
        assertEquals(
            listOf("Cliente 2", "Cliente 20", "Cliente 21"),
            viewModel.uiState.value.clients.take(3).map { it.name }
        )

        runCurrent()
        assertEquals(
            ClientListCall(name = "Cliente 2", pageIndex = 1, itemsPerPage = 25),
            clientsRepository.listCalls.last()
        )
        assertEquals(
            listOf("Cliente 2", "Cliente 20", "Cliente 21"),
            viewModel.uiState.value.clients.take(3).map { it.name }
        )

        advanceTimeBy(1_000L)
        advanceUntilIdle()

        assertEquals("Cliente 2", viewModel.uiState.value.clientFilterName)
        assertEquals("Cliente 2", viewModel.uiState.value.clients.first().name)
    }

    @Test
    fun `client selector click should clear filter and list cached clients immediately`() = runTest {
        val clientsRepository = FakeClientsRepository()
        val viewModel = AppointmentFormViewModel(clientsRepository, FakeServicesRepository())
        advanceUntilIdle()

        viewModel.onClientFilterNameChange("Cliente 2")
        runCurrent()
        advanceUntilIdle()

        assertEquals("Cliente 2", viewModel.uiState.value.clientFilterName)
        assertEquals(ClientListCall(name = "Cliente 2", pageIndex = 1, itemsPerPage = 25), clientsRepository.listCalls.last())

        viewModel.onClientSelectorClick()

        assertEquals("", viewModel.uiState.value.clientFilterName)
        assertEquals(25, viewModel.uiState.value.clients.size)
        assertEquals("Cliente 1", viewModel.uiState.value.clients.first().name)
        assertEquals("Cliente 25", viewModel.uiState.value.clients.last().name)
        assertEquals(2, clientsRepository.listCalls.size)
    }

    @Test
    fun `client selector click should preserve selected client until another option is chosen`() = runTest {
        val viewModel = AppointmentFormViewModel(FakeClientsRepository(), FakeServicesRepository())
        advanceUntilIdle()

        viewModel.selectClient(1)
        assertEquals(1L, viewModel.uiState.value.selectedClientId)

        viewModel.onClientSelectorClick()
        assertEquals(1L, viewModel.uiState.value.selectedClientId)

        viewModel.onClientFilterNameChange("Cliente 2")
        runCurrent()
        assertEquals(1L, viewModel.uiState.value.selectedClientId)

        viewModel.selectClient(2)
        assertEquals(2L, viewModel.uiState.value.selectedClientId)
    }

    @Test
    fun `stale client filter response should not override newest query`() = runTest {
        val clientsRepository = FakeClientsRepository(
            delayByName = mapOf(
                "Cliente 3" to 1_000L,
                "Cliente 21" to 10L
            )
        )
        val viewModel = AppointmentFormViewModel(clientsRepository, FakeServicesRepository())
        advanceUntilIdle()

        viewModel.onClientFilterNameChange("Cliente 3")
        runCurrent()
        viewModel.onClientFilterNameChange("Cliente 21")
        runCurrent()

        advanceTimeBy(10L)
        advanceUntilIdle()

        assertEquals("Cliente 21", viewModel.uiState.value.clientFilterName)
        assertEquals(listOf("Cliente 21"), viewModel.uiState.value.clients.map { it.name })

        advanceTimeBy(1_000L)
        advanceUntilIdle()

        assertEquals("Cliente 21", viewModel.uiState.value.clientFilterName)
        assertEquals(listOf("Cliente 21"), viewModel.uiState.value.clients.map { it.name })
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
    fun `select appointment date should update date and formatted input`() = runTest {
        val viewModel = AppointmentFormViewModel(FakeClientsRepository(), FakeServicesRepository())
        advanceUntilIdle()

        viewModel.selectAppointmentDate(LocalDate.of(2026, 6, 3))

        assertEquals(LocalDate.of(2026, 6, 3), viewModel.uiState.value.appointmentDate)
        assertEquals("03/06/2026", viewModel.uiState.value.appointmentDateInput)
        assertEquals(null, viewModel.uiState.value.appointmentDateInputErrorResId)
    }

    @Test
    fun `update appointment date should validate manual input`() = runTest {
        val viewModel = AppointmentFormViewModel(FakeClientsRepository(), FakeServicesRepository())
        advanceUntilIdle()

        viewModel.updateAppointmentDate("99/99/9999")
        assertEquals(R.string.appointment_date_invalid, viewModel.uiState.value.appointmentDateInputErrorResId)

        viewModel.updateAppointmentDate("03/06/2026")
        assertEquals(LocalDate.of(2026, 6, 3), viewModel.uiState.value.appointmentDate)
        assertEquals(null, viewModel.uiState.value.appointmentDateInputErrorResId)
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
    fun `service selector should filter from cached items immediately and request remote in parallel`() = runTest {
        val servicesRepository = FakeServicesRepository(delayByDescription = mapOf("Servico 2" to 1_000L))
        val viewModel = AppointmentFormViewModel(FakeClientsRepository(), servicesRepository)
        advanceUntilIdle()

        viewModel.onServiceFilterDescriptionChange("Servico 2")

        assertEquals(
            listOf("Servico 2", "Servico 20", "Servico 21"),
            viewModel.uiState.value.services.take(3).map { it.description }
        )

        runCurrent()
        assertEquals(
            ServiceListCall(description = "Servico 2", pageIndex = 1, pageSize = 25),
            servicesRepository.listCalls.last()
        )

        advanceTimeBy(1_000L)
        advanceUntilIdle()

        assertEquals("Servico 2", viewModel.uiState.value.serviceFilterDescription)
        assertEquals("Servico 2", viewModel.uiState.value.services.first().description)
    }

    @Test
    fun `service selector click should clear filter and list cached services immediately`() = runTest {
        val servicesRepository = FakeServicesRepository()
        val viewModel = AppointmentFormViewModel(FakeClientsRepository(), servicesRepository)
        advanceUntilIdle()

        viewModel.onServiceFilterDescriptionChange("Servico 2")
        runCurrent()
        advanceUntilIdle()

        assertEquals("Servico 2", viewModel.uiState.value.serviceFilterDescription)
        assertEquals(ServiceListCall(description = "Servico 2", pageIndex = 1, pageSize = 25), servicesRepository.listCalls.last())

        viewModel.onServiceSelectorClick()

        assertEquals("", viewModel.uiState.value.serviceFilterDescription)
        assertEquals(25, viewModel.uiState.value.services.size)
        assertEquals("Servico 1", viewModel.uiState.value.services.first().description)
        assertEquals("Servico 25", viewModel.uiState.value.services.last().description)
        assertEquals(2, servicesRepository.listCalls.size)
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

private class FakeClientsRepository(
    private val delayByName: Map<String, Long> = emptyMap()
) : ClientsRepository {
    var createCalls = 0
    var updateCalls = 0
    val listCalls = mutableListOf<ClientListCall>()

    private val allItems = (1L..60L).map { id ->
        ClientModel(id, "Cliente $id", null, null, null, null)
    }

    override suspend fun list(name: String?, pageIndex: Int, itemsPerPage: Int): ClientsPage {
        listCalls += ClientListCall(name = name, pageIndex = pageIndex, itemsPerPage = itemsPerPage)
        delayByName[name]?.let { delay(it) }
        val filteredItems = name?.let { filter ->
            allItems.filter { it.name.startsWith(filter, ignoreCase = true) }
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

private class FakeServicesRepository(
    private val delayByDescription: Map<String, Long> = emptyMap()
) : ServicesRepository {
    var createCalls = 0
    var updateCalls = 0
    val listCalls = mutableListOf<ServiceListCall>()

    private val allItems = (1L..60L).map { id ->
        ServiceModel(id, "Servico $id", BigDecimal("10.00"))
    }

    override suspend fun list(description: String?, uiPageIndex: Int, pageSize: Int): ServicesPage {
        listCalls += ServiceListCall(description = description, pageIndex = uiPageIndex, pageSize = pageSize)
        delayByDescription[description]?.let { delay(it) }
        val filteredItems = description?.let { filter ->
            allItems.filter { it.description.startsWith(filter, ignoreCase = true) }
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
