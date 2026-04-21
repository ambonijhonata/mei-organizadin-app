package com.tcc.androidnative.feature.settings.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settings_should_enable_date_field_when_checkbox_is_checked() {
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(baseState(useStartDateFilter = false))
                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onConsiderPaidAppointmentsOnlyInReportsChanged = { enabled ->
                        state = state.copy(considerPaidAppointmentsOnlyInReports = enabled)
                    },
                    onStartDateInputChanged = { input ->
                        state = state.copy(startDateInput = input)
                    },
                    onDateFieldClick = {},
                    onStartDateTooltipClick = {},
                    onStartDateTooltipClose = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Campo de data inicial da sincronizacao")
            .assertIsNotEnabled()

        composeRule
            .onNodeWithContentDescription("Habilitar filtro por data inicial")
            .performClick()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription("Campo de data inicial da sincronizacao")
            .assertIsEnabled()
    }

    @Test
    fun settings_should_open_date_picker_when_calendar_icon_is_tapped() {
        var showDatePicker by mutableStateOf(false)
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(baseState())

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                androidx.compose.material3.Text(
                                    stringResource(R.string.calendar_date_picker_confirm)
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                androidx.compose.material3.Text(
                                    stringResource(R.string.calendar_date_picker_cancel)
                                )
                            }
                        }
                    ) {
                        DatePicker(
                            state = rememberDatePickerState(
                                initialSelectedDateMillis = 0L,
                                initialDisplayedMonthMillis = 0L
                            ),
                            showModeToggle = false
                        )
                    }
                }

                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onConsiderPaidAppointmentsOnlyInReportsChanged = { enabled ->
                        state = state.copy(considerPaidAppointmentsOnlyInReports = enabled)
                    },
                    onStartDateInputChanged = { input ->
                        state = state.copy(startDateInput = input)
                    },
                    onDateFieldClick = { showDatePicker = true },
                    onStartDateTooltipClick = {},
                    onStartDateTooltipClose = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Abrir calendario de data inicial")
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Selecionar").assertIsDisplayed()
    }

    @Test
    fun settings_should_not_open_date_picker_when_filter_is_disabled() {
        var showDatePicker by mutableStateOf(false)
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(baseState(useStartDateFilter = false))

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                androidx.compose.material3.Text(
                                    stringResource(R.string.calendar_date_picker_confirm)
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                androidx.compose.material3.Text(
                                    stringResource(R.string.calendar_date_picker_cancel)
                                )
                            }
                        }
                    ) {
                        DatePicker(
                            state = rememberDatePickerState(
                                initialSelectedDateMillis = 0L,
                                initialDisplayedMonthMillis = 0L
                            ),
                            showModeToggle = false
                        )
                    }
                }

                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onConsiderPaidAppointmentsOnlyInReportsChanged = { enabled ->
                        state = state.copy(considerPaidAppointmentsOnlyInReports = enabled)
                    },
                    onStartDateInputChanged = { input ->
                        state = state.copy(startDateInput = input)
                    },
                    onDateFieldClick = { showDatePicker = true },
                    onStartDateTooltipClick = {},
                    onStartDateTooltipClose = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Abrir calendario de data inicial")
            .assertIsNotEnabled()

        composeRule.onAllNodesWithText("Selecionar").assertCountEquals(0)
    }

    @Test
    fun settings_should_allow_manual_date_input_when_enabled() {
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(baseState())
                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onConsiderPaidAppointmentsOnlyInReportsChanged = { enabled ->
                        state = state.copy(considerPaidAppointmentsOnlyInReports = enabled)
                    },
                    onStartDateInputChanged = { input ->
                        val parsedDate = runCatching { DateFormats.parseUiDate(input) }.getOrNull()
                        state = state.copy(
                            startDate = parsedDate ?: state.startDate,
                            startDateInput = input,
                            startDateInputErrorResId = if (parsedDate != null) null else R.string.settings_start_date_invalid
                        )
                    },
                    onDateFieldClick = {},
                    onStartDateTooltipClick = {},
                    onStartDateTooltipClose = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Campo de data inicial da sincronizacao")
            .performTextClearance()
        composeRule
            .onNodeWithContentDescription("Campo de data inicial da sincronizacao")
            .performTextInput("15/04/2026")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("15/04/2026").assertIsDisplayed()
    }

    @Test
    fun settings_should_show_validation_feedback_for_invalid_manual_date_input() {
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(baseState())
                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onConsiderPaidAppointmentsOnlyInReportsChanged = { enabled ->
                        state = state.copy(considerPaidAppointmentsOnlyInReports = enabled)
                    },
                    onStartDateInputChanged = { input ->
                        val parsedDate = runCatching { DateFormats.parseUiDate(input) }.getOrNull()
                        state = state.copy(
                            startDate = parsedDate ?: state.startDate,
                            startDateInput = input,
                            startDateInputErrorResId = if (parsedDate != null) null else R.string.settings_start_date_invalid
                        )
                    },
                    onDateFieldClick = {},
                    onStartDateTooltipClick = {},
                    onStartDateTooltipClose = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Campo de data inicial da sincronizacao")
            .performTextClearance()
        composeRule
            .onNodeWithContentDescription("Campo de data inicial da sincronizacao")
            .performTextInput("99/99/9999")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Data inicial invalida").assertIsDisplayed()
    }

    @Test
    fun settings_should_show_and_close_start_date_tooltip_with_x_button() {
        val tooltipText =
            "O aplicativo ira buscar os agendamentos do google agenda a partir da data definida. Agendamentos anteriores a data nao serao importados."
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(baseState())
                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onConsiderPaidAppointmentsOnlyInReportsChanged = { enabled ->
                        state = state.copy(considerPaidAppointmentsOnlyInReports = enabled)
                    },
                    onStartDateInputChanged = { input ->
                        state = state.copy(startDateInput = input)
                    },
                    onDateFieldClick = {},
                    onStartDateTooltipClick = { state = state.copy(isStartDateTooltipVisible = true) },
                    onStartDateTooltipClose = { state = state.copy(isStartDateTooltipVisible = false) },
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Exibir ajuda da data inicial")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(tooltipText).assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Fechar ajuda")
            .performClick()
        composeRule.onAllNodesWithText(tooltipText).assertCountEquals(0)
    }

    @Test
    fun settings_should_auto_close_start_date_tooltip_after_ten_seconds() {
        val tooltipText =
            "O aplicativo ira buscar os agendamentos do google agenda a partir da data definida. Agendamentos anteriores a data nao serao importados."
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(baseState())
                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onConsiderPaidAppointmentsOnlyInReportsChanged = { enabled ->
                        state = state.copy(considerPaidAppointmentsOnlyInReports = enabled)
                    },
                    onStartDateInputChanged = { input ->
                        state = state.copy(startDateInput = input)
                    },
                    onDateFieldClick = {},
                    onStartDateTooltipClick = { state = state.copy(isStartDateTooltipVisible = true) },
                    onStartDateTooltipClose = { state = state.copy(isStartDateTooltipVisible = false) },
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Exibir ajuda da data inicial")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(tooltipText).assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(10_000L)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(tooltipText).assertCountEquals(0)
    }

    @Test
    fun settings_should_render_sync_and_reports_sections_and_feedback_message() {
        composeRule.setContent {
            AndroidNativeTheme {
                SettingsContent(
                    uiState = baseState().copy(
                        considerPaidAppointmentsOnlyInReports = true,
                        transientMessage = TransientMessage(
                            text = "Configuracoes gravadas com sucesso",
                            tone = MessageTone.SUCCESS,
                            durationMillis = MessageDurations.SHORT_3S
                        )
                    ),
                    onUseStartDateFilterChanged = {},
                    onConsiderPaidAppointmentsOnlyInReportsChanged = {},
                    onStartDateInputChanged = {},
                    onDateFieldClick = {},
                    onStartDateTooltipClick = {},
                    onStartDateTooltipClose = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Sincronizacao").assertIsDisplayed()
        composeRule.onNodeWithText("Relatorios").assertIsDisplayed()
        composeRule.onNodeWithText("Configuracoes gravadas com sucesso").assertIsDisplayed()
        composeRule.onNodeWithText("Considerar nos relatórios somente agendamentos pagos.").assertIsDisplayed()
    }

    private fun baseState(useStartDateFilter: Boolean = true): SettingsUiState {
        val startDate = LocalDate.of(2026, 4, 7)
        return SettingsUiState(
            useStartDateFilter = useStartDateFilter,
            startDate = startDate,
            startDateInput = DateFormats.toUiDate(startDate),
            requiresFirstAccessSave = false
        )
    }
}
