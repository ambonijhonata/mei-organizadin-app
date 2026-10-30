package com.tcc.androidnative.feature.appointments.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.clients.data.ClientModel
import com.tcc.androidnative.feature.clients.data.ClientsRepository
import com.tcc.androidnative.feature.services.data.ServiceModel
import com.tcc.androidnative.feature.services.data.ServicesRepository
import com.tcc.androidnative.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
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
    val clientErrorResId: Int? = null,
    val clientFilterName: String = "",
    val serviceFilterDescription: String = "",
    val appointmentDate: LocalDate = LocalDate.now(),
    val appointmentDateInput: String = DateFormats.toUiDate(LocalDate.now()),
    val appointmentDateInputErrorResId: Int? = null,
    val startTime: String = "",
    val startTimeErrorResId: Int? = null,
    val hasEditedStartTime: Boolean = false,
    val endTime: String = "",
    val endTimeErrorResId: Int? = null,
    val hasEditedEndTime: Boolean = false,
    val selectedServiceIds: Set<Long> = emptySet(),
    val selectedServiceOptions: Map<Long, ServiceModel> = emptyMap(),
    val checkedServiceGridIds: Set<Long> = emptySet(),
    val servicesErrorResId: Int? = null,
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

    val availableServices: List<ServiceModel>
        get() = services.filterNot { it.id in selectedServiceIds }
}

@HiltViewModel
class AppointmentFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val clientsRepository: ClientsRepository,
    private val servicesRepository: ServicesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        createInitialAppointmentFormUiState(
            prefilledDate = savedStateHandle
                .get<String?>(AppDestination.AppointmentForm.ARG_DATE)
                ?.toLocalDateOrNull(),
            prefilledStartTime = savedStateHandle
                .get<String?>(AppDestination.AppointmentForm.ARG_START_TIME)
                ?.toLocalTimeOrNull(),
            prefilledEndTime = savedStateHandle
                .get<String?>(AppDestination.AppointmentForm.ARG_END_TIME)
                ?.toLocalTimeOrNull()
        )
    )
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
        } else {
            ensureAvailableServiceOptionsLoaded()
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
                if (requestToken != clientQueryToken ||
                    _uiState.value.clientFilterName.trim().ifBlank { null } != query
                ) {
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
                if (requestToken != clientQueryToken ||
                    _uiState.value.clientFilterName.trim().ifBlank { null } != query
                ) {
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
                if (requestToken != serviceQueryToken ||
                    _uiState.value.serviceFilterDescription.trim().ifBlank { null } != query
                ) {
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
                if (requestToken != serviceQueryToken ||
                    _uiState.value.serviceFilterDescription.trim().ifBlank { null } != query
                ) {
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
                clientErrorResId = null,
                clientFilterName = state.clients.firstOrNull { it.id == clientId }?.name ?: state.clientFilterName,
                feedbackMessage = null
            )
        }
    }

    fun updateStartTime(value: String) {
        _uiState.update { state ->
            state.copy(
                startTime = value,
                startTimeErrorResId = if (value.isNotBlank()) null else state.startTimeErrorResId,
                endTimeErrorResId = if (state.endTimeErrorResId ==
                    R.string.appointment_end_time_must_be_after_start_time
                ) {
                    null
                } else {
                    state.endTimeErrorResId
                },
                feedbackMessage = null
            )
        }
    }

    fun updateAppointmentDate(value: String) {
        val parsedDate = runCatching { DateFormats.parseUiDate(value) }.getOrNull()
        _uiState.update { state ->
            when {
                value.isBlank() -> {
                    state.copy(
                        appointmentDateInput = value,
                        appointmentDateInputErrorResId = R.string.appointment_date_invalid,
                        feedbackMessage = null
                    )
                }

                parsedDate != null -> {
                    state.copy(
                        appointmentDate = parsedDate,
                        appointmentDateInput = value,
                        appointmentDateInputErrorResId = null,
                        feedbackMessage = null
                    )
                }

                else -> {
                    state.copy(
                        appointmentDateInput = value,
                        appointmentDateInputErrorResId = R.string.appointment_date_invalid,
                        feedbackMessage = null
                    )
                }
            }
        }
    }

    fun selectAppointmentDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                appointmentDate = date,
                appointmentDateInput = DateFormats.toUiDate(date),
                appointmentDateInputErrorResId = null,
                feedbackMessage = null
            )
        }
    }

    fun updateEndTime(value: String) {
        _uiState.update { state ->
            state.copy(
                endTime = value,
                endTimeErrorResId = if (value.isNotBlank()) null else state.endTimeErrorResId,
                feedbackMessage = null
            )
        }
    }

    fun completeStartTimeEditing() {
        _uiState.update { state ->
            val markedState = state.copy(hasEditedStartTime = true)
            validateTimeRangeIfReady(markedState)
        }
    }

    fun completeEndTimeEditing() {
        _uiState.update { state ->
            val markedState = state.copy(hasEditedEndTime = true)
            validateTimeRangeIfReady(markedState)
        }
    }

    fun addService(serviceId: Long) {
        _uiState.update { state ->
            val selected = state.selectedServiceIds.toMutableSet()
            val selectedOptions = state.selectedServiceOptions.toMutableMap()
            if (!selected.contains(serviceId)) {
                selected.add(serviceId)
                (
                    state.services.firstOrNull { it.id == serviceId }
                        ?: state.serviceSeedOptions.firstOrNull { it.id == serviceId }
                        ?: selectedOptions[serviceId]
                    )?.let { selectedOptions[serviceId] = it }
            }

            state.copy(
                selectedServiceIds = selected,
                selectedServiceOptions = selectedOptions,
                checkedServiceGridIds = state.checkedServiceGridIds - serviceId,
                servicesErrorResId = if (selected.isNotEmpty()) null else state.servicesErrorResId,
                feedbackMessage = null
            )
        }
    }

    fun toggleServiceGridSelection(serviceId: Long, checked: Boolean) {
        _uiState.update { state ->
            val updated = state.checkedServiceGridIds.toMutableSet()
            if (checked) {
                updated.add(serviceId)
            } else {
                updated.remove(serviceId)
            }
            state.copy(
                checkedServiceGridIds = updated,
                feedbackMessage = null
            )
        }
    }

    fun removeCheckedServices() {
        _uiState.update { state ->
            if (state.checkedServiceGridIds.isEmpty()) {
                return@update state
            }

            val remainingIds = state.selectedServiceIds - state.checkedServiceGridIds
            val remainingOptions = state.selectedServiceOptions.filterKeys { it in remainingIds }
            state.copy(
                selectedServiceIds = remainingIds,
                selectedServiceOptions = remainingOptions,
                checkedServiceGridIds = emptySet(),
                feedbackMessage = null
            )
        }
        ensureAvailableServiceOptionsLoaded()
    }

    private fun ensureAvailableServiceOptionsLoaded() {
        val state = _uiState.value
        if (state.availableServices.isEmpty() &&
            state.servicePageIndex < state.serviceTotalPages &&
            !state.isLoadingServices &&
            !state.isAppendingServices
        ) {
            loadNextServicePage()
        }
    }

    fun saveLocalOnly() {
        val state = _uiState.value
        val clientErrorResId = if (state.selectedClientId == null) R.string.appointment_client_required else null
        val dateErrorResId = when {
            state.appointmentDateInput.isBlank() -> R.string.appointment_date_invalid
            state.appointmentDateInputErrorResId != null -> state.appointmentDateInputErrorResId
            else -> null
        }
        val startTimeErrorResId = if (state.startTime.isBlank()) R.string.appointment_start_time_required else null
        val parsedStartTime = state.startTime.parseCompleteTimeOrNull()
        val parsedEndTime = state.endTime.parseCompleteTimeOrNull()
        val endTimeErrorResId = when {
            state.endTime.isBlank() -> R.string.appointment_end_time_required
            clientErrorResId == null &&
                startTimeErrorResId == null &&
                parsedStartTime != null &&
                parsedEndTime != null &&
                parsedEndTime <= parsedStartTime -> R.string.appointment_end_time_must_be_after_start_time
            else -> null
        }
        val servicesErrorResId = if (state.selectedServiceIds.isEmpty()) R.string.appointment_services_required else null

        if (clientErrorResId != null ||
            dateErrorResId != null ||
            startTimeErrorResId != null ||
            endTimeErrorResId != null ||
            servicesErrorResId != null
        ) {
            _uiState.update {
                it.copy(
                    clientErrorResId = clientErrorResId,
                    appointmentDateInputErrorResId = dateErrorResId,
                    startTimeErrorResId = startTimeErrorResId,
                    endTimeErrorResId = endTimeErrorResId,
                    servicesErrorResId = servicesErrorResId,
                    feedbackMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                clientErrorResId = null,
                appointmentDateInputErrorResId = null,
                startTimeErrorResId = null,
                endTimeErrorResId = null,
                servicesErrorResId = null,
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

    private fun validateTimeRangeIfReady(state: AppointmentFormUiState): AppointmentFormUiState {
        if (!state.hasEditedStartTime || !state.hasEditedEndTime) {
            return state
        }

        val parsedStartTime = state.startTime.parseCompleteTimeOrNull()
        val parsedEndTime = state.endTime.parseCompleteTimeOrNull()
        val endTimeErrorResId = when {
            state.endTime.isBlank() -> state.endTimeErrorResId
            parsedStartTime != null &&
                parsedEndTime != null &&
                parsedEndTime <= parsedStartTime -> R.string.appointment_end_time_must_be_after_start_time
            else -> null
        }

        return state.copy(
            endTimeErrorResId = endTimeErrorResId,
            feedbackMessage = null
        )
    }
}

internal fun createInitialAppointmentFormUiState(
    prefilledDate: LocalDate? = null,
    prefilledStartTime: LocalTime? = null,
    prefilledEndTime: LocalTime? = null,
    currentDate: LocalDate = LocalDate.now(),
    currentTime: LocalTime = LocalTime.now()
): AppointmentFormUiState {
    val resolvedDate = prefilledDate ?: currentDate
    val resolvedStartTime = prefilledStartTime ?: currentTime
    val resolvedEndTime = prefilledEndTime ?: resolvedStartTime.plusMinutes(30)

    return AppointmentFormUiState(
        appointmentDate = resolvedDate,
        appointmentDateInput = DateFormats.toUiDate(resolvedDate),
        startTime = formatAppointmentTime(resolvedStartTime),
        endTime = formatAppointmentTime(resolvedEndTime)
    )
}

private fun formatAppointmentTime(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

private fun String.parseCompleteTimeOrNull(): LocalTime? {
    if (!matches(Regex("\\d{2}:\\d{2}"))) return null
    return runCatching(LocalTime::parse).getOrNull()
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching(LocalDate::parse).getOrNull()

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching(LocalTime::parse).getOrNull()
