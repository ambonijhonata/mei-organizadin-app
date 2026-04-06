package com.tcc.androidnative.feature.clients.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.core.util.CpfValidator
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.clients.data.ClientModel
import com.tcc.androidnative.feature.clients.data.ClientsRepository
import com.tcc.androidnative.feature.clients.data.DeleteClientOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CLIENTS_PAGE_SIZE = 25

enum class ClientFormMode {
    CREATE,
    EDIT
}

data class ClientFormState(
    val id: Long? = null,
    val name: String = "",
    val cpf: String = "",
    val birthDate: String = "",
    val email: String = "",
    val phone: String = ""
)

data class ClientsUiState(
    val items: List<ClientModel> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val filterName: String = "",
    val pageIndex: Int = 0,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isAppending: Boolean = false,
    val isSubmittingForm: Boolean = false,
    val formMode: ClientFormMode? = null,
    val formState: ClientFormState = ClientFormState(),
    val transientMessage: TransientMessage? = null
)

@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val repository: ClientsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun onFilterNameChange(value: String) {
        _uiState.update { it.copy(filterName = value) }
    }

    fun applyFilter() {
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
                formMode = ClientFormMode.CREATE,
                formState = ClientFormState()
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
                            formMode = ClientFormMode.EDIT,
                            formState = ClientFormState(
                                id = model.id,
                                name = model.name,
                                cpf = model.cpf.orEmpty(),
                                birthDate = model.dateOfBirth?.let(DateFormats::toUiDate).orEmpty(),
                                email = model.email.orEmpty(),
                                phone = model.phone.orEmpty()
                            )
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state -> state.copy(isSubmittingForm = false) }
                    showMessage(
                        textResId = R.string.feedback_client_edit_load_error,
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
                formState = ClientFormState(),
                isSubmittingForm = false
            )
        }
    }

    fun updateForm(
        name: String? = null,
        cpf: String? = null,
        birthDate: String? = null,
        email: String? = null,
        phone: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                formState = state.formState.copy(
                    name = name ?: state.formState.name,
                    cpf = cpf ?: state.formState.cpf,
                    birthDate = birthDate ?: state.formState.birthDate,
                    email = email ?: state.formState.email,
                    phone = phone ?: state.formState.phone
                )
            )
        }
    }

    fun submitForm() {
        val mode = _uiState.value.formMode ?: return
        val form = _uiState.value.formState

        if (form.name.isBlank()) {
            showMessage(R.string.feedback_client_name_required, MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }
        if (form.cpf.isNotBlank() && !CpfValidator.isValid(form.cpf)) {
            showMessage(R.string.feedback_client_cpf_invalid, MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        val parsedBirthDate = if (form.birthDate.isBlank()) {
            null
        } else {
            runCatching { DateFormats.parseUiDate(form.birthDate) }.getOrNull()
        }
        if (form.birthDate.isNotBlank() && parsedBirthDate == null) {
            showMessage(R.string.feedback_client_birth_date_invalid, MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingForm = true) }
            val payload = ClientModel(
                id = form.id ?: 0L,
                name = form.name.trim(),
                cpf = form.cpf.trim().ifBlank { null },
                dateOfBirth = parsedBirthDate,
                email = form.email.trim().ifBlank { null },
                phone = form.phone.trim().ifBlank { null }
            )
            val result = runCatching {
                when (mode) {
                    ClientFormMode.CREATE -> repository.create(payload)
                    ClientFormMode.EDIT -> repository.update(form.id ?: 0L, payload)
                }
            }
            _uiState.update { it.copy(isSubmittingForm = false) }

            result.onSuccess {
                dismissForm()
                reload()
                showMessage(
                    textResId = if (mode == ClientFormMode.CREATE) {
                        R.string.feedback_client_create_success
                    } else {
                        R.string.feedback_client_update_success
                    },
                    tone = MessageTone.SUCCESS,
                    duration = MessageDurations.SHORT_3S
                )
            }.onFailure {
                showMessage(
                    textResId = R.string.feedback_client_save_error,
                    tone = MessageTone.ERROR,
                    duration = MessageDurations.SHORT_3S
                )
            }
        }
    }

    fun deleteSelection() {
        val selected = _uiState.value.selectedIds.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            if (selected.size == 1) {
                when (repository.delete(selected.first())) {
                    DeleteClientOutcome.DELETED -> {
                        reload()
                        showMessage(
                            textResId = R.string.feedback_client_delete_success,
                            tone = MessageTone.SUCCESS,
                            duration = MessageDurations.SHORT_3S
                        )
                    }
                    DeleteClientOutcome.HAS_LINK -> {
                        showMessage(
                            textResId = R.string.feedback_client_delete_link_warning,
                            tone = MessageTone.WARNING,
                            duration = MessageDurations.SHORT_3S
                        )
                    }
                    DeleteClientOutcome.FAILED -> {
                        showMessage(
                            textResId = R.string.feedback_client_delete_error,
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
                                textResId = R.string.feedback_client_bulk_delete_success,
                                tone = MessageTone.SUCCESS,
                                duration = MessageDurations.SHORT_3S,
                                textArgs = listOf(deleted.toString())
                            )
                        }
                        if (hasLink > 0) {
                            showMessage(
                                textResId = R.string.feedback_client_bulk_delete_warning,
                                tone = MessageTone.WARNING,
                                duration = MessageDurations.SHORT_3S,
                                textArgs = listOf(hasLink.toString())
                            )
                        }
                    }
                    .onFailure {
                        showMessage(
                            textResId = R.string.feedback_client_bulk_delete_error,
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
                    name = _uiState.value.filterName.trim().ifBlank { null },
                    pageIndex = pageToLoad,
                    itemsPerPage = CLIENTS_PAGE_SIZE
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
                    textResId = R.string.feedback_client_load_error,
                    tone = MessageTone.ERROR,
                    duration = MessageDurations.SHORT_3S
                )
            }
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
            _uiState.update { it.copy(transientMessage = message) }
            delay(duration)
            _uiState.update { state ->
                if (state.transientMessage == message) state.copy(transientMessage = null) else state
            }
        }
    }
}
