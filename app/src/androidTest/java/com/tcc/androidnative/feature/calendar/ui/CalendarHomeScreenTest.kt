package com.tcc.androidnative.feature.calendar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CalendarHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home_should_render_date_header_and_daily_slots() {
        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(
                        selectedDate = LocalDate.of(2026, 4, 4),
                        items = listOf(
                            CalendarAgendaItem(
                                eventId = 1L,
                                eventStart = Instant.parse("2026-04-04T14:30:00Z"),
                                eventEnd = Instant.parse("2026-04-04T15:30:00Z"),
                                slotLabel = "14:30",
                                timeLabel = "14:30",
                                title = "Reuniao com Cliente",
                                serviceDescription = "Discussao sobre novo projeto",
                                durationLabel = "1 hora"
                            )
                        )
                    ),
                    onPreviousDay = {},
                    onNextDay = {},
                    onDateSelected = {},
                    onReauthenticateRequested = {},
                    currentLocalTimeProvider = { LocalTime.of(10, 20) }
                )
            }
        }

        composeRule.onNodeWithText("Sábado").assertIsDisplayed()
        composeRule.onNodeWithText("Abril De 2026").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ir para dia anterior").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ir para proximo dia").assertIsDisplayed()
        composeRule.onNodeWithText("04/04/2026").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Selecionar data da agenda").assertIsDisplayed()
        composeRule.onNodeWithText("Data da última sincronização: Nunca sincronizado").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sincronizar agenda agora").assertIsDisplayed()
        composeRule.onAllNodesWithText("00:00 - Livre").assertCountEquals(0)
        composeRule.onNodeWithText("14:30").assertIsDisplayed()
        composeRule.onNodeWithText("Reuniao com Cliente").assertIsDisplayed()
        composeRule.onNodeWithText("1 hora").assertIsDisplayed()
    }

    @Test
    fun home_should_keep_sync_warning_and_reauth_actions_visible() {
        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(
                        selectedDate = LocalDate.of(2026, 4, 4),
                        syncWarningMessage = TransientMessage(
                            text = "Integracao Google revogada. Reautentique para voltar a sincronizar.",
                            tone = MessageTone.WARNING,
                            durationMillis = 0L
                        ),
                        isReauthRequired = true
                    ),
                    onPreviousDay = {},
                    onNextDay = {},
                    onDateSelected = {},
                    onReauthenticateRequested = {}
                )
            }
        }

        composeRule.onNodeWithText("Integracao Google revogada. Reautentique para voltar a sincronizar.").assertIsDisplayed()
        composeRule.onNodeWithText("Reautenticar Google").assertIsDisplayed()
    }

    @Test
    fun home_should_autofocus_device_time_slot_when_day_has_no_events() {
        val resolvedIndexes = mutableListOf<Int>()
        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(selectedDate = LocalDate.of(2026, 4, 4)),
                    onPreviousDay = {},
                    onNextDay = {},
                    onDateSelected = {},
                    onReauthenticateRequested = {},
                    currentLocalTimeProvider = { LocalTime.of(10, 40) },
                    onAutoFocusIndexResolved = { index -> resolvedIndexes.add(index) }
                )
            }
        }

        composeRule.waitForIdle()
        assertTrue(resolvedIndexes.last() == 21)
        composeRule.onNodeWithText("10:30 - Livre").assertIsDisplayed()
    }

    @Test
    fun home_should_reapply_focus_after_next_day_navigation() {
        val resolvedIndexes = mutableStateListOf<Int>()

        composeRule.setContent {
            AndroidNativeTheme {
                var selectedDate by remember { mutableStateOf(LocalDate.of(2026, 4, 4)) }
                var nextFocusRequestId by remember { mutableStateOf(0) }
                var pendingFocusRequestId by remember { mutableStateOf<Int?>(null) }
                var items by remember {
                    mutableStateOf(
                        listOf(
                            CalendarAgendaItem(
                                eventId = 1L,
                                eventStart = Instant.parse("2026-04-04T14:30:00Z"),
                                eventEnd = Instant.parse("2026-04-04T15:00:00Z"),
                                slotLabel = "14:30",
                                timeLabel = "14:30",
                                title = "Evento Dia 4",
                                serviceDescription = null,
                                durationLabel = "30 min"
                            )
                        )
                    )
                }

                CalendarHomeContent(
                    uiState = CalendarHomeUiState(
                        selectedDate = selectedDate,
                        items = items
                    ),
                    onPreviousDay = {},
                    onNextDay = {
                        selectedDate = selectedDate.plusDays(1)
                        nextFocusRequestId += 1
                        pendingFocusRequestId = nextFocusRequestId
                        items = listOf(
                            CalendarAgendaItem(
                                eventId = 2L,
                                eventStart = Instant.parse("2026-04-05T08:00:00Z"),
                                eventEnd = Instant.parse("2026-04-05T08:30:00Z"),
                                slotLabel = "08:00",
                                timeLabel = "08:00",
                                title = "Evento Dia 5",
                                serviceDescription = null,
                                durationLabel = "30 min"
                            )
                        )
                    },
                    onDateSelected = { selectedDate = it },
                    onReauthenticateRequested = {},
                    autoFocusRequestId = pendingFocusRequestId,
                    onAutoFocusRequestConsumed = { pendingFocusRequestId = null },
                    currentLocalTimeProvider = { LocalTime.of(9, 10) },
                    onAutoFocusIndexResolved = { index -> resolvedIndexes.add(index) }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Ir para proximo dia").performClick()
        composeRule.waitForIdle()

        assertTrue(resolvedIndexes.contains(29))
        assertTrue(resolvedIndexes.contains(16))
        composeRule.onNodeWithText("Evento Dia 5").assertIsDisplayed()
    }

    @Test
    fun home_should_not_autofocus_when_restoring_from_payments_context() {
        val resolvedIndexes = mutableListOf<Int>()
        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(selectedDate = LocalDate.of(2026, 4, 4)),
                    onPreviousDay = {},
                    onNextDay = {},
                    onDateSelected = {},
                    onReauthenticateRequested = {},
                    allowInitialAutoFocus = false,
                    currentLocalTimeProvider = { LocalTime.of(10, 40) },
                    onAutoFocusIndexResolved = { index -> resolvedIndexes.add(index) }
                )
            }
        }

        composeRule.waitForIdle()
        assertTrue(resolvedIndexes.isEmpty())
    }

    @Test
    fun home_should_autofocus_after_explicit_date_change_even_without_initial_autofocus() {
        val resolvedIndexes = mutableListOf<Int>()

        composeRule.setContent {
            AndroidNativeTheme {
                var selectedDate by remember { mutableStateOf(LocalDate.of(2026, 4, 4)) }
                var nextFocusRequestId by remember { mutableStateOf(0) }
                var pendingFocusRequestId by remember { mutableStateOf<Int?>(null) }
                var items by remember {
                    mutableStateOf(
                        listOf(
                            CalendarAgendaItem(
                                eventId = 1L,
                                eventStart = Instant.parse("2026-04-04T14:30:00Z"),
                                eventEnd = Instant.parse("2026-04-04T15:00:00Z"),
                                slotLabel = "14:30",
                                timeLabel = "14:30",
                                title = "Evento Dia 4",
                                serviceDescription = null,
                                durationLabel = "30 min"
                            )
                        )
                    )
                }

                CalendarHomeContent(
                    uiState = CalendarHomeUiState(
                        selectedDate = selectedDate,
                        items = items
                    ),
                    onPreviousDay = {},
                    onNextDay = {
                        selectedDate = selectedDate.plusDays(1)
                        nextFocusRequestId += 1
                        pendingFocusRequestId = nextFocusRequestId
                        items = listOf(
                            CalendarAgendaItem(
                                eventId = 2L,
                                eventStart = Instant.parse("2026-04-05T08:00:00Z"),
                                eventEnd = Instant.parse("2026-04-05T08:30:00Z"),
                                slotLabel = "08:00",
                                timeLabel = "08:00",
                                title = "Evento Dia 5",
                                serviceDescription = null,
                                durationLabel = "30 min"
                            )
                        )
                    },
                    onDateSelected = { selectedDate = it },
                    onReauthenticateRequested = {},
                    allowInitialAutoFocus = false,
                    autoFocusRequestId = pendingFocusRequestId,
                    onAutoFocusRequestConsumed = { pendingFocusRequestId = null },
                    currentLocalTimeProvider = { LocalTime.of(9, 10) },
                    onAutoFocusIndexResolved = { index -> resolvedIndexes.add(index) }
                )
            }
        }

        composeRule.waitForIdle()
        assertTrue(resolvedIndexes.isEmpty())
        composeRule.onNodeWithContentDescription("Ir para proximo dia").performClick()
        composeRule.waitForIdle()

        assertTrue(resolvedIndexes.contains(16))
    }

    @Test
    fun home_should_open_date_picker_dialog_from_selector() {
        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(selectedDate = LocalDate.of(2026, 4, 5)),
                    onPreviousDay = {},
                    onNextDay = {},
                    onDateSelected = {},
                    onReauthenticateRequested = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Selecionar data da agenda").performClick()
        composeRule.onNodeWithText("Selecionar").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
    }

    @Test
    fun home_should_notify_appointment_click_for_payments_navigation() {
        var clickedEventId: Long? = null
        var clickedServiceTotal: BigDecimal? = null
        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(
                        selectedDate = LocalDate.of(2026, 4, 4),
                        items = listOf(
                            CalendarAgendaItem(
                                eventId = 88L,
                                eventStart = Instant.parse("2026-04-04T09:00:00Z"),
                                eventEnd = Instant.parse("2026-04-04T10:00:00Z"),
                                slotLabel = "09:00",
                                timeLabel = "09:00",
                                title = "Corte",
                                serviceDescription = "Cabelo e barba",
                                durationLabel = "1 hora",
                                serviceTotalValue = BigDecimal("65.00")
                            )
                        )
                    ),
                    onPreviousDay = {},
                    onNextDay = {},
                    onDateSelected = {},
                    onReauthenticateRequested = {},
                    onAppointmentClick = { item ->
                        clickedEventId = item.eventId
                        clickedServiceTotal = item.serviceTotalValue
                    }
                )
            }
        }

        composeRule.onNodeWithTag("appointment_card_88").performClick()
        assertTrue(clickedEventId == 88L)
        assertTrue(clickedServiceTotal == BigDecimal("65.00"))
    }

    @Test
    fun home_should_go_to_previous_day_when_swiping_right_over_agenda() {
        var selectedDate by mutableStateOf(LocalDate.of(2026, 4, 4))
        var previousDayCalls = 0
        var nextDayCalls = 0

        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(selectedDate = selectedDate),
                    onPreviousDay = {
                        previousDayCalls += 1
                        selectedDate = selectedDate.minusDays(1)
                    },
                    onNextDay = {
                        nextDayCalls += 1
                        selectedDate = selectedDate.plusDays(1)
                    },
                    onDateSelected = {},
                    onReauthenticateRequested = {}
                )
            }
        }

        composeRule.onNodeWithTag("calendar_agenda_container").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(1, previousDayCalls)
        assertEquals(0, nextDayCalls)
        composeRule.onNodeWithText("03/04/2026").assertIsDisplayed()
    }

    @Test
    fun home_should_go_to_next_day_when_swiping_left_over_agenda() {
        var selectedDate by mutableStateOf(LocalDate.of(2026, 4, 4))
        var previousDayCalls = 0
        var nextDayCalls = 0

        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(selectedDate = selectedDate),
                    onPreviousDay = {
                        previousDayCalls += 1
                        selectedDate = selectedDate.minusDays(1)
                    },
                    onNextDay = {
                        nextDayCalls += 1
                        selectedDate = selectedDate.plusDays(1)
                    },
                    onDateSelected = {},
                    onReauthenticateRequested = {}
                )
            }
        }

        composeRule.onNodeWithTag("calendar_agenda_container").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(0, previousDayCalls)
        assertEquals(1, nextDayCalls)
        composeRule.onNodeWithText("05/04/2026").assertIsDisplayed()
    }

    @Test
    fun home_should_not_change_day_when_swiping_outside_agenda() {
        var selectedDate by mutableStateOf(LocalDate.of(2026, 4, 4))
        var previousDayCalls = 0
        var nextDayCalls = 0

        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(selectedDate = selectedDate),
                    onPreviousDay = {
                        previousDayCalls += 1
                        selectedDate = selectedDate.minusDays(1)
                    },
                    onNextDay = {
                        nextDayCalls += 1
                        selectedDate = selectedDate.plusDays(1)
                    },
                    onDateSelected = {},
                    onReauthenticateRequested = {}
                )
            }
        }

        composeRule.onNodeWithTag("calendar_header_container").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(0, previousDayCalls)
        assertEquals(0, nextDayCalls)
        composeRule.onNodeWithText("04/04/2026").assertIsDisplayed()
    }

    @Test
    fun home_should_keep_vertical_scroll_inside_agenda_without_day_navigation() {
        var previousDayCalls = 0
        var nextDayCalls = 0

        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(selectedDate = LocalDate.of(2026, 4, 4)),
                    onPreviousDay = { previousDayCalls += 1 },
                    onNextDay = { nextDayCalls += 1 },
                    onDateSelected = {},
                    onReauthenticateRequested = {}
                )
            }
        }

        composeRule.onNodeWithTag("calendar_agenda_container").performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertEquals(0, previousDayCalls)
        assertEquals(0, nextDayCalls)
        composeRule.onNodeWithTag("calendar_agenda_list").assert(hasScrollToIndexAction())
    }

    @Test
    fun home_should_trigger_manual_sync_from_last_sync_strip() {
        var syncRequests = 0

        composeRule.setContent {
            AndroidNativeTheme {
                CalendarHomeContent(
                    uiState = CalendarHomeUiState(
                        selectedDate = LocalDate.of(2026, 4, 4),
                        lastSyncDisplayValue = "08/05/2026 12:30"
                    ),
                    onPreviousDay = {},
                    onNextDay = {},
                    onDateSelected = {},
                    onReauthenticateRequested = {},
                    onSyncRequested = { syncRequests += 1 }
                )
            }
        }

        composeRule.onNodeWithTag("calendar_last_sync_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_last_sync_label").assertIsDisplayed()
        composeRule.onNodeWithText("Data da última sincronização: 08/05/2026 12:30").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_last_sync_refresh").performClick()
        assertEquals(1, syncRequests)
    }
}
