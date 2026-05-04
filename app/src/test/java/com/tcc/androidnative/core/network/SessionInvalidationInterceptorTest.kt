package com.tcc.androidnative.core.network

import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionInvalidationInterceptorTest {
    @Test
    fun `should invalidate session only for terminal refresh errors`() {
        val interceptor = SessionInvalidationInterceptor(FakeSessionManager())

        assertFalse(interceptor.shouldInvalidateSession(401))
        assertTrue(
            interceptor.shouldInvalidateSession(
                401,
                "/api/auth/refresh",
                """{"code":"REFRESH_TOKEN_REVOKED"}"""
            )
        )
        assertFalse(interceptor.shouldInvalidateSession(403))
        assertFalse(interceptor.shouldInvalidateSession(500))
    }

    @Test
    fun `should not invalidate session for payments preload 401`() {
        val interceptor = SessionInvalidationInterceptor(FakeSessionManager())

        assertFalse(interceptor.shouldInvalidateSession(401, "/api/calendar/events/10/payments"))
        assertTrue(interceptor.isPaymentsPreloadPath("/api/calendar/events/10/payments"))
    }
}

private class FakeSessionManager : SessionManager {
    private val mutableState = MutableStateFlow<UserSession?>(null)
    override val sessionState: StateFlow<UserSession?> = mutableState

    override fun saveSession(session: UserSession) {
        mutableState.value = session
    }

    override fun clearSession() {
        mutableState.value = null
    }

    override fun currentSession(): UserSession? = mutableState.value

    override fun getIdToken(): String? = mutableState.value?.accessToken
}
