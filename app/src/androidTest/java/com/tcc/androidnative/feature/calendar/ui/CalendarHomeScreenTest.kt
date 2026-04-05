package com.tcc.androidnative.feature.calendar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
                    onReauthenticateRequested = {},
                    currentLocalTimeProvider = { LocalTime.of(10, 20) }
                )
            }
        }

        composeRule.onNodeWithText("Sábado").assertIsDisplayed()
        composeRule.onNodeWithText("Abril De 2026").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ir para dia anterior").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ir para proximo dia").assertIsDisplayed()
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
                        syncWarningMessage = "Sincronizacao parcial indisponivel",
                        isReauthRequired = true
                    ),
                    onPreviousDay = {},
                    onNextDay = {},
                    onReauthenticateRequested = {}
                )
            }
        }

        composeRule.onNodeWithText("Integracao Google requer nova autenticacao.").assertIsDisplayed()
        composeRule.onNodeWithText("Reautenticar Google").assertIsDisplayed()
        composeRule.onNodeWithText("Sincronizacao parcial indisponivel").assertIsDisplayed()
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
                    onReauthenticateRequested = {},
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
}
