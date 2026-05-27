package com.tcc.androidnative.feature.appointments.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import com.tcc.androidnative.feature.clients.data.ClientModel
import com.tcc.androidnative.feature.services.data.ServiceModel
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppointmentFormScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun form_should_render_fields_and_right_aligned_actions() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppointmentFormContent(
                    uiState = sampleState(),
                    onClientSelected = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onServiceAdded = {},
                    onServiceGridCheckedChange = { _, _ -> },
                    onDeleteCheckedServices = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Cadastrar agendamento").assertIsDisplayed()
        composeRule.onNodeWithText("Cliente").assertIsDisplayed()
        composeRule.onNodeWithText("Data do agendamento").assertIsDisplayed()
        composeRule.onNodeWithText("Horário inicial").assertIsDisplayed()
        composeRule.onNodeWithText("Horário final").assertIsDisplayed()
        composeRule.onNodeWithText("Serviços").assertIsDisplayed()
        composeRule.onNodeWithText("Gravar").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Acoes do formulario de agendamento").assertIsDisplayed()
    }

    @Test
    fun client_combo_should_open_when_clicking_field_body() {
        var openCount = 0

        composeRule.setContent {
            AndroidNativeTheme {
                AppointmentFormContent(
                    uiState = sampleState(),
                    onClientSelected = {},
                    onClientSelectorClick = { openCount++ },
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onServiceAdded = {},
                    onServiceGridCheckedChange = { _, _ -> },
                    onDeleteCheckedServices = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-client-field").performClick()

        composeRule.onNodeWithTag("appointment-client-options").assertIsDisplayed()
        composeRule.onNodeWithText("Fabiane Da Silva").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, openCount)
        }
    }

    @Test
    fun client_combo_should_close_when_clicking_field_again() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppointmentFormContent(
                    uiState = sampleState(),
                    onClientSelected = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onServiceAdded = {},
                    onServiceGridCheckedChange = { _, _ -> },
                    onDeleteCheckedServices = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-client-field").performClick()
        composeRule.onNodeWithTag("appointment-client-options").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-client-field").performClick()
        composeRule.onAllNodesWithTag("appointment-client-options").assertCountEquals(0)
    }

    @Test
    fun services_combo_should_close_when_clicking_field_again() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppointmentFormContent(
                    uiState = sampleState(),
                    onClientSelected = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onServiceAdded = {},
                    onServiceGridCheckedChange = { _, _ -> },
                    onDeleteCheckedServices = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-services-field").performClick()
        composeRule.onNodeWithTag("appointment-services-options").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-services-field").performClick()
        composeRule.onAllNodesWithTag("appointment-services-options").assertCountEquals(0)
    }

    @Test
    fun combos_should_close_when_tapping_outside() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppointmentFormContent(
                    uiState = sampleState(),
                    onClientSelected = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onServiceAdded = {},
                    onServiceGridCheckedChange = { _, _ -> },
                    onDeleteCheckedServices = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-client-field").performClick()
        composeRule.onNodeWithTag("appointment-client-options").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-save").performTouchInput { click(center) }
        composeRule.onAllNodesWithTag("appointment-client-options").assertCountEquals(0)

        composeRule.onNodeWithTag("appointment-services-field").performClick()
        composeRule.onNodeWithTag("appointment-services-options").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-save").performTouchInput { click(center) }
        composeRule.onAllNodesWithTag("appointment-services-options").assertCountEquals(0)
    }

    @Test
    fun form_should_add_services_to_grid_and_remove_from_combobox() {
        var selected = emptySet<Long>()
        var checked = emptySet<Long>()

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(selectedServiceIds = selected),
                        onClientSelected = {},
                        onStartTimeChange = {},
                        onEndTimeChange = {},
                        onServiceAdded = { id ->
                            selected = selected + id
                            checked = checked - id
                            render()
                        },
                        onServiceGridCheckedChange = { id, isChecked ->
                            checked = if (isChecked) checked + id else checked - id
                            render()
                        },
                        onDeleteCheckedServices = {
                            selected = selected - checked
                            checked = emptySet()
                            render()
                        },
                        onSaveClick = {},
                        onCancelClick = {}
                    )
                }
            }
        }

        render()

        composeRule.onNodeWithTag("appointment-services-field").performClick()
        composeRule.onNodeWithText("Design de sobrancelha").performClick()
        composeRule.onNodeWithText("Henna").performClick()

        composeRule.onNodeWithTag("appointment-selected-service-row-1").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-selected-service-row-3").assertIsDisplayed()

        composeRule.onNodeWithTag("appointment-services-field").performClick()
        composeRule.onAllNodesWithText("Design de sobrancelha").assertCountEquals(1)
        composeRule.onAllNodesWithText("Henna").assertCountEquals(1)
    }

    @Test
    fun form_should_enable_delete_only_when_grid_items_are_checked() {
        var selected = setOf(1L)
        var checked = emptySet<Long>()

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(
                            selectedServiceIds = selected,
                            checkedServiceGridIds = checked
                        ),
                        onClientSelected = {},
                        onStartTimeChange = {},
                        onEndTimeChange = {},
                        onServiceAdded = { id ->
                            selected = selected + id
                            render()
                        },
                        onServiceGridCheckedChange = { id, isChecked ->
                            checked = if (isChecked) checked + id else checked - id
                            render()
                        },
                        onDeleteCheckedServices = {
                            selected = selected - checked
                            checked = emptySet()
                            render()
                        },
                        onSaveClick = {},
                        onCancelClick = {}
                    )
                }
            }
        }

        render()

        composeRule.onNodeWithTag("appointment-services-delete").assertIsNotEnabled()
        composeRule.onNodeWithTag("appointment-selected-service-checkbox-1").performClick()
        composeRule.onNodeWithTag("appointment-services-delete").assertIsEnabled()
    }

    @Test
    fun form_should_bulk_remove_checked_services_and_make_them_selectable_again() {
        var selected = setOf(1L, 3L)
        var checked = emptySet<Long>()

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(
                            selectedServiceIds = selected,
                            checkedServiceGridIds = checked
                        ),
                        onClientSelected = {},
                        onStartTimeChange = {},
                        onEndTimeChange = {},
                        onServiceAdded = { id ->
                            selected = selected + id
                            checked = checked - id
                            render()
                        },
                        onServiceGridCheckedChange = { id, isChecked ->
                            checked = if (isChecked) checked + id else checked - id
                            render()
                        },
                        onDeleteCheckedServices = {
                            selected = selected - checked
                            checked = emptySet()
                            render()
                        },
                        onSaveClick = {},
                        onCancelClick = {}
                    )
                }
            }
        }

        render()

        composeRule.onNodeWithTag("appointment-selected-service-checkbox-1").performClick()
        composeRule.onNodeWithTag("appointment-services-delete").performClick()

        composeRule.onAllNodesWithTag("appointment-selected-service-row-1").assertCountEquals(0)
        composeRule.onNodeWithTag("appointment-selected-service-row-3").assertIsDisplayed()

        composeRule.onNodeWithTag("appointment-services-field").performClick()
        composeRule.onNodeWithText("Design de sobrancelha").assertIsDisplayed()
    }

    @Test
    fun form_should_keep_time_input_and_cancel_callback() {
        var startTime = ""
        var endTime = ""
        var cancelCount = 0

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(startTime = startTime, endTime = endTime),
                        onClientSelected = {},
                        onStartTimeChange = {
                            startTime = it
                            render()
                        },
                        onEndTimeChange = {
                            endTime = it
                            render()
                        },
                        onServiceAdded = {},
                        onServiceGridCheckedChange = { _, _ -> },
                        onDeleteCheckedServices = {},
                        onSaveClick = {},
                        onCancelClick = { cancelCount++ }
                    )
                }
            }
        }

        render()

        composeRule.onNodeWithTag("appointment-start-time").performTextInput("08:30")
        composeRule.onNodeWithTag("appointment-end-time").performTextInput("09:45")
        composeRule.onNodeWithTag("appointment-cancel").performClick()

        composeRule.runOnIdle {
            assertEquals("08:30", startTime)
            assertEquals("09:45", endTime)
            assertEquals(1, cancelCount)
        }
    }

    @Test
    fun form_should_render_clock_actions_for_both_time_fields() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppointmentFormContent(
                    uiState = sampleState(),
                    onClientSelected = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onServiceAdded = {},
                    onServiceGridCheckedChange = { _, _ -> },
                    onDeleteCheckedServices = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Abrir relogio de horario inicial")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Abrir relogio de horario final")
            .assertIsDisplayed()
    }

    @Test
    fun form_should_open_start_time_picker_and_confirm_existing_value() {
        var startTime = "08:30"

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(startTime = startTime),
                        onClientSelected = {},
                        onStartTimeChange = {
                            startTime = it
                            render()
                        },
                        onEndTimeChange = {},
                        onServiceAdded = {},
                        onServiceGridCheckedChange = { _, _ -> },
                        onDeleteCheckedServices = {},
                        onSaveClick = {},
                        onCancelClick = {}
                    )
                }
            }
        }

        render()

        composeRule
            .onNodeWithContentDescription("Abrir relogio de horario inicial")
            .performClick()
        composeRule.onNodeWithTag("appointment-start-time-picker-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Selecionar").performClick()

        composeRule.onNodeWithTag("appointment-start-time").assertTextEquals("08:30")
        composeRule.runOnIdle {
            assertEquals("08:30", startTime)
        }
    }

    @Test
    fun form_should_open_end_time_picker_and_cancel_without_changing_value() {
        var endTime = "09:45"

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(endTime = endTime),
                        onClientSelected = {},
                        onStartTimeChange = {},
                        onEndTimeChange = {
                            endTime = it
                            render()
                        },
                        onServiceAdded = {},
                        onServiceGridCheckedChange = { _, _ -> },
                        onDeleteCheckedServices = {},
                        onSaveClick = {},
                        onCancelClick = {}
                    )
                }
            }
        }

        render()

        composeRule
            .onNodeWithContentDescription("Abrir relogio de horario final")
            .performClick()
        composeRule.onNodeWithTag("appointment-end-time-picker-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()

        composeRule.onNodeWithTag("appointment-end-time").assertTextEquals("09:45")
        composeRule.runOnIdle {
            assertEquals("09:45", endTime)
        }
    }

    @Test
    fun form_should_mask_start_time_and_ignore_non_digits() {
        var startTime = ""

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(startTime = startTime),
                        onClientSelected = {},
                        onStartTimeChange = {
                            startTime = it
                            render()
                        },
                        onEndTimeChange = {},
                        onServiceAdded = {},
                        onServiceGridCheckedChange = { _, _ -> },
                        onDeleteCheckedServices = {},
                        onSaveClick = {},
                        onCancelClick = {}
                    )
                }
            }
        }

        render()

        composeRule.onNodeWithTag("appointment-start-time").performTextInput("ab12:3c4")
        composeRule.onNodeWithTag("appointment-start-time").assertTextEquals("12:34")
    }

    @Test
    fun form_should_keep_last_two_digits_in_order_for_start_time() {
        var startTime = ""

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(startTime = startTime),
                        onClientSelected = {},
                        onStartTimeChange = {
                            startTime = it
                            render()
                        },
                        onEndTimeChange = {},
                        onServiceAdded = {},
                        onServiceGridCheckedChange = { _, _ -> },
                        onDeleteCheckedServices = {},
                        onSaveClick = {},
                        onCancelClick = {}
                    )
                }
            }
        }

        render()

        composeRule.onNodeWithTag("appointment-start-time").performTextInput("1210")
        composeRule.onNodeWithTag("appointment-start-time").assertTextEquals("12:10")
    }

    @Test
    fun form_should_mask_end_time_and_ignore_non_digits() {
        var endTime = ""

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(endTime = endTime),
                        onClientSelected = {},
                        onStartTimeChange = {},
                        onEndTimeChange = {
                            endTime = it
                            render()
                        },
                        onServiceAdded = {},
                        onServiceGridCheckedChange = { _, _ -> },
                        onDeleteCheckedServices = {},
                        onSaveClick = {},
                        onCancelClick = {}
                    )
                }
            }
        }

        render()

        composeRule.onNodeWithTag("appointment-end-time").performTextInput("x9y8z7!6")
        composeRule.onNodeWithTag("appointment-end-time").assertTextEquals("98:76")
    }

    @Test
    fun form_should_keep_last_two_digits_in_order_for_end_time() {
        var endTime = ""

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(endTime = endTime),
                        onClientSelected = {},
                        onStartTimeChange = {},
                        onEndTimeChange = {
                            endTime = it
                            render()
                        },
                        onServiceAdded = {},
                        onServiceGridCheckedChange = { _, _ -> },
                        onDeleteCheckedServices = {},
                        onSaveClick = {},
                        onCancelClick = {}
                    )
                }
            }
        }

        render()

        composeRule.onNodeWithTag("appointment-end-time").performTextInput("1210")
        composeRule.onNodeWithTag("appointment-end-time").assertTextEquals("12:10")
    }

    @Test
    fun form_should_open_appointment_date_picker() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppointmentFormContent(
                    uiState = sampleState(),
                    onClientSelected = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onServiceAdded = {},
                    onServiceGridCheckedChange = { _, _ -> },
                    onDeleteCheckedServices = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Abrir calendario de data do agendamento")
            .performClick()

        composeRule.onNodeWithText("Selecionar").assertIsDisplayed()
    }

    @Test
    fun form_should_render_field_level_validation_messages() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppointmentFormContent(
                    uiState = sampleState(
                        clientErrorResId = com.tcc.androidnative.R.string.appointment_client_required,
                        appointmentDateInput = "",
                        appointmentDateInputErrorResId = com.tcc.androidnative.R.string.appointment_date_invalid,
                        startTimeErrorResId = com.tcc.androidnative.R.string.appointment_start_time_required,
                        endTimeErrorResId = com.tcc.androidnative.R.string.appointment_end_time_required,
                        servicesErrorResId = com.tcc.androidnative.R.string.appointment_services_required
                    ),
                    onClientSelected = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onServiceAdded = {},
                    onServiceGridCheckedChange = { _, _ -> },
                    onDeleteCheckedServices = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Selecione um cliente").assertIsDisplayed()
        composeRule.onNodeWithText("Data do agendamento invalida").assertIsDisplayed()
        composeRule.onNodeWithText("Informe o horário inicial").assertIsDisplayed()
        composeRule.onNodeWithText("Informe o horário final").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-services-error").assertIsDisplayed()
        composeRule.onNodeWithText("Selecione pelo menos um serviço").assertIsDisplayed()
    }

    @Test
    fun form_should_render_time_range_validation_message() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppointmentFormContent(
                    uiState = sampleState(
                        startTime = "10:00",
                        endTime = "09:00",
                        endTimeErrorResId = com.tcc.androidnative.R.string.appointment_end_time_must_be_after_start_time
                    ),
                    onClientSelected = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onServiceAdded = {},
                    onServiceGridCheckedChange = { _, _ -> },
                    onDeleteCheckedServices = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Horário final deve ser maior que o horário inicial").assertIsDisplayed()
    }

    private fun sampleState(
        selectedServiceIds: Set<Long> = emptySet(),
        checkedServiceGridIds: Set<Long> = emptySet(),
        appointmentDate: LocalDate = LocalDate.of(2026, 5, 22),
        appointmentDateInput: String = "22/05/2026",
        appointmentDateInputErrorResId: Int? = null,
        startTime: String = "",
        startTimeErrorResId: Int? = null,
        endTime: String = "",
        endTimeErrorResId: Int? = null,
        clientErrorResId: Int? = null,
        servicesErrorResId: Int? = null
    ): AppointmentFormUiState {
        val services = listOf(
            ServiceModel(1, "Design de sobrancelha", BigDecimal("50.00")),
            ServiceModel(2, "Buço", BigDecimal("20.00")),
            ServiceModel(3, "Henna", BigDecimal("25.00"))
        )
        return AppointmentFormUiState(
            clients = listOf(
                ClientModel(1, "Fabiane Da Silva", null, null, null, null),
                ClientModel(2, "Maria Santos", null, null, null, null)
            ),
            services = services,
            clientErrorResId = clientErrorResId,
            appointmentDate = appointmentDate,
            appointmentDateInput = appointmentDateInput,
            appointmentDateInputErrorResId = appointmentDateInputErrorResId,
            startTime = startTime,
            startTimeErrorResId = startTimeErrorResId,
            endTime = endTime,
            endTimeErrorResId = endTimeErrorResId,
            selectedServiceIds = selectedServiceIds,
            selectedServiceOptions = services.filter { it.id in selectedServiceIds }.associateBy { it.id },
            checkedServiceGridIds = checkedServiceGridIds,
            servicesErrorResId = servicesErrorResId
        )
    }
}
