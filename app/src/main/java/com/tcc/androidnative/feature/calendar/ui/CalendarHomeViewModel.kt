package com.tcc.androidnative.feature.calendar.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.feature.calendar.data.CalendarRepository
import com.tcc.androidnative.feature.calendar.data.CalendarSyncOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarAgendaItem(
    val eventId: Long,
    val eventStart: Instant,
    val eventEnd: Instant?,
    val slotLabel: String,
    val timeLabel: String,
    val title: String,
    val serviceDescription: String?,
    val durationLabel: String
)

data class CalendarHomeUiState(
    val selectedDate: LocalDate = LocalDate.now(ZoneOffset.UTC),
    val isLoading: Boolean = false,
    val items: List<CalendarAgendaItem> = emptyList(),
    val errorMessage: String? = null,
    val syncWarningMessage: String? = null,
    val isReauthRequired: Boolean = false
)

@HiltViewModel
class CalendarHomeViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository
) : ViewModel() {
    private val hourFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val zoneResolver: () -> ZoneId = { ZoneId.systemDefault() }
    private val selectedDateFlow = MutableStateFlow(LocalDate.now(ZoneOffset.UTC))
    private var requestCounter: Long = 0L
    private var latestRequestId: Long = 0L

    private val _uiState = MutableStateFlow(CalendarHomeUiState())
    val uiState: StateFlow<CalendarHomeUiState> = _uiState.asStateFlow()

    init {
        observeSelectedDate()
    }

    fun onPreviousDay() {
        selectedDateFlow.update { current -> current.minusDays(1) }
    }

    fun onNextDay() {
        selectedDateFlow.update { current -> current.plusDays(1) }
    }

    private fun observeSelectedDate() {
        viewModelScope.launch {
            selectedDateFlow.collectLatest { selectedDate ->
                val requestId = nextRequestId()
                latestRequestId = requestId
                loadEventsForSelectedDate(requestId = requestId, selectedDate = selectedDate)
            }
        }
    }

    private suspend fun loadEventsForSelectedDate(requestId: Long, selectedDate: LocalDate) {
        updateStateIfLatest(requestId) {
            it.copy(
                isLoading = true,
                selectedDate = selectedDate,
                errorMessage = null,
                syncWarningMessage = null,
                isReauthRequired = false
            )
        }
        logInfo("calendar_pipeline_start requestId=$requestId date=$selectedDate")

        val syncOutcome = calendarRepository.sync()
        val (syncWarningMessage, reauthRequired) = resolveSyncFeedback(syncOutcome)
        logInfo(
            "calendar_pipeline_sync_result requestId=$requestId date=$selectedDate outcome=${syncOutcome::class.simpleName}"
        )

        try {
            val items = calendarRepository.eventsByDay(selectedDate)
                    .sortedBy { it.eventStart }
                    .map { event ->
                        CalendarAgendaItem(
                            eventId = event.id,
                            eventStart = event.eventStart,
                            eventEnd = event.eventEnd,
                            slotLabel = formatSlotLabel(event.eventStart),
                            timeLabel = formatEventTimeLabel(event.eventStart),
                            title = event.title,
                            serviceDescription = event.serviceDescription,
                            durationLabel = formatDurationLabel(event.eventStart, event.eventEnd)
                        )
                    }
            updateStateIfLatest(requestId) {
                it.copy(
                    isLoading = false,
                    items = items,
                    selectedDate = selectedDate,
                    errorMessage = null,
                    syncWarningMessage = syncWarningMessage,
                    isReauthRequired = reauthRequired
                )
            }
            logInfo("calendar_pipeline_success requestId=$requestId date=$selectedDate items=${items.size}")
        } catch (error: CancellationException) {
            logInfo("calendar_pipeline_cancelled requestId=$requestId date=$selectedDate")
            throw error
        } catch (error: Throwable) {
            updateStateIfLatest(requestId) {
                it.copy(
                    isLoading = false,
                    items = emptyList(),
                    selectedDate = selectedDate,
                    errorMessage = "Erro ao carregar agenda",
                    syncWarningMessage = null,
                    isReauthRequired = reauthRequired
                )
            }
            logError(
                "calendar_pipeline_list_error requestId=$requestId date=$selectedDate message=${error.message}",
                error
            )
        }
    }

    private suspend fun resolveSyncFeedback(syncOutcome: CalendarSyncOutcome): Pair<String?, Boolean> {
        return when (syncOutcome) {
            is CalendarSyncOutcome.Success -> null to false
            is CalendarSyncOutcome.ReauthRequired -> REAUTH_REQUIRED_MESSAGE to true
            is CalendarSyncOutcome.RecoverableFailure -> {
                val reauthStatus = runCatching { calendarRepository.integrationStatus() }
                    .getOrNull()
                    ?.isReauthRequired()
                    ?: false

                if (reauthStatus) {
                    REAUTH_REQUIRED_MESSAGE to true
                } else {
                    buildSyncWarning(syncOutcome) to false
                }
            }
        }
    }

    private fun buildSyncWarning(syncFailure: CalendarSyncOutcome.RecoverableFailure): String {
        val statusText = syncFailure.httpStatus?.toString() ?: "sem status HTTP"
        val codeText = syncFailure.backendCode ?: "SEM_CODIGO"
        val messageText = syncFailure.backendMessage
            ?.takeIf { it.isNotBlank() }
            ?: "Falha ao atualizar dados no Google."
        return "Sincronizacao parcial indisponivel ($statusText/$codeText): $messageText Exibindo dados atuais."
    }

    private fun updateStateIfLatest(
        requestId: Long,
        reducer: (CalendarHomeUiState) -> CalendarHomeUiState
    ) {
        if (requestId != latestRequestId) {
            logInfo("calendar_pipeline_drop_stale requestId=$requestId latest=$latestRequestId")
            return
        }
        _uiState.update(reducer)
    }

    private fun nextRequestId(): Long {
        requestCounter += 1L
        return requestCounter
    }

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    private fun logError(message: String, error: Throwable) {
        runCatching { Log.e(TAG, message, error) }
    }

    internal fun formatEventTimeLabel(
        eventStart: Instant,
        zoneResolver: () -> ZoneId = this.zoneResolver
    ): String {
        val zone = runCatching { zoneResolver() }.getOrElse { ZoneOffset.UTC }
        return eventStart.atZone(zone).format(hourFormatter)
    }

    internal fun formatSlotLabel(
        eventStart: Instant,
        zoneResolver: () -> ZoneId = this.zoneResolver
    ): String {
        val zone = runCatching { zoneResolver() }.getOrElse { ZoneOffset.UTC }
        val zoned = eventStart.atZone(zone)
        val slotMinute = if (zoned.minute < 30) 0 else 30
        return zoned
            .withMinute(slotMinute)
            .withSecond(0)
            .withNano(0)
            .format(hourFormatter)
    }

    internal fun formatDurationLabel(eventStart: Instant, eventEnd: Instant?): String {
        if (eventEnd == null || !eventEnd.isAfter(eventStart)) {
            return UNKNOWN_DURATION_LABEL
        }

        val totalMinutes = Duration.between(eventStart, eventEnd).toMinutes()
        if (totalMinutes < 60) {
            return "$totalMinutes min"
        }

        val hours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60
        if (remainingMinutes == 0L) {
            return if (hours == 1L) "1 hora" else "$hours horas"
        }

        return "${hours}h ${remainingMinutes}min"
    }

    private companion object {
        const val TAG = "CalendarHomeViewModel"
        const val UNKNOWN_DURATION_LABEL = "Duracao nao informada"
        const val REAUTH_REQUIRED_MESSAGE =
            "Integracao Google revogada. Reautentique para voltar a sincronizar."
    }
}
