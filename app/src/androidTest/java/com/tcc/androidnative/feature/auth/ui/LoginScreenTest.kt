package com.tcc.androidnative.feature.auth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun login_should_render_brand_hierarchy_and_welcome_card() {
        composeRule.setContent {
            AndroidNativeTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onGoogleClick = {}
                )
            }
        }

        composeRule.onNodeWithText("MEI").assertIsDisplayed()
        composeRule.onNodeWithText("ORGANIZADINHO").assertIsDisplayed()
        composeRule.onNodeWithText("Organize seus agendamentos").assertIsDisplayed()
        composeRule.onNodeWithText("Bem-vindo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Entrar com Google").assertIsDisplayed()
    }

    @Test
    fun login_should_show_loading_state_and_disable_google_button() {
        composeRule.setContent {
            AndroidNativeTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoading = true),
                    onGoogleClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Entrando...").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Entrar com Google").assertIsNotEnabled()
    }

    @Test
    fun login_should_show_error_without_hiding_primary_action() {
        var clicked = false
        composeRule.setContent {
            AndroidNativeTheme {
                LoginScreen(
                    uiState = LoginUiState(
                        transientMessage = TransientMessage(
                            text = "Erro ao fazer login no Google",
                            tone = MessageTone.ERROR,
                            durationMillis = 300_000L
                        )
                    ),
                    onGoogleClick = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText("Erro ao fazer login no Google").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Entrar com Google").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }
}
