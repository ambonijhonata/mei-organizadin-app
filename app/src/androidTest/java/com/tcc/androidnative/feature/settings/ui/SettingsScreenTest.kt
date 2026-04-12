package com.tcc.androidnative.feature.settings.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settings_should_enable_date_field_when_checkbox_is_checked() {
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(
                    SettingsUiState(
                        useStartDateFilter = false,
                        startDate = LocalDate.of(2026, 4, 7),
                        requiresFirstAccessSave = false
                    )
                )
                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onDateFieldClick = {},
                    onTooltipClick = {},
                    onTooltipClose = {},
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

        composeRule
            .onNodeWithContentDescription("Campo de data inicial da sincronizacao")
            .assertIsEnabled()
    }

    @Test
    fun settings_should_show_and_close_tooltip_with_x_button() {
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(
                    SettingsUiState(
                        useStartDateFilter = true,
                        startDate = LocalDate.of(2026, 4, 7),
                        requiresFirstAccessSave = false
                    )
                )
                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onDateFieldClick = {},
                    onTooltipClick = { state = state.copy(isTooltipVisible = true) },
                    onTooltipClose = { state = state.copy(isTooltipVisible = false) },
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Exibir ajuda da data inicial")
            .performClick()

        composeRule
            .onNodeWithText(
                "O aplicativo ira buscar os agendamentos do google agenda a partir da data definida. Agendamentos anteriores a data nao serao importados."
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Fechar ajuda")
            .performClick()

        composeRule
            .onAllNodesWithText(
                "O aplicativo ira buscar os agendamentos do google agenda a partir da data definida. Agendamentos anteriores a data nao serao importados."
            )
            .assertCountEquals(0)
    }

    @Test
    fun settings_should_auto_close_tooltip_after_ten_seconds() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AndroidNativeTheme {
                var state by mutableStateOf(
                    SettingsUiState(
                        useStartDateFilter = true,
                        startDate = LocalDate.of(2026, 4, 7),
                        requiresFirstAccessSave = false
                    )
                )
                SettingsContent(
                    uiState = state,
                    onUseStartDateFilterChanged = { enabled ->
                        state = state.copy(useStartDateFilter = enabled)
                    },
                    onDateFieldClick = {},
                    onTooltipClick = { state = state.copy(isTooltipVisible = true) },
                    onTooltipClose = { state = state.copy(isTooltipVisible = false) },
                    onSaveClick = {},
                    onCancelClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Exibir ajuda da data inicial")
            .performClick()
        composeRule
            .onNodeWithText(
                "O aplicativo ira buscar os agendamentos do google agenda a partir da data definida. Agendamentos anteriores a data nao serao importados."
            )
            .assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(10_000L)
        composeRule.waitForIdle()

        composeRule
            .onAllNodesWithText(
                "O aplicativo ira buscar os agendamentos do google agenda a partir da data definida. Agendamentos anteriores a data nao serao importados."
            )
            .assertCountEquals(0)
    }
}
