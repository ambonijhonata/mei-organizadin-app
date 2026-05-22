package com.tcc.androidnative.feature.appointments.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
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
                    onServiceToggled = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Cadastrar agendamento").assertIsDisplayed()
        composeRule.onNodeWithText("Cliente").assertIsDisplayed()
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
                    onServiceToggled = {},
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
                    onServiceToggled = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-client-field").performClick()
        composeRule.onNodeWithTag("appointment-client-options").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-client-field").performClick()
        composeRule.onNodeWithTag("appointment-client-options").assertDoesNotExist()
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
                    onServiceToggled = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-services-field").performClick()
        composeRule.onNodeWithTag("appointment-services-options").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-services-field").performClick()
        composeRule.onNodeWithTag("appointment-services-options").assertDoesNotExist()
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
                    onServiceToggled = {},
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-client-field").performClick()
        composeRule.onNodeWithTag("appointment-client-options").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-save").performTouchInput { click(center) }
        composeRule.onNodeWithTag("appointment-client-options").assertDoesNotExist()

        composeRule.onNodeWithTag("appointment-services-field").performClick()
        composeRule.onNodeWithTag("appointment-services-options").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-save").performTouchInput { click(center) }
        composeRule.onNodeWithTag("appointment-services-options").assertDoesNotExist()
    }

    @Test
    fun form_should_select_and_deselect_multiple_services() {
        var selected = emptySet<Long>()

        fun render() {
            composeRule.setContent {
                AndroidNativeTheme {
                    AppointmentFormContent(
                        uiState = sampleState(selectedServiceIds = selected),
                        onClientSelected = {},
                        onStartTimeChange = {},
                        onEndTimeChange = {},
                        onServiceToggled = { id ->
                            selected = if (id in selected) selected - id else selected + id
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

        composeRule.onNodeWithTag("appointment-selected-service-1").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-selected-service-3").assertIsDisplayed()

        composeRule.onNodeWithTag("appointment-selected-service-1").performClick()

        composeRule.onNodeWithText("Henna").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-selected-service-3").assertIsDisplayed()
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
                        onServiceToggled = {},
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
                        onServiceToggled = {},
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
                        onServiceToggled = {},
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

    private fun sampleState(
        selectedServiceIds: Set<Long> = emptySet(),
        startTime: String = "",
        endTime: String = ""
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
            startTime = startTime,
            endTime = endTime,
            selectedServiceIds = selectedServiceIds,
            selectedServiceOptions = services.filter { it.id in selectedServiceIds }.associateBy { it.id }
        )
    }
}
