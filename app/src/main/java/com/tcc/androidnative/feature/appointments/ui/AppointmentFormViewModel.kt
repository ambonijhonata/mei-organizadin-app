package com.tcc.androidnative.feature.appointments.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.feature.clients.data.ClientModel
import com.tcc.androidnative.feature.clients.data.ClientsRepository
import com.tcc.androidnative.feature.services.data.ServiceModel
import com.tcc.androidnative.feature.services.data.ServicesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CLIENT_OPTION_PAGE_SIZE = 25
private const val SERVICE_OPTION_PAGE_SIZE = 25

data class AppointmentFormUiState(
    val clients: List<ClientModel> = emptyList(),
    val services: List<ServiceModel> = emptyList(),
    val clientSeedOptions: List<ClientModel> = emptyList(),
    val serviceSeedOptions: List<ServiceModel> = emptyList(),
    val selectedClientId: Long? = null,
    val clientFilterName: String = "",
    val serviceFilterDescription: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val selectedServiceIds: Set<Long> = emptySet(),
    val selectedServiceOptions: Map<Long, ServiceModel> = emptyMap(),
    val clientPageIndex: Int = 0,
    val clientTotalPages: Int = 1,
    val servicePageIndex: Int = 0,
    val serviceTotalPages: Int = 1,
    val clientSeedPageIndex: Int = 0,
    val clientSeedTotalPages: Int = 1,
    val serviceSeedPageIndex: Int = 0,
    val serviceSeedTotalPages: Int = 1,
    val isLoadingClients: Boolean = false,
    val isAppendingClients: Boolean = false,
    val isLoadingServices: Boolean = false,
    val isAppendingServices: Boolean = false,
    val feedbackMessage: TransientMessage? = null
) {
    val isLoadingOptions: Boolean
        get() = isLoadingClients || isLoadingServices
}

@HiltViewModel
class AppointmentFormViewModel @Inject constructor(
    private val clientsRepository: ClientsRepository,
    private val servicesRepository: ServicesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppointmentFormUiState())
    val uiState: StateFlow<AppointmentFormUiState> = _uiState.asStateFlow()

    private var clientRemoteSearchJob: Job? = null
    private var serviceRemoteSearchJob: Job? = null
    private var clientQueryToken: Long = 0
    private var serviceQueryToken: Long = 0

    init {
        loadOptions()
    }

    fun loadOptions() {
        refreshClientSeed()
        refreshServiceSeed()
    }

    fun onClientFilterNameChange(value: String) {
        clientRemoteSearchJob?.cancel()

        _uiState.update { state ->
            state.copy(
                clientFilterName = value,
                selectedClientId = null,
                clients = filterClientsByPrefix(state.clientSeedOptions, value),
                clientPageIndex = if (value.isBlank()) state.clientSeedPageIndex else 0,
                clientTotalPages = if (value.isBlank()) state.clientSeedTotalPages else 1,
                isLoadingClients = false,
                feedbackMessage = null
            )
        }

        val query = value.trim()
        if (query.isBlank()) {
            clientQueryToken++
            return
        }

        requestClientFilteredFirstPage(query)
    }

    fun onClientSelectorClick() {
        clientRemoteSearchJob?.cancel()
        clientQueryToken++

        _uiState.update { state ->
            val keepCurrentUnfilteredItems = state.clientFilterName.isBlank() && state.clients.isNotEmpty()
            state.copy(
                clientFilterName = "",
                selectedClientId = null,
                clients = if (keepCurrentUnfilteredItems) state.clients else state.clientSeedOptions,
                clientPageIndex = if (keepCurrentUnfilteredItems) state.clientPageIndex else state.clientSeedPageIndex,
                clientTotalPages = if (keepCurrentUnfilteredItems) state.clientTotalPages else state.clientSeedTotalPages,
                isLoadingClients = false,
                feedbackMessage = null
            )
        }

        if (_uiState.value.clientSeedOptions.isEmpty()) {
            refreshClientSeed()
        }
    }

    fun onClientOptionRendered(index: Int) {
        val state = _uiState.value
        val isNearEnd = index >= state.clients.lastIndex - 2
        val hasMore = state.clientPageIndex < state.clientTotalPages
        if (isNearEnd && hasMore && !state.isLoadingClients && !state.isAppendingClients) {
            loadNextClientPage()
        }
    }

    fun onServiceFilterDescriptionChange(value: String) {
        serviceRemoteSearchJob?.cancel()

        _uiState.update { state ->
            state.copy(
                serviceFilterDescription = value,
                services = filterServicesByPrefix(state.serviceSeedOptions, value),
                servicePageIndex = if (value.isBlank()) state.serviceSeedPageIndex else 0,
                serviceTotalPages = if (value.isBlank()) state.serviceSeedTotalPages else 1,
                isLoadingServices = false,
                feedbackMessage = null
            )
        }

        val query = value.trim()
        if (query.isBlank()) {
            serviceQueryToken++
            return
        }

        requestServiceFilteredFirstPage(query)
    }

    fun onServiceSelectorClick() {
        serviceRemoteSearchJob?.cancel()
        serviceQueryToken++

        _uiState.update { state ->
            val keepCurrentUnfilteredItems = state.serviceFilterDescription.isBlank() && state.services.isNotEmpty()
            state.copy(
                serviceFilterDescription = "",
                services = if (keepCurrentUnfilteredItems) state.services else state.serviceSeedOptions,
                servicePageIndex = if (keepCurrentUnfilteredItems) state.servicePageIndex else state.serviceSeedPageIndex,
                serviceTotalPages = if (keepCurrentUnfilteredItems) state.serviceTotalPages else state.serviceSeedTotalPages,
                isLoadingServices = false,
                feedbackMessage = null
            )
        }

        if (_uiState.value.serviceSeedOptions.isEmpty()) {
            refreshServiceSeed()
        }
    }

    fun onServiceOptionRendered(index: Int) {
        val state = _uiState.value
        val isNearEnd = index >= state.services.lastIndex - 2
        val hasMore = state.servicePageIndex < state.serviceTotalPages
        if (isNearEnd && hasMore && !state.isLoadingServices && !state.isAppendingServices) {
            loadNextServicePage()
        }
    }

    private fun refreshClientSeed() {
        val state = _uiState.value
        if (state.isLoadingClients || state.isAppendingClients) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingClients = true, feedbackMessage = null) }

            runCatching {
                clientsRepository.list(
                    name = null,
                    pageIndex = 1,
                    itemsPerPage = CLIENT_OPTION_PAGE_SIZE
                )
            }.onSuccess { page ->
                _uiState.update { current ->
                    val shouldRefreshVisible = current.clientFilterName.isBlank()
                    current.copy(
                        clientSeedOptions = page.items,
                        clientSeedPageIndex = page.pageIndex,
                        clientSeedTotalPages = page.totalPages,
                        clients = if (shouldRefreshVisible) page.items else current.clients,
                        clientPageIndex = if (shouldRefreshVisible) page.pageIndex else current.clientPageIndex,
                        clientTotalPages = if (shouldRefreshVisible) page.totalPages else current.clientTotalPages,
                        isLoadingClients = false,
                        feedbackMessage = null
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoadingClients = false,
                        feedbackMessage = TransientMessage(
                            textResId = R.string.appointment_options_load_error,
                            tone = MessageTone.ERROR,
                            durationMillis = MessageDurations.SHORT_3S
                        )
                    )
                }
            }
        }
    }

    private fun refreshServiceSeed() {
        val state = _uiState.value
        if (state.isLoadingServices || state.isAppendingServices) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingServices = true, feedbackMessage = null) }

            runCatching {
                servicesRepository.list(
                    description = null,
                    uiPageIndex = 1,
                    pageSize = SERVICE_OPTION_PAGE_SIZE
                )
            }.onSuccess { page ->
                _uiState.update { current ->
                    val shouldRefreshVisible = current.serviceFilterDescription.isBlank()
                    current.copy(
                        serviceSeedOptions = page.items,
                        serviceSeedPageIndex = page.pageIndex,
                        serviceSeedTotalPages = page.totalPages,
                        services = if (shouldRefreshVisible) page.items else current.services,
                        servicePageIndex = if (shouldRefreshVisible) page.pageIndex else current.servicePageIndex,
                        serviceTotalPages = if (shouldRefreshVisible) page.totalPages else current.serviceTotalPages,
                        isLoadingServices = false,
                        feedbackMessage = null
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoadingServices = false,
                        feedbackMessage = TransientMessage(
                            textResId = R.string.appointment_options_load_error,
                            tone = MessageTone.ERROR,
                            durationMillis = MessageDurations.SHORT_3S
                        )
                    )
                }
            }
        }
    }

    private fun requestClientFilteredFirstPage(query: String) {
        val requestToken = ++clientQueryToken
        clientRemoteSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingClients = true, feedbackMessage = null) }

            runCatching {
                clientsRepository.list(
                    name = query,
                    pageIndex = 1,
                    itemsPerPage = CLIENT_OPTION_PAGE_SIZE
                )
            }.onSuccess { page ->
                if (requestToken != clientQueryToken || _uiState.value.clientFilterName.trim() != query) {
                    return@onSuccess
                }

                _uiState.update {
                    it.copy(
                        clients = page.items,
                        clientPageIndex = page.pageIndex,
                        clientTotalPages = page.totalPages,
                        isLoadingClients = false,
                        feedbackMessage = null
                    )
                }
            }.onFailure {
                if (requestToken != clientQueryToken || _uiState.value.clientFilterName.trim() != query) {
                    return@onFailure
                }

                _uiState.update {
                    it.copy(
                        isLoadingClients = false,
                        feedbackMessage = TransientMessage(
                            textResId = R.string.appointment_options_load_error,
                            tone = MessageTone.ERROR,
                            durationMillis = MessageDurations.SHORT_3S
                        )
                    )
                }
            }
        }
    }

    private fun requestServiceFilteredFirstPage(query: String) {
        val requestToken = ++serviceQueryToken
        serviceRemoteSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingServices = true, feedbackMessage = null) }

            runCatching {
                servicesRepository.list(
                    description = query,
                    uiPageIndex = 1,
                    pageSize = SERVICE_OPTION_PAGE_SIZE
                )
            }.onSuccess { page ->
                if (requestToken != serviceQueryToken || _uiState.value.serviceFilterDescription.trim() != query) {
                    return@onSuccess
                }

                _uiState.update {
                    it.copy(
                        services = page.items,
                        servicePageIndex = page.pageIndex,
                        serviceTotalPages = page.totalPages,
                        isLoadingServices = false,
                        feedbackMessage = null
                    )
                }
            }.onFailure {
                if (requestToken != serviceQueryToken || _uiState.value.serviceFilterDescription.trim() != query) {
                    return@onFailure
                }

                _uiState.update {
                    it.copy(
                        isLoadingServices = false,
                        feedbackMessage = TransientMessage(
                            textResId = R.string.appointment_options_load_error,
                            tone = MessageTone.ERROR,
                            durationMillis = MessageDurations.SHORT_3S
                        )
                    )
                }
            }
        }
    }

    private fun loadNextClientPage() {
        val state = _uiState.value
        if (state.isLoadingClients || state.isAppendingClients) return

        val query = state.clientFilterName.trim().ifBlank { null }
        val requestToken = clientQueryToken

        viewModelScope.launch {
            _uiState.update { it.copy(isAppendingClients = true, feedbackMessage = null) }

            runCatching {
                clientsRepository.list(
                    name = query,
                    pageIndex = state.clientPageIndex + 1,
                    itemsPerPage = CLIENT_OPTION_PAGE_SIZE
                )
            }.onSuccess { page ->
                if (requestToken != clientQueryToken || _uiState.value.clientFilterName.trim().ifBlank { null } != query) {
                    return@onSuccess
                }

                _uiState.update { current ->
                    current.copy(
                        clients = current.clients + page.items.filterNot { incoming ->
                            current.clients.any { existing -> existing.id == incoming.id }
                        },
                        clientPageIndex = page.pageIndex,
                        clientTotalPages = page.totalPages,
                        isAppendingClients = false,
                        feedbackMessage = null
                    )
                }
            }.onFailure {
                if (requestToken != clientQueryToken || _uiState.value.clientFilterName.trim().ifBlank { null } != query) {
                    return@onFailure
                }

                _uiState.update {
                    it.copy(
                        isAppendingClients = false,
                        feedbackMessage = TransientMessage(
                            textResId = R.string.appointment_options_load_error,
                            tone = MessageTone.ERROR,
                            durationMillis = MessageDurations.SHORT_3S
                        )
                    )
                }
            }
        }
    }

    private fun loadNextServicePage() {
        val state = _uiState.value
        if (state.isLoadingServices || state.isAppendingServices) return

        val query = state.serviceFilterDescription.trim().ifBlank { null }
        val requestToken = serviceQueryToken

        viewModelScope.launch {
            _uiState.update { it.copy(isAppendingServices = true, feedbackMessage = null) }

            runCatching {
                servicesRepository.list(
                    description = query,
                    uiPageIndex = state.servicePageIndex + 1,
                    pageSize = SERVICE_OPTION_PAGE_SIZE
                )
            }.onSuccess { page ->
                if (requestToken != serviceQueryToken || _uiState.value.serviceFilterDescription.trim().ifBlank { null } != query) {
                    return@onSuccess
                }

                _uiState.update { current ->
                    current.copy(
                        services = current.services + page.items.filterNot { incoming ->
                            current.services.any { existing -> existing.id == incoming.id }
                        },
                        servicePageIndex = page.pageIndex,
                        serviceTotalPages = page.totalPages,
                        isAppendingServices = false,
                        feedbackMessage = null
                    )
                }
            }.onFailure {
                if (requestToken != serviceQueryToken || _uiState.value.serviceFilterDescription.trim().ifBlank { null } != query) {
                    return@onFailure
                }

                _uiState.update {
                    it.copy(
                        isAppendingServices = false,
                        feedbackMessage = TransientMessage(
                            textResId = R.string.appointment_options_load_error,
                            tone = MessageTone.ERROR,
                            durationMillis = MessageDurations.SHORT_3S
                        )
                    )
                }
            }
        }
    }

    fun selectClient(clientId: Long) {
        _uiState.update { state ->
            state.copy(
                selectedClientId = clientId,
                clientFilterName = state.clients.firstOrNull { it.id == clientId }?.name ?: state.clientFilterName,
                feedbackMessage = null
            )
        }
    }

    fun updateStartTime(value: String) {
        _uiState.update { it.copy(startTime = value, feedbackMessage = null) }
    }

    fun updateEndTime(value: String) {
        _uiState.update { it.copy(endTime = value, feedbackMessage = null) }
    }

    fun toggleService(serviceId: Long) {
        _uiState.update { state ->
            val selected = state.selectedServiceIds.toMutableSet()
            val selectedOptions = state.selectedServiceOptions.toMutableMap()
            if (selected.contains(serviceId)) {
                selected.remove(serviceId)
                selectedOptions.remove(serviceId)
            } else {
                selected.add(serviceId)
                state.services.firstOrNull { it.id == serviceId }
                    ?: state.serviceSeedOptions.firstOrNull { it.id == serviceId }
            }?.let { selectedOptions[serviceId] = it }

            state.copy(
                selectedServiceIds = selected,
                selectedServiceOptions = selectedOptions,
                feedbackMessage = null
            )
        }
    }

    fun saveLocalOnly() {
        val state = _uiState.value
        if (state.selectedClientId == null ||
            state.startTime.isBlank() ||
            state.endTime.isBlank() ||
            state.selectedServiceIds.isEmpty()
        ) {
            _uiState.update {
                it.copy(
                    feedbackMessage = TransientMessage(
                        textResId = R.string.appointment_required_feedback,
                        tone = MessageTone.ERROR,
                        durationMillis = MessageDurations.SHORT_3S
                    )
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                feedbackMessage = TransientMessage(
                    textResId = R.string.appointment_local_save_feedback,
                    tone = MessageTone.SUCCESS,
                    durationMillis = MessageDurations.SHORT_3S
                )
            )
        }
    }

    private fun filterClientsByPrefix(items: List<ClientModel>, query: String): List<ClientModel> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return items
        return items.filter { it.name.startsWith(normalizedQuery, ignoreCase = true) }
    }

    private fun filterServicesByPrefix(items: List<ServiceModel>, query: String): List<ServiceModel> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return items
        return items.filter { it.description.startsWith(normalizedQuery, ignoreCase = true) }
    }
}
