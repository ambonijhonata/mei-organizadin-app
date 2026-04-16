package com.tcc.androidnative.feature.auth.ui

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.MessageDurations
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.feature.auth.data.AuthRepository
import com.tcc.androidnative.feature.auth.data.google.GoogleAuthTokens
import com.tcc.androidnative.feature.auth.data.google.GoogleSignInGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val transientMessage: TransientMessage? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInGateway: GoogleSignInGateway
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    private val _loginSuccessEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loginSuccessEvents = _loginSuccessEvents.asSharedFlow()

    fun signInIntent(): Intent = googleSignInGateway.signInIntent()

    fun onGoogleSignInResult(data: Intent?) {
        googleSignInGateway.extractTokens(data)
            .onSuccess { tokens ->
                logInfo(
                    buildString {
                        append("auth_login_tokens_received ")
                        append("idTokenPresent=${tokens.idToken.isNotBlank()} ")
                        append("idTokenLength=${tokens.idToken.length} ")
                        append("authorizationCodePresent=${tokens.authorizationCode.isNotBlank()} ")
                        append("authorizationCodeLength=${tokens.authorizationCode.length}")
                    }
                )
                onGoogleTokensReceived(tokens)
            }
            .onFailure { error ->
                logWarn(
                    "auth_login_token_extraction_failed exceptionClass=${error::class.java.simpleName} message=${error.message}",
                    error
                )
                showLoginError()
            }
    }

    fun onGoogleTokensReceived(tokens: GoogleAuthTokens) {
        viewModelScope.launch {
            logInfo(
                buildString {
                    append("auth_login_backend_start ")
                    append("idTokenPresent=${tokens.idToken.isNotBlank()} ")
                    append("idTokenLength=${tokens.idToken.length} ")
                    append("authorizationCodePresent=${tokens.authorizationCode.isNotBlank()} ")
                    append("authorizationCodeLength=${tokens.authorizationCode.length}")
                }
            )
            _uiState.update { it.copy(isLoading = true, transientMessage = null) }
            runCatching {
                authRepository.login(
                    idToken = tokens.idToken,
                    authorizationCode = tokens.authorizationCode
                )
            }.onSuccess {
                logInfo("auth_login_backend_success")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        transientMessage = null
                    )
                }
                _loginSuccessEvents.tryEmit(Unit)
            }.onFailure { error ->
                logError(
                    "auth_login_backend_failed exceptionClass=${error::class.java.simpleName} message=${error.message}",
                    error
                )
                showLoginError()
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { it.copy(isAuthenticated = false, transientMessage = null) }
    }

    private fun showLoginError() {
        viewModelScope.launch {
            val message = loginErrorMessage()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    transientMessage = message
                )
            }
            clearErrorAfterTimeout(message)
        }
    }

    private fun loginErrorMessage(): TransientMessage {
        return TransientMessage(
            textResId = R.string.login_feedback_error,
            tone = MessageTone.ERROR,
            durationMillis = MessageDurations.LOGIN_ERROR_5_MIN
        )
    }

    private suspend fun clearErrorAfterTimeout(message: TransientMessage) {
        delay(message.durationMillis)
        _uiState.update { state ->
            if (state.transientMessage == message) {
                state.copy(transientMessage = null)
            } else {
                state
            }
        }
    }

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    private fun logWarn(message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) {
                Log.w(TAG, message, throwable)
            } else {
                Log.w(TAG, message)
            }
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        }
    }

    private companion object {
        const val TAG = "LoginViewModel"
    }
}
