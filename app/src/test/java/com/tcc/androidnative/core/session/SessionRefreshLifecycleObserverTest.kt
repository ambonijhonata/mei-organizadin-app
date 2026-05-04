package com.tcc.androidnative.core.session

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.tcc.androidnative.feature.auth.data.AuthRepository
import dagger.Lazy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRefreshLifecycleObserverTest {
    @Test
    fun `onStart should trigger silent refresh for near-expiry session`() {
        val sessionManager = LifecycleFakeSessionManager(
            UserSession(
                userId = 1L,
                email = "user@test.com",
                name = "User",
                accessToken = "old-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 20L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = LifecycleCountingAuthRepository(sessionManager)
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })
        val observer = SessionRefreshLifecycleObserver(coordinator)
        val owner = TestOwner()

        observer.onStart(owner)
        Thread.sleep(120)

        assertTrue(authRepository.refreshCalls > 0)
    }
}

private class LifecycleCountingAuthRepository(
    private val sessionManager: LifecycleFakeSessionManager
) : AuthRepository {
    @Volatile
    var refreshCalls: Int = 0

    override suspend fun login(idToken: String, authorizationCode: String): UserSession {
        throw UnsupportedOperationException("Not needed")
    }

    override suspend fun refresh(refreshToken: String): UserSession {
        refreshCalls += 1
        val current = sessionManager.currentSession() ?: error("Missing session")
        val updated = current.copy(
            accessToken = "new-access",
            accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
        )
        sessionManager.saveSession(updated)
        return updated
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }
}

private class LifecycleFakeSessionManager(initial: UserSession?) : SessionManager {
    private val state = MutableStateFlow(initial)
    override val sessionState: StateFlow<UserSession?> = state

    override fun saveSession(session: UserSession) {
        state.value = session
    }

    override fun clearSession() {
        state.value = null
    }

    override fun currentSession(): UserSession? = state.value

    override fun getIdToken(): String? = state.value?.accessToken
}

private class TestOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry
}
