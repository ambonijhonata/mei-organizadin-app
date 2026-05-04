package com.tcc.androidnative.core.session

import com.tcc.androidnative.feature.auth.data.AuthRepository
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import dagger.Lazy

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRefreshCoordinatorTest {
    @Test
    fun `should share single-flight refresh result for concurrent proactive and reactive triggers`() = runTest {
        val sessionManager = FakeSessionManager(
            UserSession(
                userId = 1L,
                email = "user@test.com",
                name = "User",
                accessToken = "old-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 30L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = CountingAuthRepository(
            sessionManager = sessionManager,
            refreshDelayMillis = 120L
        )
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })

        val first = async {
            coordinator.refreshIfNeeded(force = false, trigger = RefreshTrigger.PROACTIVE)
        }
        val second = async {
            coordinator.refreshIfNeeded(force = true, trigger = RefreshTrigger.REACTIVE_401)
        }

        val outcomes = listOf(first.await(), second.await())
        assertEquals(1, authRepository.refreshCalls)
        assertTrue(outcomes.contains(RefreshOutcome.Success(RefreshSuccessKind.REFRESHED)))
        assertTrue(outcomes.contains(RefreshOutcome.Success(RefreshSuccessKind.REUSED_IN_FLIGHT)))
    }

    @Test
    fun `should dedup immediate sequential force refresh after success`() = runTest {
        val sessionManager = FakeSessionManager(
            UserSession(
                userId = 1L,
                email = "user@test.com",
                name = "User",
                accessToken = "old-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 30L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = CountingAuthRepository(sessionManager = sessionManager)
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })

        val first = coordinator.refreshIfNeeded(force = true, trigger = RefreshTrigger.REACTIVE_401)
        val second = coordinator.refreshIfNeeded(force = true, trigger = RefreshTrigger.REACTIVE_401)

        assertEquals(1, authRepository.refreshCalls)
        assertEquals(RefreshOutcome.Success(RefreshSuccessKind.REFRESHED), first)
        assertEquals(RefreshOutcome.Success(RefreshSuccessKind.SKIPPED_RECENT_SUCCESS), second)
    }

    @Test
    fun `should classify network failure as recoverable`() = runTest {
        val sessionManager = FakeSessionManager(
            UserSession(
                userId = 1L,
                email = "user@test.com",
                name = "User",
                accessToken = "old-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) - 30L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = CountingAuthRepository(
            sessionManager = sessionManager,
            refreshFailure = IOException("timeout")
        )
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })

        val outcome = coordinator.refreshIfNeeded(force = true, trigger = RefreshTrigger.REACTIVE_401)

        assertTrue(outcome is RefreshOutcome.RecoverableFailure)
    }

    @Test
    fun `should retry recoverable refresh failures with backoff and eventually succeed`() = runTest {
        val sessionManager = FakeSessionManager(
            UserSession(
                userId = 1L,
                email = "user@test.com",
                name = "User",
                accessToken = "old-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) - 30L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = CountingAuthRepository(
            sessionManager = sessionManager,
            refreshFailuresBeforeSuccess = 2
        )
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })

        val outcome = coordinator.refreshIfNeeded(force = true, trigger = RefreshTrigger.REACTIVE_401)

        assertEquals(3, authRepository.refreshCalls)
        assertEquals(RefreshOutcome.Success(RefreshSuccessKind.REFRESHED), outcome)
    }

    @Test
    fun `should dedup immediate retries after recoverable failure`() = runTest {
        val sessionManager = FakeSessionManager(
            UserSession(
                userId = 1L,
                email = "user@test.com",
                name = "User",
                accessToken = "old-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) - 30L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = CountingAuthRepository(
            sessionManager = sessionManager,
            refreshFailure = IOException("timeout")
        )
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })

        val first = coordinator.refreshIfNeeded(force = true, trigger = RefreshTrigger.REACTIVE_401)
        val second = coordinator.refreshIfNeeded(force = true, trigger = RefreshTrigger.REACTIVE_401)

        assertTrue(first is RefreshOutcome.RecoverableFailure)
        assertTrue(second is RefreshOutcome.RecoverableFailure)
        assertEquals(3, authRepository.refreshCalls)
    }
}

private class CountingAuthRepository(
    private val sessionManager: FakeSessionManager,
    private val refreshDelayMillis: Long = 0L,
    private val refreshFailure: Throwable? = null,
    private val refreshFailuresBeforeSuccess: Int = 0
) : AuthRepository {
    var refreshCalls: Int = 0
        private set

    override suspend fun login(idToken: String, authorizationCode: String): UserSession {
        throw UnsupportedOperationException("Not needed in this test")
    }

    override suspend fun refresh(refreshToken: String): UserSession {
        refreshCalls += 1
        if (refreshDelayMillis > 0L) {
            delay(refreshDelayMillis)
        }
        if (refreshCalls <= refreshFailuresBeforeSuccess) {
            throw IOException("transient-$refreshCalls")
        }
        refreshFailure?.let { throw it }
        val current = sessionManager.currentSession() ?: error("Session missing")
        val updated = current.copy(
            accessToken = "new-access-$refreshCalls",
            accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
        )
        sessionManager.saveSession(updated)
        return updated
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }
}

private class FakeSessionManager(initialSession: UserSession?) : SessionManager {
    private val state = MutableStateFlow(initialSession)
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
