package com.tcc.androidnative.feature.auth.data

import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.UserSession
import com.tcc.androidnative.feature.auth.data.remote.AuthApi
import com.tcc.androidnative.feature.auth.data.remote.dto.LoginRequestDto
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun login(idToken: String, authorizationCode: String): UserSession
    fun logout()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    override suspend fun login(idToken: String, authorizationCode: String): UserSession {
        val response = api.login(LoginRequestDto(idToken = idToken, authorizationCode = authorizationCode))
        val session = UserSession(
            userId = response.userId,
            email = response.email,
            name = response.name,
            idToken = idToken
        )
        sessionManager.saveSession(session)
        return session
    }

    override fun logout() {
        sessionManager.clearSession()
    }
}

