package com.tcc.androidnative.feature.auth.data

import android.util.Log
import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.UserSession
import com.tcc.androidnative.feature.auth.data.remote.AuthApi
import com.tcc.androidnative.feature.auth.data.remote.dto.LoginRequestDto
import com.tcc.androidnative.feature.auth.data.remote.dto.LogoutRequestDto
import com.tcc.androidnative.feature.auth.data.remote.dto.RefreshRequestDto
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

interface AuthRepository {
    suspend fun login(idToken: String, authorizationCode: String): UserSession
    suspend fun refresh(refreshToken: String): UserSession
    suspend fun logout()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    override suspend fun login(idToken: String, authorizationCode: String): UserSession {
        logInfo("auth_login_request_start")

        return try {
            val response = api.login(
                LoginRequestDto(
                    idToken = idToken,
                    authorizationCode = authorizationCode
                )
            )
            val session = UserSession(
                userId = response.userId,
                email = response.email,
                name = response.name,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                accessTokenExpiresAtEpochSeconds = parseEpochSeconds(response.accessTokenExpiresAt),
                refreshTokenExpiresAtEpochSeconds = parseEpochSeconds(response.refreshTokenExpiresAt)
            )
            sessionManager.saveSession(session)
            logInfo("auth_login_request_success")
            session
        } catch (error: HttpException) {
            logError(
                "auth_login_request_failed failure_category=http status=${error.code()}",
                error
            )
            throw error
        } catch (error: Throwable) {
            logError(
                "auth_login_request_failed failure_category=unexpected error_type=${error::class.java.simpleName}",
                error
            )
            throw error
        }
    }

    override suspend fun refresh(refreshToken: String): UserSession {
        val current = sessionManager.currentSession()
            ?: throw IllegalStateException("No active session to refresh")
        val response = api.refresh(
            RefreshRequestDto(refreshToken = refreshToken)
        )
        val updated = current.copy(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            accessTokenExpiresAtEpochSeconds = parseEpochSeconds(response.accessTokenExpiresAt),
            refreshTokenExpiresAtEpochSeconds = parseEpochSeconds(response.refreshTokenExpiresAt)
        )
        sessionManager.saveSession(updated)
        return updated
    }

    override suspend fun logout() {
        val refreshToken = sessionManager.getRefreshToken()
        runCatching {
            if (!refreshToken.isNullOrBlank()) {
                api.logout(LogoutRequestDto(refreshToken = refreshToken))
            }
        }.onFailure { error ->
            logError(
                "auth_logout_remote_failed error_type=${error::class.java.simpleName}",
                error
            )
        }
        sessionManager.clearSession()
    }

    private fun parseEpochSeconds(value: String): Long {
        return Instant.parse(value).epochSecond
    }

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
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
        const val TAG = "AuthRepository"
    }
}
