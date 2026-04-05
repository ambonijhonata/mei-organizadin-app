package com.tcc.androidnative.feature.services.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.core.util.CurrencyFormats
import com.tcc.androidnative.feature.services.data.ServiceModel
import com.tcc.androidnative.feature.services.data.ServicesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SERVICES_PAGE_SIZE = 25

enum class ServiceFormMode {
    CREATE,
    EDIT
}

data class ServiceFormState(
    val id: Long? = null,
    val description: String = "",
    val valueInput: String = ""
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
    val transientMessage: TransientMessage? = null
)

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val repository: ServicesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServicesUiState())
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun onFilterDescriptionChange(value: String) {
        _uiState.update { it.copy(filterDescription = value) }
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
                                valueInput = model.value.toPlainString()
                            )
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state -> state.copy(isSubmittingForm = false) }
                    showMessage(
                        text = "Erro ao carregar servico para edicao",
                        tone = MessageTone.ERROR,
                        duration = MessageDurations.MEDIUM_5S
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
            state.copy(
                formState = state.formState.copy(
                    description = description ?: state.formState.description,
                    valueInput = valueInput ?: state.formState.valueInput
                )
            )
        }
    }

    fun submitForm() {
        val mode = _uiState.value.formMode ?: return
        val form = _uiState.value.formState
        if (form.description.isBlank()) {
            showMessage("Descricao obrigatoria", MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }
        if (form.valueInput.isBlank()) {
            showMessage("Valor obrigatorio", MessageTone.ERROR, MessageDurations.SHORT_3S)
            return
        }

        val parsed = runCatching { CurrencyFormats.parseUiValue(form.valueInput) }.getOrNull()
        if (parsed == null || parsed <= java.math.BigDecimal.ZERO) {
            showMessage("Valor deve ser maior que zero", MessageTone.ERROR, MessageDurations.SHORT_3S)
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
                    text = if (mode == ServiceFormMode.CREATE) "Servico cadastrado com sucesso" else "Servico atualizado com sucesso",
                    tone = MessageTone.SUCCESS,
                    duration = MessageDurations.SHORT_3S
                )
            }.onFailure {
                showMessage(
                    text = "Erro ao salvar servico",
                    tone = MessageTone.ERROR,
                    duration = MessageDurations.MEDIUM_5S
                )
            }
        }
    }

    fun deleteSelection() {
        val selected = _uiState.value.selectedIds.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            if (selected.size == 1) {
                runCatching { repository.delete(selected.first()) }
                    .onSuccess {
                        reload()
                        showMessage(
                            text = "Servico excluido com sucesso",
                            tone = MessageTone.SUCCESS,
                            duration = MessageDurations.MEDIUM_5S
                        )
                    }
                    .onFailure {
                        showMessage(
                            text = "Erro ao excluir servico",
                            tone = MessageTone.ERROR,
                            duration = MessageDurations.MEDIUM_5S
                        )
                    }
            } else {
                runCatching { repository.bulkDelete(selected) }
                    .onSuccess { (deleted, hasLink) ->
                        reload()
                        if (deleted > 0) {
                            showMessage(
                                text = "$deleted servicos excluidos com sucesso",
                                tone = MessageTone.SUCCESS,
                                duration = MessageDurations.MEDIUM_5S
                            )
                        }
                        if (hasLink > 0) {
                            showMessage(
                                text = "$hasLink servicos vinculados nao foram excluidos",
                                tone = MessageTone.WARNING,
                                duration = MessageDurations.LONG_8S
                            )
                        }
                    }
                    .onFailure {
                        showMessage(
                            text = "Erro ao excluir servicos em lote",
                            tone = MessageTone.ERROR,
                            duration = MessageDurations.MEDIUM_5S
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
                    text = "Erro ao carregar servicos",
                    tone = MessageTone.ERROR,
                    duration = MessageDurations.MEDIUM_5S
                )
            }
        }
    }

    private fun showMessage(text: String, tone: MessageTone, duration: Long) {
        viewModelScope.launch {
            val message = TransientMessage(text = text, tone = tone, durationMillis = duration)
            _uiState.update { it.copy(transientMessage = message) }
            delay(duration)
            _uiState.update { state ->
                if (state.transientMessage == message) state.copy(transientMessage = null) else state
            }
        }
    }
}

