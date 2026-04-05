package com.tcc.androidnative.feature.auth.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.tcc.androidnative.core.config.ApiConfig
import com.tcc.androidnative.feature.auth.data.AuthRepository
import com.tcc.androidnative.feature.auth.data.google.GoogleAuthTokens
import com.tcc.androidnative.feature.auth.data.google.GoogleSignInGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

private const val LOGIN_ERROR_DURATION_MS = 5_000L

data class LoginUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
    val errorDurationMillis: Long = LOGIN_ERROR_DURATION_MS
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInGateway: GoogleSignInGateway
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signInIntent(): Intent = googleSignInGateway.signInIntent()

    fun onGoogleSignInResult(data: Intent?) {
        googleSignInGateway.extractTokens(data)
            .onSuccess { tokens -> onGoogleTokensReceived(tokens) }
            .onFailure { error -> showLoginError(error) }
    }

    fun onGoogleTokensReceived(tokens: GoogleAuthTokens) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                authRepository.login(
                    idToken = tokens.idToken,
                    authorizationCode = tokens.authorizationCode
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        errorMessage = mapErrorMessage(error)
                    )
                }
                clearErrorAfterTimeout()
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { it.copy(isAuthenticated = false, errorMessage = null) }
    }

    private fun showLoginError(error: Throwable) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    errorMessage = mapErrorMessage(error)
                )
            }
            clearErrorAfterTimeout()
        }
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                10 -> "Falha na configuracao do Google Login (SHA-1, pacote ou Client ID)."
                7 -> "Sem conexao com internet para autenticar no Google."
                12501 -> "Login cancelado."
                else -> LOGIN_ERROR_MESSAGE
            }
            is IllegalStateException -> {
                if (error.message?.contains("GOOGLE_WEB_CLIENT_ID") == true) {
                    "GOOGLE_WEB_CLIENT_ID nao configurado no Gradle."
                } else {
                    LOGIN_ERROR_MESSAGE
                }
            }
            is ConnectException, is UnknownHostException, is IOException ->
                "Nao foi possivel conectar na API (${ApiConfig.BASE_URL})."
            is HttpException -> mapHttpErrorMessage(error)
            else -> LOGIN_ERROR_MESSAGE
        }
    }

    private fun mapHttpErrorMessage(error: HttpException): String {
        val status = error.code()
        val body = runCatching { error.response()?.errorBody()?.string().orEmpty() }
            .getOrDefault("")

        val backendCode = extractJsonField(body, "code")
        val backendMessage = extractJsonField(body, "message")

        return when {
            !backendMessage.isNullOrBlank() && !backendCode.isNullOrBlank() ->
                "HTTP $status ($backendCode): $backendMessage"
            !backendMessage.isNullOrBlank() ->
                "HTTP $status: $backendMessage"
            !error.message().isNullOrBlank() ->
                "HTTP $status: ${error.message()}"
            else ->
                "HTTP $status: erro na API."
        }
    }

    private fun extractJsonField(json: String, field: String): String? {
        val match = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"").find(json) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private suspend fun clearErrorAfterTimeout() {
        delay(LOGIN_ERROR_DURATION_MS)
        _uiState.update { state ->
            if (state.errorMessage.isNullOrBlank()) state else state.copy(errorMessage = null)
        }
    }

    private companion object {
        const val LOGIN_ERROR_MESSAGE = "Erro ao fazer login no Google"
    }
}
