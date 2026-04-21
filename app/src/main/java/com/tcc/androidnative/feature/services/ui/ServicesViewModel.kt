package com.tcc.androidnative.feature.services.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.core.network.BackendApiException
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.core.util.CurrencyFormats
import com.tcc.androidnative.feature.services.data.DeleteServiceOutcome
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

private const val SERVICES_PAGE_SIZE = 25
private const val FILTER_DEBOUNCE_MS = 500L
private const val SERVICE_FIELD_DESCRIPTION = "description"
private const val SERVICE_FIELD_VALUE = "valueInput"
private val DUPLICATE_SERVICE_MESSAGE_REGEX = Regex("^\\s*(.+?)\\s+[Jj](?:á|a) cadastrado\\.?\\s*$")
private const val LEGACY_DUPLICATE_SERVICE_MESSAGE = "Service with this description already exists"
private const val DEFAULT_DUPLICATE_SERVICE_MESSAGE = "Serviço já cadastrado"

enum class ServiceFormMode {
    CREATE,
    EDIT
}

data class ServiceFormState(
    val id: Long? = null,
    val description: String = "",
    val valueInput: String = "",
    val fieldErrors: Map<String, TransientMessage> = emptyMap(),
    val bannerMessage: TransientMessage? = null
)

data class ServicesUiState(
    val items: List<ServiceModel> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val filterDescription: String = "",
    val pageIndex: Int = 0,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isAppending: Boolean = false,
    val isSubmittingForm: Boolean = false,
    val formMode: ServiceFormMode? = null,
    val formState: ServiceFormState = ServiceFormState(),
    val transientMessages: List<TransientMessage> = emptyList()
)

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val repository: ServicesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServicesUiState())
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()
    private var filterDebounceJob: Job? = null

    init {
        reload()
    }

    fun onFilterDescriptionChange(value: String) {
        _uiState.update { it.copy(filterDescription = value) }
        scheduleFilterReload()
    }

    fun applyFilter() {
        filterDebounceJob?.cancel()
        filterDebounceJob = null
        reload()
    }

    fun onListItemRendered(index: Int) {
        val state = _uiState.value
        val isNearEnd = index >= state.items.lastIndex - 2
        val hasMore = state.pageIndex < state.totalPages
        if (isNearEnd && hasMore && !state.isLoading && !state.isAppending) {
            loadPage(reset = false)
        }
    }

    fun toggleRowSelection(id: Long, checked: Boolean) {
        _uiState.update { state ->
            val updated = state.selectedIds.toMutableSet()
            if (checked) updated += id else updated -= id
            state.copy(selectedIds = updated)
        }
    }

    fun toggleHeaderSelection(checked: Boolean) {
        _uiState.update { state ->
            val visibleIds = state.items.map { it.id }.toSet()
            val updated = state.selectedIds.toMutableSet()
            if (checked) updated += visibleIds else updated -= visibleIds
            state.copy(selectedIds = updated)
        }
    }

    fun openCreateForm() {
        _uiState.update {
            it.copy(
                formMode = ServiceFormMode.CREATE,
                formState = ServiceFormState()
            )
        }
    }

    fun openEditForm() {
        val selected = _uiState.value.selectedIds.singleOrNull() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingForm = true) }
            runCatching { repository.getById(selected) }
                .onSuccess { model ->
                    _uiState.update {
                        it.copy(
                            isSubmittingForm = false,
                            formMode = ServiceFormMode.EDIT,
                            formState = ServiceFormState(
                                id = model.id,
                                description = model.description,
                                valueInput = CurrencyFormats.formatForUi(model.value)
                            )
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state -> state.copy(isSubmittingForm = false) }
                    showMessage(
                        textResId = R.string.feedback_service_edit_load_error,
                        tone = MessageTone.ERROR,
                        duration = MessageDurations.SHORT_3S
                    )
                }
        }
    }

    fun dismissForm() {
        _uiState.update {
            it.copy(
                formMode = null,
                formState = ServiceFormState(),
                isSubmittingForm = false
            )
        }
    }

    fun updateForm(
        description: String? = null,
        valueInput: String? = null
    ) {
        _uiState.update { state ->
            val updatedFieldErrors = state.formState.fieldErrors.toMutableMap().apply {
                if (description != null) remove(SERVICE_FIELD_DESCRIPTION)
                if (valueInput != null) remove(SERVICE_FIELD_VALUE)
            }
            state.copy(
                formState = state.formState.copy(
                    description = description ?: state.formState.description,
                    valueInput = valueInput?.let(CurrencyFormats::formatInput) ?: state.formState.valueInput,
                    fieldErrors = updatedFieldErrors,
                    bannerMessage = null
                )
            )
        }
    }

    fun submitForm() {
        val mode = _uiState.value.formMode ?: return
        val form = _uiState.value.formState
        if (form.description.isBlank()) {
            setServiceFormErrors(
                fieldErrors = mapOf(
                    SERVICE_FIELD_DESCRIPTION to TransientMessage(
                        textResId = R.string.feedback_service_description_required,
                        tone = MessageTone.ERROR,
                        durationMillis = MessageDurations.SHORT_3S
                    )
                )
            )
            return
        }
        if (form.valueInput.isBlank()) {
            setServiceFormErrors(
                fieldErrors = mapOf(
                    SERVICE_FIELD_VALUE to TransientMessage(
                        textResId = R.string.feedback_service_value_required,
                        tone = MessageTone.ERROR,
                        durationMillis = MessageDurations.SHORT_3S
                    )
                )
            )
            return
        }

        val parsed = runCatching { CurrencyFormats.parseUiValue(form.valueInput) }.getOrNull()
        if (parsed == null || parsed <= java.math.BigDecimal.ZERO) {
            setServiceFormErrors(
                fieldErrors = mapOf(
                    SERVICE_FIELD_VALUE to TransientMessage(
                        textResId = R.string.feedback_service_value_positive_required,
                        tone = MessageTone.ERROR,
                        durationMillis = MessageDurations.SHORT_3S
                    )
                )
            )
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingForm = true) }
            val result = runCatching {
                when (mode) {
                    ServiceFormMode.CREATE -> repository.create(
                        description = form.description.trim(),
                        valueInput = form.valueInput
                    )
                    ServiceFormMode.EDIT -> repository.update(
                        id = form.id ?: 0L,
                        description = form.description.trim(),
                        valueInput = form.valueInput
                    )
                }
            }
            _uiState.update { it.copy(isSubmittingForm = false) }

            result.onSuccess {
                dismissForm()
                reload()
                showMessage(
                    textResId = if (mode == ServiceFormMode.CREATE) {
                        R.string.feedback_service_create_success
                    } else {
                        R.string.feedback_service_update_success
                    },
                    tone = MessageTone.SUCCESS,
                    duration = MessageDurations.SHORT_3S
                )
            }.onFailure {
                handleServiceSaveFailure(it)
            }
        }
    }

    fun deleteSelection() {
        val selected = _uiState.value.selectedIds.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            if (selected.size == 1) {
                when (repository.delete(selected.first())) {
                    DeleteServiceOutcome.DELETED -> {
                        reload()
                        showMessage(
                            textResId = R.string.feedback_service_delete_success,
                            tone = MessageTone.SUCCESS,
                            duration = MessageDurations.MEDIUM_5S
                        )
                    }
                    DeleteServiceOutcome.HAS_LINK -> {
                        showMessage(
                            textResId = R.string.feedback_service_delete_link_warning,
                            tone = MessageTone.WARNING,
                            duration = MessageDurations.LONG_8S
                        )
                    }
                    DeleteServiceOutcome.FAILED -> {
                        showMessage(
                            textResId = R.string.feedback_service_delete_error,
                            tone = MessageTone.ERROR,
                            duration = MessageDurations.SHORT_3S
                        )
                    }
                }
            } else {
                runCatching { repository.bulkDelete(selected) }
                    .onSuccess { (deleted, hasLink) ->
                        reload()
                        if (deleted > 0) {
                            showMessage(
                                textResId = R.string.feedback_service_bulk_delete_success,
                                tone = MessageTone.SUCCESS,
                                duration = MessageDurations.MEDIUM_5S,
                                textArgs = listOf(deleted.toString())
                            )
                        }
                        if (hasLink > 0) {
                            showMessage(
                                textResId = R.string.feedback_service_bulk_delete_warning,
                                tone = MessageTone.WARNING,
                                duration = MessageDurations.LONG_8S,
                                textArgs = listOf(hasLink.toString())
                            )
                        }
                    }
                    .onFailure {
                        showMessage(
                            textResId = R.string.feedback_service_bulk_delete_error,
                            tone = MessageTone.ERROR,
                            duration = MessageDurations.SHORT_3S
                        )
                    }
            }
        }
    }

    private fun reload() {
        loadPage(reset = true)
    }

    private fun loadPage(reset: Boolean) {
        val state = _uiState.value
        if (state.isLoading || state.isAppending) return

        viewModelScope.launch {
            if (reset) {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        pageIndex = 0,
                        totalPages = 1,
                        items = emptyList(),
                        selectedIds = emptySet()
                    )
                }
            } else {
                _uiState.update { it.copy(isAppending = true) }
            }

            val pageToLoad = if (reset) 1 else (_uiState.value.pageIndex + 1)
            runCatching {
                repository.list(
                    description = _uiState.value.filterDescription.trim().ifBlank { null },
                    uiPageIndex = pageToLoad,
                    pageSize = SERVICES_PAGE_SIZE
                )
            }.onSuccess { page ->
                _uiState.update { current ->
                    val merged = if (reset) {
                        page.items
                    } else {
                        current.items + page.items.filterNot { incoming ->
                            current.items.any { existing -> existing.id == incoming.id }
                        }
                    }
                    current.copy(
                        items = merged,
                        pageIndex = page.pageIndex,
                        totalPages = page.totalPages,
                        isLoading = false,
                        isAppending = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, isAppending = false) }
                showMessage(
                    textResId = R.string.feedback_service_load_error,
                    tone = MessageTone.ERROR,
                    duration = MessageDurations.SHORT_3S
                )
            }
        }
    }

    private fun scheduleFilterReload() {
        filterDebounceJob?.cancel()
        filterDebounceJob = viewModelScope.launch {
            delay(FILTER_DEBOUNCE_MS)
            reload()
        }
    }

    private fun showMessage(
        @StringRes textResId: Int,
        tone: MessageTone,
        duration: Long,
        textArgs: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val message = TransientMessage(
                textResId = textResId,
                textArgs = textArgs,
                tone = tone,
                durationMillis = duration
            )
            _uiState.update { it.copy(transientMessages = it.transientMessages + message) }
            delay(duration)
            _uiState.update { state ->
                state.copy(transientMessages = state.transientMessages.filterNot { it === message })
            }
        }
    }

    private fun setServiceFormErrors(
        fieldErrors: Map<String, TransientMessage> = emptyMap(),
        bannerMessage: TransientMessage? = null
    ) {
        _uiState.update { state ->
            state.copy(
                isSubmittingForm = false,
                formState = state.formState.copy(
                    fieldErrors = fieldErrors,
                    bannerMessage = bannerMessage
                )
            )
        }
    }

    private fun handleServiceSaveFailure(error: Throwable) {
        when (error) {
            is BackendApiException -> {
                val details = error.details
                if (details.fieldErrors.isNotEmpty()) {
                    setServiceFormErrors(
                        fieldErrors = details.fieldErrors.mapValues { (_, message) ->
                            TransientMessage(
                                text = normalizeServiceSaveMessage(message),
                                tone = MessageTone.ERROR,
                                durationMillis = MessageDurations.SHORT_3S
                            )
                        }
                    )
                } else {
                    setServiceFormErrors(
                        bannerMessage = TransientMessage(
                            text = normalizeServiceSaveMessage(details.message),
                            tone = MessageTone.ERROR,
                            durationMillis = MessageDurations.SHORT_3S
                        )
                    )
                }
            }
            else -> {
                setServiceFormErrors(
                    bannerMessage = TransientMessage(
                        textResId = R.string.feedback_service_save_error,
                        tone = MessageTone.ERROR,
                        durationMillis = MessageDurations.SHORT_3S
                    )
                )
            }
        }
    }

    private fun normalizeServiceSaveMessage(message: String?): String {
        val trimmedMessage = message?.trim().orEmpty()
        val currentDescription = _uiState.value.formState.description.trim()
        if (trimmedMessage.isBlank()) {
            return currentDescription.ifBlank { null }?.let { "$it já cadastrado" }
                ?: DEFAULT_DUPLICATE_SERVICE_MESSAGE
        }
        if (trimmedMessage.equals(LEGACY_DUPLICATE_SERVICE_MESSAGE, ignoreCase = true)) {
            return currentDescription.ifBlank { null }?.let { "$it já cadastrado" }
                ?: DEFAULT_DUPLICATE_SERVICE_MESSAGE
        }

        val duplicateMatch = DUPLICATE_SERVICE_MESSAGE_REGEX.matchEntire(trimmedMessage)
            ?: return trimmedMessage
        val description = duplicateMatch.groupValues.getOrNull(1)?.trim().orEmpty()
        return description.ifBlank { currentDescription }.ifBlank { DEFAULT_DUPLICATE_SERVICE_MESSAGE }
            .let { resolvedDescription ->
                if (resolvedDescription == DEFAULT_DUPLICATE_SERVICE_MESSAGE) {
                    resolvedDescription
                } else {
                    "$resolvedDescription já cadastrado"
                }
            }
    }
}
