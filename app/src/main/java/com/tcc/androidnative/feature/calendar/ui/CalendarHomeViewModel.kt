package com.tcc.androidnative.feature.calendar.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.feature.calendar.data.CalendarRepository
import com.tcc.androidnative.feature.calendar.data.CalendarSyncOutcome
import com.tcc.androidnative.feature.settings.data.CalendarSyncSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    val isRefreshing: Boolean = false,
    val items: List<CalendarAgendaItem> = emptyList(),
    val errorMessage: TransientMessage? = null,
    val syncWarningMessage: TransientMessage? = null,
    val isReauthRequired: Boolean = false
)

@HiltViewModel
class CalendarHomeViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val calendarSyncSettingsStore: CalendarSyncSettingsStore
) : ViewModel() {
    private val hourFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val zoneResolver: () -> ZoneId = { ZoneId.systemDefault() }
    private val selectedDateFlow = MutableStateFlow(LocalDate.now(ZoneOffset.UTC))
    private var requestCounter: Long = 0L
    private var latestRequestId: Long = 0L
    private var backgroundSyncJob: Job? = null
    private var lastBackgroundSyncAt: Instant? = null

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

    fun onDateSelected(selectedDate: LocalDate) {
        selectedDateFlow.update { selectedDate }
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
        val renderStart = System.nanoTime()
        val shouldBlockWithLoading = _uiState.value.items.isEmpty()
        updateStateIfLatest(requestId) {
            it.copy(
                isLoading = shouldBlockWithLoading,
                selectedDate = selectedDate,
                errorMessage = null,
                isReauthRequired = it.isReauthRequired
            )
        }
        logInfo("calendar_pipeline_start requestId=$requestId date=$selectedDate")

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
                    errorMessage = null
                )
            }
            logInfo(
                "calendar_pipeline_success requestId=$requestId date=$selectedDate items=${items.size} render_ms=${elapsedMs(renderStart)}"
            )
            launchBackgroundSyncIfDue()
        } catch (error: CancellationException) {
            logInfo("calendar_pipeline_cancelled requestId=$requestId date=$selectedDate")
            throw error
        } catch (error: Throwable) {
            updateStateIfLatest(requestId) {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    items = emptyList(),
                    selectedDate = selectedDate,
                    errorMessage = TransientMessage(
                        textResId = R.string.feedback_calendar_load_error,
                        tone = MessageTone.ERROR,
                        durationMillis = 0L
                    ),
                    syncWarningMessage = null,
                    isReauthRequired = false
                )
            }
            logError(
                "calendar_pipeline_list_error requestId=$requestId date=$selectedDate message=${error.message}",
                error
            )
        }
    }

    private fun launchBackgroundSyncIfDue() {
        if (!shouldTriggerBackgroundSync()) {
            logInfo("calendar_background_sync_skipped reason=freshness_or_running")
            return
        }
        backgroundSyncJob = viewModelScope.launch {
            runBackgroundSync()
        }
    }

    private fun shouldTriggerBackgroundSync(): Boolean {
        if (backgroundSyncJob?.isActive == true) {
            return false
        }
        val lastRun = lastBackgroundSyncAt ?: return true
        return Duration.between(lastRun, Instant.now()) >= BACKGROUND_SYNC_FRESHNESS_WINDOW
    }

    private suspend fun runBackgroundSync() {
        val syncStart = System.nanoTime()
        lastBackgroundSyncAt = Instant.now()
        _uiState.update { state ->
            state.copy(isRefreshing = true)
        }
        logInfo("calendar_background_sync_start")

        val settings = calendarSyncSettingsStore.getSettings()
        val syncStartDate = if (settings.useStartDateFilter) settings.startDate else null
        val syncOutcome = calendarRepository.sync(startDate = syncStartDate)
        val (syncWarningMessage, reauthRequired) = resolveSyncFeedback(syncOutcome)
        val shouldReloadSelectedDate = syncOutcome is CalendarSyncOutcome.Success && hasDelta(syncOutcome.result)
        logInfo(
            "calendar_background_sync_result outcome=${syncOutcome::class.simpleName} reload_selected_date=$shouldReloadSelectedDate sync_ms=${elapsedMs(syncStart)}"
        )

        if (shouldReloadSelectedDate) {
            reloadSelectedDateAfterSyncDelta(syncWarningMessage, reauthRequired)
            return
        }

        _uiState.update { state ->
            state.copy(
                isRefreshing = false,
                syncWarningMessage = syncWarningMessage,
                isReauthRequired = reauthRequired
            )
        }
    }

    private suspend fun reloadSelectedDateAfterSyncDelta(
        syncWarningMessage: TransientMessage?,
        reauthRequired: Boolean
    ) {
        val selectedDate = selectedDateFlow.value
        val requestId = nextRequestId()
        latestRequestId = requestId
        val reloadStart = System.nanoTime()

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
                    isRefreshing = false,
                    items = items,
                    selectedDate = selectedDate,
                    errorMessage = null,
                    syncWarningMessage = syncWarningMessage,
                    isReauthRequired = reauthRequired
                )
            }
            logInfo(
                "calendar_background_sync_reloaded_selected_date requestId=$requestId date=$selectedDate items=${items.size} reload_ms=${elapsedMs(reloadStart)}"
            )
        } catch (error: CancellationException) {
            logInfo("calendar_background_sync_reload_cancelled requestId=$requestId date=$selectedDate")
            throw error
        } catch (error: Throwable) {
            _uiState.update { state ->
                state.copy(
                    isRefreshing = false,
                    syncWarningMessage = syncWarningMessage,
                    isReauthRequired = reauthRequired
                )
            }
            logError(
                "calendar_background_sync_reload_error requestId=$requestId date=$selectedDate message=${error.message}",
                error
            )
        }
    }

    private fun hasDelta(result: com.tcc.androidnative.feature.calendar.data.CalendarSyncResult): Boolean {
        return result.created > 0 || result.updated > 0 || result.deleted > 0
    }

    private suspend fun resolveSyncFeedback(syncOutcome: CalendarSyncOutcome): Pair<TransientMessage?, Boolean> {
        return when (syncOutcome) {
            is CalendarSyncOutcome.Success -> null to false
            is CalendarSyncOutcome.ReauthRequired -> reauthRequiredMessage() to true
            is CalendarSyncOutcome.RecoverableFailure -> {
                val reauthStatus = runCatching { calendarRepository.integrationStatus() }
                    .getOrNull()
                    ?.isReauthRequired()
                    ?: false

                if (reauthStatus) {
                    reauthRequiredMessage() to true
                } else {
                    buildSyncWarning(syncOutcome) to false
                }
            }
        }
    }

    private fun reauthRequiredMessage(): TransientMessage {
        return TransientMessage(
            textResId = R.string.feedback_calendar_reauth_required,
            tone = MessageTone.WARNING,
            durationMillis = 0L
        )
    }

    private fun buildSyncWarning(syncFailure: CalendarSyncOutcome.RecoverableFailure): TransientMessage {
        val statusText = syncFailure.httpStatus?.toString() ?: "sem status HTTP"
        val codeText = syncFailure.backendCode ?: "SEM_CODIGO"
        val messageText = syncFailure.backendMessage
            ?.takeIf { it.isNotBlank() }
            ?: "Falha ao atualizar dados no Google."
        return TransientMessage(
            textResId = R.string.feedback_calendar_sync_warning,
            textArgs = listOf(statusText, codeText, messageText),
            tone = MessageTone.WARNING,
            durationMillis = 0L
        )
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

    private fun elapsedMs(startNs: Long): Long {
        return (System.nanoTime() - startNs) / 1_000_000L
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
        val BACKGROUND_SYNC_FRESHNESS_WINDOW: Duration = Duration.ofMinutes(1)
    }
}
