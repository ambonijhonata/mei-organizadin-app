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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CLIENT_OPTION_PAGE_SIZE = 25
private const val SERVICE_OPTION_PAGE_SIZE = 25
private const val CLIENT_FILTER_DEBOUNCE_MS = 500L
private const val SERVICE_FILTER_DEBOUNCE_MS = 500L

data class AppointmentFormUiState(
    val clients: List<ClientModel> = emptyList(),
    val services: List<ServiceModel> = emptyList(),
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
    private var clientFilterDebounceJob: Job? = null
    private var serviceFilterDebounceJob: Job? = null

    init {
        loadOptions()
    }

    fun loadOptions() {
        reloadClients()
        reloadServices()
    }

    fun onClientFilterNameChange(value: String) {
        _uiState.update {
            it.copy(
                clientFilterName = value,
                selectedClientId = null,
                feedbackMessage = null
            )
        }
        scheduleClientFilterReload()
    }

    fun onClientSelectorClick() {
        clientFilterDebounceJob?.cancel()
        _uiState.update {
            it.copy(
                clientFilterName = "",
                selectedClientId = null,
                feedbackMessage = null
            )
        }
        reloadClients()
    }

    fun onClientOptionRendered(index: Int) {
        val state = _uiState.value
        val isNearEnd = index >= state.clients.lastIndex - 2
        val hasMore = state.clientPageIndex < state.clientTotalPages
        if (isNearEnd && hasMore && !state.isLoadingClients && !state.isAppendingClients) {
            loadClientPage(reset = false)
        }
    }

    fun onServiceFilterDescriptionChange(value: String) {
        _uiState.update {
            it.copy(
                serviceFilterDescription = value,
                feedbackMessage = null
            )
        }
        scheduleServiceFilterReload()
    }

    fun onServiceSelectorClick() {
        serviceFilterDebounceJob?.cancel()
        _uiState.update {
            it.copy(
                serviceFilterDescription = "",
                feedbackMessage = null
            )
        }
        reloadServices()
    }

    fun onServiceOptionRendered(index: Int) {
        val state = _uiState.value
        val isNearEnd = index >= state.services.lastIndex - 2
        val hasMore = state.servicePageIndex < state.serviceTotalPages
        if (isNearEnd && hasMore && !state.isLoadingServices && !state.isAppendingServices) {
            loadServicePage(reset = false)
        }
    }

    private fun reloadClients() {
        loadClientPage(reset = true)
    }

    private fun reloadServices() {
        loadServicePage(reset = true)
    }

    private fun loadClientPage(reset: Boolean) {
        val state = _uiState.value
        if (state.isLoadingClients || state.isAppendingClients) return

        viewModelScope.launch {
            if (reset) {
                _uiState.update {
                    it.copy(
                        clients = emptyList(),
                        clientPageIndex = 0,
                        clientTotalPages = 1,
                        isLoadingClients = true,
                        feedbackMessage = null
                    )
                }
            } else {
                _uiState.update { it.copy(isAppendingClients = true, feedbackMessage = null) }
            }

            val pageToLoad = if (reset) 1 else (_uiState.value.clientPageIndex + 1)
            runCatching {
                clientsRepository.list(
                    name = _uiState.value.clientFilterName.trim().ifBlank { null },
                    pageIndex = pageToLoad,
                    itemsPerPage = CLIENT_OPTION_PAGE_SIZE
                )
            }.onSuccess { page ->
                _uiState.update { current ->
                    val mergedClients = if (reset) {
                        page.items
                    } else {
                        current.clients + page.items.filterNot { incoming ->
                            current.clients.any { existing -> existing.id == incoming.id }
                        }
                    }
                    current.copy(
                        clients = mergedClients,
                        clientPageIndex = page.pageIndex,
                        clientTotalPages = page.totalPages,
                        isLoadingClients = false,
                        isAppendingClients = false,
                        feedbackMessage = null
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoadingClients = false,
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

    private fun loadServicePage(reset: Boolean) {
        val state = _uiState.value
        if (state.isLoadingServices || state.isAppendingServices) return

        viewModelScope.launch {
            if (reset) {
                _uiState.update {
                    it.copy(
                        services = emptyList(),
                        servicePageIndex = 0,
                        serviceTotalPages = 1,
                        isLoadingServices = true,
                        feedbackMessage = null
                    )
                }
            } else {
                _uiState.update { it.copy(isAppendingServices = true, feedbackMessage = null) }
            }

            val pageToLoad = if (reset) 1 else (_uiState.value.servicePageIndex + 1)
            runCatching {
                servicesRepository.list(
                    description = _uiState.value.serviceFilterDescription.trim().ifBlank { null },
                    uiPageIndex = pageToLoad,
                    pageSize = SERVICE_OPTION_PAGE_SIZE
                )
            }.onSuccess { page ->
                _uiState.update { current ->
                    val mergedServices = if (reset) {
                        page.items
                    } else {
                        current.services + page.items.filterNot { incoming ->
                            current.services.any { existing -> existing.id == incoming.id }
                        }
                    }
                    current.copy(
                        services = mergedServices,
                        servicePageIndex = page.pageIndex,
                        serviceTotalPages = page.totalPages,
                        isLoadingServices = false,
                        isAppendingServices = false,
                        feedbackMessage = null
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoadingServices = false,
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
                state.services.firstOrNull { it.id == serviceId }?.let { selectedOptions[serviceId] = it }
            }
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

    private fun scheduleClientFilterReload() {
        clientFilterDebounceJob?.cancel()
        clientFilterDebounceJob = viewModelScope.launch {
            delay(CLIENT_FILTER_DEBOUNCE_MS)
            reloadClients()
        }
    }

    private fun scheduleServiceFilterReload() {
        serviceFilterDebounceJob?.cancel()
        serviceFilterDebounceJob = viewModelScope.launch {
            delay(SERVICE_FILTER_DEBOUNCE_MS)
            reloadServices()
        }
    }
}
