package com.tcc.androidnative.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import org.junit.Rule
import org.junit.Test

class AppShellScaffoldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun drawer_should_highlight_only_active_child_route() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppShellScaffold(
                    currentRoute = AppDestination.Clients.route,
                    onNavigate = {},
                    onLogout = {}
                ) {
                    Text("Conteudo")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Abrir menu hamburguer").performClick()
        composeRule.onNodeWithText("MEI ORGANIZADINHO").assertIsDisplayed()
        composeRule.onNodeWithText("Clientes").assertIsSelected()
        composeRule.onNodeWithText("Cadastros").assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Recolher submenu Cadastros").assertIsDisplayed()
    }

    @Test
    fun drawer_should_use_chevrons_for_expand_and_collapse() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppShellScaffold(
                    currentRoute = AppDestination.Home.route,
                    onNavigate = {},
                    onLogout = {}
                ) {
                    Text("Conteudo")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Abrir menu hamburguer").performClick()
        composeRule.onNodeWithContentDescription("Expandir submenu Relatorios").assertIsDisplayed()
        composeRule.onNodeWithText("Relatorios").performClick()
        composeRule.onNodeWithContentDescription("Recolher submenu Relatorios").assertIsDisplayed()
        composeRule.onNodeWithText("Fluxo de Caixa").assertIsDisplayed()
        composeRule.onNodeWithText("Faturamento").assertIsDisplayed()
    }

    @Test
    fun topbar_should_keep_branding_and_home_action() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppShellScaffold(
                    currentRoute = AppDestination.Revenue.route,
                    onNavigate = {},
                    onLogout = {}
                ) {
                    Text("Conteudo")
                }
            }
        }

        composeRule.onNodeWithText("MEI ORGANIZADINHO").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ir para tela inicial").assertIsDisplayed()
    }

    @Test
    fun drawer_should_show_settings_item() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppShellScaffold(
                    currentRoute = AppDestination.Home.route,
                    onNavigate = {},
                    onLogout = {}
                ) {
                    Text("Conteudo")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Abrir menu hamburguer").performClick()
        composeRule.onNodeWithText("Configuracoes").assertIsDisplayed()
    }
}
