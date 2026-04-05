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
    fun `should invalidate session only for 401`() {
        val interceptor = SessionInvalidationInterceptor(FakeSessionManager())

        assertTrue(interceptor.shouldInvalidateSession(401))
        assertFalse(interceptor.shouldInvalidateSession(403))
        assertFalse(interceptor.shouldInvalidateSession(500))
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

    override fun getIdToken(): String? = mutableState.value?.idToken
}
