package com.tcc.androidnative.feature.auth.data.google

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
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
        if (GoogleAuthConfig.webClientId.isBlank()) {
            return Result.failure(IllegalStateException("GOOGLE_WEB_CLIENT_ID nao configurado."))
        }
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            val idToken = account.idToken
            val authorizationCode = account.serverAuthCode
            if (idToken.isNullOrBlank()) {
                Result.failure(IllegalStateException("Google nao retornou idToken."))
            } else {
                Result.success(
                    GoogleAuthTokens(
                        idToken = idToken,
                        authorizationCode = authorizationCode.orEmpty()
                    )
                )
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }
}
