package com.tcc.androidnative.feature.calendar.ui

import com.tcc.androidnative.R
import com.tcc.androidnative.feature.calendar.data.CalendarEventModel
import com.tcc.androidnative.feature.calendar.data.CalendarIntegrationStatus
import com.tcc.androidnative.feature.calendar.data.CalendarRepository
import com.tcc.androidnative.feature.calendar.data.CalendarSyncOutcome
import com.tcc.androidnative.feature.calendar.data.CalendarSyncResult
import com.tcc.androidnative.testutil.MainDispatcherRule
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarHomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init should load current day before background sync`() = runTest {
        val fakeRepo = FakeCalendarRepository()
        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        assertTrue(fakeRepo.callTrace.isNotEmpty())
        assertTrue(fakeRepo.callTrace.first().startsWith("events:"))
        assertEquals(1, fakeRepo.syncCalls)
        assertEquals(1, fakeRepo.requestedDates.size)
        assertFalse(viewModel.uiState.value.items.isEmpty())
        assertEquals(fakeRepo.requestedDates.first(), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `date navigation should request previous and next day`() = runTest {
        val fakeRepo = FakeCalendarRepository(
            syncOutcomes = mutableListOf(
                CalendarSyncOutcome.Success(CalendarSyncResult(1, 0, 0)),
                CalendarSyncOutcome.Success(CalendarSyncResult(1, 0, 0)),
                CalendarSyncOutcome.Success(CalendarSyncResult(1, 0, 0))
            )
        )
        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        val initialDate = fakeRepo.requestedDates.last()
        viewModel.onPreviousDay()
        advanceUntilIdle()
        val previousDate = fakeRepo.requestedDates.last()
        assertEquals(initialDate.minusDays(1), previousDate)

        viewModel.onNextDay()
        advanceUntilIdle()
        val nextDate = fakeRepo.requestedDates.last()
        assertEquals(initialDate, nextDate)
        assertEquals(1, fakeRepo.syncCalls)
    }

    @Test
    fun `recoverable sync failure should not block day list`() = runTest {
        val fakeRepo = FakeCalendarRepository(
            syncOutcomes = mutableListOf(
                CalendarSyncOutcome.RecoverableFailure(
                    httpStatus = 503,
                    backendCode = "TEMPORARY_ERROR",
                    backendMessage = "temporarily unavailable"
                )
            )
        )
        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.items.isEmpty())
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(R.string.feedback_calendar_sync_warning, viewModel.uiState.value.syncWarningMessage?.textResId)
        assertFalse(viewModel.uiState.value.isReauthRequired)
    }

    @Test
    fun `reauth outcome should expose dedicated state and preserve items`() = runTest {
        val fakeRepo = FakeCalendarRepository(
            syncOutcomes = mutableListOf(CalendarSyncOutcome.ReauthRequired)
        )
        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isReauthRequired)
        assertFalse(viewModel.uiState.value.items.isEmpty())
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(R.string.feedback_calendar_reauth_required, viewModel.uiState.value.syncWarningMessage?.textResId)
    }

    @Test
    fun `recoverable 403 and REAUTH_REQUIRED status should become reauth state`() = runTest {
        val fakeRepo = FakeCalendarRepository(
            syncOutcomes = mutableListOf(
                CalendarSyncOutcome.RecoverableFailure(
                    httpStatus = 403,
                    backendCode = "UNCLASSIFIED",
                    backendMessage = "forbidden"
                )
            ),
            integrationStatusResult = CalendarIntegrationStatus(
                status = "REAUTH_REQUIRED",
                lastSyncAt = null,
                errorCategory = "REVOKED",
                errorMessage = "revoked"
            )
        )
        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isReauthRequired)
        assertEquals(R.string.feedback_calendar_reauth_required, viewModel.uiState.value.syncWarningMessage?.textResId)
    }

    @Test
    fun `when day listing fails should expose blocking load error`() = runTest {
        val today = LocalDate.now(ZoneOffset.UTC)
        val fakeRepo = FakeCalendarRepository(
            eventErrorsByDate = mutableMapOf(today to IllegalStateException("events error"))
        )
        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        assertEquals(R.string.feedback_calendar_load_error, viewModel.uiState.value.errorMessage?.textResId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `date selected directly should trigger day reload pipeline`() = runTest {
        val fakeRepo = FakeCalendarRepository(
            syncOutcomes = mutableListOf(
                CalendarSyncOutcome.Success(CalendarSyncResult(0, 0, 0))
            )
        )
        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        val targetDate = LocalDate.of(2026, 4, 5)
        viewModel.onDateSelected(targetDate)
        advanceUntilIdle()

        assertEquals(targetDate, fakeRepo.requestedDates.last())
        assertEquals(targetDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `background sync with delta should reload selected day`() = runTest {
        val today = LocalDate.now(ZoneOffset.UTC)
        val fakeRepo = FakeCalendarRepository(
            syncOutcomes = mutableListOf(
                CalendarSyncOutcome.Success(CalendarSyncResult(created = 2, updated = 1, deleted = 0))
            )
        )

        CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        val todayRequests = fakeRepo.requestedDates.count { it == today }
        assertTrue(todayRequests >= 2)
    }

    @Test
    fun `agenda should remain ordered by event instant`() = runTest {
        val today = LocalDate.now(ZoneOffset.UTC)
        val fakeRepo = FakeCalendarRepository(
            eventsByDate = mutableMapOf(
                today to listOf(
                    CalendarEventModel(
                        id = 2L,
                        title = "Evento-tarde",
                        eventStart = Instant.parse("2026-04-04T16:00:00Z"),
                        eventEnd = null,
                        identified = true,
                        serviceDescription = "Servico tarde",
                        serviceValue = BigDecimal("70.00")
                    ),
                    CalendarEventModel(
                        id = 1L,
                        title = "Evento-manha",
                        eventStart = Instant.parse("2026-04-04T13:00:00Z"),
                        eventEnd = null,
                        identified = true,
                        serviceDescription = "Servico manha",
                        serviceValue = BigDecimal("50.00")
                    )
                )
            )
        )
        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        assertEquals(listOf("Evento-manha", "Evento-tarde"), viewModel.uiState.value.items.map { it.title })
    }

    @Test
    fun `mapped item should include slot label and duration`() = runTest {
        val today = LocalDate.now(ZoneOffset.UTC)
        val fakeRepo = FakeCalendarRepository(
            eventsByDate = mutableMapOf(
                today to listOf(
                    CalendarEventModel(
                        id = 10L,
                        title = "Evento-completo",
                        eventStart = Instant.parse("2026-04-04T13:45:00Z"),
                        eventEnd = Instant.parse("2026-04-04T14:45:00Z"),
                        identified = true,
                        serviceDescription = "Servico teste",
                        serviceValue = BigDecimal("99.90")
                    )
                )
            )
        )

        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        val item = viewModel.uiState.value.items.single()
        assertTrue(item.timeLabel.endsWith(":45"))
        assertTrue(item.slotLabel.endsWith(":30"))
        assertEquals("1 hora", item.durationLabel)
        assertEquals("Servico teste", item.serviceDescription)
    }

    @Test
    fun `time label should use GMT-3 when user timezone is minus three`() = runTest {
        val viewModel = CalendarHomeViewModel(FakeCalendarRepository())

        val label = viewModel.formatEventTimeLabel(Instant.parse("2026-04-04T13:00:00Z")) {
            ZoneOffset.ofHours(-3)
        }

        assertEquals("10:00", label)
    }

    @Test
    fun `time label should use UTC when user timezone is UTC`() = runTest {
        val viewModel = CalendarHomeViewModel(FakeCalendarRepository())

        val label = viewModel.formatEventTimeLabel(Instant.parse("2026-04-04T13:00:00Z")) {
            ZoneOffset.UTC
        }

        assertEquals("13:00", label)
    }

    @Test
    fun `time label should fallback to UTC when timezone resolution fails`() = runTest {
        val viewModel = CalendarHomeViewModel(FakeCalendarRepository())

        val label = viewModel.formatEventTimeLabel(Instant.parse("2026-04-04T13:00:00Z")) {
            throw IllegalStateException("timezone unavailable")
        }

        assertEquals("13:00", label)
    }

    @Test
    fun `duration label should fallback when end instant is missing`() = runTest {
        val viewModel = CalendarHomeViewModel(FakeCalendarRepository())

        val duration = viewModel.formatDurationLabel(
            eventStart = Instant.parse("2026-04-04T13:00:00Z"),
            eventEnd = null
        )

        assertEquals("Duracao nao informada", duration)
    }

    @Test
    fun `rapid day changes should keep latest selected day result`() = runTest {
        val today = LocalDate.now(ZoneOffset.UTC)
        val previousDay = today.minusDays(1)
        val fakeRepo = FakeCalendarRepository(
            syncOutcomes = mutableListOf(
                CalendarSyncOutcome.Success(CalendarSyncResult(0, 0, 0))
            ),
            eventDelayByDateMs = mutableMapOf(previousDay to 1_000L)
        )
        val viewModel = CalendarHomeViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onPreviousDay()
        viewModel.onNextDay()
        advanceUntilIdle()

        assertEquals(today, fakeRepo.requestedDates.last())
        assertTrue(viewModel.uiState.value.items.first().title.contains(today.toString()))
        assertTrue(fakeRepo.syncCalls >= 1)
    }

    @Test
    fun `high volume navigation should not wait for slow background sync`() = runTest {
        val today = LocalDate.now(ZoneOffset.UTC)
        val nextDay = today.plusDays(1)
        val highVolumeEvents = (0 until 14_354).map { index ->
            CalendarEventModel(
                id = index.toLong() + 1L,
                title = "Evento-$index",
                eventStart = today.atTime(9, 0).plusMinutes(index.toLong()).toInstant(ZoneOffset.UTC),
                eventEnd = null,
                identified = true,
                serviceDescription = "Servico",
                serviceValue = BigDecimal("10.00")
            )
        }
        val fakeRepo = FakeCalendarRepository(
            eventsByDate = mutableMapOf(
                today to highVolumeEvents,
                nextDay to listOf(
                    CalendarEventModel(
                        id = 99_999L,
                        title = "Evento-$nextDay",
                        eventStart = nextDay.atTime(10, 0).toInstant(ZoneOffset.UTC),
                        eventEnd = null,
                        identified = true,
                        serviceDescription = "Servico",
                        serviceValue = BigDecimal("50.00")
                    )
                )
            ),
            syncDelayMs = 120_000L
        )
        val viewModel = CalendarHomeViewModel(fakeRepo)

        runCurrent()
        assertTrue(viewModel.uiState.value.items.isNotEmpty())
        viewModel.onNextDay()

        runCurrent()
        assertTrue(fakeRepo.requestedDates.contains(nextDay))
        assertEquals(nextDay, viewModel.uiState.value.selectedDate)

        // Ensure we are still inside slow sync window and navigation result was not blocked.
        advanceTimeBy(1_000L)
        runCurrent()
        assertTrue(fakeRepo.syncCalls >= 1)
        assertTrue(viewModel.uiState.value.items.any { it.title.contains(nextDay.toString()) })
    }
}

private class FakeCalendarRepository(
    private val syncOutcomes: MutableList<CalendarSyncOutcome> = mutableListOf(
        CalendarSyncOutcome.Success(CalendarSyncResult(created = 0, updated = 0, deleted = 0))
    ),
    private val eventsByDate: MutableMap<LocalDate, List<CalendarEventModel>> = mutableMapOf(),
    private val eventErrorsByDate: MutableMap<LocalDate, Throwable> = mutableMapOf(),
    private val eventDelayByDateMs: MutableMap<LocalDate, Long> = mutableMapOf(),
    private val syncDelayMs: Long = 0L,
    private val integrationStatusResult: CalendarIntegrationStatus = CalendarIntegrationStatus(
        status = "SYNCED",
        lastSyncAt = null,
        errorCategory = null,
        errorMessage = null
    )
) : CalendarRepository {
    var syncCalls: Int = 0
    val requestedDates: MutableList<LocalDate> = mutableListOf()
    val callTrace: MutableList<String> = mutableListOf()

    override suspend fun sync(): CalendarSyncOutcome {
        syncCalls += 1
        callTrace += "sync"
        if (syncDelayMs > 0L) {
            delay(syncDelayMs)
        }
        return if (syncOutcomes.isEmpty()) {
            CalendarSyncOutcome.Success(CalendarSyncResult(created = 0, updated = 0, deleted = 0))
        } else {
            syncOutcomes.removeAt(0)
        }
    }

    override suspend fun integrationStatus(): CalendarIntegrationStatus {
        return integrationStatusResult
    }

    override suspend fun eventsByDay(date: LocalDate): List<CalendarEventModel> {
        requestedDates += date
        callTrace += "events:$date"
        eventDelayByDateMs[date]?.let { delay(it) }
        eventErrorsByDate[date]?.let { throw it }
        return eventsByDate[date] ?: listOf(
            CalendarEventModel(
                id = 1L,
                title = "Evento-$date",
                eventStart = date.atTime(10, 0).toInstant(ZoneOffset.UTC),
                eventEnd = Instant.parse("2026-01-01T10:30:00Z"),
                identified = true,
                serviceDescription = "Corte",
                serviceValue = BigDecimal("50.00")
            )
        )
    }
}
