package com.tcc.androidnative.core.network

import com.tcc.androidnative.core.session.RefreshOutcome
import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.SessionRefreshCoordinator
import com.tcc.androidnative.core.session.UserSession
import com.tcc.androidnative.feature.auth.data.AuthRepository
import dagger.Lazy
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRefreshInterceptorTest {
    @Test
    fun `should retry request once after successful refresh`() {
        val sessionManager = InterceptorSessionManager(
            UserSession(
                userId = 1L,
                email = "u@test.com",
                name = "User",
                accessToken = "old-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) - 10L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = object : AuthRepository {
            override suspend fun login(idToken: String, authorizationCode: String): UserSession {
                throw UnsupportedOperationException("Not needed in this test")
            }

            override suspend fun refresh(refreshToken: String): UserSession {
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
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })
        val interceptor = SessionRefreshInterceptor(coordinator, sessionManager)

        val request = Request.Builder()
            .url("https://example.com/api/calendar/events")
            .header("Authorization", "Bearer old-access")
            .build()
        val chain = RefreshAwareChain(request)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.calls)
    }

    @Test
    fun `should retry payments request after successful refresh`() {
        val sessionManager = InterceptorSessionManager(
            UserSession(
                userId = 1L,
                email = "u@test.com",
                name = "User",
                accessToken = "old-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) - 10L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = object : AuthRepository {
            override suspend fun login(idToken: String, authorizationCode: String): UserSession {
                throw UnsupportedOperationException("Not needed in this test")
            }

            override suspend fun refresh(refreshToken: String): UserSession {
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
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })
        val interceptor = SessionRefreshInterceptor(coordinator, sessionManager)

        val request = Request.Builder()
            .url("https://example.com/api/calendar/events/10/payments")
            .header("Authorization", "Bearer old-access")
            .build()
        val chain = RefreshAwareChain(request)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.calls)
    }

    @Test
    fun `should retry with latest token without refresh when request carries stale token`() {
        val sessionManager = InterceptorSessionManager(
            UserSession(
                userId = 1L,
                email = "u@test.com",
                name = "User",
                accessToken = "new-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = CountingInterceptorAuthRepository(sessionManager)
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })
        val interceptor = SessionRefreshInterceptor(coordinator, sessionManager)

        val request = Request.Builder()
            .url("https://example.com/api/calendar/events")
            .header("Authorization", "Bearer old-access")
            .build()
        val chain = RefreshAwareChain(request)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.calls)
        assertEquals(0, authRepository.refreshCalls)
    }

    @Test
    fun `should keep original 401 when refresh fails recoverably`() {
        val sessionManager = InterceptorSessionManager(
            UserSession(
                userId = 1L,
                email = "u@test.com",
                name = "User",
                accessToken = "old-access",
                refreshToken = "refresh-token",
                accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) - 10L,
                refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L
            )
        )
        val authRepository = CountingInterceptorAuthRepository(
            sessionManager = sessionManager,
            refreshFailure = IOException("timeout")
        )
        val coordinator = SessionRefreshCoordinator(sessionManager, Lazy { authRepository })
        val interceptor = SessionRefreshInterceptor(coordinator, sessionManager)

        val request = Request.Builder()
            .url("https://example.com/api/calendar/events")
            .header("Authorization", "Bearer old-access")
            .build()
        val chain = RefreshAwareChain(request)

        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        assertEquals(1, chain.calls)
        assertTrue(authRepository.refreshCalls >= 1)
    }
}

private class RefreshAwareChain(private val originalRequest: Request) : Interceptor.Chain {
    var calls: Int = 0
        private set
    private val fakeCall = SessionRefreshTestCall(originalRequest)

    override fun request(): Request = originalRequest

    override fun proceed(request: Request): Response {
        calls += 1
        val token = request.header("Authorization")
        return if (token == "Bearer old-access") {
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body("{}".toResponseBody())
                .build()
        } else {
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody())
                .build()
        }
    }

    override fun call(): Call = fakeCall

    override fun connection(): Connection? = null

    override fun connectTimeoutMillis(): Int = TimeUnit.SECONDS.toMillis(30).toInt()

    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun readTimeoutMillis(): Int = TimeUnit.SECONDS.toMillis(45).toInt()

    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun writeTimeoutMillis(): Int = TimeUnit.SECONDS.toMillis(30).toInt()

    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
}

private class InterceptorSessionManager(initial: UserSession?) : SessionManager {
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

private class CountingInterceptorAuthRepository(
    private val sessionManager: InterceptorSessionManager,
    private val refreshFailure: Throwable? = null
) : AuthRepository {
    var refreshCalls: Int = 0
        private set

    override suspend fun login(idToken: String, authorizationCode: String): UserSession {
        throw UnsupportedOperationException("Not needed in this test")
    }

    override suspend fun refresh(refreshToken: String): UserSession {
        refreshCalls += 1
        refreshFailure?.let { throw it }
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

private class SessionRefreshTestCall(
    private val request: Request
) : Call {
    override fun request(): Request = request
    override fun execute(): Response = throw UnsupportedOperationException("Not needed in this test")
    override fun enqueue(responseCallback: Callback) = throw UnsupportedOperationException("Not needed in this test")
    override fun cancel() = Unit
    override fun isExecuted(): Boolean = false
    override fun isCanceled(): Boolean = false
    override fun timeout(): Timeout = Timeout.NONE
    override fun clone(): Call = SessionRefreshTestCall(request)
}
