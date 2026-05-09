package com.tcc.androidnative.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import org.junit.Assert.assertEquals
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
        composeRule.onNodeWithContentDescription("Expandir submenu Relatórios").assertIsDisplayed()
        composeRule.onNodeWithText("Relatórios").performClick()
        composeRule.onNodeWithContentDescription("Recolher submenu Relatórios").assertIsDisplayed()
        composeRule.onNodeWithText("Fluxo de Caixa").assertIsDisplayed()
        composeRule.onNodeWithText("Faturamento").assertIsDisplayed()
        composeRule.onNodeWithText("Faturamento por método de pagamento").assertIsDisplayed()
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

    @Test
    fun topbar_home_should_trigger_navigation_when_current_route_is_payments() {
        var navigatedRoute: String? = null
        composeRule.setContent {
            AndroidNativeTheme {
                AppShellScaffold(
                    currentRoute = AppDestination.Payments.route,
                    onNavigate = { route -> navigatedRoute = route },
                    onLogout = {}
                ) {
                    Text("Conteudo")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Ir para tela inicial").performClick()
        composeRule.runOnIdle {
            assertEquals(AppDestination.Home.route, navigatedRoute)
        }
    }

    @Test
    fun drawer_should_highlight_payment_method_revenue_child_only() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppShellScaffold(
                    currentRoute = AppDestination.PaymentMethodRevenue.route,
                    onNavigate = {},
                    onLogout = {}
                ) {
                    Text("Conteudo")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Abrir menu hamburguer").performClick()
        composeRule.onNodeWithText("Faturamento por método de pagamento").assertIsSelected()
        composeRule.onNodeWithText("Relatórios").assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Recolher submenu Relatórios").assertIsDisplayed()
    }
    @Test
    fun drawer_gesture_should_remain_available_on_other_authenticated_screens() {
        composeRule.setContent {
            AndroidNativeTheme {
                AppShellScaffold(
                    currentRoute = AppDestination.Clients.route,
                    onNavigate = {},
                    onLogout = {}
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("scaffold_content")
                    ) {
                        Text("Conteudo")
                    }
                }
            }
        }

        composeRule.onNodeWithTag("scaffold_content").performTouchInput { swipeRight() }
        composeRule.onNodeWithText("MEI ORGANIZADINHO").assertIsDisplayed()
        composeRule.onNodeWithText("Clientes").assertIsDisplayed()
    }
}
