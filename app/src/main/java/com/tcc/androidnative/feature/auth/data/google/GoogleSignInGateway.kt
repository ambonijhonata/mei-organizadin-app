package com.tcc.androidnative.feature.auth.data.google

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.tcc.androidnative.core.config.ApiConfig
import com.tcc.androidnative.core.config.GoogleAuthConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleAuthTokens(
    val idToken: String,
    val authorizationCode: String
)

interface GoogleSignInGateway {
    fun signInIntent(): Intent
    fun extractTokens(data: Intent?): Result<GoogleAuthTokens>
}

@Singleton
class GoogleSignInGatewayImpl @Inject constructor(
    @ApplicationContext context: Context
) : GoogleSignInGateway {
    private val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(ApiConfig.GOOGLE_CALENDAR_READONLY_SCOPE))
        .apply {
            if (GoogleAuthConfig.webClientId.isNotBlank()) {
                requestIdToken(GoogleAuthConfig.webClientId)
                requestServerAuthCode(GoogleAuthConfig.webClientId, true)
            }
        }
        .build()

    private val client = GoogleSignIn.getClient(context, options)

    override fun signInIntent(): Intent = client.signInIntent

    override fun extractTokens(data: Intent?): Result<GoogleAuthTokens> {
        logInfo(
            "auth_google_extract_start data_present=${data != null} web_client_id_configured=${GoogleAuthConfig.webClientId.isNotBlank()}"
        )

        if (GoogleAuthConfig.webClientId.isBlank()) {
            logError("auth_google_extract_failed reason=missing_web_client_id")
            return Result.failure(IllegalStateException("GOOGLE_WEB_CLIENT_ID nao configurado."))
        }
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            val idToken = account.idToken
            val authorizationCode = account.serverAuthCode
            if (idToken.isNullOrBlank()) {
                logWarn("auth_google_extract_failed reason=missing_id_token")
                Result.failure(IllegalStateException("Google nao retornou idToken."))
            } else {
                logInfo("auth_google_extract_success has_authorization_code=${!authorizationCode.isNullOrBlank()}")
                Result.success(
                    GoogleAuthTokens(
                        idToken = idToken,
                        authorizationCode = authorizationCode.orEmpty()
                    )
                )
            }
        } catch (ex: Exception) {
            logExtractionFailure(ex)
            Result.failure(ex)
        }
    }

    private fun logExtractionFailure(ex: Exception) {
        if (ex is ApiException) {
            logError(
                "auth_google_extract_failed reason=api_exception statusCode=${ex.statusCode}",
                ex
            )
            return
        }

        logError(
            "auth_google_extract_failed reason=unexpected_exception error_type=${ex::class.java.simpleName}",
            ex
        )
    }

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    private fun logWarn(message: String) {
        runCatching { Log.w(TAG, message) }
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
        const val TAG = "GoogleSignInGateway"
    }
}
